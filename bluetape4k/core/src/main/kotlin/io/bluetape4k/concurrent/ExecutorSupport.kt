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
 * [ForkJoinPool.commonPool]을 사용하는 [ExecutorService]
 */
object ForkJoinExecutor: ExecutorService by ForkJoinPool.commonPool()

/**
 * [Executors.newVirtualThreadPerTaskExecutor]를 사용하는 [ExecutorService]
 */
@Deprecated(
    message = "User io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor instead",
    replaceWith = ReplaceWith("VirtualThreadExecutor"),
)
/**
 * VirtualThreadExecutor 타입을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val type = VirtualThreadExecutor::class
 * println(type.simpleName)
 * check(type.simpleName != null)
 * ```
 */
object VirtualThreadExecutor: ExecutorService by Executors.newVirtualThreadPerTaskExecutor()

/**
 * Direct executor
 */
object DirectExecutor: Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}

private val log by lazy { KotlinLogging.logger {} }

/**
 * [task] 을 WorkStealingPool 을 이용하여 병렬로 수행합니다.
 *
 * ```
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

    return executor
        .invokeAll(listOf(Callable { task() }))
        .first()
        .asCompletableFuture()
        .whenComplete { result, error ->
            log.debug { "WorkStealingPool is shutdown ... result=$result, error=$error" }
            runCatching { executor.shutdown() }
        }
}

/**
 * 복수개의 [tasks] 을 WorkStealingPool 을 이용하여 병렬로 수행합니다.
 *
 * ```
 * val tasks = List(10) {
 *      Thread.sleep(100)
 *      it
 * }
 * val future: CompletableFuture<List<Int>> = withWorkStealingPool<Long>(4, tasks)
 * ```
 * @param parallelism Int 병렬 처리할 수
 * @param tasks WorkStealingPool 에서 실행할 [Callable]의 컬렉션
 */
fun <T> withWorkStealingPool(
    parallelism: Int = Runtime.getRuntime().availableProcessors() * 2,
    tasks: Collection<() -> T>,
): CompletableFuture<List<T>> {
    parallelism.assertPositiveNumber("parallelism")
    val executor = Executors.newWorkStealingPool(parallelism)

    return executor
        .invokeAll(tasks.map { Callable { it.invoke() } })
        .map { it.asCompletableFuture() }
        .sequence(executor)
        .whenComplete { result, error ->
            log.debug { "WorkStealingPool is shutdown ... result=$result, error=$error" }
            runCatching { executor.shutdown() }
        }
}
