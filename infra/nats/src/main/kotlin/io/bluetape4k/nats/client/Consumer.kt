package io.bluetape4k.nats.client

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.nats.client.Consumer
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.time.toJavaDuration

/**
 * 밀리초 단위 타임아웃으로 Consumer drain을 시작합니다.
 *
 * @param timeoutMillis drain 타임아웃 (밀리초, 0 이상)
 * @return drain 완료 여부를 담은 [CompletableFuture]
 */
fun Consumer.drain(timeoutMillis: Long): CompletableFuture<Boolean> {
    timeoutMillis.requireZeroOrPositiveNumber("timeoutMillis")
    return drain(Duration.ofMillis(timeoutMillis))
}

/**
 * Kotlin [Duration] 기반으로 Consumer drain을 시작합니다.
 *
 * @param timeout drain 타임아웃 (0 이상)
 * @return drain 완료 여부를 담은 [CompletableFuture]
 */
fun Consumer.drain(timeout: kotlin.time.Duration): CompletableFuture<Boolean> {
    timeout.requireGe(kotlin.time.Duration.ZERO, "timeout")
    return drain(timeout.toJavaDuration())
}

/**
 * 밀리초 단위 타임아웃으로 Consumer drain을 suspend 함수로 실행합니다.
 */
suspend fun Consumer.drainSuspending(timeoutMillis: Long): Boolean = drain(timeoutMillis).await()

/**
 * Kotlin [Duration] 기반으로 Consumer drain을 suspend 함수로 실행합니다.
 */
suspend fun Consumer.drainSuspending(timeout: kotlin.time.Duration): Boolean = drain(timeout).await()

/**
 * Java [Duration][java.time.Duration] 기반으로 Consumer drain을 suspend 함수로 실행합니다.
 */
suspend fun Consumer.drainSuspending(timeout: java.time.Duration): Boolean {
    timeout.requireGe(java.time.Duration.ZERO, "timeout")
    return drain(timeout).await()
}
