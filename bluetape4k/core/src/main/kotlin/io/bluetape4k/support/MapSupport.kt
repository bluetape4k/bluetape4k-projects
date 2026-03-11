package io.bluetape4k.support

import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.*

private fun Map<String, Any?>.requireValue(name: String): Any =
    requireNotNull(this[name]) { "Map[$name] is missing or null." }

/**
 * 맵에서 [name] 키 값을 `Boolean`으로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asBoolean] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("enabled" to "true")
 * val enabled = source.boolean("enabled")
 * // enabled
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.boolean(name: String): Boolean = requireValue(name).asBoolean()

/**
 * 맵에서 [name] 키 값을 `Boolean`으로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asBooleanOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("enabled" to "true")
 * val enabled = source.booleanOrNull("enabled")
 * // enabled == true
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.booleanOrNull(name: String): Boolean? = this[name].asBooleanOrNull()

/**
 * 맵에서 [name] 키 값을 `Char`로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asChar] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("grade" to "A")
 * val grade = source.char("grade")
 * // grade == 'A'
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.char(name: String): Char = requireValue(name).asChar()

/**
 * 맵에서 [name] 키 값을 `Char`로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asCharOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("grade" to "A")
 * val grade = source.charOrNull("grade")
 * // grade == 'A'
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.charOrNull(name: String): Char? = this[name].asCharOrNull()

/**
 * 맵에서 [name] 키 값을 `Byte`로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asByte] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("small" to "12")
 * val small = source.byte("small")
 * // small == 12.toByte()
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.byte(name: String): Byte = requireValue(name).asByte()

/**
 * 맵에서 [name] 키 값을 `Byte`로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asByteOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("small" to "12")
 * val small = source.byteOrNull("small")
 * // small == 12.toByte()
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.byteOrNull(name: String): Byte? = this[name].asByteOrNull()

/**
 * 맵에서 [name] 키 값을 `Short`로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asShort] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("port" to "8080")
 * val port = source.short("port")
 * // port == 8080.toShort()
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.short(name: String): Short = requireValue(name).asShort()

/**
 * 맵에서 [name] 키 값을 `Short`로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asShortOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("port" to "8080")
 * val port = source.shortOrNull("port")
 * // port == 8080.toShort()
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.shortOrNull(name: String): Short? = this[name].asShortOrNull()

/**
 * 맵에서 [name] 키 값을 `Int`로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asInt] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("retry" to "3")
 * val retry = source.int("retry")
 * // retry == 3
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.int(name: String): Int = requireValue(name).asInt()

/**
 * 맵에서 [name] 키 값을 `Int`로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asIntOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("retry" to "3")
 * val retry = source.intOrNull("retry")
 * // retry == 3
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.intOrNull(name: String): Int? = this[name].asIntOrNull()

/**
 * 맵에서 [name] 키 값을 `Long`으로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asLong] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("id" to "42")
 * val id = source.long("id")
 * // id == 42L
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.long(name: String): Long = requireValue(name).asLong()

/**
 * 맵에서 [name] 키 값을 `Long`으로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asLongOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("id" to "42")
 * val id = source.longOrNull("id")
 * // id == 42L
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.longOrNull(name: String): Long? = this[name].asLongOrNull()

/**
 * 맵에서 [name] 키 값을 [BigDecimal]로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asBigDecimal] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("price" to "12.34")
 * val price = source.bigDecimal("price")
 * // price.toPlainString() == "12.34"
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.bigDecimal(name: String): BigDecimal = requireValue(name).asBigDecimal()

/**
 * 맵에서 [name] 키 값을 [BigDecimal]로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asBigDecimalOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("price" to "12.34")
 * val price = source.bigDecimalOrNull("price")
 * // price?.toPlainString() == "12.34"
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.bigDecimalOrNull(name: String): BigDecimal? = this[name].asBigDecimalOrNull()

/**
 * 맵에서 [name] 키 값을 `String`으로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asString] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("name" to "debop")
 * val name = source.string("name")
 * // name == "debop"
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.string(name: String): String = requireValue(name).asString()

/**
 * 맵에서 [name] 키 값을 `String`으로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asStringOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("name" to "debop")
 * val name = source.stringOrNull("name")
 * // name == "debop"
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.stringOrNull(name: String): String? = this[name].asStringOrNull()

/**
 * 맵에서 [name] 키 값을 `ByteArray`로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asByteArray] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 배열을 반환할 수 있습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("payload" to "abc")
 * val payload = source.byteArray("payload")
 * // payload.isNotEmpty()
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.byteArray(name: String): ByteArray = requireValue(name).asByteArray()

/**
 * 맵에서 [name] 키 값을 `ByteArray`로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asByteArrayOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 변환 결과 배열은 새로 할당될 수 있습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("payload" to "abc")
 * val payload = source.byteArrayOrNull("payload")
 * // payload != null
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.byteArrayOrNull(name: String): ByteArray? = this[name].asByteArrayOrNull()

/**
 * 맵에서 [name] 키 값을 [Date]로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asDate] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [Date] 객체가 생성됩니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("createdAt" to Date())
 * val createdAt = source.date("createdAt")
 * // createdAt.time > 0L
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.date(name: String): Date = requireValue(name).asDate()

/**
 * 맵에서 [name] 키 값을 [Date]로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asDateOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [Date] 객체가 생성됩니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("createdAt" to Date())
 * val createdAt = source.dateOrNull("createdAt")
 * // createdAt != null
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.dateOrNull(name: String): Date? = this[name].asDateOrNull()

/**
 * 맵에서 [name] 키 값을 [Timestamp]로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asTimestamp] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [Timestamp] 객체가 생성됩니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("ts" to Timestamp(System.currentTimeMillis()))
 * val ts = source.timestamp("ts")
 * // ts.time > 0L
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.timestamp(name: String): Timestamp = requireValue(name).asTimestamp()

/**
 * 맵에서 [name] 키 값을 [Timestamp]로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asTimestampOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [Timestamp] 객체가 생성됩니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("ts" to Timestamp(System.currentTimeMillis()))
 * val ts = source.timestampOrNull("ts")
 * // ts != null
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.timestampOrNull(name: String): Timestamp? = this[name].asTimestampOrNull()

/**
 * 맵에서 [name] 키 값을 [Instant]로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asInstant] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [Instant] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("when" to Instant.now())
 * val whenInstant = source.instant("when")
 * // !whenInstant.isBefore(Instant.EPOCH)
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.instant(name: String): Instant = requireValue(name).asInstant()

/**
 * 맵에서 [name] 키 값을 [Instant]로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asInstantOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [Instant] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("when" to Instant.now())
 * val whenInstant = source.instantOrNull("when")
 * // whenInstant != null
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.instantOrNull(name: String): Instant? = this[name].asInstantOrNull()

/**
 * 맵에서 [name] 키 값을 [LocalDate]로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asLocalDate] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [LocalDate] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("birthday" to LocalDate.of(2000, 1, 1))
 * val birthday = source.localDate("birthday")
 * // birthday.year == 2000
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.localDate(name: String): LocalDate = requireValue(name).asLocalDate()

/**
 * 맵에서 [name] 키 값을 [LocalDate]로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asLocalDateOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [LocalDate] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("birthday" to LocalDate.of(2000, 1, 1))
 * val birthday = source.localDateOrNull("birthday")
 * // birthday?.year == 2000
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.localDateOrNull(name: String): LocalDate? = this[name].asLocalDateOrNull()

/**
 * 맵에서 [name] 키 값을 [LocalTime]으로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asLocalTime] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [LocalTime] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("openAt" to LocalTime.of(9, 0))
 * val openAt = source.localTime("openAt")
 * // openAt.hour == 9
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.localTime(name: String): LocalTime = requireValue(name).asLocalTime()

/**
 * 맵에서 [name] 키 값을 [LocalTime]으로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asLocalTimeOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [LocalTime] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("openAt" to LocalTime.of(9, 0))
 * val openAt = source.localTimeOrNull("openAt")
 * // openAt?.hour == 9
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.localTimeOrNull(name: String): LocalTime? = this[name].asLocalTimeOrNull()

/**
 * 맵에서 [name] 키 값을 [LocalDateTime]으로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asLocalDateTime] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [LocalDateTime] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("scheduledAt" to LocalDateTime.of(2026, 3, 2, 10, 30))
 * val scheduledAt = source.localDateTime("scheduledAt")
 * // scheduledAt.year == 2026
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.localDateTime(name: String): LocalDateTime = requireValue(name).asLocalDateTime()

/**
 * 맵에서 [name] 키 값을 [LocalDateTime]으로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asLocalDateTimeOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [LocalDateTime] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("scheduledAt" to LocalDateTime.of(2026, 3, 2, 10, 30))
 * val scheduledAt = source.localDateTimeOrNull("scheduledAt")
 * // scheduledAt?.year == 2026
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.localDateTimeOrNull(name: String): LocalDateTime? = this[name].asLocalDateTimeOrNull()

/**
 * 맵에서 [name] 키 값을 [OffsetDateTime]으로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asOffsetDateTime] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [OffsetDateTime] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("createdAt" to OffsetDateTime.now())
 * val createdAt = source.offsetDateTime("createdAt")
 * // createdAt.offset.totalSeconds % 60 == 0
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.offsetDateTime(name: String): OffsetDateTime = requireValue(name).asOffsetDateTime()

/**
 * 맵에서 [name] 키 값을 [OffsetDateTime]으로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asOffsetDateTimeOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 필요 시 새 [OffsetDateTime] 객체를 반환합니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val source = mapOf("createdAt" to OffsetDateTime.now())
 * val createdAt = source.offsetDateTimeOrNull("createdAt")
 * // createdAt != null
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.offsetDateTimeOrNull(name: String): OffsetDateTime? = this[name].asOffsetDateTimeOrNull()

/**
 * 맵에서 [name] 키 값을 [UUID]로 변환해 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 [IllegalArgumentException]이 발생합니다.
 * - 값 변환은 [asUUID] 규칙을 따르며, 변환 불가 시 해당 함수의 예외를 전파합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val id = UUID.randomUUID()
 * val source = mapOf("id" to id.toString())
 * // source.uuid("id") == id
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.uuid(name: String): UUID = requireValue(name).asUUID()

/**
 * 맵에서 [name] 키 값을 [UUID]로 변환 가능하면 반환하고, 불가능하면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없거나 값이 null이면 null을 반환합니다.
 * - 값 변환은 [asUUIDOrNull] 규칙을 따르며, 변환 실패 시 null을 반환합니다.
 * - 수신 맵은 변경하지 않으며 새 컬렉션도 할당하지 않습니다.
 * - 조회 비용은 일반적인 해시 맵 기준으로 평균 `O(1)`입니다.
 *
 * ```kotlin
 * val id = UUID.randomUUID()
 * val source = mapOf("id" to id.toString())
 * // source.uuidOrNull("id") == id
 * ```
 *
 * @param name 조회할 키 이름
 */
fun Map<String, Any?>.uuidOrNull(name: String): UUID? = this[name].asUUIDOrNull()
