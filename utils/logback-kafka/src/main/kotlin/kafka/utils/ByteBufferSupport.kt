package io.bluetape4k.logback.kafka.utils

import java.nio.ByteBuffer

/**
 * 문자열을 Kafka 파티셔닝용 바이트 키로 변환합니다.
 *
 * ## 동작/계약
 * - null/blank 입력이면 null을 반환합니다.
 * - 그 외에는 `hashCode()`를 4바이트 배열로 변환합니다.
 *
 * ```kotlin
 * val key = "service-a".hashBytes()
 * // key?.size == 4
 * ```
 */
fun String?.hashBytes(): ByteArray? {
    return if (this.isNullOrBlank()) null
    else this.hashCode().toByteArray()
}

/**
 * Int 값을 big-endian 4바이트 배열로 변환합니다.
 *
 * ## 동작/계약
 * - 호출마다 새 [ByteArray]를 할당합니다.
 */
internal fun Int.toByteArray(): ByteArray =
    ByteBuffer.allocate(4).putInt(this).array()
