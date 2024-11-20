package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest

inline fun CreatePlatformEndpointRequest(
    initializer: CreatePlatformEndpointRequest.Builder.() -> Unit,
): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest.builder().apply(initializer).build()

fun createPlatformEndpointRequestOf(
    platformApplicationArn: String,
    token: String,
    customUserData: String? = null,
    attributes: Map<String, String>? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: CreatePlatformEndpointRequest.Builder.() -> Unit = {},
): CreatePlatformEndpointRequest = CreatePlatformEndpointRequest {
    platformApplicationArn(platformApplicationArn)
    token(token)
    customUserData?.run { customUserData(this) }
    attributes?.run { attributes(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
