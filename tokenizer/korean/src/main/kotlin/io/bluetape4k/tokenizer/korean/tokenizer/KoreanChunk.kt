package io.bluetape4k.tokenizer.korean.tokenizer

import java.io.Serializable

/**
 * 한국어 형태소 분석기에서 사용하는 Chunk
 *
 * @property text 형태소 분석기에서 추출한 텍스트
 * @property offset 형태소 분석기에서 추출한 텍스트의 시작 위치
 * @property length 형태소 분석기에서 추출한 텍스트의 길이
 */
data class KoreanChunk(
    val text: String,
    val offset: Int,
    val length: Int,
): Serializable
