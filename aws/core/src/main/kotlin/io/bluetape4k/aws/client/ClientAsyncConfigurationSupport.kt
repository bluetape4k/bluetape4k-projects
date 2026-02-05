package io.bluetape4k.aws.client

import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption

inline fun clientAsyncConfiguration(
    @BuilderInference builder: ClientAsyncConfiguration.Builder.() -> Unit,
): ClientAsyncConfiguration {
    return ClientAsyncConfiguration.builder().apply(builder).build()
}

fun <T> clientAsyncConfigurationOf(
    asyncOption: SdkAdvancedAsyncClientOption<T>,
    value: T,
): ClientAsyncConfiguration = clientAsyncConfiguration {
    advancedOption(asyncOption, value)
}
