package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.CollectionExpression
import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Operation
import com.querydsl.core.types.Operator
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.PredicateOperation
import com.querydsl.core.types.SubQueryExpression
import com.querydsl.core.types.Template
import com.querydsl.core.types.TemplateExpression

/**
 * 현 [Operator]에 대해 [exprs]를 적용한 [Operation]을 생성합니다.
 */
inline fun <reified T: Any> Operator.newOperation(vararg exprs: Expression<*>): Operation<T> =
    ExpressionUtils.operation(T::class.java, this, *exprs)

fun Operator.newPredicate(vararg exprs: Expression<*>): PredicateOperation =
    ExpressionUtils.predicate(this, *exprs)

inline fun <reified T: Any> pathOf(variable: String): Path<T> =
    ExpressionUtils.path(T::class.java, variable)

inline fun <reified T: Any> pathOf(parent: Path<*>, property: String): Path<T> =
    ExpressionUtils.path(T::class.java, parent, property)

inline fun <reified T: Any> pathOf(metadata: PathMetadata): Path<T> =
    ExpressionUtils.path(T::class.java, metadata)

inline fun <reified T: Any> templateExpressionOf(template: String, vararg args: Any?): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, template, *args)

inline fun <reified T: Any> templateExpressionOf(template: String, args: List<*>): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, template, args)

inline fun <reified T: Any> Template.newTemplateExpression(vararg args: Any?): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, this, *args)

inline fun <reified T: Any> Template.newTemplateExpression(args: List<*>): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, this, args)


fun <T> CollectionExpression<*, T>.all(): Expression<T> = ExpressionUtils.all(this)
fun <T> CollectionExpression<*, T>.any(): Expression<T> = ExpressionUtils.any(this)

fun <T> SubQueryExpression<T>.all(): Expression<T> = ExpressionUtils.all(this)
fun <T> SubQueryExpression<T>.any(): Expression<T> = ExpressionUtils.any(this)

fun Collection<Predicate>.allOrNull(): Predicate? = ExpressionUtils.allOf(this)

infix fun Predicate.and(right: Predicate): Predicate = ExpressionUtils.and(this, right)

fun Collection<Predicate>.anyOrNull(): Predicate? = ExpressionUtils.anyOf(this)

fun Expression<*>.count(): Expression<Long> = ExpressionUtils.count(this)

fun <D> Expression<D>.eqConst(constant: D): Predicate = ExpressionUtils.eqConst(this, constant)

fun <D> Expression<D>.eq(right: Expression<out D>): Predicate = ExpressionUtils.eq(this, right)

fun <D> Expression<D>.inValues(right: CollectionExpression<*, out D>): Predicate = ExpressionUtils.`in`(this, right)
fun <D> Expression<D>.inValues(right: SubQueryExpression<out D>): Predicate = ExpressionUtils.`in`(this, right)
fun <D> Expression<D>.inValues(right: Collection<D>): Predicate = ExpressionUtils.`in`(this, right)

fun <D> Expression<D>.inAny(right: Iterable<Collection<D>>): Predicate = ExpressionUtils.inAny(this, right)

fun Expression<*>.isNull(): Predicate = ExpressionUtils.isNull(this)
fun Expression<*>.isNotNull(): Predicate = ExpressionUtils.isNotNull(this)

fun Expression<String>.likeToRegex(matchStartAndEnd: Boolean = true): Expression<String> =
    ExpressionUtils.likeToRegex(this, matchStartAndEnd)


fun Expression<String>.regexToLike(): Expression<String> = ExpressionUtils.regexToLike(this)

fun <D> Expression<D>.neConst(constant: D): Predicate = ExpressionUtils.neConst(this, constant)
fun <D> Expression<D>.ne(right: Expression<in D>): Predicate = ExpressionUtils.ne(this, right)

fun <D> Expression<D>.notIn(right: CollectionExpression<*, out D>): Predicate = ExpressionUtils.notIn(this, right)
fun <D> Expression<D>.notIn(right: SubQueryExpression<out D>): Predicate = ExpressionUtils.notIn(this, right)
fun <D> Expression<D>.notIn(right: Collection<D>): Predicate = ExpressionUtils.notIn(this, right)

fun <D> Expression<D>.notInAny(right: Iterable<Collection<D>>): Predicate = ExpressionUtils.notInAny(this, right)

infix fun Predicate.or(right: Predicate): Predicate = ExpressionUtils.or(this, right)

fun Collection<Expression<*>>.distinctList(): List<Expression<*>> = ExpressionUtils.distinctList(this.toTypedArray())

fun <T> Expression<T>.extract(): Expression<T> = ExpressionUtils.extract(this)

fun Path<*>.rootVariable(): String = ExpressionUtils.createRootVariable(this)
fun Path<*>.rootVariable(suffix: Int): String = ExpressionUtils.createRootVariable(this, suffix)

fun Any.toExpression(): Expression<*> = ExpressionUtils.toExpression(this)
fun Expression<String>.lowercase(): Expression<String> = ExpressionUtils.toLower(this)

fun List<OrderSpecifier<*>>.orderBy(): Expression<*> = ExpressionUtils.orderBy(this)
