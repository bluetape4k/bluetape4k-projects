package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.SimpleExpression

/**
 * 현 [SimpleExpression]이 주어진 [rights] 중 하나와 같은지 확인하는 [BooleanExpression]을 반환합니다.
 * [SimpleExpression. in]과 동일합니다.
 *
 * @see [SimpleExpression. in]
 */
fun <T> SimpleExpression<T>.inValues(vararg rights: T): BooleanExpression = `in`(*rights)
