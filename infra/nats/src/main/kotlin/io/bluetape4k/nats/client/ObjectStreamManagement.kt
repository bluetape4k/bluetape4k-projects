package io.bluetape4k.nats.client

import io.nats.client.JetStreamApiException
import io.nats.client.ObjectStoreManagement

fun ObjectStoreManagement.tryDelete(bucketName: String) {
    try {
        delete(bucketName)
    } catch (e: JetStreamApiException) {
        if (e.apiErrorCode != JET_STREAM_NOT_FOUND) {
            throw e
        }
    }
}
