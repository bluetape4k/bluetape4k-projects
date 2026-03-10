package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.random.Random

class RedissonSuspendLeaderGroupElectionTest: AbstractRedissonTest() {

    companion object: KLoggingChannel()

    private val maxLeaders = 3
    private val options = LeaderGroupElectionOptions(
        maxLeaders = maxLeaders,
        waitTime = Duration.ofSeconds(30),
        leaseTime = Duration.ofSeconds(60),
    )
    private val election by lazy { RedissonSuspendLeaderGroupElection(redissonClient, options) }

    // ── 기본 동작 ──────────────────────────────────────────────────────────

    @Test
    fun `runIfLeader - 리더로 선출되어 suspend action 을 실행하고 결과를 반환한다`() = runSuspendIO {
        val result = election.runIfLeader(randomName()) { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적인 슬롯 풀을 가진다`() = runSuspendIO {
        val result1 = election.runIfLeader(randomName()) { "a" }
        val result2 = election.runIfLeader(randomName()) { "b" }

        result1 shouldBeEqualTo "a"
        result2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() = runSuspendIO {
        val result = runCatching {
            election.runIfLeader(randomName()) { throw RuntimeException("테스트 예외") }
        }
        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `runIfLeader - action 예외 발생 후에도 슬롯이 반환되어 다음 호출이 성공한다`() = runSuspendIO {
        val lockName = randomName()
        runCatching { election.runIfLeader(lockName) { throw RuntimeException("실패") } }

        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    // ── 동시 실행 제한 ────────────────────────────────────────────────────

    @Test
    fun `동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() = runSuspendIO {
        val lockName = randomName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)
        val numWorkers = maxLeaders * 4

        SuspendedJobTester()
            .workers(numWorkers)
            .rounds(numWorkers * 2)
            .add {
                election.runIfLeader(lockName) {
                    val current = currentConcurrent.incrementAndGet()
                    peakConcurrent.updateAndGet { max(it, current) }
                    delay(Random.nextLong(5, 15))
                    currentConcurrent.decrementAndGet()
                }
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=$maxLeaders" }
        peakConcurrent.get() shouldBeLessOrEqualTo maxLeaders
    }

    @Test
    fun `maxLeaders 개 슬롯이 모두 사용 중일 때 상태가 isFull=true 이다`() = runSuspendIO {
        val lockName = randomName()

        coroutineScope {
            val holdSignal = CompletableDeferred<Unit>()
            val startedCount = AtomicInteger(0)

            val jobs = (1..maxLeaders).map {
                async {
                    election.runIfLeader(lockName) {
                        startedCount.incrementAndGet()
                        holdSignal.await()
                    }
                }
            }

            // 모든 슬롯이 점유될 때까지 대기
            while (startedCount.get() < maxLeaders) {
                delay(5)
            }

            election.state(lockName).isFull.shouldBeTrue()
            election.activeCount(lockName) shouldBeEqualTo maxLeaders
            election.availableSlots(lockName) shouldBeEqualTo 0

            holdSignal.complete(Unit)
            jobs.awaitAll()
        }

        // 완료 후 초기 상태 복귀
        election.activeCount(lockName) shouldBeEqualTo 0
        election.availableSlots(lockName) shouldBeEqualTo maxLeaders
    }

    // ── 상태 정보 ────────────────────────────────────────────────────────

    @Test
    fun `state - 초기 상태는 activeCount=0, isFull=false, isEmpty=true 이다`() = runSuspendIO {
        val lockName = randomName()
        val state = election.state(lockName)

        state.lockName shouldBeEqualTo lockName
        state.maxLeaders shouldBeEqualTo maxLeaders
        state.activeCount shouldBeEqualTo 0
        state.availableSlots shouldBeEqualTo maxLeaders
        state.isEmpty.shouldBeTrue()
        state.isFull.shouldBeFalse()
    }

    // ── 스트레스 테스트 ────────────────────────────────────────────────────

    @Test
    fun `코루틴 스트레스 - 모든 실행이 완료되고 카운터가 정확하다`() = runSuspendIO {
        val lockName = randomName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numWorkers = 8
        val roundsPerJob = 4

        SuspendedJobTester()
            .workers(numWorkers)
            .rounds(numWorkers * roundsPerJob)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "suspend 작업 1. task1=${task1.get()}" }
                    delay(Random.nextLong(1, 5))
                    task1.incrementAndGet()
                }
            }
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "suspend 작업 2. task2=${task2.get()}" }
                    delay(Random.nextLong(1, 5))
                    task2.incrementAndGet()
                }
            }
            .run()

        task1.get() shouldBeEqualTo numWorkers * roundsPerJob
        task2.get() shouldBeEqualTo numWorkers * roundsPerJob
    }
}
