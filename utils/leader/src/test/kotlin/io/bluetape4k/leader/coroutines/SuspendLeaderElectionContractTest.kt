package io.bluetape4k.leader.coroutines

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * [SuspendLeaderElection] 인터페이스 계약을 검증하는 테스트입니다.
 *
 * [LocalSuspendLeaderElection] 구현체를 [SuspendLeaderElection] 타입으로 참조하여
 * 인터페이스 계약([runIfLeader])을 검증합니다.
 */
class SuspendLeaderElectionContractTest {

    companion object: KLoggingChannel()

    private val election: SuspendLeaderElection = LocalSuspendLeaderElection()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    @Test
    fun `runIfLeader - 리더 획득 성공 시 suspend action 을 실행하고 결과를 반환한다`() = runSuspendIO {
        val result = election.runIfLeader(randomLockName()) { "suspend-contract-ok" }
        result shouldBeEqualTo "suspend-contract-ok"
    }

    @Test
    fun `runIfLeader - Unit 반환 타입도 정상 실행된다`() = runSuspendIO {
        var executed = false
        election.runIfLeader(randomLockName()) { executed = true }
        executed.shouldBeTrue()
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적으로 실행된다`() = runSuspendIO {
        val r1 = election.runIfLeader(randomLockName()) { "a" }
        val r2 = election.runIfLeader(randomLockName()) { "b" }
        r1 shouldBeEqualTo "a"
        r2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() = runSuspendIO {
        val result = runCatching {
            election.runIfLeader(randomLockName()) {
                throw IllegalStateException("계약 예외 테스트")
            }
        }
        result.isFailure.shouldBeTrue()
        (result.exceptionOrNull() is IllegalStateException).shouldBeTrue()
    }

    @Test
    fun `runIfLeader - action 예외 후에도 락이 해제되어 다음 호출이 성공한다`() = runSuspendIO {
        val lockName = randomLockName()
        runCatching { election.runIfLeader(lockName) { throw RuntimeException("실패") } }

        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `runIfLeader - delay 를 포함한 suspend action 이 정상 실행된다`() = runSuspendIO {
        val result = election.runIfLeader(randomLockName()) {
            delay(10.milliseconds)
            "delay 완료"
        }
        result shouldBeEqualTo "delay 완료"
    }

    @Test
    fun `runIfLeader - 여러 코루틴 동시 실행 시 직렬 처리를 보장한다`() = runSuspendIO {
        val lockName = randomLockName()
        val counter = AtomicInteger(0)
        val numWorkers = 8
        val roundsPerJob = 4

        SuspendedJobTester()
            .workers(numWorkers)
            .rounds(numWorkers * roundsPerJob)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "계약 테스트 suspend 작업. counter=${counter.get()}" }
                    delay(Random.nextLong(1, 5).milliseconds)
                    counter.incrementAndGet()
                }
            }
            .run()

        counter.get() shouldBeEqualTo numWorkers * roundsPerJob
    }
}
