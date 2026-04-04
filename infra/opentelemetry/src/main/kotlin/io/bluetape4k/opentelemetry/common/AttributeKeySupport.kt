package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey

/**
 * [String] 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = stringAttributeKeyOf("http.method")
 * // key.key == "http.method"
 * ```
 *
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun stringAttributeKeyOf(key: String): AttributeKey<String> = AttributeKey.stringKey(key)

/**
 * [String] 배열 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = stringArrayAttributeKeyOf("tag1", "tag2")
 * // key.key == "tag1,tag2"
 * ```
 *
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun stringArrayAttributeKeyOf(vararg keys: String): AttributeKey<List<String>> =
    AttributeKey.stringArrayKey(keys.joinToString(","))

/**
 * [Boolean] 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = booleanAttributeKeyOf("http.error")
 * // key.key == "http.error"
 * ```
 *
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun booleanAttributeKeyOf(key: String): AttributeKey<Boolean> = AttributeKey.booleanKey(key)

/**
 * [Boolean] 배열 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = booleanArrayAttributeKeyOf("flag1", "flag2")
 * // key.key == "flag1,flag2"
 * ```
 *
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun booleanArrayAttributeKeyOf(vararg keys: String): AttributeKey<List<Boolean>> =
    AttributeKey.booleanArrayKey(keys.joinToString(","))

/**
 * [Long] 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = longAttributeKeyOf("http.status_code")
 * // key.key == "http.status_code"
 * ```
 *
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun longAttributeKeyOf(key: String): AttributeKey<Long> = AttributeKey.longKey(key)

/**
 * [Long] 배열 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = longArrayAttributeKeyOf("id1", "id2")
 * // key.key == "id1,id2"
 * ```
 *
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun longArrayAttributeKeyOf(vararg keys: String): AttributeKey<MutableList<Long>> =
    AttributeKey.longArrayKey(keys.joinToString(","))

/**
 * [Double] 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = doubleAttributeKeyOf("latency.ms")
 * // key.key == "latency.ms"
 * ```
 *
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun doubleAttributeKeyOf(key: String): AttributeKey<Double> = AttributeKey.doubleKey(key)

/**
 * [Double] 배열 타입의 [AttributeKey]를 생성합니다.
 *
 * ```kotlin
 * val key = doubleArrayAttributeKeyOf("p50", "p99")
 * // key.key == "p50,p99"
 * ```
 *
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun doubleArrayAttributeKeyOf(vararg keys: String): AttributeKey<MutableList<Double>> =
    AttributeKey.doubleArrayKey(keys.joinToString(","))

/**
 * [String]을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "service.name".toAttributeKey()
 * // key.key == "service.name"
 * ```
 */
fun String.toAttributeKey(): AttributeKey<String> = AttributeKey.stringKey(this)

/**
 * [String]을 [String] 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "http.method".toStringAttributeKey()
 * // key.key == "http.method"
 * ```
 */
fun String.toStringAttributeKey(): AttributeKey<String> = AttributeKey.stringKey(this)

/**
 * [String]을 [String] 배열 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "tags".toStringArrayAttributeKey()
 * // key.key == "tags"
 * ```
 */
fun String.toStringArrayAttributeKey(): AttributeKey<List<String>> = AttributeKey.stringArrayKey(this)

/**
 * [String] 배열을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = arrayOf("tag1", "tag2").toAttributeKey()
 * // key.key == "tag1,tag2"
 * ```
 */
fun Array<String>.toAttributeKey(): AttributeKey<List<String>> =
    AttributeKey.stringArrayKey(this.joinToString(","))

/**
 * [String]을 [Boolean] 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "is.error".toBooleanAttributeKey()
 * // key.key == "is.error"
 * ```
 */
fun String.toBooleanAttributeKey(): AttributeKey<Boolean> = AttributeKey.booleanKey(this)

/**
 * [String]을 [Boolean] 배열 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "flags".toBooleanArrayAttributeKey()
 * // key.key == "flags"
 * ```
 */
fun String.toBooleanArrayAttributeKey(): AttributeKey<List<Boolean>> = AttributeKey.booleanArrayKey(this)

/**
 * [String]을 [Long] 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "response.size".toLongAttributeKey()
 * // key.key == "response.size"
 * ```
 */
fun String.toLongAttributeKey(): AttributeKey<Long> = AttributeKey.longKey(this)

/**
 * [String]을 [Long] 배열 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "ids".toLongArrayAttributeKey()
 * // key.key == "ids"
 * ```
 */
fun String.toLongArrayAttributeKey(): AttributeKey<List<Long>> = AttributeKey.longArrayKey(this)

/**
 * [String]을 [Double] 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "latency.ms".toDoubleAttributeKey()
 * // key.key == "latency.ms"
 * ```
 */
fun String.toDoubleAttributeKey(): AttributeKey<Double> = AttributeKey.doubleKey(this)

/**
 * [String]을 [Double] 배열 타입의 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = "percentiles".toDoubleArrayAttributeKey()
 * // key.key == "percentiles"
 * ```
 */
fun String.toDoubleArrayAttributeKey(): AttributeKey<List<Double>> = AttributeKey.doubleArrayKey(this)

/**
 * [Boolean]을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = true.toAttributeKey()
 * // key.key == "true"
 * ```
 */
fun Boolean.toAttributeKey(): AttributeKey<Boolean> = AttributeKey.booleanKey(this.toString())

/**
 * [Boolean] 배열을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = booleanArrayOf(true, false).toAttributeKey()
 * // key.key == "true,false"
 * ```
 */
fun BooleanArray.toAttributeKey(): AttributeKey<List<Boolean>> = AttributeKey.booleanArrayKey(this.joinToString(","))

/**
 * [Long]을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = 200L.toAttributeKey()
 * // key.key == "200"
 * ```
 */
fun Long.toAttributeKey(): AttributeKey<Long> = AttributeKey.longKey(this.toString())

/**
 * [Long] 배열을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = longArrayOf(1L, 2L).toAttributeKey()
 * // key.key == "1,2"
 * ```
 */
fun LongArray.toAttributeKey(): AttributeKey<List<Long>> = AttributeKey.longArrayKey(this.joinToString(","))

/**
 * [Double]을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = 3.14.toAttributeKey()
 * // key.key == "3.14"
 * ```
 */
fun Double.toAttributeKey(): AttributeKey<Double> = AttributeKey.doubleKey(this.toString())

/**
 * [Double] 배열을 [AttributeKey]로 변환합니다.
 *
 * ```kotlin
 * val key = doubleArrayOf(0.5, 0.99).toAttributeKey()
 * // key.key == "0.5,0.99"
 * ```
 */
fun DoubleArray.toAttributeKey(): AttributeKey<List<Double>> = AttributeKey.doubleArrayKey(this.joinToString(","))
