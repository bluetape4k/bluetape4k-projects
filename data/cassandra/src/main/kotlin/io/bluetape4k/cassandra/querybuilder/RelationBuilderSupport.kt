package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.BindMarker
import com.datastax.oss.driver.api.querybuilder.relation.ArithmeticRelationBuilder
import com.datastax.oss.driver.api.querybuilder.relation.InRelationBuilder
import com.datastax.oss.driver.api.querybuilder.term.Term

/**
 * `=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.eq(rightOperand: Term): T = isEqualTo(rightOperand)

/**
 * `!=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.ne(rightOperand: Term): T = isNotEqualTo(rightOperand)

/**
 * `<` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.lt(rightOperand: Term): T = isLessThan(rightOperand)

/**
 * `<=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.lte(rightOperand: Term): T = isLessThanOrEqualTo(rightOperand)

/**
 * `>` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.gt(rightOperand: Term): T = isGreaterThan(rightOperand)

/**
 * `>=` 비교 관계를 Kotlin infix 구문으로 표현합니다.
 *
 * @param rightOperand 비교 대상 [Term]
 * @return 관계가 적용된 결과
 */
infix fun <T> ArithmeticRelationBuilder<T>.gte(rightOperand: Term): T = isGreaterThanOrEqualTo(rightOperand)

/**
 * `IN` 절을 [BindMarker]로 표현합니다. Kotlin의 예약어 `in`을 회피합니다.
 *
 * @param bindMarker 바인드 마커
 * @return 관계가 적용된 결과
 */
fun <T> InRelationBuilder<T>.inValues(bindMarker: BindMarker): T = `in`(bindMarker)

/**
 * `IN` 절을 [Term] 목록으로 표현합니다. Kotlin의 예약어 `in`을 회피합니다.
 *
 * @param alternatives 대안 값 목록
 * @return 관계가 적용된 결과
 */
fun <T> InRelationBuilder<T>.inValues(alternatives: Iterable<Term>): T = `in`(alternatives)

/**
 * `IN` 절을 vararg [Term]으로 표현합니다. Kotlin의 예약어 `in`을 회피합니다.
 *
 * @param alternatives 대안 값들
 * @return 관계가 적용된 결과
 */
fun <T> InRelationBuilder<T>.inValues(vararg alternatives: Term): T = `in`(*alternatives)
