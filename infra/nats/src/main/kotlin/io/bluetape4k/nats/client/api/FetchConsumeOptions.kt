package io.bluetape4k.nats.client.api

import io.nats.client.FetchConsumeOptions

/**
 * [FetchConsumeOptions]를 DSL로 생성합니다.
 */
inline fun fetchConsumeOptions(
    @BuilderInference builder: FetchConsumeOptions.Builder.() -> Unit,
): FetchConsumeOptions =
    FetchConsumeOptions.builder().apply(builder).build()

/**
 * 메시지 개수/만료 시간/바이트 한도로 [FetchConsumeOptions]를 생성합니다.
 */
fun fetchConsumeOptionsOf(
    maxMessages: Int = 100,
    expiresInMillis: Long = 1000,
    maxBytes: Long? = null,
): FetchConsumeOptions = fetchConsumeOptions {
    maxMessages(maxMessages)
    expiresIn(expiresInMillis)
    maxBytes?.run { maxBytes(this) }
}
