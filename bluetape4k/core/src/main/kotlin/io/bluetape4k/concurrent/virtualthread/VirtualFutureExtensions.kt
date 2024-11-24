package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.utils.ShutdownQueue
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Virtual Thread를 사용하는 Executor의 Singleton 인스턴스
 */
object VirtualThreadExecutor: ExecutorService by Executors.newThreadPerTaskExecutor(
    Thread.ofVirtual().name("bluetape4k-vt-", 0).factory()
) {
    init {
        // Thread memory leak 방지를 위해 ShutdownQueue에 등록
        ShutdownQueue.register(this)
    }
}


/**
 * Virtual thread 를 이용하여 비동기 작업을 수행합니다.
 *
 * ```
 * val vfuture: VirtualFuture<Int> = virtualFuture {
 *  // 작업 내용
 *  Thread.sleep(1000)
 *  42
 * }
 * val result = vfuture.await()  // 42
 * ```
 *
 * @param callable 비동기로 수행할 작업
 * @return [VirtualFuture] 인스턴스
 */
@JvmName("virtualFutureForCallable")
fun <T> virtualFuture(
    executor: ExecutorService = VirtualThreadExecutor,
    callable: () -> T,
): VirtualFuture<T> {
    return VirtualFuture(executor.submit<T>(callable))
}

/**
 * 복수의 작업들을 Virtual thread 를 이용하여 비동기로 수행합니다. 결과는 [List]로 반환됩니다.
 *
 * ```
 * val tasks = listOf(
 *   { Thread.sleep(1000); 1 },
 *   { Thread.sleep(2000); 2 },
 *   { Thread.sleep(3000); 3 }
 * )
 * val future = virtualFutureAll(tasks)
 * val result = future.await() // [1, 2, 3]
 * ```
 *
 * @param T 작업 결과 타입
 * @param tasks 작업 목록
 * @return [VirtualFuture] 인스턴스
 */
fun <T> virtualFutureAll(
    tasks: Collection<() -> T>,
    executor: ExecutorService = VirtualThreadExecutor,
): VirtualFuture<List<T>> {
    val future = executor
        .invokeAll(tasks.map { Callable { it.invoke() } })
        .map { it.asCompletableFuture() }
        .sequence(executor)

    return VirtualFuture(future)
}

/**
 * 복수의 작업들을 Virtual thread 를 이용하여 비동기로 제한시간 [timeout] 동안 수행합니다. 결과는 [List]로 반환됩니다.
 *
 * ```
 * val tasks = listOf(
 *      { Thread.sleep(1000); 1 },
 *      { Thread.sleep(2000); 2 },
 *      { Thread.sleep(3000); 3 }
 * )
 * val future = virtualFutureAll(tasks, timeout = 5.seconds)
 * val result = future.await() // [1, 2, 3]
 * ```
 *
 * @param T 작업 결과 타입
 * @param tasks 작업 목록
 * @param timeout 대기 시간
 * @return [VirtualFuture] 인스턴스
 */
fun <T> virtualFutureAll(
    tasks: Collection<() -> T>,
    executor: ExecutorService = VirtualThreadExecutor,
    timeout: Duration,
): VirtualFuture<List<T>> {
    val future = executor
        .invokeAll(
            tasks.map { Callable { it.invoke() } },
            timeout.toMillis(),
            TimeUnit.MILLISECONDS
        )
        .map { it.asCompletableFuture() }
        .sequence(executor)

    return VirtualFuture(future)
}

/**
 * 모든 [VirtualFuture]의 작업이 완료될 때가지 대기한다.
 */
fun <T> Iterable<VirtualFuture<T>>.awaitAll(): List<T> {
    return map { it.asCompletableFuture() }.sequence(VirtualThreadExecutor).get()
}

/**
 * 모든 [VirtualFuture]의 작업이 제한시간[timeout] 동안 완료될 때까지 대기한다.
 */
fun <T> Iterable<VirtualFuture<T>>.awaitAll(timeout: Duration): List<T> {
    return map { it.asCompletableFuture() }.sequence(VirtualThreadExecutor)
        .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
}

/**
 * 모든 [VirtualFuture]의 작업이 완료될 때까지 대기한다.
 */
fun <T> awaitAll(vararg vfutures: VirtualFuture<T>): List<T> {
    return vfutures.toList().awaitAll()
}

/**
 * 모든 [VirtualFuture]의 작업이 제한시간[timeout] 동안 완료될 때까지 대기한다.
 */
fun <T> awaitAll(timeout: Duration, vararg vfutures: VirtualFuture<T>): List<T> {
    return vfutures.toList().awaitAll(timeout)
}
