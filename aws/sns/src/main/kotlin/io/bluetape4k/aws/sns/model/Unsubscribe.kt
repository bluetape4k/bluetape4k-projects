package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest

inline fun unsubscribeRequest(
    @BuilderInference builder: UnsubscribeRequest.Builder.() -> Unit,
): UnsubscribeRequest = UnsubscribeRequest.builder().apply(builder).build()

inline fun unsubscribeRequestOf(
    subscriptionArn: String,
    @BuilderInference builder: UnsubscribeRequest.Builder.() -> Unit = {},
): UnsubscribeRequest {
    subscriptionArn.requireNotBlank("subscriptionArn")

    return unsubscribeRequest {
        this.subscriptionArn(subscriptionArn)

        builder()
    }
}
