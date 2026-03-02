package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.random.Random

class RedissonLeaderGroupElectionTest: AbstractRedissonTest() {

    companion object: KLogging()

    private val maxLeaders = 3
    private val options = RedissonLeaderElectionOptions(
        waitTime = Duration.ofSeconds(30),
        leaseTime = Duration.ofSeconds(60),
    )
    private val election by lazy { RedissonLeaderGroupElection(redissonClient, maxLeaders, options) }

    // ── 기본 동작 ──────────────────────────────────────────────────────────

    @Test
    fun `runIfLeader - 리더로 선출되어 action 을 실행하고 결과를 반환한다`() {
        val result = election.runIfLeader(randomName()) { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적인 슬롯 풀을 가진다`() {
        val result1 = election.runIfLeader(randomName()) { "a" }
        val result2 = election.runIfLeader(randomName()) { "b" }

        result1 shouldBeEqualTo "a"
        result2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() {
        assertThrows<RuntimeException> {
            election.runIfLeader(randomName()) { throw RuntimeException("테스트 예외") }
        }
    }

    @Test
    fun `runIfLeader - action 예외 발생 후에도 슬롯이 반환되어 다음 호출이 성공한다`() {
        val lockName = randomName()
        runCatching { election.runIfLeader(lockName) { throw RuntimeException("실패") } }

        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `maxLeaders=1 이면 LeaderElection 과 동일하게 직렬 실행된다`() {
        val singleElection = RedissonLeaderGroupElection(redissonClient, maxLeaders = 1, options)
        val lockName = randomName()
        val counter = AtomicInteger(0)
        val numThreads = 6

        MultithreadingTester()
            .workers(numThreads)
            .rounds(2)
            .add { singleElection.runIfLeader(lockName) { counter.incrementAndGet() } }
            .run()

        counter.get() shouldBeEqualTo numThreads * 2
    }

    // ── 동시 실행 제한 ────────────────────────────────────────────────────

    @Test
    fun `동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() {
        val lockName = randomName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)

        MultithreadingTester()
            .workers(maxLeaders * 4)
            .rounds(2)
            .add {
                election.runIfLeader(lockName) {
                    val current = currentConcurrent.incrementAndGet()
                    peakConcurrent.updateAndGet { max(it, current) }
                    Thread.sleep(Random.nextLong(5, 15))
                    currentConcurrent.decrementAndGet()
                }
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=$maxLeaders" }
        peakConcurrent.get() shouldBeLessOrEqualTo maxLeaders
    }

    // ── 상태 정보 ────────────────────────────────────────────────────────

    @Test
    fun `state - 초기 상태는 activeCount=0, isFull=false, isEmpty=true 이다`() {
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
    fun `멀티스레드 스트레스 - 모든 실행이 완료되고 카운터가 정확하다`() {
        val lockName = randomName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 1. task1=${task1.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    task1.incrementAndGet()
                }
            }
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 2. task2=${task2.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    task2.incrementAndGet()
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }
}
