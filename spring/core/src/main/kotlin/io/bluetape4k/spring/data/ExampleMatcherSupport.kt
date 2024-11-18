package io.bluetape4k.spring.data

import org.springframework.data.domain.ExampleMatcher
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

/**
 * 지정한 수형의 Example matching 을 위한 [ExampleMatcher]를 구성합니다.
 *
 * 참고: Redis 에서는 [GenericPropertyMatchers.exact()] 만 지원한다.
 *
 * ```
 * val exampleMatcher = User::class.buildExampleMatcher("name", "age")
 * ```
 *
 * @param T Example 수형
 * @param searchFields Example 을 이용하여 검색하고자 하는 속성 명
 * @return [ExampleMatcher] 인스턴스
 */
inline fun <reified T: Any> KClass<T>.buildExampleMatcher(vararg searchFields: String): ExampleMatcher {
    var matcher = ExampleMatcher
        .matching()
        .withIgnorePaths(*ignoredProperties(*searchFields))

    searchFields
        .filterNot { it.isEmpty() }
        .forEach {
            matcher = matcher.withMatcher(it, ExampleMatcher.GenericPropertyMatchers.exact())
        }

    return matcher
}

/**
 * 지정한 수형의 Example matching 을 위한 [ExampleMatcher]를 구성합니다.
 *
 * Redis 에서는 [GenericPropertyMatchers.exact()] 만 지원한다.
 *
 * ```
 * val exampleMatcher = User::class.buildExampleMatcher(User::name, User::age)
 * ```
 *
 * @param T Example 수형
 * @param searchFields Example 을 이용하여 검색하고자 하는 속성 명
 * @return [ExampleMatcher] 인스턴스
 */
inline fun <reified T: Any> KClass<T>.buildExampleMatcher(vararg searchFields: KProperty<*>): ExampleMatcher {
    return buildExampleMatcher(*searchFields.map { it.name }.toTypedArray())
}

/**
 * [ExampleMatcher]의 ignorePaths를 구성하기 위한 속성 명을 반환한다.
 *
 * @param T
 * @param exclusions 무시해야 할 속성에서 제외할 속성들. 즉 Example에 포함되어야 할 속성 명 (예: "lastname")
 * @return [ExampleMatcher]에서 제외할 속성 명 배열
 */
fun <T: Any> KClass<T>.ignoredProperties(vararg exclusions: String): Array<String> =
    declaredMemberProperties
        .filterNot { exclusions.contains(it.name) }
        .map { it.name }
        .toTypedArray()

/**
 * [ExampleMatcher]의 ignorePaths를 구성하기 위한 속성 명을 반환한다.
 *
 * @param T
 * @param exclusions 무시해야 할 속성에서 제외할 속성들. 즉 Example에 포함되어야 할 속성 명 (예: "lastname")
 * @return [ExampleMatcher]에서 제외할 속성 명 배열
 */
internal fun <T: Any> KClass<T>.ignoredProperties(vararg exclusions: KProperty<*>): Array<String> =
    ignoredProperties(*exclusions.map { it.name }.toTypedArray())
