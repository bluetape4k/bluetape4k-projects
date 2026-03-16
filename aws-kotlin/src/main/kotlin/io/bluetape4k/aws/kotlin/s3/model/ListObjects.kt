package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.EncodingType
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import io.bluetape4k.support.requireNotBlank

inline fun listObjectsRequestOf(
    bucket: String,
    prefix: String? = null,
    delimiter: String? = null,
    maxKeys: Int? = null,
    encondingType: EncodingType? = null,
    crossinline builder: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    bucket.requireNotBlank("bucket")

    return ListObjectsRequest {
        this.bucket = bucket
        this.prefix = prefix
        this.delimiter = delimiter
        this.maxKeys = maxKeys
        this.encodingType = encondingType

        builder()
    }
}
