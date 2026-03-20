package io.bluetape4k.spring4.beans

import org.springframework.beans.PropertyAccessorUtils

/**
 * 프로퍼티 경로에서 실제 프로퍼티 이름을 추출합니다.
 *
 * ## 동작/계약
 * - `map[key]` 같은 인덱스 표현이 포함된 경로에서도 기본 이름을 반환합니다.
 * - 구현은 [PropertyAccessorUtils.getPropertyName]에 위임합니다.
 *
 * ```kotlin
 * val name = "user.address[0]".getPropertyName()
 * // name == "user.address"
 * ```
 */
fun String.getPropertyName(): String = PropertyAccessorUtils.getPropertyName(this)

/**
 * 프로퍼티 경로가 중첩 또는 인덱스 표현을 포함하는지 확인합니다.
 *
 * ## 동작/계약
 * - 점(`.`) 중첩 또는 대괄호 인덱스가 있으면 `true`를 반환합니다.
 * - 구현은 [PropertyAccessorUtils.isNestedOrIndexedProperty]에 위임합니다.
 *
 * ```kotlin
 * val nested = "user.address[0]".isNestedOrIndexedProperty()
 * // nested == true
 * ```
 */
fun String.isNestedOrIndexedProperty(): Boolean = PropertyAccessorUtils.isNestedOrIndexedProperty(this)

/**
 * 첫 번째 중첩 구분자 인덱스를 반환합니다.
 *
 * ## 동작/계약
 * - `map["my.key"]` 내부 점은 무시하고 첫 중첩 구분자를 계산합니다.
 * - 구분자가 없으면 음수 값을 반환합니다.
 *
 * ```kotlin
 * val idx = "user.address.street".getFirstNestedPropertySeparatorIndex()
 * // idx == 4
 * ```
 */
fun String.getFirstNestedPropertySeparatorIndex(): Int =
    PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(this)

/**
 * 마지막 중첩 구분자 인덱스를 반환합니다.
 *
 * ## 동작/계약
 * - 키 내부의 점은 무시하고 마지막 중첩 구분자 위치를 계산합니다.
 * - 구분자가 없으면 음수 값을 반환합니다.
 *
 * ```kotlin
 * val idx = "user.address.street".getLastNestedPropertySeparatorIndex()
 * // idx == 12
 * ```
 */
fun String.getLastNestedPropertySeparatorIndex(): Int = PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(this)

/**
 * 등록 경로가 대상 [propertyPath]와 일치하는지 확인합니다.
 *
 * ## 동작/계약
 * - 수신 경로가 대상 프로퍼티 자체이거나 인덱스 표현이면 `true`를 반환할 수 있습니다.
 * - 구현은 [PropertyAccessorUtils.matchesProperty]에 위임합니다.
 *
 * ```kotlin
 * val matched = "items[0]".matchesProperty("items")
 * // matched == true
 * ```
 */
fun String.matchesProperty(propertyPath: String): Boolean = PropertyAccessorUtils.matchesProperty(this, propertyPath)

/**
 * 프로퍼티 경로를 정규화된 canonical 이름으로 변환합니다.
 *
 * ## 동작/계약
 * - 맵 키를 감싼 따옴표를 제거합니다.
 * - 구현은 [PropertyAccessorUtils.canonicalPropertyName]에 위임합니다.
 *
 * ```kotlin
 * val canonical = "map['my.key']".canonicalPropertyName()
 * // canonical == "map[my.key]"
 * ```
 */
fun String.canonicalPropertyName(): String = PropertyAccessorUtils.canonicalPropertyName(this)

/**
 * 프로퍼티 경로 배열을 canonical 이름 배열로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 배열과 동일한 길이의 배열을 반환합니다.
 * - 구현은 [PropertyAccessorUtils.canonicalPropertyNames]에 위임합니다.
 *
 * ```kotlin
 * val names = arrayOf("map['a']", "map['b']").canonicalPropertyNames()
 * // names?.toList() == listOf("map[a]", "map[b]")
 * ```
 */
fun Array<String>.canonicalPropertyNames(): Array<String>? = PropertyAccessorUtils.canonicalPropertyNames(this)
