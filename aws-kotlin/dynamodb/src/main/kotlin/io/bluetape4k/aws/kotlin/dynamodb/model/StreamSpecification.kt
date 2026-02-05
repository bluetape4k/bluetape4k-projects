package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.StreamSpecification
import aws.sdk.kotlin.services.dynamodb.model.StreamViewType

fun streamSpecificationOf(
    streamEnabled: Boolean? = null,
    streamViewType: StreamViewType? = null,
    @BuilderInference builder: StreamSpecification.Builder.() -> Unit = {},
): StreamSpecification =
    StreamSpecification {
        this.streamEnabled = streamEnabled
        this.streamViewType = streamViewType

        builder()
    }
