package io.bluetape4k.concurrent.virtualthread


import io.bluetape4k.concurrent.asCompletableFuture
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * JDK 21 의 Virtual Thread를 이용하여 비동기 작업을 수행하는 [Future]
 *
 * ```
 * val vfuture: VirtualFuture<Int> = virtualFuture {
 *   // 작업 내용
 *   Thread.sleep(1000)
 *   42
 * }
 *
 * val result = vfuture.await()  // 42
 * ```
 *
 * @see virtualFuture
 *
 * @param T 작업 결과 타입
 * @param future [Future] 인스턴스
 */
class VirtualFuture<T>(private val future: Future<T>): Future<T> by future {
    /**
     * Virtual thread 가 완료되기를 기다림
     *
     * @return 작업 결과
     */
    fun await(): T {
        return awaitInternal(null)
    }

    /**
     * Virtual thread 가 완료되기를 기다림
     *
     * @param timeout 대기 시간
     * @return 작업 결과
     */
    fun await(timeout: Duration): T {
        return awaitInternal(timeout)
    }

    /**
     * [CompletableFuture]로 변환합니다.
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

    override fun exceptionNow(): Throwable? {
        return future.exceptionNow()
    }

    override fun state(): Future.State? {
        return future.state()
    }

}
