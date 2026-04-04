package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.BindMarker
import com.datastax.oss.driver.api.querybuilder.relation.ArithmeticRelationBuilder
import com.datastax.oss.driver.api.querybuilder.relation.InRelationBuilder
import com.datastax.oss.driver.api.querybuilder.term.Term

/**
 * `=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("users").all()
 *     .where(QueryBuilder.column("id") eq QueryBuilder.literal(1))
 * // select.asCql() contains "WHERE id=1"
 * ```
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.eq(rightOperand: Term): T = isEqualTo(rightOperand)

/**
 * `!=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("users").all()
 *     .where(QueryBuilder.column("status") ne QueryBuilder.literal("deleted"))
 * // select.asCql() contains "WHERE status!='deleted'"
 * ```
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.ne(rightOperand: Term): T = isNotEqualTo(rightOperand)

/**
 * `<` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("events").all()
 *     .where(QueryBuilder.column("age") lt QueryBuilder.literal(18))
 * // select.asCql() contains "WHERE age<18"
 * ```
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.lt(rightOperand: Term): T = isLessThan(rightOperand)

/**
 * `<=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("events").all()
 *     .where(QueryBuilder.column("age") lte QueryBuilder.literal(17))
 * // select.asCql() contains "WHERE age<=17"
 * ```
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.lte(rightOperand: Term): T = isLessThanOrEqualTo(rightOperand)

/**
 * `>` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("orders").all()
 *     .where(QueryBuilder.column("amount") gt QueryBuilder.literal(100))
 * // select.asCql() contains "WHERE amount>100"
 * ```
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.gt(rightOperand: Term): T = isGreaterThan(rightOperand)

/**
 * `>=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("orders").all()
 *     .where(QueryBuilder.column("amount") gte QueryBuilder.literal(100))
 * // select.asCql() contains "WHERE amount>=100"
 * ```
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.gte(rightOperand: Term): T = isGreaterThanOrEqualTo(rightOperand)

/**
 * `IN` 절을 [BindMarker]로 표현합니다. Kotlin의 예약어 `in`을 회피합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("users").all()
 *     .where(QueryBuilder.column("id").inValues(QueryBuilder.bindMarker()))
 * // select.asCql() contains "WHERE id IN ?"
 * ```
 *
 * @param bindMarker 바인드 마커
 * @return 관계가 적용된 결과
 */
fun <T> InRelationBuilder<T>.inValues(bindMarker: BindMarker): T = `in`(bindMarker)

/**
 * `IN` 절을 [Term] 목록으로 표현합니다. Kotlin의 예약어 `in`을 회피합니다.
 *
 * ```kotlin
 * val terms = listOf(QueryBuilder.literal(1), QueryBuilder.literal(2))
 * val select = QueryBuilder.selectFrom("users").all()
 *     .where(QueryBuilder.column("id").inValues(terms))
 * // select.asCql() contains "WHERE id IN (1,2)"
 * ```
 *
 * @param alternatives 대안 값 목록
 * @return 관계가 적용된 결과
 */
fun <T> InRelationBuilder<T>.inValues(alternatives: Iterable<Term>): T = `in`(alternatives)

/**
 * `IN` 절을 vararg [Term]으로 표현합니다. Kotlin의 예약어 `in`을 회피합니다.
 *
 * ```kotlin
 * val select = QueryBuilder.selectFrom("users").all()
 *     .where(QueryBuilder.column("id").inValues(QueryBuilder.literal(1), QueryBuilder.literal(2)))
 * // select.asCql() contains "WHERE id IN (1,2)"
 * ```
 *
 * @param alternatives 대안 값들
 * @return 관계가 적용된 결과
 */
fun <T> InRelationBuilder<T>.inValues(vararg alternatives: Term): T = `in`(*alternatives)
