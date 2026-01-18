package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest

inline fun ConfirmSubscriptionRequest(
    builder: ConfirmSubscriptionRequest.Builder.() -> Unit,
): ConfirmSubscriptionRequest =
    ConfirmSubscriptionRequest.builder().apply(builder).build()

inline fun confirmSubscriptionRequestOf(
    topicArn: String,
    token: String,
    builder: ConfirmSubscriptionRequest.Builder.() -> Unit = {},
): ConfirmSubscriptionRequest = ConfirmSubscriptionRequest {
    topicArn(topicArn)
    token(token)
    builder()
}
