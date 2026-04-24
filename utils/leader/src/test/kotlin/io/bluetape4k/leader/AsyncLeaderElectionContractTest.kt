package io.bluetape4k.leader

import io.bluetape4k.leader.local.LocalAsyncLeaderElection
import io.bluetape4k.leader.local.LocalLeaderElection
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors

/**
 * [AsyncLeaderElection] 인터페이스 계약을 검증하는 테스트입니다.
 *
 * [LocalAsyncLeaderElection]과 [LocalLeaderElection] 구현체를 통해
 * [AsyncLeaderElection.runAsyncIfLeader] 계약을 검증합니다.
 */
class AsyncLeaderElectionContractTest {

    companion object: KLogging()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    // ── LocalAsyncLeaderElection 을 통한 AsyncLeaderElection 계약 검증 ──

    @Test
    fun `runAsyncIfLeader - 리더 획득 성공 시 CompletableFuture action 을 실행한다`() {
        val election: AsyncLeaderElection = LocalAsyncLeaderElection()
        val result = election.runAsyncIfLeader(randomLockName()) {
            CompletableFuture.completedFuture("contract-ok")
        }.join()
        result shouldBeEqualTo "contract-ok"
    }

    @Test
    fun `runAsyncIfLeader - 서로 다른 lockName 은 독립적으로 실행된다`() {
        val election: AsyncLeaderElection = LocalAsyncLeaderElection()
        val f1 = election.runAsyncIfLeader(randomLockName()) { CompletableFuture.completedFuture(1) }
        val f2 = election.runAsyncIfLeader(randomLockName()) { CompletableFuture.completedFuture(2) }

        f1.join() shouldBeEqualTo 1
        f2.join() shouldBeEqualTo 2
    }

    @Test
    fun `runAsyncIfLeader - action future 실패 시 CompletionException 이 전파된다`() {
        val election: AsyncLeaderElection = LocalAsyncLeaderElection()
        assertThrows<CompletionException> {
            election.runAsyncIfLeader(randomLockName()) {
                CompletableFuture.failedFuture<String>(RuntimeException("계약 위반 예외"))
            }.join()
        }
    }

    @Test
    fun `runAsyncIfLeader - action 실패 후에도 락이 해제되어 다음 호출이 성공한다`() {
        val election: AsyncLeaderElection = LocalAsyncLeaderElection()
        val lockName = randomLockName()
        runCatching {
            election.runAsyncIfLeader(lockName) {
                CompletableFuture.failedFuture<Unit>(RuntimeException("실패"))
            }.join()
        }

        val result = election.runAsyncIfLeader(lockName) {
            CompletableFuture.completedFuture("복구")
        }.join()
        result shouldBeEqualTo "복구"
    }

    @Test
    fun `runAsyncIfLeader - 커스텀 executor 를 사용할 수 있다`() {
        val election: AsyncLeaderElection = LocalAsyncLeaderElection()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = election.runAsyncIfLeader(randomLockName(), executor) {
                CompletableFuture.completedFuture("custom-executor-ok")
            }.join()
            result shouldBeEqualTo "custom-executor-ok"
        } finally {
            executor.shutdown()
        }
    }

    // ── LocalLeaderElection 을 통한 AsyncLeaderElection 계약 검증 ──

    @Test
    fun `runAsyncIfLeader - LocalLeaderElection 도 AsyncLeaderElection 계약을 준수한다`() {
        val election: AsyncLeaderElection = LocalLeaderElection()
        val result = election.runAsyncIfLeader(randomLockName()) {
            CompletableFuture.completedFuture(99)
        }.join()
        result shouldBeEqualTo 99
    }

    @Test
    fun `runAsyncIfLeader - isFailure 가 action 실패를 정확히 반영한다`() {
        val election: AsyncLeaderElection = LocalAsyncLeaderElection()
        val outcome = runCatching {
            election.runAsyncIfLeader(randomLockName()) {
                CompletableFuture.failedFuture<Int>(IllegalArgumentException("invalid"))
            }.join()
        }
        outcome.isFailure.shouldBeTrue()
    }
}
