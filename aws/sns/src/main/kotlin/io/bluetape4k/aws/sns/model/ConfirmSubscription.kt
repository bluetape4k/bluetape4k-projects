package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest

inline fun confirmSubscriptionRequest(
    @BuilderInference builder: ConfirmSubscriptionRequest.Builder.() -> Unit,
): ConfirmSubscriptionRequest =
    ConfirmSubscriptionRequest.builder().apply(builder).build()

inline fun confirmSubscriptionRequestOf(
    topicArn: String,
    token: String,
    @BuilderInference builder: ConfirmSubscriptionRequest.Builder.() -> Unit = {},
): ConfirmSubscriptionRequest {
    topicArn.requireNotBlank("topicArn")
    token.requireNotBlank("token")

    return confirmSubscriptionRequest {
        topicArn(topicArn)
        token(token)
        builder()
    }
}
