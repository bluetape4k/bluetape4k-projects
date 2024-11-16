package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.CompletableFuture

/**
 * 지정한 block을 Virtual Threads 를 이용하여 비동기로 실행하고, [CompletableFuture]를 반환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = virtualFutureOf {
 *      Thread.sleep(1000)
 *      42
 * }
 *
 * val result = future.get() // 42
 * ```
 * @param V 작업 결과 타입
 * @param block 비동기로 수행할 작업
 * @return [CompletableFuture] 인스턴스
 */
inline fun <V: Any> virtualFutureOf(
    crossinline block: () -> V,
): CompletableFuture<V> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
