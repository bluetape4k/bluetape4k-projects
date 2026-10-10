package io.bluetape4k.spring.core

import org.springframework.core.type.AnnotationMetadata

/**
 * reified 클래스의 애너테이션 메타데이터를 Spring 표준 reflection으로 분석합니다.
 *
 * Spring의 `introspect` 결과와 예외 동작을 그대로 반환합니다. 애너테이션 속성이 참조하는 `Class` 또는 `Enum`이
 * classpath에 없으면 해당 애너테이션은 [AnnotationMetadata]에서 접근할 수 없습니다.
 */
inline fun <reified T : Any> annotationMetadataOf(): AnnotationMetadata =
    AnnotationMetadata.introspect(T::class.java)
