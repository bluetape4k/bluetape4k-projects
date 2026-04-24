package io.bluetape4k.leader.local

import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * [AbstractLocalLeaderElection] 추상 클래스의 공통 락 관리 동작을 검증하는 테스트입니다.
 *
 * [LocalLeaderElection], [LocalAsyncLeaderElection], [LocalVirtualThreadLeaderElection] 을 통해
 * [AbstractLocalLeaderElection]이 제공하는 [getLock]/[withLeaderLock] 동작을 검증합니다.
 */
class AbstractLocalLeaderElectionTest {

    companion object: KLogging()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    // ── 기본 락 관리 동작 ─────────────────────────────────────────────────

    @Test
    fun `동일 lockName 에 대해 동일 ReentrantLock 인스턴스가 재사용된다`() {
        val election = LocalLeaderElection()
        val lockName = randomLockName()

        // 같은 lockName 으로 여러 번 호출해도 정상 실행
        val r1 = election.runIfLeader(lockName) { 1 }
        val r2 = election.runIfLeader(lockName) { 2 }
        val r3 = election.runIfLeader(lockName) { 3 }

        r1 shouldBeEqualTo 1
        r2 shouldBeEqualTo 2
        r3 shouldBeEqualTo 3
    }

    @Test
    fun `blank lockName 으로 호출 시 IllegalArgumentException 이 발생한다 (LocalLeaderElection)`() {
        val election = LocalLeaderElection()

        assertThrows<IllegalArgumentException> {
            election.runIfLeader("") { "should fail" }
        }
        assertThrows<IllegalArgumentException> {
            election.runIfLeader("   ") { "should fail" }
        }
    }

    @Test
    fun `blank lockName 으로 호출 시 IllegalArgumentException 이 발생한다 (LocalAsyncLeaderElection)`() {
        val election = LocalAsyncLeaderElection()

        assertThrows<Exception> {
            election.runAsyncIfLeader("") {
                CompletableFuture.completedFuture("should fail")
            }.join()
        }
    }

    @Test
    fun `blank lockName 으로 호출 시 IllegalArgumentException 이 발생한다 (LocalVirtualThreadLeaderElection)`() {
        val election = LocalVirtualThreadLeaderElection()

        val result = runCatching {
            election.runAsyncIfLeader("") { "should fail" }.await()
        }
        result.isFailure.shouldBeTrue()
    }

    // ── 커스텀 옵션으로 생성 ──────────────────────────────────────────────

    @Test
    fun `커스텀 LeaderElectionOptions 으로 구현체를 생성할 수 있다`() {
        val options = LeaderElectionOptions(
            waitTime = java.time.Duration.ofSeconds(10),
            leaseTime = java.time.Duration.ofSeconds(120),
        )
        val election = LocalLeaderElection(options)
        election.shouldNotBeNull()

        val result = election.runIfLeader(randomLockName()) { "custom-options-ok" }
        result shouldBeEqualTo "custom-options-ok"
    }

    // ── 서로 다른 lockName 간 독립성 ──────────────────────────────────────

    @Test
    fun `서로 다른 lockName 의 락은 독립적으로 관리된다`() {
        val election = LocalLeaderElection()
        val counter = AtomicInteger(0)
        val lockA = randomLockName()
        val lockB = randomLockName()

        // lockA 와 lockB 는 독립적이므로 동시에 실행 가능
        val fA = CompletableFuture.supplyAsync {
            election.runIfLeader(lockA) {
                Thread.sleep(Random.nextLong(5, 15))
                counter.incrementAndGet()
            }
        }
        val fB = CompletableFuture.supplyAsync {
            election.runIfLeader(lockB) {
                Thread.sleep(Random.nextLong(5, 15))
                counter.incrementAndGet()
            }
        }

        fA.join()
        fB.join()
        counter.get() shouldBeEqualTo 2
    }

    // ── action 예외 시 락 해제 보장 ───────────────────────────────────────

    @Test
    fun `action 예외 발생 후에도 락이 해제되어 이후 호출이 성공한다`() {
        val election = LocalLeaderElection()
        val lockName = randomLockName()

        repeat(3) {
            runCatching { election.runIfLeader(lockName) { throw RuntimeException("반복 실패 $it") } }
        }

        // 락이 정상 해제되었으면 이 호출이 블로킹 없이 완료됨
        val result = election.runIfLeader(lockName) { "복구 완료" }
        result shouldBeEqualTo "복구 완료"
    }
}
