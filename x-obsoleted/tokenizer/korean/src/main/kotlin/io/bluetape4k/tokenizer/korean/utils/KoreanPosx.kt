package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Alpha
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.CashTag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Determiner
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Email
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Foreign
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Hashtag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Korean
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.KoreanParticle
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Number
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Others
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Punctuation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ScreenName
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.URL
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.VerbPrefix

/**
 * 품사 규칙 문자열을 트라이 구조로 변환하는 유틸리티입니다.
 *
 * ## 동작/계약
 * - 축약 기호(`N`, `V`, `j` 등)를 `KoreanPos`로 매핑해 파서 규칙을 구성한다.
 * - `buildTrie`는 `+`, `*`, `1`, `0` 규칙 문법만 지원한다.
 * - `SelfNode`는 반복 규칙 전이에서 현재 노드 재사용 마커로 동작한다.
 *
 * ```kotlin
 * val trie = KoreanPosx.buildTrie("N1", KoreanPos.Noun)
 * // trie.isNotEmpty() == true
 * ```
 */
object KoreanPosx: KLogging() {

    /**
     * 청크 레벨 기타 품사 집합입니다.
     *
     * ## 동작/계약
     * - `Korean`, `Foreign`, `Number` 등 어절 파싱 이전 품사를 포함한다.
     *
     * ```kotlin
     * val hasUrl = KoreanPosx.OtherPoses.contains(KoreanPos.URL)
     * // hasUrl == true
     * ```
     */
    val OtherPoses = hashSetOf(
        Korean,
        Foreign,
        Number,
        KoreanParticle,
        Alpha,
        Punctuation,
        Hashtag,
        ScreenName,
        Email,
        URL,
        CashTag
    )

    /**
     * 규칙 문자열 문자와 품사의 매핑 테이블입니다.
     *
     * ## 동작/계약
     * - `buildTrie`가 첫 글자를 해석할 때 이 매핑을 사용한다.
     *
     * ```kotlin
     * val noun = KoreanPosx.shortCut['N']
     * // noun == KoreanPos.Noun
     * ```
     */
    val shortCut = hashMapOf(
        'N' to Noun,
        'V' to Verb,
        'J' to Adjective,
        'A' to Adverb,
        'D' to Determiner,
        'E' to Exclamation,
        'C' to Conjunction,

        'j' to Josa,
        'e' to Eomi,
        'r' to PreEomi,
        'm' to Modifier,
        'v' to VerbPrefix,
        's' to Suffix,

        'a' to Alpha,
        'n' to Number,

        'o' to Others
    )

    private const val PLUS = '+'
    private const val ANY = '*'
    private const val ONE = '1'
    private const val ZERO = '0'

    /**
     * 반복 규칙에서 현재 노드를 그대로 재사용할 때 쓰는 센티널 노드입니다.
     *
     * ## 동작/계약
     * - `curPos`, `nextTrie`, `ending`이 모두 null인 노드다.
     *
     * ```kotlin
     * val self = KoreanPosx.SelfNode
     * // self.curPos == null
     * ```
     */
    val SelfNode = KoreanPosTrie(curPos = null, nextTrie = null, ending = null)

    /**
     * 규칙 문자열을 품사 트라이 노드 목록으로 변환합니다.
     *
     * ## 동작/계약
     * - 길이가 2 미만인 문자열은 해석하지 않고 빈 리스트를 반환한다.
     * - 지원 규칙은 `+`, `*`, `1`, `0`이며 그 외 문자는 `error`를 발생시킨다.
     *
     * ```kotlin
     * val trie = KoreanPosx.buildTrie("N1", KoreanPos.Noun)
     * // trie.first().curPos == KoreanPos.Noun
     * ```
     */
    fun buildTrie(s: String, endingPos: KoreanPos): List<KoreanPosTrie> {

        fun isFinal(rest: String): Boolean {
            return rest.isEmpty() ||
                    rest.fold(true) { output, c ->
                        if (c == '+' || c == '1') false
                        else output
                    }
        }

        // 한자라면 Trie 를 빌드하지 않습니다.
        if (s.length < 2) {
            return emptyList()
        }

        val pos = shortCut[s[0]]
        val rule = s[1]
        val rest = s.slice(2 until s.length)
        val end = if (isFinal(rest)) endingPos else null

        return when (rule) {
            PLUS -> mutableListOf(
                KoreanPosTrie(
                    pos,
                    mutableListOf<KoreanPosTrie>().apply {
                        add(SelfNode)
                        addAll(buildTrie(rest, endingPos))
                    },
                    end
                )
            )

            ANY  -> mutableListOf<KoreanPosTrie>().apply {
                add(KoreanPosTrie(pos, mutableListOf(SelfNode) + buildTrie(rest, endingPos), end))
                addAll(buildTrie(rest, endingPos))
            }

            ONE  -> mutableListOf(KoreanPosTrie(pos, buildTrie(rest, endingPos), end))
            ZERO -> mutableListOf<KoreanPosTrie>().apply {
                add(KoreanPosTrie(pos, buildTrie(rest, endingPos), end))
                addAll(buildTrie(rest, endingPos))
            }

            else -> error("Not supported rule. only support [$PLUS, $ANY, $ONE, $ZERO]")
        }
    }

    /**
     * 규칙 맵 전체를 하나의 트라이 목록으로 병합합니다.
     *
     * ## 동작/계약
     * - 각 `(규칙, 종결품사)`에 대해 `buildTrie` 결과를 앞쪽에 누적한다.
     * - 반환 리스트는 `destination` 인스턴스를 그대로 사용한다.
     *
     * ```kotlin
     * val trie = KoreanPosx.getTrie(mapOf("N1" to KoreanPos.Noun))
     * // trie.isNotEmpty() == true
     * ```
     */
    internal fun getTrie(
        sequences: Map<String, KoreanPos>,
        destination: MutableList<KoreanPosTrie> = mutableListOf(),
    ): List<KoreanPosTrie> {
        sequences.forEach { (key, value) ->
            destination.addAll(0, buildTrie(key, value))
        }
        return destination
    }

    /**
     * 용언 품사 집합입니다.
     *
     * ## 동작/계약
     * - `Verb`, `Adjective` 두 품사만 포함한다.
     *
     * ```kotlin
     * val predicate = KoreanPosx.Predicates.contains(KoreanPos.Verb)
     * // predicate == true
     * ```
     */
    @JvmField
    val Predicates = setOf(Verb, Adjective)
}
