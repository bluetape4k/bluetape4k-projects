package io.bluetape4k.spring4.mongodb.query

import org.springframework.data.mongodb.core.query.Criteria

// ====================================================
// 비교 연산자
// ====================================================

/**
 * [Criteria.`is`]를 infix 형태로 호출할 수 있게 해 주는 별칭입니다.
 *
 * ## 동작/계약
 * - `Criteria.`is`(value)`와 동일하게 동작합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("name") eq "Alice"
 * // 동일: Criteria.where("name").`is`("Alice")
 * ```
 */
infix fun Criteria.eq(value: Any?): Criteria = `is`(value)

/**
 * [Criteria.ne]를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("status") ne "inactive"
 * ```
 */
infix fun Criteria.ne(value: Any?): Criteria = ne(value)

/**
 * [Criteria.gt]를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("age") gt 20
 * ```
 */
infix fun Criteria.gt(value: Any): Criteria = gt(value)

/**
 * [Criteria.gte]를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("age") gte 20
 * ```
 */
infix fun Criteria.gte(value: Any): Criteria = gte(value)

/**
 * [Criteria.lt]를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("age") lt 65
 * ```
 */
infix fun Criteria.lt(value: Any): Criteria = lt(value)

/**
 * [Criteria.lte]를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("age") lte 65
 * ```
 */
infix fun Criteria.lte(value: Any): Criteria = lte(value)

// ====================================================
// 컬렉션 연산자
// ====================================================

/**
 * [Criteria.`in`]을 infix 형태로 호출합니다.
 *
 * ## 동작/계약
 * - 지정한 컬렉션의 값 중 하나와 일치하는 문서를 조회합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("city") inValues listOf("Seoul", "Busan")
 * ```
 */
infix fun Criteria.inValues(values: Collection<*>): Criteria = `in`(values)

/**
 * vararg 방식으로 [Criteria.`in`]을 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("city") inValues arrayOf("Seoul", "Busan")
 * ```
 */
infix fun Criteria.inValues(values: Array<*>): Criteria = `in`(*values)

/**
 * [Criteria.nin]을 infix 형태로 호출합니다.
 *
 * ## 동작/계약
 * - 지정한 컬렉션의 값에 포함되지 않는 문서를 조회합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("status") notInValues listOf("deleted", "blocked")
 * ```
 */
infix fun Criteria.notInValues(values: Collection<*>): Criteria = nin(values)

/**
 * vararg 방식으로 [Criteria.nin]을 호출합니다.
 */
infix fun Criteria.notInValues(values: Array<*>): Criteria = nin(*values)

// ====================================================
// 문자열 연산자
// ====================================================

/**
 * [Criteria.regex] (패턴 문자열)를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("name") regex "^Alice"
 * ```
 */
infix fun Criteria.regex(pattern: String): Criteria = regex(pattern)

/**
 * [Criteria.regex] (Kotlin [Regex])를 infix 형태로 호출합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("name") regex Regex("^Alice", RegexOption.IGNORE_CASE)
 * ```
 */
infix fun Criteria.regex(pattern: Regex): Criteria = regex(pattern.toPattern())

// ====================================================
// Null / 존재 여부
// ====================================================

/**
 * null 값과 일치하는 조건을 추가합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("deletedAt").isNull()
 * ```
 */
val Criteria.isNull: Criteria get() = isNullValue()

/**
 * 필드가 존재하는 조건을 추가합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("email").fieldExists()
 * ```
 */
fun Criteria.fieldExists(): Criteria = exists(true)

/**
 * 필드가 존재하지 않는 조건을 추가합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("deletedAt").fieldNotExists()
 * ```
 */
fun Criteria.fieldNotExists(): Criteria = exists(false)

// ====================================================
// 배열 연산자
// ====================================================

/**
 * 배열 필드에 지정한 모든 값이 포함된 문서를 조회합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("tags") allValues listOf("kotlin", "mongodb")
 * ```
 */
infix fun Criteria.allValues(values: Collection<*>): Criteria = all(values)

/**
 * 배열 필드의 크기가 일치하는 문서를 조회합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("tags") sizeOf 3
 * ```
 */
infix fun Criteria.sizeOf(size: Int): Criteria = size(size)

/**
 * 배열 필드의 요소 중 지정한 [Criteria]에 맞는 요소가 있는 문서를 조회합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("scores") elemMatches Criteria.where("value").gt(90)
 * ```
 */
infix fun Criteria.elemMatches(criteria: Criteria): Criteria = elemMatch(criteria)

// ====================================================
// 논리 연산자
// ====================================================

/**
 * AND 조건을 추가합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("age").gt(20) andWith Criteria.where("city").`is`("Seoul")
 * ```
 */
infix fun Criteria.andWith(other: Criteria): Criteria = andOperator(other)

/**
 * OR 조건을 추가합니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("age").gt(20) orWith Criteria.where("city").`is`("Seoul")
 * ```
 */
infix fun Criteria.orWith(other: Criteria): Criteria = orOperator(other)

/**
 * NOR 조건을 추가합니다.
 *
 * ```kotlin
 * val criteria = Criteria().norOperatorWith(
 *     Criteria.where("status").`is`("deleted"),
 *     Criteria.where("blocked").`is`(true)
 * )
 * ```
 */
fun Criteria.norOperatorWith(vararg criteria: Criteria): Criteria = norOperator(*criteria)

// ====================================================
// 편의 팩토리 함수
// ====================================================

/**
 * 필드명 [String]에서 바로 [Criteria]를 시작합니다.
 *
 * ## 동작/계약
 * - `Criteria.where(this)`를 호출합니다.
 *
 * ```kotlin
 * val criteria = "name".criteria() eq "Alice"
 * // 동일: Criteria.where("name").`is`("Alice")
 * ```
 */
fun String.criteria(): Criteria = Criteria.where(this)

/**
 * 여러 [Criteria]을 AND로 결합한 [Criteria]를 생성합니다.
 *
 * ```kotlin
 * val criteria = criteriaOf(
 *     Criteria.where("age").gt(20),
 *     Criteria.where("city").`is`("Seoul")
 * )
 * ```
 */
fun criteriaOf(vararg criteria: Criteria): Criteria = Criteria().andOperator(*criteria)
