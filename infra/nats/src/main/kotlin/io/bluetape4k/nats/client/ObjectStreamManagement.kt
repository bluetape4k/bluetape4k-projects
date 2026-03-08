package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.JetStreamApiException
import io.nats.client.ObjectStoreManagement

/**
 * Object Store 버킷이 존재할 때만 삭제하고, 이미 없어진 상태는 무시합니다.
 *
 * ## 동작/계약
 * - not-found 오류만 정상 흐름으로 간주합니다.
 * - 그 외 JetStream 예외는 그대로 전파합니다.
 */
fun ObjectStoreManagement.tryDelete(bucketName: String) {
    bucketName.requireNotBlank("bucketName")

    try {
        delete(bucketName)
    } catch (e: JetStreamApiException) {
        if (e.apiErrorCode != JET_STREAM_NOT_FOUND) {
            throw e
        }
    }
}
