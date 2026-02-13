package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest

inline fun createPlatformEndpointRequest(
    @BuilderInference builder: CreatePlatformEndpointRequest.Builder.() -> Unit,
): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest.builder().apply(builder).build()

inline fun createPlatformEndpointRequestOf(
    platformApplicationArn: String,
    token: String,
    customUserData: String? = null,
    attributes: Map<String, String>? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: CreatePlatformEndpointRequest.Builder.() -> Unit = {},
): CreatePlatformEndpointRequest {
    platformApplicationArn.requireNotBlank("platformApplicationArn")
    token.requireNotBlank("token")

    return createPlatformEndpointRequest {
        platformApplicationArn(platformApplicationArn)
        token(token)
        customUserData?.run { customUserData(this) }
        attributes?.run { attributes(this) }
        overrideConfiguration?.run { overrideConfiguration(this) }

        builder()
    }
}
