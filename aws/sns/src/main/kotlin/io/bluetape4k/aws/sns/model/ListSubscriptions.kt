package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListSubscriptionsRequest

inline fun listSubscriptionsRequest(
    @BuilderInference builder: ListSubscriptionsRequest.Builder.() -> Unit,
): ListSubscriptionsRequest =
    ListSubscriptionsRequest.builder().apply(builder).build()

inline fun listSubscriptionsRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: ListSubscriptionsRequest.Builder.() -> Unit = {},
): ListSubscriptionsRequest =
    listSubscriptionsRequest {
        nextToken?.let {
            nextToken.requireNotBlank("nextToken")
            nextToken(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }
        builder()
    }
