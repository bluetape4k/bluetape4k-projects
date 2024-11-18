package io.bluetape4k.hibernate.querydsl.jpa

import com.querydsl.core.types.CollectionExpression
import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.dsl.BeanPath
import com.querydsl.core.types.dsl.ComparableExpression
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.jpa.JPAExpressions

inline fun <reified U: BeanPath<out T>, T: Any> BeanPath<out T>.treat(): U =
    JPAExpressions.treat(this, U::class.java)

/**
 * 현 [CollectionExpression]의 평균값을 반환합니다.
 */
fun <T: Comparable<T>> CollectionExpression<*, T>.avg(): ComparableExpression<T> =
    JPAExpressions.avg(this)

/**
 * 현 [CollectionExpression]의 최대값을 반환합니다.
 */
fun <T: Comparable<T>> CollectionExpression<*, T>.max(): ComparableExpression<T> =
    JPAExpressions.max(this)

/**
 * 현 [CollectionExpression]의 최소값을 반환합니다.
 */
fun <T: Comparable<T>> CollectionExpression<*, T>.min(): ComparableExpression<T> =
    JPAExpressions.min(this)

/**
 * [EntityPath]의 타입을 나타내는 [StringExpression]을 반환합니다.
 */
fun EntityPath<*>.type(): StringExpression =
    JPAExpressions.type(this)
