package io.bluetape4k.leader.local

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.leader.VirtualThreadLeaderElection
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [ReentrantLock]과 Virtual Thread를 이용한 로컬(단일 JVM) 리더 선출 구현체입니다.
 *
 * ## 동작
 * - [VirtualThreadLeaderElection]을 구현하며, `action`은 `() -> T` 람다로 결과를 직접 반환합니다.
 * - [VirtualFuture]를 통해 비동기로 실행되며, Virtual Thread가 락 대기 시 carrier thread를 반납합니다.
 * - [ReentrantLock]을 사용하여 동일 `lockName`에 대해 직렬 실행을 보장합니다.
 * - [ReentrantLock] 특성상 동일 스레드에서 동일 `lockName`으로 중첩 호출(재진입)이 가능합니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 비동기 실행 직렬화에 적합합니다.
 *
 * ## [LocalAsyncLeaderElection] 과의 차이
 * - `action`이 `() -> T`로 단순하며, [java.util.concurrent.CompletableFuture] 래핑이 불필요합니다.
 * - 반환이 [VirtualFuture]로 `await()` API가 명시적입니다.
 * - Virtual Thread 기반이므로 I/O 블로킹 작업에 carrier thread를 소모하지 않습니다.
 *
 * ```kotlin
 * val election = LocalVirtualThreadLeaderElection()
 * val result = election.runAsyncIfLeader("job-lock") { "done" }.await()
 * // result == "done"
 * ```
 */
class LocalVirtualThreadLeaderElection : AbstractLocalLeaderElection(), VirtualThreadLeaderElection {

    /**
     * [lockName]에 대한 [ReentrantLock]을 Virtual Thread에서 획득하고 [action]을 실행합니다.
     *
     * - Virtual Thread는 [ReentrantLock.lock] 블로킹 시 carrier thread를 반납합니다.
     * - [action] 예외 발생 시에도 락이 안전하게 해제됩니다.
     * - 동일 스레드에서 동일 `lockName`으로 중첩 호출(재진입)이 가능합니다.
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param action 리더 획득 성공 시 실행할 작업
     * @return [action] 실행 결과를 담은 [VirtualFuture]
     */
    override fun <T> runAsyncIfLeader(lockName: String, action: () -> T): VirtualFuture<T> =
        virtualFuture {
            getLock(lockName).withLock { action() }
        }
}
