package io.bluetape4k.nats.client

import io.bluetape4k.nats.client.api.streamConfiguration
import io.bluetape4k.support.requireNotBlank
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamManagement
import io.nats.client.api.ConsumerInfo
import io.nats.client.api.PurgeResponse
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.api.StreamInfo
import java.util.LinkedHashSet

/**
 * 스트림이 이미 제거되었더라도 실패로 간주하지 않고 삭제를 시도합니다.
 *
 * ## 동작/계약
 * - 스트림이 없을 때만 `false`를 반환합니다.
 * - 그 외 JetStream API 예외는 숨기지 않고 그대로 전파합니다.
 */
fun JetStreamManagement.forcedDeleteStream(streamName: String): Boolean {
    streamName.requireNotBlank("streamName")
    return ignoreNotFound(false) { deleteStream(streamName) }
}

/**
 * Consumer가 이미 제거되었더라도 실패로 간주하지 않고 삭제를 시도합니다.
 *
 * ## 동작/계약
 * - Consumer가 없을 때만 `false`를 반환합니다.
 * - 그 외 JetStream API 예외는 호출자에게 그대로 전달합니다.
 */
fun JetStreamManagement.forcedDeleteConsumer(streamName: String, consumerName: String): Boolean {
    streamName.requireNotBlank("streamName")
    consumerName.requireNotBlank("consumerName")
    return ignoreNotFound(false) { deleteConsumer(streamName, consumerName) }
}

/**
 * 스트림이 존재할 때만 purge를 수행합니다.
 *
 * ## 동작/계약
 * - 스트림이 없으면 `null`을 반환합니다.
 * - purge 자체의 다른 실패는 숨기지 않습니다.
 */
fun JetStreamManagement.forcedPurgeStream(streamName: String): PurgeResponse? {
    streamName.requireNotBlank("streamName")
    return ignoreNotFound(null) { purgeStream(streamName) }
}

/**
 * 스트림이 없으면 생성하고, 존재하면 purge를 시도합니다.
 *
 * ## 동작/계약
 * - 스트림이 없을 때만 [streamConfigurationCreator]로 새 스트림을 생성합니다.
 * - 스트림 생성 후에는 purge 결과 대신 `null`을 반환합니다.
 * - not-found 이외의 JetStream 예외는 그대로 전파합니다.
 */
inline fun JetStreamManagement.tryPurgeStream(
    streamName: String,
    streamConfigurationCreator: () -> StreamConfiguration = { StreamConfiguration.builder().name(streamName).build() },
): PurgeResponse? {
    streamName.requireNotBlank("streamName")

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

/**
 * 스트림 정보를 조회하고, 스트림이 없으면 `null`을 반환합니다.
 */
fun JetStreamManagement.getStreamInfoOrNull(streamName: String): StreamInfo? {
    streamName.requireNotBlank("streamName")

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

/**
 * 스트림 존재 여부를 조회합니다.
 */
fun JetStreamManagement.streamExists(streamName: String): Boolean =
    getStreamInfoOrNull(streamName) != null

/**
 * Consumer 정보를 조회하고, 대상 Consumer가 없으면 `null`을 반환합니다.
 */
fun JetStreamManagement.getConsumerInfoOrNull(streamName: String, consumerName: String): ConsumerInfo? {
    streamName.requireNotBlank("streamName")
    consumerName.requireNotBlank("consumerName")

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

/**
 * Consumer 존재 여부를 조회합니다.
 */
fun JetStreamManagement.consumerExists(streamName: String, consumerName: String): Boolean {
    streamName.requireNotBlank("streamName")
    consumerName.requireNotBlank("consumerName")
    return getConsumerInfoOrNull(streamName, consumerName) != null
}

/**
 * 최소 설정만으로 새 스트림을 생성합니다.
 */
fun JetStreamManagement.createStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")

    val sc = streamConfiguration {
        name(streamName)
        storageType(storageType)
        subjects(*subjects)
    }
    return addStream(sc)
}

/**
 * 단일 subject를 가진 스트림을 교체 생성합니다.
 */
fun JetStreamManagement.createOrReplaceStream(
    streamName: String,
    subject: String,
): StreamInfo = createOrReplaceStream(streamName, subjects = arrayOf(subject))

/**
 * 기존 스트림을 제거한 뒤 새 설정으로 다시 생성합니다.
 *
 * ## 동작/계약
 * - 스트림이 없을 때의 삭제 실패만 무시합니다.
 * - 삭제 단계의 다른 실패는 전파하여 손상된 상태를 숨기지 않습니다.
 */
fun JetStreamManagement.createOrReplaceStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")

    ignoreNotFound(false) { deleteStream(streamName) }
    return createStream(streamName, storageType, *subjects)
}

/**
 * 스트림이 없으면 생성하고, 있으면 subject 집합에 없는 항목만 추가합니다.
 *
 * ## 동작/계약
 * - 기존 subject 순서를 유지하면서 신규 subject만 뒤에 추가합니다.
 * - 이미 모두 포함되어 있으면 `updateStream`을 호출하지 않습니다.
 */
fun JetStreamManagement.createStreamOrUpdateSubjects(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")

    val si = getStreamInfoOrNull(streamName)
        ?: return createStream(streamName, storageType, *subjects)

    val sc = si.configuration
    val mergedSubjects = LinkedHashSet(sc.subjects)
    val needToUpdate = mergedSubjects.addAll(subjects.asList())
    return if (needToUpdate) {
        val updatedSc = streamConfiguration(sc) { subjects(mergedSubjects) }
        updateStream(updatedSc)
    } else {
        si
    }
}

private inline fun <T> ignoreNotFound(defaultValue: T, block: () -> T): T =
    try {
        block()
    } catch (ex: JetStreamApiException) {
        if (ex.isNotFound) defaultValue else throw ex
    }
