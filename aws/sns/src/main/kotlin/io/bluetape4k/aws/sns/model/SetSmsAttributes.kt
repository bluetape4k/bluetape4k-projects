package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSmsAttributesRequest

inline fun setSmsAttributesRequest(
    @BuilderInference builder: SetSmsAttributesRequest.Builder.() -> Unit,
): SetSmsAttributesRequest =
    SetSmsAttributesRequest.builder().apply(builder).build()

inline fun setSmsAttributesRequestOf(
    attributes: Map<String, String>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: SetSmsAttributesRequest.Builder.() -> Unit = {},
): SetSmsAttributesRequest = setSmsAttributesRequest {
    attributes(attributes)
    overrideConfiguration?.let { overrideConfiguration(it) }

    builder()
}
