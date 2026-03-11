package io.bluetape4k.leader.local

import io.bluetape4k.leader.AsyncLeaderElection
import io.bluetape4k.leader.LeaderElectionOptions
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock

/**
 * [ReentrantLock]을 이용한 로컬(단일 JVM) 비동기 리더 선출 구현체입니다.
 *
 * ## 동작
 * - [AsyncLeaderElection]만 구현하며, 동기 [runIfLeader]가 불필요한 경우 사용합니다.
 * - 동일 [lockName]에 대해 [CompletableFuture] 기반 비동기 작업을 직렬로 실행합니다.
 * - 락을 획득한 [Executor] 스레드가 [action]이 반환한 [CompletableFuture]가 완료될 때까지 락을 보유합니다.
 * - [action] 실행 중 예외 또는 future 실패가 발생해도 락은 반드시 해제됩니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 비동기 실행 직렬화에 적합합니다.
 *
 * ```kotlin
 * val election = LocalAsyncLeaderElection()
 * val result = election.runAsyncIfLeader("job-lock") {
 *     CompletableFuture.completedFuture("done")
 * }.join()
 * // result == "done"
 * ```
 */
class LocalAsyncLeaderElection(
    options: LeaderElectionOptions = LeaderElectionOptions.Default,
): AbstractLocalLeaderElection(options), AsyncLeaderElection {

    /**
     * [lockName]에 대한 [ReentrantLock]을 획득하고 [action]을 [executor]에서 비동기로 실행합니다.
     *
     * - [action]이 반환하는 [CompletableFuture]가 완료될 때까지 락을 보유합니다.
     * - [action] 실패(예외 또는 future 실패) 시에도 락을 안전하게 해제합니다.
     * - 다른 스레드가 동일 [lockName]의 락을 보유 중이면 [executor] 스레드가 블로킹됩니다.
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param executor 비동기 실행에 사용할 [Executor]
     * @param action 리더 획득 성공 시 실행할 비동기 작업
     * @return [action] 실행 결과를 담은 [CompletableFuture]
     */
    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> =
        CompletableFuture.supplyAsync(
            { withLeaderLock(lockName) { action().join() } },
            executor
        )
}
