package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.ObjectLink

fun objectLinkOf(bucket: String): ObjectLink {
    bucket.requireNotBlank("bucket")
    return ObjectLink.bucket(bucket)
}

fun objectLinkOf(bucket: String, objectName: String): ObjectLink {
    bucket.requireNotBlank("bucket")
    objectName.requireNotBlank("objectName")
    return ObjectLink.`object`(bucket, objectName)
}
