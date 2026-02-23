package io.bluetape4k.nats.client

import io.nats.client.JetStreamOptions
import java.time.Duration

@JvmField
val defaultJetStreamOptions: JetStreamOptions = JetStreamOptions.DEFAULT_JS_OPTIONS

inline fun jetStreamOptions(
    @BuilderInference builder: JetStreamOptions.Builder.() -> Unit,
): JetStreamOptions {
    return JetStreamOptions.builder().apply(builder).build()
}

inline fun jetStreamOptionsOf(
    prefix: String? = null,
    requestTimeout: Duration? = null,
    publishNoAck: Boolean? = null,
    optOut290ConsumerCreate: Boolean? = null,
    @BuilderInference builder: JetStreamOptions.Builder.() -> Unit = {},
): JetStreamOptions = jetStreamOptions {
    prefix?.run { prefix(this) }
    requestTimeout?.run { requestTimeout(this) }
    publishNoAck?.run { publishNoAck(this) }
    optOut290ConsumerCreate?.run { optOut290ConsumerCreate(this) }
    builder()
}
