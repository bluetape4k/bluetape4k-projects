package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.S3Location
import io.bluetape4k.support.requireNotBlank

fun s3LocationOf(
    bucket: String,
    @BuilderInference builder: S3Location.Builder.() -> Unit = {},
): S3Location {
    bucket.requireNotBlank("bucket")

    return S3Location.invoke {
        this.bucketName = bucket

        builder()
    }
}
