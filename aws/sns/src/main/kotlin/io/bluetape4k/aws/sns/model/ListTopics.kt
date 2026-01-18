package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListTopicsRequest

inline fun ListTopicsRequest(
    builder: ListTopicsRequest.Builder.() -> Unit,
): ListTopicsRequest =
    ListTopicsRequest.builder().apply(builder).build()

inline fun listTopicsRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: ListTopicsRequest.Builder.() -> Unit = {},
): ListTopicsRequest = ListTopicsRequest {
    nextToken?.run { nextToken(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
