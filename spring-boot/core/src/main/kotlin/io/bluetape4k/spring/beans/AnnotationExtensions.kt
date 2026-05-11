package io.bluetape4k.spring.beans

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.ReflectionUtils
import org.springframework.util.StringValueResolver

/**
 * 지정한 애너테이션 타입의 merged annotation 을 찾습니다.
 *
 * ## 동작/계약
 * - Spring [AnnotatedElementUtils.findMergedAnnotation]에 reified 타입을 전달합니다.
 * - 직접 애너테이션과 메타 애너테이션을 모두 탐색하고, `@AliasFor` 기반 속성 병합을 적용합니다.
 *
 * ```kotlin
 * val mapping = method.findMergedAnnotationOrNull<RequestMapping>()
 * ```
 */
inline fun <reified A : Annotation> AnnotatedElement.findMergedAnnotationOrNull(): A? =
    AnnotatedElementUtils.findMergedAnnotation(this, A::class.java)

/**
 * 지정한 애너테이션 타입의 merged annotation 존재 여부를 확인합니다.
 *
 * ## 동작/계약
 * - Spring [AnnotatedElementUtils.hasAnnotation]에 reified 타입을 전달합니다.
 * - [findMergedAnnotationOrNull]이 값을 찾을 수 있으면 `true`를 반환합니다.
 *
 * ```kotlin
 * if (method.hasMergedAnnotation<RequestMapping>()) {
 *     // merged RequestMapping 이 존재함
 * }
 * ```
 */
inline fun <reified A : Annotation> AnnotatedElement.hasMergedAnnotation(): Boolean =
    AnnotatedElementUtils.hasAnnotation(this, A::class.java)

/**
 * 지정한 애너테이션 타입의 get-semantics merged annotation 을 조회합니다.
 *
 * ## 동작/계약
 * - Spring [AnnotatedElementUtils.getMergedAnnotation]에 reified 타입을 전달합니다.
 * - get-semantics 로 현재 요소 위의 애너테이션 계층을 조회하고, 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val mapping = method.getMergedAnnotation<RequestMapping>()
 * ```
 */
inline fun <reified A : Annotation> AnnotatedElement.getMergedAnnotation(): A? =
    AnnotatedElementUtils.getMergedAnnotation(this, A::class.java)

/**
 * 지정한 애너테이션 타입의 모든 merged annotation 을 찾습니다.
 *
 * ## 동작/계약
 * - Spring [AnnotatedElementUtils.findAllMergedAnnotations]에 reified 타입을 전달합니다.
 * - find-semantics 로 현재 요소와 관련 계층을 탐색하고, 없으면 빈 [Set]을 반환합니다.
 *
 * ```kotlin
 * val mappings = method.findAllMergedAnnotations<RequestMapping>()
 * ```
 */
inline fun <reified A : Annotation> AnnotatedElement.findAllMergedAnnotations(): Set<A> =
    AnnotatedElementUtils.findAllMergedAnnotations(this, A::class.java)

/**
 * [AnnotatedElement]에서 지정한 애너테이션 타입을 찾습니다.
 *
 * ## 동작/계약
 * - Spring [AnnotationUtils.findAnnotation]에 reified 타입을 전달합니다.
 * - 일반 [AnnotatedElement] 검색 의미를 따르며, 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val annotation = element.findAnnotationOrNull<MyAnnotation>()
 * ```
 */
inline fun <reified A : Annotation> AnnotatedElement.findAnnotationOrNull(): A? =
    AnnotationUtils.findAnnotation(this, A::class.java)

/**
 * [Method]에서 지정한 애너테이션 타입을 찾습니다.
 *
 * ## 동작/계약
 * - Spring [AnnotationUtils.findAnnotation]의 [Method] 전용 검색 의미를 보존합니다.
 * - 브리지 메서드와 상위 메서드 계층을 고려하며, 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val annotation = method.findAnnotationOrNull<MyAnnotation>()
 * ```
 */
inline fun <reified A : Annotation> Method.findAnnotationOrNull(): A? =
    AnnotationUtils.findAnnotation(this, A::class.java)

/**
 * [Class]에서 지정한 애너테이션 타입을 찾습니다.
 *
 * ## 동작/계약
 * - Spring [AnnotationUtils.findAnnotation]의 [Class] 전용 검색 의미를 보존합니다.
 * - 인터페이스와 상위 클래스 계층을 고려하며, 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val annotation = beanClass.findAnnotationOrNull<MyAnnotation>()
 * ```
 */
inline fun <reified A : Annotation> Class<*>.findAnnotationOrNull(): A? =
    AnnotationUtils.findAnnotation(this, A::class.java)

/**
 * [AnnotatedElement]에서 지정한 애너테이션 타입을 조회합니다.
 *
 * ## 동작/계약
 * - Spring [AnnotationUtils.getAnnotation]에 reified 타입을 전달합니다.
 * - 현재 요소의 present/meta-present 애너테이션을 조회하며, 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val annotation = element.getAnnotationOrNull<MyAnnotation>()
 * ```
 */
inline fun <reified A : Annotation> AnnotatedElement.getAnnotationOrNull(): A? =
    AnnotationUtils.getAnnotation(this, A::class.java)

/**
 * [Method]에서 지정한 애너테이션 타입을 조회합니다.
 *
 * ## 동작/계약
 * - Spring [AnnotationUtils.getAnnotation]의 [Method] 전용 조회 의미를 보존합니다.
 * - 브리지 메서드를 해석하며, 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val annotation = method.getAnnotationOrNull<MyAnnotation>()
 * ```
 */
inline fun <reified A : Annotation> Method.getAnnotationOrNull(): A? =
    AnnotationUtils.getAnnotation(this, A::class.java)

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
fun Annotation.copyPropertiesToBean(
    bean: Any,
    vararg excludedProperties: String,
) {
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
