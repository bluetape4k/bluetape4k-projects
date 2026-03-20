package io.bluetape4k.spring4.mongodb.query

import org.springframework.data.mongodb.core.query.Update

// ====================================================
// Update 팩토리 함수
// ====================================================

/**
 * 여러 필드-값 쌍으로 [Update]를 생성합니다.
 *
 * ## 동작/계약
 * - 각 쌍은 `$set` 연산으로 처리됩니다.
 *
 * ```kotlin
 * val update = updateOf("name" to "Alice", "age" to 31)
 * // 동일: Update().set("name", "Alice").set("age", 31)
 * ```
 */
fun updateOf(vararg pairs: Pair<String, Any?>): Update {
    val update = Update()
    pairs.forEach { (field, value) -> update.set(field, value) }
    return update
}

// ====================================================
// Update infix DSL
// ====================================================

/**
 * 필드에 값을 설정하는 [Update]를 생성합니다.
 *
 * ## 동작/계약
 * - `Update.update(this, value)`를 호출합니다.
 *
 * ```kotlin
 * val update = "name" setTo "Alice"
 * // 동일: Update.update("name", "Alice")
 * ```
 */
infix fun String.setTo(value: Any?): Update = Update.update(this, value)

/**
 * 필드 값을 증가시키는 [Update]를 생성합니다.
 *
 * ## 동작/계약
 * - `Update().inc(this, value)`를 호출합니다.
 *
 * ```kotlin
 * val update = "score" incBy 10
 * ```
 */
infix fun String.incBy(value: Number): Update = Update().inc(this, value)

/**
 * 필드를 제거하는 [Update]를 생성합니다.
 *
 * ## 동작/계약
 * - `Update().unset(this)`를 호출합니다.
 *
 * ```kotlin
 * val update = "deletedAt".unsetField()
 * ```
 */
fun String.unsetField(): Update = Update().unset(this)

/**
 * 배열 필드에 값을 추가하는 [Update]를 생성합니다.
 *
 * ```kotlin
 * val update = "tags" pushValue "kotlin"
 * ```
 */
infix fun String.pushValue(value: Any): Update = Update().push(this, value)

/**
 * 배열 필드에서 값을 제거하는 [Update]를 생성합니다.
 *
 * ```kotlin
 * val update = "tags" pullValue "deprecated"
 * ```
 */
infix fun String.pullValue(value: Any): Update = Update().pull(this, value)

// ====================================================
// Update 체이닝 확장
// ====================================================

/**
 * 기존 [Update]에 추가 `$set` 연산을 체이닝합니다.
 *
 * ```kotlin
 * val update = ("name" setTo "Alice").andSet("age", 31).andSet("city", "Seoul")
 * ```
 */
fun Update.andSet(
    field: String,
    value: Any?,
): Update = set(field, value)

/**
 * 기존 [Update]에 추가 `$inc` 연산을 체이닝합니다.
 *
 * ```kotlin
 * val update = ("score" incBy 10).andInc("level", 1)
 * ```
 */
fun Update.andInc(
    field: String,
    value: Number,
): Update = inc(field, value)

/**
 * 기존 [Update]에 추가 `$unset` 연산을 체이닝합니다.
 *
 * ```kotlin
 * val update = ("name" setTo "Alice").andUnset("tempField")
 * ```
 */
fun Update.andUnset(field: String): Update = unset(field)

/**
 * 기존 [Update]에 추가 `$push` 연산을 체이닝합니다.
 *
 * ```kotlin
 * val update = ("score" incBy 10).andPush("history", 100)
 * ```
 */
fun Update.andPush(
    field: String,
    value: Any,
): Update = push(field, value)
