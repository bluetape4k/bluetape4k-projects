package io.bluetape4k.jackson3

import io.bluetape4k.support.requireNotBlank
import tools.jackson.core.JsonGenerator

/**
 * 현재 위치에 JSON 객체를 시작/종료하며 내용을 기록합니다.
 *
 * ## 동작/계약
 * - `writeStartObject()`와 `writeEndObject()`를 자동 호출합니다.
 * - [writeValueAction] 내부에서 필드 기록을 수행해야 합니다.
 *
 * ```kotlin
 * generator.writeValue { writeStringField("name", "debop") }
 * // {"name":"debop"}
 * ```
 */
inline fun JsonGenerator.writeValue(writeValueAction: JsonGenerator.() -> Unit) {
    writeStartObject()
    writeValueAction()
    writeEndObject()
}

/**
 * [fieldName]을 키로 하여 [writeValueAction]을 통해 객체를 추가합니다.
 *
 * ## 동작/계약
 * - [fieldName]이 blank면 [IllegalArgumentException]이 발생합니다.
 * - 필드 이름을 쓴 뒤 내부 객체 작성 블록을 실행합니다.
 *
 * ```kotlin
 * generator.writeValue("user") { writeStringField("name", "debop") }
 * // {"user":{"name":"debop"}}
 * ```
 * @param fieldName 필드 이름. blank면 [IllegalArgumentException]이 발생합니다.
 */
inline fun JsonGenerator.writeValue(fieldName: String, writeValueAction: JsonGenerator.() -> Unit) {
    fieldName.requireNotBlank("fieldName")

    writeValue {
        writeName(fieldName)
        writeValueAction()
    }
}

/**
 * [fieldName]을 키로 하여 [value]를 문자열 표현으로 추가합니다.
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
fun JsonGenerator.writeValue(fieldName: String, value: Any?) {
    writeValue(fieldName) {
        value?.let { writeString(it.toString()) } ?: writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 JSON null 값을 추가합니다.
 *
 * ## 동작/계약
 * - [fieldName]이 blank면 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * generator.writeNull("deletedAt")
 * // {"deletedAt":null}
 * ```
 */
fun JsonGenerator.writeNull(fieldName: String) {
    fieldName.requireNotBlank("fieldName")

    writeValue {
        writeNull(fieldName)
    }
}

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
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
fun JsonGenerator.writeString(fieldName: String, value: String?) {
    fieldName.requireNotBlank("fieldName")

    writeValue(fieldName) {
        value?.let { writeString(it) } ?: writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 숫자 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [fieldName]이 blank면 [IllegalArgumentException]이 발생합니다.
 * - `toLong()` 변환 후 숫자 필드로 기록합니다.
 *
 * ```kotlin
 * generator.writeNumber("count", 3)
 * // {"count":3}
 * ```
 */
fun JsonGenerator.writeNumber(fieldName: String, value: Number) {
    fieldName.requireNotBlank("fieldName")

    writeValue(fieldName) {
        writeNumber(value.toLong())
    }
}

/**
 * [fieldName]을 키로 하여 불리언 [value]를 추가합니다.
 *
 * ## 동작/계약
 * - [fieldName]이 blank면 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * generator.writeBoolean("active", true)
 * // {"active":true}
 * ```
 */
fun JsonGenerator.writeBoolean(fieldName: String, value: Boolean) {
    fieldName.requireNotBlank("fieldName")

    writeValue(fieldName) {
        writeBoolean(value)
    }
}

/**
 * [writeArrayAction]을 통해 JSON 배열을 시작/종료하며 내용을 기록합니다.
 *
 * ## 동작/계약
 * - `writeStartArray()`와 `writeEndArray()`를 자동 호출합니다.
 *
 * ```kotlin
 * generator.writeArray { writeNumber(1); writeNumber(2) }
 * // [1,2]
 * ```
 */
inline fun JsonGenerator.writeArray(writeArrayAction: JsonGenerator.() -> Unit) {
    writeStartArray()
    writeArrayAction()
    writeEndArray()
}

/**
 * 주어진 컬렉션 항목을 JSON 배열로 기록합니다.
 *
 * ## 동작/계약
 * - 각 요소를 [writePOJO]로 순서대로 기록합니다.
 * - 입력 컬렉션은 변경하지 않습니다.
 *
 * ```kotlin
 * generator.writeObjects(listOf(mapOf("id" to 1), mapOf("id" to 2)))
 * // [{"id":1},{"id":2}]
 * ```
 */
fun <T: Any> JsonGenerator.writeObjects(items: Iterable<T>) {
    writeArray {
        items.forEach {
            writePOJO(it)
        }
    }
}

/**
 * 주어진 컬렉션 항목을 POJO JSON 배열로 직렬화합니다.
 *
 * ## 동작/계약
 * - 각 요소를 [writePOJO]로 순서대로 기록합니다.
 * - [writeObjects]와 동일한 동작이며 의미를 명시적으로 표현합니다.
 *
 * ```kotlin
 * generator.writePOJOs(listOf(mapOf("id" to 1), mapOf("id" to 2)))
 * // [{"id":1},{"id":2}]
 * ```
 */
fun <T: Any> JsonGenerator.writePOJOs(items: Iterable<T>) {
    writeArray {
        items.forEach {
            writePOJO(it)
        }
    }
}
