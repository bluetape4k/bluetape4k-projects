package io.bluetape4k.spring.core

import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.getProperty
import org.springframework.core.env.getRequiredProperty
import kotlin.reflect.KClass

/**
 * 지정한 키의 문자열 속성 값을 조회합니다.
 *
 * ## 동작/계약
 * - 키가 존재하면 문자열 값을 반환하고, 없으면 `null`을 반환합니다.
 * - Spring [PropertyResolver.getProperty] 호출을 그대로 위임합니다.
 *
 * ```kotlin
 * val foo = propertyResolver["foo"]
 * // foo == "bar"
 * ```
 */
operator fun PropertyResolver.get(key: String): String? = getProperty(key)

/**
 * 지정한 키의 문자열 속성 값을 조회하고 없으면 기본값을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 [defaultValue]를 반환합니다.
 * - 키가 있으면 변환된 문자열 값을 반환합니다.
 *
 * ```kotlin
 * val value = propertyResolver["foo", "myDefault"]
 * // value == "myDefault"
 * ```
 */
operator fun PropertyResolver.get(key: String, defaultValue: String): String =
    getProperty(key, defaultValue)

/**
 * 지정한 키의 속성 값을 [targetType]으로 조회합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 `null`을 반환합니다.
 * - 키가 있으면 [targetType]에 맞게 변환된 값을 반환합니다.
 *
 * ```kotlin
 * val num = propertyResolver["num", Int::class]
 * // num == 5
 * ```
 */
operator fun <T: Any> PropertyResolver.get(key: String, targetType: KClass<T>): T? =
    getProperty(key, targetType.java)

/**
 * 지정한 키의 속성 값을 reified 타입으로 조회합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 `null`을 반환합니다.
 * - 제네릭 [T]의 런타임 타입으로 변환을 시도합니다.
 *
 * ```kotlin
 * val enabled = propertyResolver.getAs<Boolean>("enabled")
 * // enabled == true
 * ```
 */
inline fun <reified T: Any> PropertyResolver.getAs(key: String): T? {
    return getProperty<T>(key)
}

/**
 * 지정한 키의 속성 값을 [targetType]으로 조회하고 없으면 기본값을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 [defaultValue]를 반환합니다.
 * - 키가 있으면 [targetType]으로 변환한 값을 반환합니다.
 *
 * ```kotlin
 * val enabled = propertyResolver["enabled", Boolean::class, false]
 * // enabled == true
 * ```
 */
operator fun <T: Any> PropertyResolver.get(key: String, targetType: KClass<T>, defaultValue: T): T {
    return getProperty(key, targetType.java, defaultValue)
}

/**
 * 지정한 키의 속성 값을 reified 타입으로 조회하고 없으면 기본값을 반환합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 [defaultValue]를 반환합니다.
 * - 키가 있으면 제네릭 [T] 타입으로 변환된 값을 반환합니다.
 *
 * ```kotlin
 * val enabled = propertyResolver.getAs("enabled", false)
 * // enabled == true
 * ```
 */
inline fun <reified T: Any> PropertyResolver.getAs(key: String, defaultValue: T): T {
    return getProperty<T>(key, defaultValue)
}

/**
 * 지정한 키의 속성 값을 [targetType]으로 필수 조회합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 [IllegalStateException]을 던집니다.
 * - 키가 있으면 [targetType]으로 변환된 값을 반환합니다.
 *
 * ```kotlin
 * val required = propertyResolver.getRequiredProperty("required.key", String::class)
 * // required == "required.value"
 * ```
 */
@Throws(IllegalStateException::class)
fun <T: Any> PropertyResolver.getRequiredProperty(key: String, targetType: KClass<T>): T {
    return getRequiredProperty(key, targetType.java)
}

/**
 * 지정한 키의 속성 값을 reified 타입으로 필수 조회합니다.
 *
 * ## 동작/계약
 * - 키가 없으면 [IllegalStateException]을 던집니다.
 * - 키가 있으면 제네릭 [T] 타입으로 변환된 값을 반환합니다.
 *
 * ```kotlin
 * val required = propertyResolver.getRequiredPropertyAs<String>("required.key")
 * // required == "required.value"
 * ```
 */
inline fun <reified T: Any> PropertyResolver.getRequiredPropertyAs(key: String): T {
    return getRequiredProperty<T>(key)
}
