package io.bluetape4k.nats.client.api

import io.nats.client.FetchConsumeOptions

inline fun fetchConsumeOptions(initializer: FetchConsumeOptions.Builder.() -> Unit): FetchConsumeOptions =
    FetchConsumeOptions.builder().apply(initializer).build()

fun fetchConsumeOptionsOf(
    maxMessages: Int = 100,
    expiresInMillis: Long = 1000,
    maxBytes: Long? = null,
): FetchConsumeOptions = fetchConsumeOptions {
    maxMessages(maxMessages)
    expiresIn(expiresInMillis)
    maxBytes?.run { maxBytes(this) }
}
