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
 */
inline fun JsonGenerator.writeValue(fieldName: String, writeValueAction: JsonGenerator.() -> Unit) {
    fieldName.requireNotBlank("fieldName")

    writeValue {
        writeName(fieldName)
        writeValueAction()
    }
}

/**
 * [fieldName]을 키로 하여 [value]를 추가합니다.
 */
fun JsonGenerator.writeValue(fieldName: String, value: Any?) {
    writeValue(fieldName) {
        value?.let { writeString(it.toString()) } ?: writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 null 값을 추가합니다.
 */
fun JsonGenerator.writeNull(fieldName: String) {
    fieldName.requireNotBlank("fieldName")

    writeValue {
        writeNull(fieldName)
    }
}

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
 */
fun JsonGenerator.writeString(fieldName: String, value: String?) {
    fieldName.requireNotBlank("fieldName")

    writeValue(fieldName) {
        value?.let { writeString(it) } ?: writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 숫자 [value]를 추가합니다.
 */
fun JsonGenerator.writeNumber(fieldName: String, value: Number) {
    fieldName.requireNotBlank("fieldName")

    writeValue(fieldName) {
        writeNumber(value.toLong())
    }
}

/**
 * [fieldName]을 키로 하여 불리언 [value]를 추가합니다.
 */
fun JsonGenerator.writeBoolean(fieldName: String, value: Boolean) {
    fieldName.requireNotBlank("fieldName")

    writeValue(fieldName) {
        writeBoolean(value)
    }
}

/**
 * [writeArrayAction]을 통해 배열을 추가합니다.
 */
inline fun JsonGenerator.writeArray(writeArrayAction: JsonGenerator.() -> Unit) {
    writeStartArray()
    writeArrayAction()
    writeEndArray()
}

/**
 * [items]를 배열로 추가합니다.
 */
fun <T: Any> JsonGenerator.writeObjects(items: Iterable<T>) {
    writeArray {
        items.forEach {
            writePOJO(it)
        }
    }
}

/**
 * [items]를 POJO 배열로 직렬화합니다.
 */
fun <T: Any> JsonGenerator.writePOJOs(items: Iterable<T>) {
    writeArray {
        items.forEach {
            writePOJO(it)
        }
    }
}
