package io.bluetape4k.aws.client

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration

inline fun clientOverrideConfiguration(
    @BuilderInference builder: ClientOverrideConfiguration.Builder.() -> Unit,
): ClientOverrideConfiguration {
    return ClientOverrideConfiguration.builder().apply(builder).build()
}
