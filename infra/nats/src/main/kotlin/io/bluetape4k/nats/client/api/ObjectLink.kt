package io.bluetape4k.nats.client.api

import io.nats.client.api.ObjectLink

fun objectLinkOf(bucket: String): ObjectLink = ObjectLink.bucket(bucket)

fun objectLinkOf(bucket: String, objectName: String): ObjectLink = ObjectLink.`object`(bucket, objectName) 
