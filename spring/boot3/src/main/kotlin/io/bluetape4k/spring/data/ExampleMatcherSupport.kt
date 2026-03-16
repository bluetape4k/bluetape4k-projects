package io.bluetape4k.spring.data

import org.springframework.data.domain.ExampleMatcher
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

/**
 * 검색 대상 속성 이름으로 [ExampleMatcher]를 생성합니다.
 *
 * ## 동작/계약
 * - 지정한 [searchFields]를 제외한 나머지 프로퍼티를 `ignorePaths`로 설정합니다.
 * - 빈 문자열이 아닌 각 검색 필드에 `exact()` 매처를 적용합니다.
 *
 * ```kotlin
 * val matcher = User::class.buildExampleMatcher("name", "age")
 * // matcher != null
 * ```
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
 * 검색 대상 프로퍼티 참조로 [ExampleMatcher]를 생성합니다.
 *
 * ## 동작/계약
 * - [KProperty] 목록의 이름을 추출해 문자열 기반 [buildExampleMatcher]로 위임합니다.
 * - 결과 매처는 문자열 기반 호출과 동일합니다.
 *
 * ```kotlin
 * val matcher = User::class.buildExampleMatcher(User::name, User::age)
 * // matcher != null
 * ```
 */
inline fun <reified T: Any> KClass<T>.buildExampleMatcher(vararg searchFields: KProperty<*>): ExampleMatcher {
    return buildExampleMatcher(*searchFields.map { it.name }.toTypedArray())
}

/**
 * [ExampleMatcher.withIgnorePaths]에 전달할 프로퍼티 이름 배열을 계산합니다.
 *
 * ## 동작/계약
 * - 수신 클래스의 선언 프로퍼티 중 [exclusions]에 없는 이름만 반환합니다.
 * - 반환 배열은 `ignorePaths` 인자로 바로 사용할 수 있습니다.
 *
 * ```kotlin
 * val ignored = User::class.ignoredProperties("name")
 * // ignored.contains("name") == false
 * ```
 */
fun <T: Any> KClass<T>.ignoredProperties(vararg exclusions: String): Array<String> =
    declaredMemberProperties
        .filterNot { exclusions.contains(it.name) }
        .map { it.name }
        .toTypedArray()

/**
 * [KProperty] 제외 목록으로 [ignoredProperties]를 계산합니다.
 *
 * ## 동작/계약
 * - [KProperty.name]을 추출해 문자열 기반 [ignoredProperties]로 위임합니다.
 * - `internal` 함수이며 모듈 내부 구현에서 사용됩니다.
 *
 * ```kotlin
 * val ignored = User::class.ignoredProperties(User::name)
 * // ignored.contains("name") == false
 * ```
 */
internal fun <T: Any> KClass<T>.ignoredProperties(vararg exclusions: KProperty<*>): Array<String> =
    ignoredProperties(*exclusions.map { it.name }.toTypedArray())
