package io.bluetape4k.nats.client

import io.nats.client.JetStreamOptions
import io.nats.client.KeyValueOptions

inline fun keyValueOptions(
    initializer: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions {
    return KeyValueOptions.builder().apply(initializer).build()
}

inline fun keyValueOptions(
    kvo: KeyValueOptions,
    initializer: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions {
    return KeyValueOptions.builder(kvo).apply(initializer).build()
}

inline fun keyValueOptions(
    jso: JetStreamOptions,
    initializer: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions = keyValueOptions {
    this.jetStreamOptions(jso)
    initializer()
}
