package io.bluetape4k.leader.coroutines

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.random.Random

class LocalSuspendLeaderGroupElectionTest {

    companion object : KLoggingChannel()

    private val maxLeaders = 3
    private val election = LocalSuspendLeaderGroupElection(maxLeaders)

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    // ── 기본 동작 ──────────────────────────────────────────────────────────

    @Test
    fun `runIfLeader - 리더로 선출되어 suspend action 을 실행하고 결과를 반환한다`() = runSuspendIO {
        val result = election.runIfLeader(randomLockName()) { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적인 슬롯 풀을 가진다`() = runSuspendIO {
        val result1 = election.runIfLeader(randomLockName()) { "a" }
        val result2 = election.runIfLeader(randomLockName()) { "b" }

        result1 shouldBeEqualTo "a"
        result2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() = runSuspendIO {
        val result = runCatching {
            election.runIfLeader(randomLockName()) { throw RuntimeException("테스트 예외") }
        }
        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `runIfLeader - action 예외 발생 후에도 슬롯이 반환되어 다음 호출이 성공한다`() = runSuspendIO {
        val lockName = randomLockName()
        runCatching { election.runIfLeader(lockName) { throw RuntimeException("실패") } }

        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `maxLeaders=1 이면 SuspendLeaderElection 과 동일하게 직렬 실행된다`() = runSuspendIO {
        val singleElection = LocalSuspendLeaderGroupElection(maxLeaders = 1)
        val lockName = randomLockName()
        val counter = AtomicInteger(0)
        val numWorkers = 6

        SuspendedJobTester()
            .workers(numWorkers)
            .rounds(numWorkers)
            .add { singleElection.runIfLeader(lockName) { counter.incrementAndGet() } }
            .run()

        counter.get() shouldBeEqualTo numWorkers  // rounds(numWorkers) × 1 block = numWorkers 회 실행
    }

    // ── 동시 실행 제한 ────────────────────────────────────────────────────

    @Test
    fun `동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() = runSuspendIO {
        val lockName = randomLockName()
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
        val lockName = randomLockName()

        // maxLeaders 개 코루틴이 동시에 슬롯 점유 후 상태 확인
        coroutineScope {
            val holdSignal = kotlinx.coroutines.CompletableDeferred<Unit>()
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
        val lockName = randomLockName()
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
        val lockName = randomLockName()
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
