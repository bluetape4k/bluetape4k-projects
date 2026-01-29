package io.bluetape4k.micrometer.common

import io.bluetape4k.support.requireNotBlank
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues

/**
 * Create a [KeyValue] from a key and a value.
 *
 * ```
 * val kv = keyValueOf("key1", "value1")
 * ```
 *
 * @param key
 * @param value
 * @return
 */
fun keyValueOf(key: String, value: String): KeyValue {
    key.requireNotBlank("key")
    return KeyValue.of(key, value)
}

/**
 * Create a [KeyValue] from key and a value with value validation.
 *
 * ```
 * val kv = keyValueOf("key1", 123) { it > 100 }
 * ```
 */
fun <T: Any> keyValueOf(key: String, value: T, valueValidator: (T) -> Boolean): KeyValue {
    key.requireNotBlank("key")
    return KeyValue.of(key, value, valueValidator)
}

/**
 * Create a [KeyValues] from a list of key-value pairs.
 *
 * ```
 * val kvs = keyValuesOf("key1", "value1", "key2", "value2")
 * ```
 *
 * @param keyValues
 * @return
 */
fun keyValuesOf(vararg keyValues: String): KeyValues = KeyValues.of(*keyValues)

fun keyValueOf(keyValues: Iterable<KeyValue>): KeyValues = KeyValues.of(keyValues)

fun keyValuesOf(vararg keyValues: KeyValue): KeyValues = KeyValues.of(*keyValues)

fun keyValuesOf(vararg keyValues: Pair<String, String>): KeyValues =
    keyValueOf(keyValues.associate { it.first to it.second })

fun keyValueOf(keyValues: Map<String, String>): KeyValues =
    keyValueOf(keyValues.map { (k, v) -> KeyValue.of(k, v) })
