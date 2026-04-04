package io.bluetape4k.concurrent

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.assertPositiveNumber

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

/**
 * [ForkJoinPool.commonPool]을 사용하는 [ExecutorService].
 *
 * ```kotlin
 * val future = CompletableFuture.supplyAsync({ 42 }, ForkJoinExecutor)
 * val result = future.get()  // 42
 * ```
 */
object ForkJoinExecutor: ExecutorService by ForkJoinPool.commonPool()

/**
 * [Executors.newVirtualThreadPerTaskExecutor]를 사용하는 [ExecutorService]
 */
/**
 * [Executors.newVirtualThreadPerTaskExecutor]를 사용하는 [ExecutorService].
 * [io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor]로 대체되었습니다.
 */
@Deprecated(
    message = "Use io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor instead",
    replaceWith = ReplaceWith("VirtualThreadExecutor"),
)
object VirtualThreadExecutor: ExecutorService by Executors.newVirtualThreadPerTaskExecutor()

/**
 * 호출 스레드에서 즉시 실행하는 동기 [Executor].
 *
 * ```kotlin
 * var ran = false
 * DirectExecutor.execute { ran = true }
 * ran  // true
 * ```
 */
object DirectExecutor: Executor {
    override fun execute(command: Runnable) = command.run()
}

private val log by lazy { KotlinLogging.logger {} }

/**
 * [task] 을 WorkStealingPool 을 이용하여 병렬로 수행합니다.
 *
 * ```kotlin
 * val future = withWorkStealingPool<Long>(4) {
 *      Thread.sleep(100L)
 *      42L
 * }
 * ```
 * @param parallelism Int 병렬 처리할 수
 * @param task WorkStealingPool 에서 실행할 [Callable] instance
 */
fun <T> withWorkStealingPool(
    parallelism: Int = Runtime.getRuntime().availableProcessors() * 2,
    task: () -> T,
): CompletableFuture<T> {
    parallelism.assertPositiveNumber("parallelism")
    val executor = Executors.newWorkStealingPool(parallelism)

    return try {
        executor
            .invokeAll(listOf(Callable { task() }))
            .first()
            .asCompletableFuture()
            .whenComplete { result, error ->
                log.debug { "WorkStealingPool is shutdown ... result=$result, error=$error" }
                runCatching { executor.shutdown() }
            }
    } catch (e: Exception) {
        executor.shutdown()
        throw e
    }
}

/**
 * 복수개의 [tasks] 을 WorkStealingPool 을 이용하여 병렬로 수행합니다.
 *
 * ```kotlin
 * val tasks: List<() -> Int> = List(4) { index -> { index * 2 } }
 * val future: CompletableFuture<List<Int>> = withWorkStealingPool(parallelism = 4, tasks = tasks)
 * val results = future.get()  // [0, 2, 4, 6]
 * ```
 *
 * @param parallelism Int 병렬 처리할 수
 * @param tasks WorkStealingPool 에서 실행할 [Callable]의 컬렉션
 */
fun <T> withWorkStealingPool(
    parallelism: Int = Runtime.getRuntime().availableProcessors() * 2,
    tasks: Collection<() -> T>,
): CompletableFuture<List<T>> {
    parallelism.assertPositiveNumber("parallelism")
    val executor = Executors.newWorkStealingPool(parallelism)

    return try {
        executor
            .invokeAll(tasks.map { Callable { it.invoke() } })
            .map { it.asCompletableFuture() }
            .sequence(executor)
            .whenComplete { result, error ->
                log.debug { "WorkStealingPool is shutdown ... result=$result, error=$error" }
                runCatching { executor.shutdown() }
            }
    } catch (e: Exception) {
        executor.shutdown()
        throw e
    }
}
