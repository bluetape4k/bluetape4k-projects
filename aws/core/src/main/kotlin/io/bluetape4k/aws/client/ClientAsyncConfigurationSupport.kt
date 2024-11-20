package io.bluetape4k.aws.client

import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption

inline fun clientAsyncConfiguration(
    initializer: ClientAsyncConfiguration.Builder.() -> Unit,
): ClientAsyncConfiguration {
    return ClientAsyncConfiguration.builder().apply(initializer).build()
}

fun <T> clientAsyncConfigurationOf(
    asyncOption: SdkAdvancedAsyncClientOption<T>,
    value: T,
): ClientAsyncConfiguration = clientAsyncConfiguration {
    advancedOption(asyncOption, value)
}
