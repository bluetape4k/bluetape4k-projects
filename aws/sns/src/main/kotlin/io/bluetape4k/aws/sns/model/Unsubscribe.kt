package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.UnsubscribeRequest

inline fun UnsubscribeRequest(
    initializer: UnsubscribeRequest.Builder.() -> Unit,
): UnsubscribeRequest = UnsubscribeRequest.builder().apply(initializer).build()

fun unsubscribeRequestOf(
    subscriptionArn: String,
    initializer: UnsubscribeRequest.Builder.() -> Unit = {},
): UnsubscribeRequest = UnsubscribeRequest {
    this.subscriptionArn(subscriptionArn)

    initializer()
}
