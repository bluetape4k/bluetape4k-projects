package io.bluetape4k.aws.core

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration

inline fun awsRequestOverrideConfiguration(
    initializer: AwsRequestOverrideConfiguration.Builder.() -> Unit,
): AwsRequestOverrideConfiguration {
    return AwsRequestOverrideConfiguration.builder().apply(initializer).build()
}

fun awsRequestOverrideConfigurationOf(
    credentialsProvider: AwsCredentialsProvider,
): AwsRequestOverrideConfiguration {
    return awsRequestOverrideConfiguration {
        credentialsProvider(credentialsProvider)
    }
}
