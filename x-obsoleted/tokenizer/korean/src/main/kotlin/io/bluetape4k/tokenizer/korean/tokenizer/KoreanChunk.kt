package io.bluetape4k.tokenizer.korean.tokenizer

import java.io.Serializable

/**
 * 원문에서 잘라낸 청크 텍스트와 위치 정보를 표현합니다.
 *
 * ## 동작/계약
 * - `text`는 원문 일부 구간 문자열이다.
 * - `offset`은 원문 기준 시작 인덱스다.
 * - `length`는 청크 문자열 길이다.
 *
 * ```kotlin
 * val chunk = KoreanChunk("한국어", 0, 3)
 * // chunk.length == 3
 * ```
 *
 * @property text 청크 텍스트
 * @property offset 원문 기준 시작 위치
 * @property length 청크 길이
 */
data class KoreanChunk(
    val text: String,
    val offset: Int,
    val length: Int,
): Serializable
