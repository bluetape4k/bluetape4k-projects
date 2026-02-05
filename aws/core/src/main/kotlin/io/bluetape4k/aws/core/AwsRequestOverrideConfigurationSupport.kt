package io.bluetape4k.aws.core

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration

inline fun awsRequestOverrideConfiguration(
    @BuilderInference builder: AwsRequestOverrideConfiguration.Builder.() -> Unit,
): AwsRequestOverrideConfiguration {
    return AwsRequestOverrideConfiguration.builder().apply(builder).build()
}

fun awsRequestOverrideConfigurationOf(
    credentialsProvider: AwsCredentialsProvider,
): AwsRequestOverrideConfiguration {
    return awsRequestOverrideConfiguration {
        credentialsProvider(credentialsProvider)
    }
}
