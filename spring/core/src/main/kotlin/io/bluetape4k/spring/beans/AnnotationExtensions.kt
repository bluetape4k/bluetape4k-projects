package io.bluetape4k.spring.beans

import io.bluetape4k.collections.eclipse.toUnifiedSet
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.util.ReflectionUtils
import org.springframework.util.StringValueResolver

/**
 * Annotation의 속성을 Bean에 복사합니다.
 *
 * @receiver Annotation
 * @param bean Annotation의 속성을 복사할 Bean
 * @param excludedProperties 제외할 속성 이름들
 */
fun Annotation.copyPropertiesToBean(bean: Any, vararg excludedProperties: String) {
    copyPropertiesToBean(bean, null, *excludedProperties)
}

/**
 * Annotation의 속성을 Bean에 복사합니다.
 *
 * @receiver Annotation
 * @param bean Annotation의 속성을 복사할 Bean
 * @param valueResolver StringValueResolver
 * @param excludedProperties 제외할 속성 이름들
 */
fun Annotation.copyPropertiesToBean(
    bean: Any,
    valueResolver: StringValueResolver?,
    vararg excludedProperties: String,
) {
    val excluded = excludedProperties.toUnifiedSet()
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
