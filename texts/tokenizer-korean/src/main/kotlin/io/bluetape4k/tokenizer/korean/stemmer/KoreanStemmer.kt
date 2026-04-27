package io.bluetape4k.tokenizer.korean.stemmer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import java.util.*

/**
 * 형태소 분석 결과의 용언 토큰에 원형(stem)을 채우고 어미 토큰을 병합합니다.
 *
 * ## 동작/계약
 * - 입력이 비어 있거나 용언(`Verb`, `Adjective`)이 없으면 원본 리스트를 그대로 반환한다.
 * - 용언 뒤에 `Eomi`, `PreEomi`가 이어지면 앞선 용언 토큰 텍스트/길이에 어미를 합친다.
 * - 용언 토큰은 `KoreanDictionaryProvider.predicateStems`를 조회해 `stem` 값을 채운다.
 *
 * ```kotlin
 * val tokens = KoreanTokenizer.tokenizeTopN("가느다란").flatMap { it.first() }
 * val stemmed = KoreanStemmer.stem(tokens)
 * // stemmed == [KoreanToken("가느다란", Verb, 0, 4, stem = "갈다")]
 * ```
 */
object KoreanStemmer: KLogging() {

    /**
     * 용언 뒤에 붙는 어미 품사 집합입니다.
     *
     * ## 동작/계약
     * - `stem`에서 현재 토큰이 이 집합에 속하면 직전 용언에 병합 대상이 된다.
     *
     * ```kotlin
     * val mergeable = KoreanStemmer.Endings.contains(Eomi)
     * // mergeable == true
     * ```
     */
    @JvmField
    val Endings = setOf(Eomi, PreEomi)

    /**
     * 원형 사전 조회 대상인 용언 품사 집합입니다.
     *
     * ## 동작/계약
     * - `stem`은 이 집합에 속한 토큰만 `predicateStems` 조회로 `stem`을 채운다.
     *
     * ```kotlin
     * val isPredicate = KoreanStemmer.Predicates.contains(Verb)
     * // isPredicate == true
     * ```
     */
    @JvmField
    val Predicates = setOf(Verb, Adjective)

    /**
     * 명사 뒤 결합 시 용언으로 처리하는 표면형 집합입니다.
     *
     * ## 동작/계약
     * - 품사 평가 로직에서 명사 결합형(`하다`, `되다`, `없다`) 판별 기준으로 사용된다.
     *
     * ```kotlin
     * val supported = "하다" in KoreanStemmer.EndingForNouns
     * // supported == true
     * ```
     */
    @JvmField
    val EndingForNouns = setOf("하다", "되다", "없다")


    /**
     * 입력 토큰 열에 대해 어미 병합과 용언 원형 복원을 수행합니다.
     *
     * ## 동작/계약
     * - `KoreanStemmerTest` 기준으로 `이럴(Adjective)+수(PreEomi)+가(Eomi)`를 `이럴수가(Adjective, stem=이렇다)`로 병합한다.
     * - `KoreanStemmerTest` 기준으로 `"가느다란"` 분석 결과를 `Verb` 토큰 하나로 유지하고 `stem="갈다"`를 채운다.
     * - 병합/복원은 새 리스트에 누적한 뒤 입력 순서로 다시 뒤집어 반환한다.
     *
     * ```kotlin
     * val tokens = KoreanTokenizer.tokenizeTopN("가느다란").flatMap { it.first() }
     * val result = KoreanStemmer.stem(tokens)
     * // result.first().stem == "갈다"
     * ```
     */
    fun stem(tokens: List<KoreanToken>): List<KoreanToken> {
        if (tokens.isEmpty()) {
            return tokens
        }
        if (!tokens.any { it.pos in Predicates }) {
            return tokens
        }

        val stemmed = LinkedList<KoreanToken>()

        tokens
            .onEach { token ->
                if (stemmed.isNotEmpty() && Endings.contains(token.pos)) {
                    if (Predicates.contains(stemmed.first().pos)) {
                        val prevToken = stemmed.first()
                        val token1 = prevToken.copy(
                            text = prevToken.text + token.text,
                            length = prevToken.length + token.length
                        )
                        stemmed[0] = token1
                    } else {
                        stemmed.add(0, token)
                    }
                } else if (token.pos in Predicates) {
                    val stem = KoreanDictionaryProvider.predicateStems[token.pos]?.get(token.text)
                    // val token1 = token.copy(stem = stem)
                    // log.trace { "동사, 형용사 활용: text=${token.text}, stem=$stem : $token -> $token1" }
                    stemmed.add(0, token.copy(stem = stem))
                } else {
                    log.trace { "not found stem for $token" }
                    stemmed.add(0, token)
                }
            }

        return stemmed.reversed()
    }
}
