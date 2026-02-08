package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonGenerator
import io.bluetape4k.support.requireNotBlank
import java.math.BigDecimal
import java.math.BigInteger

/**
 * [valueWriter]을 통해 객체를 추가합니다.
 */
inline fun JsonGenerator.writeValue(valueWriter: JsonGenerator.() -> Unit) = apply {
    writeStartObject()
    valueWriter()
    writeEndObject()
}

/**
 * [fieldName]을 키로 하여 [valueWriter]을 통해 객체를 추가합니다.
 */
inline fun JsonGenerator.writeField(fieldName: String, valueWriter: JsonGenerator.() -> Unit) = apply {
    fieldName.requireNotBlank("fieldName")

    writeValue {
        writeFieldName(fieldName)
        valueWriter()
    }
}

/**
 * [fieldName]을 키로 하여 [value]를 추가합니다.
 */
fun JsonGenerator.writeValue(fieldName: String, value: Any?) = apply {
    writeField(fieldName) {
        value?.let { writeString(it.toString()) } ?: writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 null 값을 추가합니다.
 */
fun JsonGenerator.writeNull(fieldName: String) = apply {
    writeField(fieldName) {
        writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 문자열 [value]를 추가합니다.
 */
fun JsonGenerator.writeString(fieldName: String, value: String?) = apply {
    writeField(fieldName) {
        value?.let { writeString(it) } ?: writeNull()
    }
}

/**
 * [fieldName]을 키로 하여 숫자 [value]를 추가합니다.
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
 * [fieldName]을 키로 하여 불리언 [value]를 추가합니다.
 */
fun JsonGenerator.writeBoolean(fieldName: String, value: Boolean) = apply {
    writeField(fieldName) {
        writeBoolean(value)
    }
}

/**
 * [arrayWriter]을 통해 배열을 추가합니다.
 */
inline fun JsonGenerator.writeArray(arrayWriter: JsonGenerator.() -> Unit) = apply {
    writeStartArray()
    arrayWriter()
    writeEndArray()
}

/**
 * [items]를 배열로 추가합니다.
 */
fun <T> JsonGenerator.writeObjects(items: Iterable<T>) = apply {
    writeArray {
        items.forEach(::writeObject)
    }
}
