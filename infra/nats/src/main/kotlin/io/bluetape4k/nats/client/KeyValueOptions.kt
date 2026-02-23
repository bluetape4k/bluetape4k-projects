package io.bluetape4k.nats.client

import io.nats.client.JetStreamOptions
import io.nats.client.KeyValueOptions

inline fun keyValueOptions(
    @BuilderInference builder: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions =
    KeyValueOptions.builder().apply(builder).build()

inline fun keyValueOptions(
    kvo: KeyValueOptions,
    @BuilderInference builder: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions =
    KeyValueOptions.builder(kvo).apply(builder).build()

inline fun keyValueOptions(
    jso: JetStreamOptions,
    @BuilderInference builder: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions =
    keyValueOptions {
        this.jetStreamOptions(jso)
        builder()
    }
