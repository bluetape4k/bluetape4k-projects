package io.bluetape4k.aws.client

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration

inline fun clientOverrideConfiguration(
    initializer: ClientOverrideConfiguration.Builder.() -> Unit,
): ClientOverrideConfiguration {
    return ClientOverrideConfiguration.builder().apply(initializer).build()
}
