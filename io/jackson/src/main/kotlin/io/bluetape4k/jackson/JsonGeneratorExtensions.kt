package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonGenerator
import io.bluetape4k.support.requireNotBlank
import java.math.BigDecimal
import java.math.BigInteger

/**
 * 현재 위치에 JSON 객체를 시작/종료하며 본문을 작성합니다.
 *
 * ## 동작/계약
 * - `writeStartObject()`와 `writeEndObject()`를 자동으로 호출합니다.
 * - [valueWriter] 블록 안에서 필드 작성을 수행해야 합니다.
 *
 * ```kotlin
 * generator.writeValue { writeStringField("name", "debop") }
 * // {"name":"debop"}
 * ```
 */
inline fun JsonGenerator.writeValue(valueWriter: JsonGenerator.() -> Unit) = apply {
    writeStartObject()
    valueWriter()
    writeEndObject()
}

/**
 * 지정한 필드 이름으로 중첩 객체를 작성합니다.
 *
 * ## 동작/계약
 * - [fieldName]이 blank면 [IllegalArgumentException]이 발생합니다.
 * - 필드 이름을 쓴 뒤 내부 객체 작성 블록을 실행합니다.
 *
 * ```kotlin
 * generator.writeField("user") { writeStringField("name", "debop") }
 * // {"user":{"name":"debop"}}
 * ```
 * @param fieldName 필드 이름. blank면 [IllegalArgumentException]이 발생합니다.
 */
inline fun JsonGenerator.writeField(fieldName: String, valueWriter: JsonGenerator.() -> Unit) = apply {
    fieldName.requireNotBlank("fieldName")

    writeValue {
        writeFieldName(fieldName)
        valueWriter()
    }
}

/**
 * 지정한 필드 이름으로 문자열 표현 값을 작성합니다.
 *
 * ## 동작/계약
 * - [value]가 null이면 JSON null을 씁니다.
 * - null이 아니면 `toString()` 결과를 문자열 필드로 씁니다.
 *
 * ```kotlin
 * generator.writeValue("age", 10)
 * // {"age":"10"}
 * ```
 */
fun JsonGenerator.writeValue(fieldName: String, value: Any?) = apply {
    writeField(fieldName) {
        value?.let { writeString(it.toString()) } ?: writeNull()
    }
}

/**
 * 지정한 필드 이름으로 JSON null 값을 작성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [writeField]를 사용합니다.
 *
 * ```kotlin
 * generator.writeNull("deletedAt")
 * // {"deletedAt":null}
 * ```
 */
fun JsonGenerator.writeNull(fieldName: String) = apply {
    writeField(fieldName) {
        writeNull()
    }
}

/**
 * 지정한 필드 이름으로 문자열 값을 작성합니다.
 *
 * ## 동작/계약
 * - [value]가 null이면 JSON null을 씁니다.
 * - null이 아니면 문자열 필드를 씁니다.
 *
 * ```kotlin
 * generator.writeString("name", "debop")
 * // {"name":"debop"}
 * ```
 */
fun JsonGenerator.writeString(fieldName: String, value: String?) = apply {
    writeField(fieldName) {
        value?.let { writeString(it) } ?: writeNull()
    }
}

/**
 * 지정한 필드 이름으로 숫자 값을 작성합니다.
 *
 * ## 동작/계약
 * - [fieldName]이 blank면 [IllegalArgumentException]이 발생합니다.
 * - 기본 숫자 타입은 전용 `writeNumber` 오버로드로 처리합니다.
 * - 미지원 `Number` 구현은 `writeObject`로 위임합니다.
 *
 * ```kotlin
 * generator.writeNumber("count", 3)
 * // {"count":3}
 * ```
 * @param fieldName 필드 이름. blank면 [IllegalArgumentException]이 발생합니다.
 */
fun JsonGenerator.writeNumber(fieldName: String, value: Number) = apply {
    fieldName.requireNotBlank("fieldName")

    writeField(fieldName) {
        when (value) {
            is Byte -> writeNumber(value.toInt())
            is Short -> writeNumber(value)
            is Int -> writeNumber(value)
            is Long -> writeNumber(value)
            is Float -> writeNumber(value)
            is Double -> writeNumber(value)
            is BigDecimal -> writeNumber(value)
            is BigInteger -> writeNumber(value)
            else -> writeObject(value)
        }
    }
}

/**
 * 지정한 필드 이름으로 불리언 값을 작성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [writeField]를 사용해 필드를 씁니다.
 *
 * ```kotlin
 * generator.writeBoolean("active", true)
 * // {"active":true}
 * ```
 */
fun JsonGenerator.writeBoolean(fieldName: String, value: Boolean) = apply {
    writeField(fieldName) {
        writeBoolean(value)
    }
}

/**
 * 현재 위치에 JSON 배열을 시작/종료하며 본문을 작성합니다.
 *
 * ## 동작/계약
 * - `writeStartArray()`와 `writeEndArray()`를 자동 호출합니다.
 *
 * ```kotlin
 * generator.writeArray { writeNumber(1); writeNumber(2) }
 * // [1,2]
 * ```
 */
inline fun JsonGenerator.writeArray(arrayWriter: JsonGenerator.() -> Unit) = apply {
    writeStartArray()
    arrayWriter()
    writeEndArray()
}

/**
 * 주어진 컬렉션 항목을 JSON 배열로 작성합니다.
 *
 * ## 동작/계약
 * - 각 요소를 [writeObject]로 순서대로 기록합니다.
 * - 입력 컬렉션은 변경하지 않습니다.
 *
 * ```kotlin
 * generator.writeObjects(listOf(1, 2, 3))
 * // [1,2,3]
 * ```
 */
fun <T> JsonGenerator.writeObjects(items: Iterable<T>) = apply {
    writeArray {
        items.forEach(::writeObject)
    }
}
