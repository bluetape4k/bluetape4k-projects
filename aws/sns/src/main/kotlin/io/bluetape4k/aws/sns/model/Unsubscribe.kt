package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.UnsubscribeRequest

inline fun UnsubscribeRequest(
    builder: UnsubscribeRequest.Builder.() -> Unit,
): UnsubscribeRequest = UnsubscribeRequest.builder().apply(builder).build()

inline fun unsubscribeRequestOf(
    subscriptionArn: String,
    builder: UnsubscribeRequest.Builder.() -> Unit = {},
): UnsubscribeRequest = UnsubscribeRequest {
    this.subscriptionArn(subscriptionArn)

    builder()
}
