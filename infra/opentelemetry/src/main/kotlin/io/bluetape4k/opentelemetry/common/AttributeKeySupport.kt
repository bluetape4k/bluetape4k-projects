package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey

/**
 * [String] 타입의 [AttributeKey]를 생성합니다.
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun stringAttributeKeyOf(key: String): AttributeKey<String> = AttributeKey.stringKey(key)

/**
 * [String] 배열 타입의 [AttributeKey]를 생성합니다.
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun stringArrayAttributeKeyOf(vararg keys: String): AttributeKey<List<String>> =
    AttributeKey.stringArrayKey(keys.joinToString(","))

/**
 * [Boolean] 타입의 [AttributeKey]를 생성합니다.
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun booleanAttributeKeyOf(key: String): AttributeKey<Boolean> = AttributeKey.booleanKey(key)

/**
 * [Boolean] 배열 타입의 [AttributeKey]를 생성합니다.
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun booleanArrayAttributeKeyOf(vararg keys: String): AttributeKey<List<Boolean>> =
    AttributeKey.booleanArrayKey(keys.joinToString(","))

/**
 * [Long] 타입의 [AttributeKey]를 생성합니다.
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun longAttributeKeyOf(key: String): AttributeKey<Long> = AttributeKey.longKey(key)

/**
 * [Long] 배열 타입의 [AttributeKey]를 생성합니다.
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun longArrayAttributeKeyOf(vararg keys: String): AttributeKey<MutableList<Long>> =
    AttributeKey.longArrayKey(keys.joinToString(","))

/**
 * [Double] 타입의 [AttributeKey]를 생성합니다.
 * @param key 속성 키
 * @return [AttributeKey] 인스턴스
 */
fun doubleAttributeKeyOf(key: String): AttributeKey<Double> = AttributeKey.doubleKey(key)

/**
 * [Double] 배열 타입의 [AttributeKey]를 생성합니다.
 * @param keys 속성 키 배열
 * @return [AttributeKey] 인스턴스
 */
fun doubleArrayAttributeKeyOf(vararg keys: String): AttributeKey<MutableList<Double>> =
    AttributeKey.doubleArrayKey(keys.joinToString(","))

/**
 * [String]을 [AttributeKey]로 변환합니다.
 */
fun String.toAttributeKey(): AttributeKey<String> = AttributeKey.stringKey(this)

/**
 * [String]을 [String] 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toStringAttributeKey(): AttributeKey<String> = AttributeKey.stringKey(this)

/**
 * [String]을 [String] 배열 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toStringArrayAttributeKey(): AttributeKey<List<String>> = AttributeKey.stringArrayKey(this)

/**
 * [String] 배열을 [AttributeKey]로 변환합니다.
 */
fun Array<String>.toAttributeKey(): AttributeKey<List<String>> =
    AttributeKey.stringArrayKey(this.joinToString(","))

/**
 * [String]을 [Boolean] 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toBooleanAttributeKey(): AttributeKey<Boolean> = AttributeKey.booleanKey(this)

/**
 * [String]을 [Boolean] 배열 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toBooleanArrayAttributeKey(): AttributeKey<List<Boolean>> = AttributeKey.booleanArrayKey(this)

/**
 * [String]을 [Long] 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toLongAttributeKey(): AttributeKey<Long> = AttributeKey.longKey(this)

/**
 * [String]을 [Long] 배열 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toLongArrayAttributeKey(): AttributeKey<List<Long>> = AttributeKey.longArrayKey(this)

/**
 * [String]을 [Double] 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toDoubleAttributeKey(): AttributeKey<Double> = AttributeKey.doubleKey(this)

/**
 * [String]을 [Double] 배열 타입의 [AttributeKey]로 변환합니다.
 */
fun String.toDoubleArrayAttributeKey(): AttributeKey<List<Double>> = AttributeKey.doubleArrayKey(this)

/**
 * [Boolean]을 [AttributeKey]로 변환합니다.
 */
fun Boolean.toAttributeKey(): AttributeKey<Boolean> = AttributeKey.booleanKey(this.toString())

/**
 * [Boolean] 배열을 [AttributeKey]로 변환합니다.
 */
fun BooleanArray.toAttributeKey(): AttributeKey<List<Boolean>> = AttributeKey.booleanArrayKey(this.joinToString(","))

/**
 * [Long]을 [AttributeKey]로 변환합니다.
 */
fun Long.toAttributeKey(): AttributeKey<Long> = AttributeKey.longKey(this.toString())

/**
 * [Long] 배열을 [AttributeKey]로 변환합니다.
 */
fun LongArray.toAttributeKey(): AttributeKey<List<Long>> = AttributeKey.longArrayKey(this.joinToString(","))

/**
 * [Double]을 [AttributeKey]로 변환합니다.
 */
fun Double.toAttributeKey(): AttributeKey<Double> = AttributeKey.doubleKey(this.toString())

/**
 * [Double] 배열을 [AttributeKey]로 변환합니다.
 */
fun DoubleArray.toAttributeKey(): AttributeKey<List<Double>> = AttributeKey.doubleArrayKey(this.joinToString(","))
