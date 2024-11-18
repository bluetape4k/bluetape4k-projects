package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.FactoryExpression
import com.querydsl.core.types.FactoryExpressionUtils

/**
 * [Expression] 리스트를 감싸서 [FactoryExpression]으로 만듭니다.
 */
fun List<Expression<*>>.wrap(): FactoryExpression<*> =
    FactoryExpressionUtils.wrap(this)

/**
 * [FactoryExpression]을 [conversions]로 감싸서 [FactoryExpression]으로 만듭니다.
 */
fun <T> FactoryExpression<T>.wrap(conversions: List<Expression<*>>): FactoryExpression<T> =
    FactoryExpressionUtils.wrap(this, conversions)

/**
 * [FactoryExpression]을 감싸서 [FactoryExpression]으로 만듭니다.
 */
fun <T> FactoryExpression<T>.wrap(): FactoryExpression<T> =
    FactoryExpressionUtils.wrap(this)
