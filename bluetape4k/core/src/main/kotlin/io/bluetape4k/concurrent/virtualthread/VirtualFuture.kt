package io.bluetape4k.concurrent.virtualthread


import io.bluetape4k.concurrent.asCompletableFuture
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * JDK 21 의 Virtual Thread를 이용하여 비동기 작업을 수행하는 [Future]
 *
 * ```kotlin
 * val vfuture: VirtualFuture<Int> = virtualFuture {
 *     Thread.sleep(500)
 *     42
 * }
 * val result = vfuture.await() // 42
 * ```
 *
 * @see virtualFuture
 *
 * @param T 작업 결과 타입
 * @property future 내부적으로 위임하는 [Future] 인스턴스
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class VirtualFuture<T>(private val future: Future<T>): Future<T> by future {
    /**
     * Virtual thread 작업이 완료될 때까지 블로킹 방식으로 대기하고 결과를 반환합니다.
     *
     * ```kotlin
     * val vfuture = virtualFuture {
     *     Thread.sleep(500)
     *     42
     * }
     * val result = vfuture.await() // 42
     * ```
     *
     * @return 작업 결과
     */
    fun await(): T {
        return awaitInternal(null)
    }

    /**
     * Virtual thread 작업이 완료될 때까지 [timeout] 동안 대기하고 결과를 반환합니다.
     * 제한 시간 내에 완료되지 않으면 [java.util.concurrent.TimeoutException]을 던집니다.
     *
     * ```kotlin
     * val vfuture = virtualFuture {
     *     Thread.sleep(500)
     *     42
     * }
     * val result = vfuture.await(Duration.ofSeconds(2)) // 42
     * ```
     *
     * @param timeout 최대 대기 시간
     * @return 작업 결과
     * @throws java.util.concurrent.TimeoutException 제한 시간 초과 시
     */
    fun await(timeout: Duration): T {
        return awaitInternal(timeout)
    }

    /**
     * [VirtualFuture]를 [CompletableFuture]로 변환합니다.
     * [CompletableFuture]의 조합 연산(thenApply, thenCompose 등)을 사용할 때 유용합니다.
     *
     * ```kotlin
     * val vfuture = virtualFuture { 42 }
     * val cf: CompletableFuture<String> = vfuture.toCompletableFuture()
     *     .thenApply { "result: $it" }
     * val result = cf.get() // "result: 42"
     * ```
     *
     * @return [CompletableFuture] 인스턴스
     */
    fun toCompletableFuture(): CompletableFuture<T> {
        return future.asCompletableFuture()
    }

    private fun awaitInternal(timeout: Duration? = null): T {
        return when (timeout) {
            null -> future.get()
            else -> future.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
        }
    }
}
