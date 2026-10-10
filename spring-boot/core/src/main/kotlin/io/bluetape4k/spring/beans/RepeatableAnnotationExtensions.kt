package io.bluetape4k.spring.beans

import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.AnnotatedElement

/**
 * 지정한 반복 애너테이션 타입의 merged annotation을 조회합니다.
 *
 * - Spring [AnnotatedElementUtils.getMergedRepeatableAnnotations]의 get 조회 의미를 보존합니다.
 * - [findAllMergedAnnotations]와 탐색 범위가 다를 수 있으므로 현재 요소의 반복 애너테이션 조회에 사용합니다.
 * - 애너테이션이 없으면 빈 [Set]을 반환합니다.
 */
inline fun <reified A : Annotation> AnnotatedElement.getMergedRepeatableAnnotations(): Set<A> =
    AnnotatedElementUtils.getMergedRepeatableAnnotations(this, A::class.java)
