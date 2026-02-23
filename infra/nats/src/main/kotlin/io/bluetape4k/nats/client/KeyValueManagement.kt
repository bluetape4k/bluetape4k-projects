package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.JetStreamApiException
import io.nats.client.KeyValueManagement
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.KeyValueStatus

fun KeyValueManagement.createOrUpdate(config: KeyValueConfiguration): KeyValueStatus {
    return try {
        create(config)
    } catch (je: JetStreamApiException) {
        if (je.isNotFound) {
            update(config)
        } else {
            throw je
        }
    }
}

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

fun KeyValueManagement.existsBucket(bucketName: String): Boolean {
    bucketName.requireNotBlank("bucketName")
    return getStatusOrNull(bucketName) != null
}

fun KeyValueManagement.forcedDelete(bucketName: String) {
    bucketName.requireNotBlank("bucketName")
    runCatching { delete(bucketName) }
}
