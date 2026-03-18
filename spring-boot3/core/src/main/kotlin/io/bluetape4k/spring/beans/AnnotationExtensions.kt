package io.bluetape4k.spring.beans

import org.springframework.beans.PropertyAccessorFactory
import org.springframework.util.ReflectionUtils
import org.springframework.util.StringValueResolver

/**
 * 애너테이션 속성을 대상 빈 프로퍼티로 복사합니다.
 *
 * ## 동작/계약
 * - [excludedProperties]에 포함되지 않고 쓰기 가능한 프로퍼티만 설정합니다.
 * - 문자열 해석기 없이 [copyPropertiesToBean] 오버로드를 호출합니다.
 *
 * ```kotlin
 * annotation.copyPropertiesToBean(bean, "value")
 * // 제외 목록 외 속성만 bean에 반영
 * ```
 */
fun Annotation.copyPropertiesToBean(bean: Any, vararg excludedProperties: String) {
    copyPropertiesToBean(bean, null, *excludedProperties)
}

/**
 * 애너테이션 속성을 대상 빈 프로퍼티로 복사하고 문자열 값을 해석합니다.
 *
 * ## 동작/계약
 * - 애너테이션 선언 메서드를 순회해 프로퍼티 이름과 값을 읽습니다.
 * - [valueResolver]가 있고 값이 문자열이면 해석된 문자열을 저장합니다.
 *
 * ```kotlin
 * annotation.copyPropertiesToBean(bean, valueResolver, "value")
 * // 문자열 속성은 valueResolver 결과로 설정
 * ```
 */
fun Annotation.copyPropertiesToBean(
    bean: Any,
    valueResolver: StringValueResolver?,
    vararg excludedProperties: String,
) {
    val excluded = excludedProperties.toSet()
    val annotationProperties = this.annotationClass.java.declaredMethods
    val bw = PropertyAccessorFactory.forBeanPropertyAccess(bean)

    annotationProperties.forEach { annotationProperty ->
        val propertyName = annotationProperty.name
        if (!excluded.contains(propertyName) && bw.isWritableProperty(propertyName)) {
            var value = ReflectionUtils.invokeMethod(annotationProperty, this)
            if (valueResolver != null && value is String) {
                value = valueResolver.resolveStringValue(value)
            }
            bw.setPropertyValue(propertyName, value)
        }
    }
}
