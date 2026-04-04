package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.utils.ShutdownQueue
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * Virtual Thread를 사용하는 Executor의 Singleton 인스턴스
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
object VirtualThreadExecutor: ExecutorService by VirtualThreads.executorService() {
    init {
        // Thread memory leak 방지를 위해 ShutdownQueue에 등록
        ShutdownQueue.register(this)
    }
}


/**
 * Virtual thread 를 이용하여 비동기 작업을 수행합니다.
 *
 * ```kotlin
 * val vfuture: VirtualFuture<Int> = virtualFuture {
 *     Thread.sleep(500)
 *     42
 * }
 * val result = vfuture.await() // 42
 * ```
 *
 * @param executor 작업을 실행할 [ExecutorService] (기본값: [VirtualThreadExecutor])
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
 * ```kotlin
 * val tasks = listOf(
 *     { Thread.sleep(100); 1 },
 *     { Thread.sleep(200); 2 },
 *     { Thread.sleep(300); 3 },
 * )
 * val future = virtualFutureAll(tasks)
 * val result = future.await() // [1, 2, 3]
 * ```
 *
 * @param T 작업 결과 타입
 * @param tasks 병렬로 실행할 작업 목록
 * @param executor 작업을 실행할 [ExecutorService] (기본값: [VirtualThreadExecutor])
 * @return 모든 작업의 결과를 담은 [VirtualFuture] 인스턴스
 */
fun <T> virtualFutureAll(
    tasks: Collection<() -> T>,
    executor: ExecutorService = VirtualThreadExecutor,
): VirtualFuture<List<T>> {
    if (tasks.isEmpty()) {
        return VirtualFuture(java.util.concurrent.CompletableFuture.completedFuture(emptyList()))
    }
    val future = executor
        .invokeAll(tasks.map { Callable { it.invoke() } })
        .map { it.asCompletableFuture() }
        .sequence(executor)

    return VirtualFuture(future)
}

/**
 * 복수의 작업들을 Virtual thread 를 이용하여 비동기로 제한시간 [timeout] 동안 수행합니다. 결과는 [List]로 반환됩니다.
 * 모든 작업이 제한시간 내에 완료되지 않으면 [java.util.concurrent.TimeoutException]을 던집니다.
 *
 * ```kotlin
 * val tasks = listOf(
 *     { Thread.sleep(100); 1 },
 *     { Thread.sleep(200); 2 },
 *     { Thread.sleep(300); 3 },
 * )
 * val future = virtualFutureAll(tasks, timeout = Duration.ofSeconds(5))
 * val result = future.await() // [1, 2, 3]
 * ```
 *
 * @param T 작업 결과 타입
 * @param tasks 병렬로 실행할 작업 목록
 * @param executor 작업을 실행할 [ExecutorService] (기본값: [VirtualThreadExecutor])
 * @param timeout 각 작업의 최대 대기 시간
 * @return 모든 작업의 결과를 담은 [VirtualFuture] 인스턴스
 * @throws java.util.concurrent.TimeoutException 제한 시간 초과 시
 */
fun <T> virtualFutureAll(
    tasks: Collection<() -> T>,
    executor: ExecutorService = VirtualThreadExecutor,
    timeout: Duration,
): VirtualFuture<List<T>> {
    if (tasks.isEmpty()) {
        return VirtualFuture(java.util.concurrent.CompletableFuture.completedFuture(emptyList()))
    }
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
 * 모든 [VirtualFuture]의 작업이 완료될 때까지 대기하고, 결과를 [List]로 반환합니다.
 *
 * ```kotlin
 * val futures = listOf(
 *     virtualFuture { Thread.sleep(100); 1 },
 *     virtualFuture { Thread.sleep(200); 2 },
 *     virtualFuture { Thread.sleep(300); 3 },
 * )
 * val results = futures.awaitAll() // [1, 2, 3]
 * ```
 *
 * @return 모든 작업의 결과 목록 (입력 순서 유지)
 */
fun <T> Iterable<VirtualFuture<T>>.awaitAll(): List<T> {
    if (this is Collection && isEmpty()) {
        return emptyList()
    }
    return map { it.toCompletableFuture() }.sequence(VirtualThreadExecutor).get()
}

/**
 * 모든 [VirtualFuture]의 작업이 제한시간 [timeout] 동안 완료될 때까지 대기하고, 결과를 [List]로 반환합니다.
 * 제한 시간 내에 완료되지 않으면 [java.util.concurrent.TimeoutException]을 던집니다.
 *
 * ```kotlin
 * val futures = listOf(
 *     virtualFuture { Thread.sleep(100); 1 },
 *     virtualFuture { Thread.sleep(200); 2 },
 * )
 * val results = futures.awaitAll(Duration.ofSeconds(2)) // [1, 2]
 * ```
 *
 * @param timeout 최대 대기 시간
 * @return 모든 작업의 결과 목록 (입력 순서 유지)
 * @throws java.util.concurrent.TimeoutException 제한 시간 초과 시
 */
fun <T> Iterable<VirtualFuture<T>>.awaitAll(timeout: Duration): List<T> {
    if (this is Collection && isEmpty()) {
        return emptyList()
    }
    return map { it.toCompletableFuture() }.sequence(VirtualThreadExecutor)
        .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
}

/**
 * vararg 형태로 전달한 모든 [VirtualFuture]의 작업이 완료될 때까지 대기하고, 결과를 [List]로 반환합니다.
 *
 * ```kotlin
 * val f1 = virtualFuture { Thread.sleep(100); 1 }
 * val f2 = virtualFuture { Thread.sleep(200); 2 }
 * val f3 = virtualFuture { Thread.sleep(300); 3 }
 *
 * val results = awaitAll(f1, f2, f3) // [1, 2, 3]
 * ```
 *
 * @param vfutures 대기할 [VirtualFuture] 목록
 * @return 모든 작업의 결과 목록 (입력 순서 유지)
 */
fun <T> awaitAll(vararg vfutures: VirtualFuture<T>): List<T> {
    return vfutures.toList().awaitAll()
}

/**
 * vararg 형태로 전달한 모든 [VirtualFuture]의 작업이 제한시간 [timeout] 동안 완료될 때까지 대기하고, 결과를 [List]로 반환합니다.
 * 제한 시간 내에 완료되지 않으면 [java.util.concurrent.TimeoutException]을 던집니다.
 *
 * ```kotlin
 * val f1 = virtualFuture { Thread.sleep(100); 1 }
 * val f2 = virtualFuture { Thread.sleep(200); 2 }
 *
 * val results = awaitAll(Duration.ofSeconds(2), f1, f2) // [1, 2]
 * ```
 *
 * @param timeout 최대 대기 시간
 * @param vfutures 대기할 [VirtualFuture] 목록
 * @return 모든 작업의 결과 목록 (입력 순서 유지)
 * @throws java.util.concurrent.TimeoutException 제한 시간 초과 시
 */
fun <T> awaitAll(timeout: Duration, vararg vfutures: VirtualFuture<T>): List<T> {
    return vfutures.toList().awaitAll(timeout)
}
