package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.support.emptyIntArray
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ProperNoun
import java.io.Serializable

/**
 * 한글 형태소 분석기의 분석 옵션을 설정하는 프로필 클래스입니다.
 *
 * 각 가중치 값은 형태소 분석 시 후보 선택에 영향을 미칩니다.
 * 값이 클수록 해당 항목을 선호하며, 음수 값은 해당 항목을 비선호합니다.
 *
 * @property tokenCount 토큰 수 가중치 (기본값: 0.18f)
 * @property unknown 미등록 단어(Unknown)에 대한 가중치 (기본값: 0.3f)
 * @property wordCount 단어 수 가중치 (기본값: 0.3f)
 * @property freq 단어 빈도수 가중치 (기본값: 0.2f)
 * @property unknownCoverage 미등록 단어 커버리지 가중치 (기본값: 0.5f)
 * @property exactMatch 정확한 매칭에 대한 가중치 (기본값: 0.5f)
 * @property allNoun 전체 명사 처리에 대한 가중치 (기본값: 0.1f)
 * @property unknownPosCount 미등록 품사 수에 대한 가중치 (기본값: 10.0f)
 * @property determinerPosCount 관형사 수에 대한 가중치 (기본값: -0.01f)
 * @property exclamationPosCount 감탄사 수에 대한 가중치 (기본값: 0.01f)
 * @property initialPostPosition 초기 조사 위치 가중치 (기본값: 0.2f)
 * @property haVerb '하다' 동사 가중치 (기본값: 0.3f)
 * @property preferredPattern 선호 패턴 가중치 (기본값: 0.6f)
 * @property preferredPatterns 선호하는 품사 패턴 목록 (기본값: [[Noun, Josa], [ProperNoun, Josa]])
 * @property spaceGuide 공백 위치 가이드 배열 (기본값: 빈 배열)
 * @property spaceGuidePenalty 공백 가이드 미준수 시 패널티 (기본값: 3.0f)
 * @property josaUnmatchedPenalty 조사 불일치 패널티 (기본값: 3.0f)
 *
 * @see KoreanTokenizer
 */
data class TokenizerProfile(
    val tokenCount: Float = 0.18f,
    val unknown: Float = 0.3f,
    val wordCount: Float = 0.3f,
    val freq: Float = 0.2f,
    val unknownCoverage: Float = 0.5f,
    val exactMatch: Float = 0.5f,
    val allNoun: Float = 0.1f,
    val unknownPosCount: Float = 10.0f,
    val determinerPosCount: Float = -0.01f,
    val exclamationPosCount: Float = 0.01f,
    val initialPostPosition: Float = 0.2f,
    val haVerb: Float = 0.3f,
    val preferredPattern: Float = 0.6f,
    val preferredPatterns: List<List<KoreanPos>> = listOf(listOf(Noun, Josa), listOf(ProperNoun, Josa)),
    val spaceGuide: IntArray = emptyIntArray,
    val spaceGuidePenalty: Float = 3.0f,
    val josaUnmatchedPenalty: Float = 3.0f,
): Serializable {
    companion object {
        /** 기본 분석 프로필 */
        @JvmField
        val DefaultProfile = TokenizerProfile()
    }
}
