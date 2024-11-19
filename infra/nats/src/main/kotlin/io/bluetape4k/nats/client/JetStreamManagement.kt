package io.bluetape4k.nats.client

import io.bluetape4k.nats.client.api.streamConfiguration
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamManagement
import io.nats.client.api.ConsumerInfo
import io.nats.client.api.PurgeResponse
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.api.StreamInfo

fun JetStreamManagement.forcedDeleteStream(streamName: String): Boolean =
    runCatching { deleteStream(streamName) }.getOrDefault(false)

fun JetStreamManagement.forcedDeleteConsumer(streamName: String, consumerName: String): Boolean =
    runCatching { deleteConsumer(streamName, consumerName) }.getOrDefault(false)

fun JetStreamManagement.forcedPurgeStream(streamName: String): PurgeResponse? =
    runCatching { purgeStream(streamName) }.getOrNull()

fun JetStreamManagement.tryPurgeStream(
    streamName: String,
    streamConfigurationCreator: () -> StreamConfiguration = { StreamConfiguration.builder().name(streamName).build() },
): PurgeResponse? {
    return try {
        purgeStream(streamName)
    } catch (je: JetStreamApiException) {
        if (je.isNotFound) {
            addStream(streamConfigurationCreator())
        } else {
            throw je
        }
        null
    }
}

fun JetStreamManagement.getStreamInfoOrNull(streamName: String): StreamInfo? {
    return try {
        getStreamInfo(streamName)
    } catch (je: JetStreamApiException) {
        if (je.isNotFound) {
            null
        } else {
            throw je
        }
    }
}

fun JetStreamManagement.streamExists(streamName: String): Boolean =
    getStreamInfoOrNull(streamName) != null

fun JetStreamManagement.getConsumerInfoOrNull(streamName: String, consumerName: String): ConsumerInfo? {
    return try {
        getConsumerInfo(streamName, consumerName)
    } catch (ex: JetStreamApiException) {
        if (ex.apiErrorCode == JET_STREAM_NOT_FOUND) {
            null
        } else {
            throw ex
        }
    }
}

fun JetStreamManagement.consumerExists(streamName: String, consumerName: String): Boolean =
    getConsumerInfoOrNull(streamName, consumerName) != null


fun JetStreamManagement.createStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    val sc = streamConfiguration {
        name(streamName)
        storageType(storageType)
        subjects(*subjects)
    }
    return addStream(sc)
}

fun JetStreamManagement.createOrReplaceStream(
    streamName: String,
    subject: String,
): StreamInfo = createOrReplaceStream(streamName, subjects = arrayOf(subject))


fun JetStreamManagement.createOrReplaceStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    runCatching { deleteStream(streamName) }
    // Create a stream
    return createStream(streamName, storageType, *subjects)
}

fun JetStreamManagement.createStreamOrUpdateSubjects(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    val si = getStreamInfoOrNull(streamName)
        ?: return createStream(streamName, storageType, *subjects)

    val sc = si.configuration
    var needToUpdate = false
    subjects.forEach {
        if (!sc.subjects.contains(it)) {
            needToUpdate = true
            sc.subjects.add(it)
        }
    }
    return if (needToUpdate) {
        val updatedSc = streamConfiguration(sc) { subjects(sc.subjects) }
        updateStream(updatedSc)
    } else {
        si
    }
}
