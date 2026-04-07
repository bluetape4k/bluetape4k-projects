package io.bluetape4k.leader.coroutines

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class LocalSuspendLeaderElectionTest {

    companion object: KLoggingChannel()

    private val election = LocalSuspendLeaderElection()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    @Test
    fun `runIfLeader - 리더로 선출되어 suspend action 을 실행하고 결과를 반환한다`() = runSuspendIO {
        val result = election.runIfLeader(randomLockName()) { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적으로 실행된다`() = runSuspendIO {
        val result1 = election.runIfLeader(randomLockName()) { "a" }
        val result2 = election.runIfLeader(randomLockName()) { "b" }

        result1 shouldBeEqualTo "a"
        result2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() = runSuspendIO {
        val result = runCatching {
            election.runIfLeader(randomLockName()) {
                throw RuntimeException("테스트 예외")
            }
        }
        result.isFailure.shouldBeTrue()
        (result.exceptionOrNull() is RuntimeException).shouldBeTrue()
    }

    @Test
    fun `runIfLeader - action 예외 후에도 Mutex 가 해제되어 다음 호출이 성공한다`() = runSuspendIO {
        val lockName = randomLockName()
        runCatching {
            election.runIfLeader(lockName) { throw RuntimeException("실패") }
        }

        // Mutex 가 해제된 상태여야 다음 호출이 정상 실행된다
        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `runIfLeader - delay 를 포함한 suspend action 이 정상 실행된다`() = runSuspendIO {
        val result = election.runIfLeader(randomLockName()) {
            delay(10)
            "delay 완료"
        }
        result shouldBeEqualTo "delay 완료"
    }

    @Test
    fun `runIfLeader - 여러 코루틴 동시 실행 시 직렬 처리를 보장한다`() = runSuspendIO {
        val lockName = randomLockName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numWorkers = 8
        val roundsPerJob = 4

        // SuspendedJobTester: rounds * numBlocks 개의 Job 을 생성
        // block 당 rounds 번 실행 → task1 = task2 = numWorkers * roundsPerJob = 32
        SuspendedJobTester()
            .workers(numWorkers)
            .rounds(numWorkers * roundsPerJob)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "suspend 작업 1 실행. task1=${task1.get()}" }
                    delay(Random.nextLong(1, 5))
                    task1.incrementAndGet()
                }
            }
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "suspend 작업 2 실행. task2=${task2.get()}" }
                    delay(Random.nextLong(1, 5))
                    task2.incrementAndGet()
                }
            }
            .run()

        task1.get() shouldBeEqualTo numWorkers * roundsPerJob
        task2.get() shouldBeEqualTo numWorkers * roundsPerJob
    }
}
