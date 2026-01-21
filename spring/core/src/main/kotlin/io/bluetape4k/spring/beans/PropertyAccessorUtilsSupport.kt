package io.bluetape4k.spring.beans

import org.springframework.beans.PropertyAccessorUtils

/**
 * 지정한 property path의 실제 property name을 추출합니다.
 */
fun String.getPropertyName(): String =
    PropertyAccessorUtils.getPropertyName(this)

/**
 * 지정한 property path가  indexed 나 nested property 인지 나타냅니다.
 */
fun String.isNestedOrIndexedProperty(): Boolean =
    PropertyAccessorUtils.isNestedOrIndexedProperty(this)

/**
 * Determine the first nested property separator in the given property path, ignoring dots in keys (like `map["my.key"]`).
 */
fun String.getFirstNestedPropertySeparatorIndex(): Int =
    PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(this)

/**
 * 마지막 중첩된 속성 구분자의 인덱스를 결정합니다. (예: `map["my.key"]`와 같은 키의 점을 무시합니다.)
 */
fun String.getLastNestedPropertySeparatorIndex(): Int =
    PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(this)

/**
 * 등록된 경로 (receiver)가 속성 경로([propertyPath])와 일치하는지 여부를 결정합니다.
 * 속성 자체를 나타내거나 속성의 색인 요소를 나타낼 수 있습니다.
 *
 * @param propertyPath the property path (typically without index)
 */
fun String.matchesProperty(propertyPath: String): Boolean =
    PropertyAccessorUtils.matchesProperty(this, propertyPath)

/**
 * Determine the canonical name for the given property path.
 * Removes surrounding quotes from map keys:<br>
 */
fun String.canonicalPropertyName(): String =
    PropertyAccessorUtils.canonicalPropertyName(this)

/**
 * Determine the canonical names for the given property paths.
 *
 * @return the canonical representation of the property paths
 * (as array of the same size)
 */
fun Array<String>.canonicalPropertyNames(): Array<String>? =
    PropertyAccessorUtils.canonicalPropertyNames(this)
