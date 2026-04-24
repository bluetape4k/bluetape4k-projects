package io.bluetape4k.leader.coroutines

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.leader.LeaderGroupElectionOptions
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
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * [SuspendLeaderGroupElection] 인터페이스 계약을 검증하는 테스트입니다.
 *
 * [LocalSuspendLeaderGroupElection] 구현체를 [SuspendLeaderGroupElection] 타입으로 참조하여
 * 인터페이스 계약([runIfLeader], [maxLeaders], [activeCount], [availableSlots], [state])을 검증합니다.
 */
class SuspendLeaderGroupElectionContractTest {

    companion object: KLoggingChannel()

    private val maxLeaders = 3
    private val options = LeaderGroupElectionOptions(maxLeaders)
    private val election: SuspendLeaderGroupElection = LocalSuspendLeaderGroupElection(options)

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    // ── 기본 동작 ──────────────────────────────────────────────────────────

    @Test
    fun `runIfLeader - 리더 획득 성공 시 suspend action 을 실행하고 결과를 반환한다`() = runSuspendIO {
        val result = election.runIfLeader(randomLockName()) { "group-suspend-ok" }
        result shouldBeEqualTo "group-suspend-ok"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적인 슬롯 풀을 가진다`() = runSuspendIO {
        val r1 = election.runIfLeader(randomLockName()) { "a" }
        val r2 = election.runIfLeader(randomLockName()) { "b" }
        r1 shouldBeEqualTo "a"
        r2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() = runSuspendIO {
        val result = runCatching {
            election.runIfLeader(randomLockName()) { throw RuntimeException("그룹 계약 예외") }
        }
        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `runIfLeader - action 예외 후에도 슬롯이 반환되어 다음 호출이 성공한다`() = runSuspendIO {
        val lockName = randomLockName()
        runCatching { election.runIfLeader(lockName) { throw RuntimeException("실패") } }

        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    // ── 상태 조회 (LeaderGroupElectionState 상속) ─────────────────────────

    @Test
    fun `maxLeaders - 인터페이스를 통해 maxLeaders 값을 조회할 수 있다`() = runSuspendIO {
        election.maxLeaders shouldBeEqualTo maxLeaders
    }

    @Test
    fun `state - 초기 상태는 isEmpty=true, isFull=false 이다`() = runSuspendIO {
        val lockName = randomLockName()
        val state = election.state(lockName)

        state.lockName shouldBeEqualTo lockName
        state.maxLeaders shouldBeEqualTo maxLeaders
        state.activeCount shouldBeEqualTo 0
        state.isEmpty.shouldBeTrue()
        state.isFull.shouldBeFalse()
    }

    @Test
    fun `activeCount 와 availableSlots - 슬롯 점유 중 정확히 반영된다`() = runSuspendIO {
        val lockName = randomLockName()

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

            while (startedCount.get() < maxLeaders) {
                delay(5.milliseconds)
            }

            election.activeCount(lockName) shouldBeEqualTo maxLeaders
            election.availableSlots(lockName) shouldBeEqualTo 0
            election.state(lockName).isFull.shouldBeTrue()

            holdSignal.complete(Unit)
            jobs.awaitAll()
        }

        election.activeCount(lockName) shouldBeEqualTo 0
        election.availableSlots(lockName) shouldBeEqualTo maxLeaders
    }

    // ── 동시 실행 제한 ────────────────────────────────────────────────────

    @Test
    fun `동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() = runSuspendIO {
        val lockName = randomLockName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)

        SuspendedJobTester()
            .workers(maxLeaders * 4)
            .rounds(maxLeaders * 4 * 2)
            .add {
                election.runIfLeader(lockName) {
                    val current = currentConcurrent.incrementAndGet()
                    peakConcurrent.updateAndGet { max(it, current) }
                    delay(Random.nextLong(5, 15).milliseconds)
                    currentConcurrent.decrementAndGet()
                }
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=$maxLeaders" }
        peakConcurrent.get() shouldBeLessOrEqualTo maxLeaders
    }
}
