package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.StreamSpecification
import aws.sdk.kotlin.services.dynamodb.model.StreamViewType

inline fun streamSpecificationOf(
    streamEnabled: Boolean? = null,
    streamViewType: StreamViewType? = null,
    crossinline configurer: StreamSpecification.Builder.() -> Unit = {},
): StreamSpecification {

    return StreamSpecification {
        this.streamEnabled = streamEnabled
        this.streamViewType = streamViewType

        configurer()
    }
}
