package io.bluetape4k.science.exposed.service.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.bluetape4k.science.exposed.NetCdfException
import java.nio.charset.StandardCharsets
import java.text.Normalizer

private val auxiliaryMapper = jacksonObjectMapper()

/**
 * CF auxiliary 좌표를 JSONB 문자열로 직렬화합니다.
 *
 * 위치·시간·레벨 축은 호출자가 제외하고 넘겨야 하며, 비유한 값은 저장하지 않습니다.
 * 키 순서는 입력 Map 순서를 따르고, UTF-8 크기와 예약 namespace를 검증합니다.
 */
internal fun serializeAuxiliaryAttributes(values: Map<String, Double>): String? {
    if (values.isEmpty()) return null
    val filtered = LinkedHashMap<String, Double>()
    values.forEach { (key, value) ->
        validateAuxiliaryKey(key)
        if (value.isFinite()) filtered[key] = value
    }
    if (filtered.isEmpty()) return null

    val json = auxiliaryMapper.writeValueAsString(filtered)
    val size = json.toByteArray(StandardCharsets.UTF_8).size.toLong()
    if (size > MAX_AUXILIARY_JSONB_BYTES) {
        throw NetCdfException.ResourceLimitExceeded("auxiliary-jsonb", MAX_AUXILIARY_JSONB_BYTES, size)
    }
    return json
}

private fun validateAuxiliaryKey(key: String) {
    val normalized = Normalizer.normalize(key, Normalizer.Form.NFC)
    val keyBytes = key.toByteArray(StandardCharsets.UTF_8).size.toLong()
    val invalid = key.isEmpty() || keyBytes > MAX_VARIABLE_NAME_BYTES || normalized != key ||
        key.startsWith("__bluetape4k_") ||
        key.any(Char::isISOControl) || containsUnpairedSurrogate(key)
    if (invalid) {
        throw NetCdfException.ResourceLimitExceeded(
            resource = "auxiliary-key",
            limit = MAX_VARIABLE_NAME_BYTES,
            actual = keyBytes,
        )
    }
}

private fun containsUnpairedSurrogate(value: String): Boolean {
    var index = 0
    while (index < value.length) {
        val character = value[index]
        when {
            Character.isHighSurrogate(character) -> {
                if (index + 1 >= value.length || !Character.isLowSurrogate(value[index + 1])) return true
                index += 2
            }
            Character.isLowSurrogate(character) -> return true
            else -> index++
        }
    }
    return false
}

/** DB writer가 공통으로 소비하는 하나의 격자 행입니다. */
internal data class TileRow(
    val fileId: Long,
    val variableName: String,
    val longitude: Double?,
    val latitude: Double?,
    val timeIdx: Int,
    val levelIdx: Int,
    val value: Double,
    val attrsJson: String? = null,
)

internal data class BatchWriteResult(
    val inserted: Int,
    val conflicts: Int,
)
