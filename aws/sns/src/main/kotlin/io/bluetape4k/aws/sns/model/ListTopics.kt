package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListTopicsRequest

inline fun listTopicsRequest(
    @BuilderInference builder: ListTopicsRequest.Builder.() -> Unit,
): ListTopicsRequest =
    ListTopicsRequest.builder().apply(builder).build()

inline fun listTopicsRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: ListTopicsRequest.Builder.() -> Unit = {},
): ListTopicsRequest =
    listTopicsRequest {
        nextToken?.let {
            nextToken.requireNotBlank("nextToken")
            nextToken(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
