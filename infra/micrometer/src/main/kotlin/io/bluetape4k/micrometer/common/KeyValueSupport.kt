package io.bluetape4k.micrometer.common

import io.bluetape4k.support.requireNotBlank
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues

/**
 * 키와 값으로 [KeyValue]를 생성합니다.
 *
 * ```kotlin
 * val kv = keyValueOf("key1", "value1")
 * ```
 *
 * @param key 키 이름 (비어있을 수 없음)
 * @param value 값
 * @return 생성된 [KeyValue] 인스턴스
 * @throws IllegalArgumentException key가 비어있는 경우
 */
fun keyValueOf(
    key: String,
    value: String,
): KeyValue {
    key.requireNotBlank("key")
    return KeyValue.of(key, value)
}

/**
 * 값 검증과 함께 [KeyValue]를 생성합니다.
 *
 * ```kotlin
 * val kv = keyValueOf("key1", 123) { it > 100 }
 * ```
 *
 * @param T 값의 타입
 * @param key 키 이름 (비어있을 수 없음)
 * @param value 값
 * @param valueValidator 값을 검증하는 함수
 * @return 생성된 [KeyValue] 인스턴스
 * @throws IllegalArgumentException key가 비어있거나 값 검증에 실패한 경우
 */
fun <T: Any> keyValueOf(
    key: String,
    value: T,
    valueValidator: (T) -> Boolean,
): KeyValue {
    key.requireNotBlank("key")
    return KeyValue.of(key, value, valueValidator)
}

/**
 * 가변 인자로 [KeyValues]를 생성합니다.
 *
 * ```kotlin
 * val kvs = keyValuesOf("key1", "value1", "key2", "value2")
 * ```
 *
 * @param keyValues 키-값 쌍의 가변 인자 (짝수 개수여야 함)
 * @return 생성된 [KeyValues] 인스턴스
 */
fun keyValuesOf(vararg keyValues: String): KeyValues = KeyValues.of(*keyValues)

/**
 * [KeyValue] 컬렉션으로 [KeyValues]를 생성합니다.
 *
 * @param keyValues KeyValue 컬렉션
 * @return 생성된 [KeyValues] 인스턴스
 */
fun keyValueOf(keyValues: Iterable<KeyValue>): KeyValues = KeyValues.of(keyValues)

/**
 * 가변 인자 [KeyValue]로 [KeyValues]를 생성합니다.
 *
 * @param keyValues KeyValue 가변 인자
 * @return 생성된 [KeyValues] 인스턴스
 */
fun keyValuesOf(vararg keyValues: KeyValue): KeyValues = KeyValues.of(*keyValues)

/**
 * Pair 배열로 [KeyValues]를 생성합니다.
 *
 * ```kotlin
 * val kvs = keyValuesOf("key1" to "value1", "key2" to "value2")
 * ```
 *
 * @param keyValues 키-값 쌍의 가변 인자
 * @return 생성된 [KeyValues] 인스턴스
 */
fun keyValuesOf(vararg keyValues: Pair<String, String>): KeyValues =
    keyValueOf(keyValues.associate { it.first to it.second })

/**
 * Map으로 [KeyValues]를 생성합니다.
 *
 * ```kotlin
 * val kvs = keyValueOf(mapOf("key1" to "value1", "key2" to "value2"))
 * ```
 *
 * @param keyValues 키-값 쌍의 Map
 * @return 생성된 [KeyValues] 인스턴스
 */
fun keyValueOf(keyValues: Map<String, String>): KeyValues = keyValueOf(keyValues.map { (k, v) -> KeyValue.of(k, v) })
