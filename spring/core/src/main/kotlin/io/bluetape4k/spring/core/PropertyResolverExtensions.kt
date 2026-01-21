package io.bluetape4k.spring.core

import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.getProperty
import org.springframework.core.env.getRequiredProperty
import kotlin.reflect.KClass

/**
 * 지정한 [key]에 해당하는 속성 값을 반환합니다. [key]에 해당하는 값이 없는 경우에는 defaultValue를 반환합니다.
 *
 * ```
 * val testProperties = Properties()
 * val propertySources = MutablePropertySources().apply {
 *      addFirst(PropertiesPropertySource("testProperties", testProperties))
 * }
 * val propertyResolver = PropertySourcesPropertyResolver(propertySources)
 *
 * testProperties["foo"] = "bar"
 * testProperties["num"] = 5
 *
 * propertyResolver["foo"] shouldBeEqualTo "bar"
 * propertyResolver["num", Int::class, 1] shouldBeEqualTo 5
 * ```
 *
 * @receiver PropertyResolver
 * @param key 속성 값을 얻기 위한 속성 명
 */
operator fun PropertyResolver.get(key: String): String? = getProperty(key)

/**
 * 지정한 [key]에 해당하는 속성 값을 반환합니다. [key]에 해당하는 값이 없는 경우에는 defaultValue를 반환합니다.
 *
 * ```
 * propertyResolver["not-exists", "myDefault"] // "myDefault"
 * ```
 *
 * @receiver PropertyResolver
 * @param key 속성 값을 얻기 위한 속성 명
 * @param defaultValue 값이 없는 경우에 반환할 기본 값
 */
operator fun PropertyResolver.get(key: String, defaultValue: String): String =
    getProperty(key, defaultValue)

/**
 * 지정한 [key]에 해당하는 속성 값을 반환합니다.
 *
 * ```
 * propertyResolver["num", Int::class] // 5
 * ```
 *
 * @receiver PropertyResolver
 * @param key the property name to resolve
 * @param targetType the expected type of the property value
 * @return T? 속성 값, 없는 경우에는 null을 반환
 */
operator fun <T: Any> PropertyResolver.get(key: String, targetType: KClass<T>): T? =
    getProperty(key, targetType.java)

/**
 * 지정한 [key]에 해당하는 속성 값을 반환합니다.
 *
 * ```
 * propertyResolver["num", Int::class] // 5
 * ```
 * @typeparam T 속성 값의 수형
 * @receiver PropertyResolver
 * @param key the property name to resolve
 * @return T? 속성 값, 없는 경우에는 null을 반환
 */
inline fun <reified T: Any> PropertyResolver.getAs(key: String): T? {
    return getProperty<T>(key)
}


/**
 * 지정한 [key]에 해당하는 속성 값을 반환합니다. [key]에 해당하는 값이 없는 경우에는 defaultValue를 반환합니다.
 *
 * ```
 * propertyResolver["exists", Boolean::class, false] // true
 * propertyResolver["not-exists", Boolean::class, false] // false
 * ```
 *
 * @receiver PropertyResolver
 * @param key the property name to resolve
 * @param targetType the expected type of the property value
 * @param defaultValue 기본 값
 * @return T 속성 값의 수형
 */
operator fun <T: Any> PropertyResolver.get(key: String, targetType: KClass<T>, defaultValue: T): T {
    return getProperty(key, targetType.java, defaultValue)
}

/**
 * 지정한 [key]에 해당하는 속성 값을 반환합니다.
 *
 * ```
 * propertyResolver["exists", Int::class, 42] // 5
 * propertyResolver["not-exists", Int::class, 42] // 42
 * ```
 * @typeparam T 속성 값의 수형
 * @receiver PropertyResolver
 * @param key the property name to resolve
 * @return T? 속성 값, 없는 경우에는 null을 반환
 */
inline fun <reified T: Any> PropertyResolver.getAs(key: String, defaultValue: T): T {
    return getProperty<T>(key, defaultValue)
}


/**
 * [key]에 해당하는 속성 값을 [targetType]으로 변환하여 반환합니다. 속성 값이 없는 경우에는 IllegalStateException을 발생합니다.
 *
 * ```
 * propertyResolver.getRequiredProperty("num", Int::class) // 5
 * propertyResolver.getRequiredProperty("num", Long::class) // 5L
 * ```
 *
 * @throws IllegalStateException if the given key cannot be resolved
 *
 * @receiver PropertyResolver
 * @param key the property name to resolve
 * @param targetType the expected type of the property value
 * @return T 속성 값의 수형
 */
@Throws(IllegalStateException::class)
fun <T: Any> PropertyResolver.getRequiredProperty(key: String, targetType: KClass<T>): T {
    return getRequiredProperty(key, targetType.java)
}

/**
 * [key]에 해당하는 속성 값을 [T]로 변환하여 반환합니다. 속성 값이 없는 경우에는 IllegalStateException을 발생합니다.
 *
 * ```
 * propertyResolver.getRequiredPropertyAs<Int>("num") // 5
 * propertyResolver.getRequiredPropertyAs<Long>("num") // 5L
 * ```
 *
 * @throws IllegalStateException if the given key cannot be resolved
 *
 * @receiver PropertyResolver
 * @param key the property name to resolve
 * @return T 속성 값의 수형
 */
inline fun <reified T: Any> PropertyResolver.getRequiredPropertyAs(key: String): T {
    return getRequiredProperty<T>(key)
}
