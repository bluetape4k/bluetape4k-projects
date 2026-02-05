package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest

inline fun ConfirmSubscriptionRequest(
    @BuilderInference builder: ConfirmSubscriptionRequest.Builder.() -> Unit,
): ConfirmSubscriptionRequest =
    ConfirmSubscriptionRequest.builder().apply(builder).build()

inline fun confirmSubscriptionRequestOf(
    topicArn: String,
    token: String,
    @BuilderInference builder: ConfirmSubscriptionRequest.Builder.() -> Unit = {},
): ConfirmSubscriptionRequest = ConfirmSubscriptionRequest {
    topicArn(topicArn)
    token(token)
    builder()
}
