package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.support.emptyIntArray
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ProperNoun
import java.io.Serializable

/**
 * 파싱 후보 점수 계산에 사용하는 가중치 프로필입니다.
 *
 * ## 동작/계약
 * - `ParsedChunk.score` 계산식은 이 프로필 값을 선형 결합해 후보 순위를 정한다.
 * - 값이 클수록 해당 항목을 더 선호하며, 음수 값은 패널티로 동작한다.
 * - `spaceGuide`는 원문 공백 위치 힌트로 사용되어 가이드 밖 분해에 패널티를 부여한다.
 *
 * ```kotlin
 * val profile = TokenizerProfile.DefaultProfile.copy(unknown = 0.5f)
 * // profile.unknown == 0.5f
 * ```
 *
 * @property tokenCount 토큰 수 가중치
 * @property unknown 미등록 단어 가중치
 * @property wordCount 단어 수 가중치
 * @property freq 명사 빈도 가중치
 * @property unknownCoverage 미등록 토큰 길이 커버리지 가중치
 * @property exactMatch 단일 토큰 정확 매칭 가중치
 * @property allNoun 전부 명사인 경우 가중치
 * @property unknownPosCount Unknown 품사 개수 가중치
 * @property determinerPosCount 관형사 개수 가중치
 * @property exclamationPosCount 감탄사 개수 가중치
 * @property initialPostPosition 접미/조사 시작 패널티 가중치
 * @property haVerb `하다/해` 결합 선호 가중치
 * @property preferredPattern 선호 품사 패턴 가중치
 * @property preferredPatterns 선호 품사 패턴 목록
 * @property spaceGuide 공백 가이드 인덱스 배열
 * @property spaceGuidePenalty 공백 가이드 위반 패널티
 * @property josaUnmatchedPenalty 조사 불일치 패널티
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
        /**
         * 기본 형태소 분석 프로필입니다.
         *
         * ## 동작/계약
         * - `TokenizerProfile()` 기본 생성값으로 초기화된다.
         *
         * ```kotlin
         * val profile = TokenizerProfile.DefaultProfile
         * // profile.spaceGuide.isEmpty() == true
         * ```
         */
        @JvmField
        val DefaultProfile = TokenizerProfile()
    }
}
