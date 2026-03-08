package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.JetStreamApiException
import io.nats.client.KeyValueManagement
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.KeyValueStatus

/**
 * Key-Value 버킷이 이미 있으면 갱신하고, 없으면 생성합니다.
 *
 * ## 동작/계약
 * - 먼저 `update`를 시도해 기존 버킷을 재설정합니다.
 * - 버킷이 없을 때(`not found`)만 `create`로 폴백합니다.
 * - not-found 이외의 오류는 숨기지 않고 전파합니다.
 */
fun KeyValueManagement.createOrUpdate(config: KeyValueConfiguration): KeyValueStatus {
    return try {
        update(config)
    } catch (je: JetStreamApiException) {
        if (je.isNotFound) {
            create(config)
        } else {
            throw je
        }
    }
}

/**
 * 버킷 상태를 조회하고, 버킷이 없으면 `null`을 반환합니다.
 */
fun KeyValueManagement.getStatusOrNull(bucketName: String): KeyValueStatus? {
    bucketName.requireNotBlank("bucketName")
    return try {
        getStatus(bucketName)
    } catch (je: JetStreamApiException) {
        if (je.isNotFound) {
            null
        } else {
            throw je
        }
    }
}

/**
 * 버킷 존재 여부를 조회합니다.
 */
fun KeyValueManagement.existsBucket(bucketName: String): Boolean {
    bucketName.requireNotBlank("bucketName")
    return getStatusOrNull(bucketName) != null
}

/**
 * 버킷이 존재할 때만 삭제하고, 이미 삭제된 상태는 정상으로 취급합니다.
 */
fun KeyValueManagement.forcedDelete(bucketName: String) {
    bucketName.requireNotBlank("bucketName")

    try {
        delete(bucketName)
    } catch (je: JetStreamApiException) {
        if (!je.isNotFound) {
            throw je
        }
    }
}
