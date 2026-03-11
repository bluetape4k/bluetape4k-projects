package io.bluetape4k.leader.local

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.leader.AsyncLeaderGroupElection
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requirePositiveNumber
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * [AbstractLocalLeaderGroupElection]을 상속한 로컬(단일 JVM) 복수 리더 비동기 전용 선출 구현체입니다.
 *
 * ## 동작
 * - 비동기 [runAsyncIfLeader]만 지원합니다. 동기 실행이 필요하면 [LocalLeaderGroupElection]을 사용합니다.
 * - 슬롯 관리(Semaphore 풀, 상태 조회)는 [AbstractLocalLeaderGroupElection]에서 처리합니다.
 * - 기본 [Executor]는 [VirtualThreadExecutor] 싱글턴입니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 동시 실행 제한에 적합합니다.
 *
 * ```kotlin
 * val election = LocalAsyncLeaderGroupElection(maxLeaders = 3)
 *
 * // 최대 3개 Virtual Thread 가 동시에 실행
 * val result = election.runAsyncIfLeader("batch-job") {
 *     CompletableFuture.completedFuture(processChunk())
 * }.join()
 * ```
 *
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 */
class LocalAsyncLeaderGroupElection private constructor(
    options: LeaderGroupElectionOptions,
): AbstractLocalLeaderGroupElection(options), AsyncLeaderGroupElection {

    companion object: KLogging() {
        operator fun invoke(
            options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
        ): AsyncLeaderGroupElection =
            options
                .also { it.maxLeaders.requirePositiveNumber("maxLeaders") }
                .let(::LocalAsyncLeaderGroupElection)
    }

    /**
     * [lockName]의 슬롯을 [executor]에서 획득하고 비동기 [action]을 실행합니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param executor 비동기 실행에 사용할 [Executor]
     * @param action 슬롯 획득 성공 시 실행할 비동기 작업
     * @return [action] 실행 결과를 담은 [CompletableFuture]
     */
    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> =
        CompletableFuture.supplyAsync(
            { withPermit(lockName) { action().join() } },
            executor
        )
}
