package io.bluetape4k.leader.local

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.leader.LeaderGroupElection
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requirePositiveNumber
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * [AbstractLocalLeaderGroupElection]을 상속한 로컬(단일 JVM) 복수 리더 선출 구현체입니다.
 *
 * ## 동작
 * - 동기 [runIfLeader]와 비동기 [runAsyncIfLeader] 모두 지원합니다.
 * - 슬롯 관리(Semaphore 풀, 상태 조회)는 [AbstractLocalLeaderGroupElection]에서 처리합니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 동시 실행 제한에 적합합니다.
 *
 * ```kotlin
 * val election = LocalLeaderGroupElection(maxLeaders = 3)
 *
 * // 동기 실행 (최대 3개 스레드 동시)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 *
 * // 비동기 실행
 * val future = election.runAsyncIfLeader("batch-job") { CompletableFuture.completedFuture(42) }
 * ```
 *
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 */
class LocalLeaderGroupElection private constructor(options: LeaderGroupElectionOptions):
    AbstractLocalLeaderGroupElection(options), LeaderGroupElection {

    companion object: KLogging() {

        /**
         * [LeaderGroupElectionOptions]을 이용해 [LocalLeaderGroupElection] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val election = LocalLeaderGroupElection(LeaderGroupElectionOptions(maxLeaders = 3))
         * val result = election.runIfLeader("batch-job") { "done" }
         * // result == "done"
         * ```
         *
         * @param options 리더 그룹 선출 옵션. 기본값은 [LeaderGroupElectionOptions.Default]
         * @return [LeaderGroupElection] 구현체 인스턴스
         */
        operator fun invoke(options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default): LeaderGroupElection =
            options
                .also { it.maxLeaders.requirePositiveNumber("maxLeaders") }
                .let(::LocalLeaderGroupElection)
    }

    /**
     * [lockName]의 슬롯을 획득하고 [action]을 동기로 실행합니다.
     *
     * ```kotlin
     * val election = LocalLeaderGroupElection(LeaderGroupElectionOptions(maxLeaders = 3))
     * val result = election.runIfLeader("batch-job") { "done" }
     * // result == "done"
     * ```
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 슬롯 획득 성공 시 실행할 동기 작업
     * @return [action] 실행 결과
     */
    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        return withPermit(lockName, action)
    }

    /**
     * [lockName]의 슬롯을 [executor]에서 획득하고 비동기 [action]을 실행합니다.
     *
     * ```kotlin
     * val election = LocalLeaderGroupElection(LeaderGroupElectionOptions(maxLeaders = 3))
     * val result = election.runAsyncIfLeader("batch-job") {
     *     CompletableFuture.completedFuture(42)
     * }.join()
     * // result == 42
     * ```
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param executor 비동기 실행에 사용할 [Executor]. 기본값은 [VirtualThreadExecutor]
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
