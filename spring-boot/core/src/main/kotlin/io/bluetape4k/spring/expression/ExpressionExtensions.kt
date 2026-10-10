package io.bluetape4k.spring.expression

import org.springframework.expression.EvaluationContext
import org.springframework.expression.Expression

/**
 * 기본 context에서 expression 결과를 reified 타입으로 변환해 조회합니다.
 *
 * 결과가 null일 수 있으며 Spring의 평가·변환 예외를 그대로 전달합니다. 신뢰할 수 없는 expression을 검증하지
 * 않으므로 호출자가 입력과 평가 context를 적절히 관리해야 합니다.
 */
inline fun <reified T : Any> Expression.getValueAs(): T? =
    getValue(T::class.java)

/** 기본 context와 지정한 root object를 사용해 expression 결과를 조회합니다. */
inline fun <reified T : Any> Expression.getValueAs(rootObject: Any?): T? =
    getValue(rootObject, T::class.java)

/** 호출자가 제공한 [EvaluationContext]에서 expression 결과를 조회합니다. */
inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext): T? =
    getValue(context, T::class.java)

/** 호출자가 제공한 [EvaluationContext]와 root object로 expression 결과를 조회합니다. */
inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext, rootObject: Any?): T? =
    getValue(context, rootObject, T::class.java)
