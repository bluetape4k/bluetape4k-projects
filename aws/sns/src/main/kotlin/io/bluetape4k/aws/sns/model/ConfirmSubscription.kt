package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest

inline fun ConfirmSubscriptionRequest(
    initializer: ConfirmSubscriptionRequest.Builder.() -> Unit,
): ConfirmSubscriptionRequest =
    ConfirmSubscriptionRequest.builder().apply(initializer).build()

fun confirmSubscriptionRequestOf(
    topicArn: String,
    token: String,
    initializer: ConfirmSubscriptionRequest.Builder.() -> Unit = {},
): ConfirmSubscriptionRequest = ConfirmSubscriptionRequest {
    topicArn(topicArn)
    token(token)
    initializer()
}
