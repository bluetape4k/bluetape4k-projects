package io.bluetape4k.tokenizer.korean.utils

/**
 * 문자가 유니코드 Space 문자 범주인지 확인합니다.
 *
 * ## 동작/계약
 * - `Character.isSpaceChar(this)` 호출 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val space = ' '.isSpaceChar
 * // space == true
 * ```
 */
val Char.isSpaceChar: Boolean get() = Character.isSpaceChar(this)

/**
 * 마지막 문자를 제거한 새 문자열을 반환합니다.
 *
 * ## 동작/계약
 * - 빈 문자열이면 빈 문자열을 반환한다.
 * - 비어 있지 않으면 `dropLast(1)` 결과를 반환한다.
 *
 * ```kotlin
 * val head = "한국어".init()
 * // head == "한국"
 * ```
 */
fun String.init(): String = if (isEmpty()) "" else dropLast(1)

/**
 * 마지막 문자를 제거한 `CharSequence`를 반환합니다.
 *
 * ## 동작/계약
 * - 빈 시퀀스면 빈 문자열을 반환한다.
 * - 비어 있지 않으면 `dropLast(1)` 결과를 반환한다.
 *
 * ```kotlin
 * val cs: CharSequence = "사람"
 * val head = cs.init()
 * // head == "사"
 * ```
 */
fun CharSequence.init(): CharSequence = if (isEmpty()) "" else dropLast(1)

/**
 * 리스트의 마지막 원소를 제외한 새 리스트를 반환합니다.
 *
 * ## 동작/계약
 * - 빈 리스트면 `emptyList()`를 반환한다.
 * - 비어 있지 않으면 `dropLast(1)` 결과를 반환한다.
 *
 * ```kotlin
 * val head = listOf(1, 2, 3).init()
 * // head == [1, 2]
 * ```
 */
fun <T> List<T>.init(): List<T> = if (isEmpty()) emptyList() else dropLast(1)
