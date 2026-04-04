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
 *
 * ```kotlin
 * val op = Ops.EQ.newOperation<Boolean>(nameExpr, constantOf("Alice"))
 * // op 는 nameExpr == "Alice" 를 나타내는 Operation
 * ```
 */
inline fun <reified T: Any> Operator.newOperation(vararg exprs: Expression<*>): Operation<T> =
    ExpressionUtils.operation(T::class.java, this, *exprs)

/**
 * 현 [Operator]에 대해 [exprs]를 적용한 [PredicateOperation]을 생성합니다.
 *
 * ```kotlin
 * val pred = Ops.EQ.newPredicate(nameExpr, constantOf("Alice"))
 * // pred 는 nameExpr == "Alice" 를 나타내는 PredicateOperation
 * ```
 */
fun Operator.newPredicate(vararg exprs: Expression<*>): PredicateOperation =
    ExpressionUtils.predicate(this, *exprs)

/**
 * 변수 이름으로 타입 [T]의 [Path]를 생성합니다.
 *
 * ```kotlin
 * val path = pathOf<String>("name")
 * // path.metadata.name == "name"
 * ```
 */
inline fun <reified T: Any> pathOf(variable: String): Path<T> =
    ExpressionUtils.path(T::class.java, variable)

/**
 * 부모 [Path]와 프로퍼티 이름으로 타입 [T]의 [Path]를 생성합니다.
 *
 * ```kotlin
 * val parentPath = pathOf<User>("user")
 * val namePath = pathOf<String>(parentPath, "name")
 * ```
 */
inline fun <reified T: Any> pathOf(parent: Path<*>, property: String): Path<T> =
    ExpressionUtils.path(T::class.java, parent, property)

/**
 * [PathMetadata]로 타입 [T]의 [Path]를 생성합니다.
 */
inline fun <reified T: Any> pathOf(metadata: PathMetadata): Path<T> =
    ExpressionUtils.path(T::class.java, metadata)

/**
 * 템플릿 문자열과 인자들로 타입 [T]의 [TemplateExpression]을 생성합니다.
 *
 * ```kotlin
 * val expr = templateExpressionOf<String>("upper({0})", nameExpr)
 * ```
 */
inline fun <reified T: Any> templateExpressionOf(template: String, vararg args: Any?): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, template, *args)

/**
 * 템플릿 문자열과 인자 목록으로 타입 [T]의 [TemplateExpression]을 생성합니다.
 */
inline fun <reified T: Any> templateExpressionOf(template: String, args: List<*>): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, template, args)

/**
 * 현 [Template]과 인자들로 타입 [T]의 [TemplateExpression]을 생성합니다.
 */
inline fun <reified T: Any> Template.newTemplateExpression(vararg args: Any?): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, this, *args)

/**
 * 현 [Template]과 인자 목록으로 타입 [T]의 [TemplateExpression]을 생성합니다.
 */
inline fun <reified T: Any> Template.newTemplateExpression(args: List<*>): TemplateExpression<T> =
    ExpressionUtils.template(T::class.java, this, args)

/**
 * [CollectionExpression]의 모든 원소를 나타내는 [Expression]을 반환합니다 (SQL `ALL` 서브쿼리용).
 */
fun <T> CollectionExpression<*, T>.all(): Expression<T> = ExpressionUtils.all(this)

/**
 * [CollectionExpression]의 임의 원소를 나타내는 [Expression]을 반환합니다 (SQL `ANY` 서브쿼리용).
 */
fun <T> CollectionExpression<*, T>.any(): Expression<T> = ExpressionUtils.any(this)

/**
 * [SubQueryExpression]의 모든 원소를 나타내는 [Expression]을 반환합니다 (SQL `ALL` 서브쿼리용).
 */
fun <T> SubQueryExpression<T>.all(): Expression<T> = ExpressionUtils.all(this)

/**
 * [SubQueryExpression]의 임의 원소를 나타내는 [Expression]을 반환합니다 (SQL `ANY` 서브쿼리용).
 */
fun <T> SubQueryExpression<T>.any(): Expression<T> = ExpressionUtils.any(this)

/**
 * [Predicate] 컬렉션의 모든 항목을 AND로 묶은 [Predicate]를 반환합니다. 비어 있으면 null을 반환합니다.
 */
fun Collection<Predicate>.allOrNull(): Predicate? = ExpressionUtils.allOf(this)

/**
 * 두 [Predicate]를 AND로 묶습니다.
 *
 * ```kotlin
 * val combined = pred1 and pred2
 * ```
 */
infix fun Predicate.and(right: Predicate): Predicate = ExpressionUtils.and(this, right)

/**
 * [Predicate] 컬렉션의 임의 항목을 OR로 묶은 [Predicate]를 반환합니다. 비어 있으면 null을 반환합니다.
 */
fun Collection<Predicate>.anyOrNull(): Predicate? = ExpressionUtils.anyOf(this)

/**
 * 현 [Expression]의 COUNT 집계 표현식을 반환합니다.
 *
 * ```kotlin
 * val countExpr = nameExpr.count()
 * // SQL: COUNT(name)
 * ```
 */
fun Expression<*>.count(): Expression<Long> = ExpressionUtils.count(this)

/**
 * 현 [Expression]이 [constant]와 같은지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.eqConst(constant: D): Predicate = ExpressionUtils.eqConst(this, constant)

/**
 * 현 [Expression]이 [right]와 같은지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.eq(right: Expression<out D>): Predicate = ExpressionUtils.eq(this, right)

/**
 * 현 [Expression]이 [right] 컬렉션 표현식에 포함되는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.inValues(right: CollectionExpression<*, out D>): Predicate = ExpressionUtils.`in`(this, right)

/**
 * 현 [Expression]이 [right] 서브쿼리 결과에 포함되는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.inValues(right: SubQueryExpression<out D>): Predicate = ExpressionUtils.`in`(this, right)

/**
 * 현 [Expression]이 [right] 컬렉션에 포함되는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.inValues(right: Collection<D>): Predicate = ExpressionUtils.`in`(this, right)

/**
 * 현 [Expression]이 [right]의 임의 컬렉션에 포함되는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.inAny(right: Iterable<Collection<D>>): Predicate = ExpressionUtils.inAny(this, right)

/**
 * 현 [Expression]이 null인지 확인하는 [Predicate]를 반환합니다.
 */
fun Expression<*>.isNull(): Predicate = ExpressionUtils.isNull(this)

/**
 * 현 [Expression]이 null이 아닌지 확인하는 [Predicate]를 반환합니다.
 */
fun Expression<*>.isNotNull(): Predicate = ExpressionUtils.isNotNull(this)

/**
 * LIKE 패턴 표현식을 정규식 표현식으로 변환합니다.
 */
fun Expression<String>.likeToRegex(matchStartAndEnd: Boolean = true): Expression<String> =
    ExpressionUtils.likeToRegex(this, matchStartAndEnd)

/**
 * 정규식 표현식을 LIKE 패턴 표현식으로 변환합니다.
 */
fun Expression<String>.regexToLike(): Expression<String> = ExpressionUtils.regexToLike(this)

/**
 * 현 [Expression]이 [constant]와 다른지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.neConst(constant: D): Predicate = ExpressionUtils.neConst(this, constant)

/**
 * 현 [Expression]이 [right]와 다른지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.ne(right: Expression<in D>): Predicate = ExpressionUtils.ne(this, right)

/**
 * 현 [Expression]이 [right] 컬렉션 표현식에 포함되지 않는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.notIn(right: CollectionExpression<*, out D>): Predicate = ExpressionUtils.notIn(this, right)

/**
 * 현 [Expression]이 [right] 서브쿼리 결과에 포함되지 않는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.notIn(right: SubQueryExpression<out D>): Predicate = ExpressionUtils.notIn(this, right)

/**
 * 현 [Expression]이 [right] 컬렉션에 포함되지 않는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.notIn(right: Collection<D>): Predicate = ExpressionUtils.notIn(this, right)

/**
 * 현 [Expression]이 [right]의 모든 컬렉션에 포함되지 않는지 확인하는 [Predicate]를 반환합니다.
 */
fun <D> Expression<D>.notInAny(right: Iterable<Collection<D>>): Predicate = ExpressionUtils.notInAny(this, right)

/**
 * 두 [Predicate]를 OR로 묶습니다.
 *
 * ```kotlin
 * val combined = pred1 or pred2
 * ```
 */
infix fun Predicate.or(right: Predicate): Predicate = ExpressionUtils.or(this, right)

/**
 * [Expression] 컬렉션에서 중복을 제거한 목록을 반환합니다.
 */
fun Collection<Expression<*>>.distinctList(): List<Expression<*>> = ExpressionUtils.distinctList(this.toTypedArray())

/**
 * 현 [Expression]에서 내부 래핑을 제거한 표현식을 반환합니다.
 */
fun <T> Expression<T>.extract(): Expression<T> = ExpressionUtils.extract(this)

/**
 * 현 [Path]의 루트 변수 이름을 반환합니다.
 */
fun Path<*>.rootVariable(): String = ExpressionUtils.createRootVariable(this)

/**
 * 현 [Path]의 루트 변수 이름을 [suffix]를 붙여서 반환합니다.
 */
fun Path<*>.rootVariable(suffix: Int): String = ExpressionUtils.createRootVariable(this, suffix)

/**
 * 현 값을 QueryDSL [Expression]으로 변환합니다.
 */
fun Any.toExpression(): Expression<*> = ExpressionUtils.toExpression(this)

/**
 * 현 문자열 [Expression]을 소문자 변환 표현식으로 반환합니다.
 */
fun Expression<String>.lowercase(): Expression<String> = ExpressionUtils.toLower(this)

/**
 * [OrderSpecifier] 목록을 order by 표현식으로 변환합니다.
 */
fun List<OrderSpecifier<*>>.orderBy(): Expression<*> = ExpressionUtils.orderBy(this)
