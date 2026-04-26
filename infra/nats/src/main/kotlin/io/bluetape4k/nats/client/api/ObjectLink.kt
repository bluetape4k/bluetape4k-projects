package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.ObjectLink

/**
 * 버킷 레벨 [ObjectLink]를 생성합니다.
 *
 * @param bucket Object Store 버킷 이름
 */
fun objectLinkOf(bucket: String): ObjectLink {
    bucket.requireNotBlank("bucket")
    return ObjectLink.bucket(bucket)
}

/**
 * 특정 객체를 가리키는 [ObjectLink]를 생성합니다.
 *
 * @param bucket Object Store 버킷 이름
 * @param objectName 버킷 내 객체 이름
 */
fun objectLinkOf(bucket: String, objectName: String): ObjectLink {
    bucket.requireNotBlank("bucket")
    objectName.requireNotBlank("objectName")
    return ObjectLink.`object`(bucket, objectName)
}
