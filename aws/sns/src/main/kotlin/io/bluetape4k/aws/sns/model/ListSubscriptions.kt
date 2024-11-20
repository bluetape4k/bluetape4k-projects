package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListSubscriptionsRequest

inline fun ListSubscriptionsRequest(
    initializer: ListSubscriptionsRequest.Builder.() -> Unit,
): ListSubscriptionsRequest =
    ListSubscriptionsRequest.builder().apply(initializer).build()

fun listSubscriptionsRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: ListSubscriptionsRequest.Builder.() -> Unit = {},
): ListSubscriptionsRequest = ListSubscriptionsRequest {

    nextToken?.run { nextToken(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
