package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.Hangul.composeHangul
import io.bluetape4k.tokenizer.korean.utils.Hangul.hasCoda
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun

/**
 * 명사/조사 결합과 이름·수사 판별에 사용하는 명사 유틸입니다.
 *
 * ## 동작/계약
 * - 조사 결합 가능성은 받침 유무와 조사 첫 글자 규칙으로 판정한다.
 * - 이름 판별은 `KoreanDictionaryProvider.nameDictionary` 분류 사전을 사용한다.
 * - 미등록 1글자 명사 연쇄는 `collapseNouns`에서 하나의 unknown 명사로 합친다.
 *
 * ```kotlin
 * val attachable = KoreanSubstantive.isJosaAttachable('플', '은')
 * // attachable == true
 * ```
 */
object KoreanSubstantive: KLogging() {

    private val JOSA_HEAD_FOR_CODA = setOf('은', '이', '을', '과', '아')
    private val JOSA_HEAD_FOR_NO_CODA = setOf('는', '가', '를', '와', '야', '여', '라')

    /**
     * 앞 글자와 조사 첫 글자 조합이 문법적으로 가능한지 확인합니다.
     *
     * ## 동작/계약
     * - 앞 글자에 받침이 있으면 `는/가/를/와/야/여/라`를 제외한 조사만 허용한다.
     * - 앞 글자에 받침이 없으면 `은/이/을/과/아`를 제외한 조사만 허용한다.
     *
     * ```kotlin
     * val ok = KoreanSubstantive.isJosaAttachable('플', '은')
     * // ok == true
     * ```
     */
    fun isJosaAttachable(prevChar: Char, headChar: Char): Boolean {
        return (hasCoda(prevChar) && headChar !in JOSA_HEAD_FOR_NO_CODA) ||
                (!hasCoda(prevChar) && headChar !in JOSA_HEAD_FOR_CODA)
    }

    //  fun isName(str: String): Boolean = isName(str as CharSequence)

    /**
     * 주어진 문자열이 이름 사전 규칙에 맞는지 확인합니다.
     *
     * ## 동작/계약
     * - `full_name` 또는 `given_name` 사전에 있으면 즉시 `true`다.
     * - 길이 3/4인 경우 성+이름 분해(`family_name` + `given_name`) 조합으로 판정한다.
     * - 그 외 길이는 `false`를 반환한다.
     *
     * ```kotlin
     * val isName = KoreanSubstantive.isName("문재인")
     * // isName == true
     * ```
     */
    fun isName(chunk: CharSequence): Boolean {
        if (nameDictionaryContains("full_name", chunk) || nameDictionaryContains("given_name", chunk)) {
            return true
        }

        return when (chunk.length) {
            3 -> nameDictionaryContains("family_name", chunk[0].toString()) &&
                    nameDictionaryContains("given_name", chunk.subSequence(1, 3).toString())

            4 -> nameDictionaryContains("family_name", chunk.subSequence(0, 2).toString()) &&
                    nameDictionaryContains("given_name", chunk.subSequence(2, 4).toString())

            else -> false
        }
    }

    private val NUMBER_CHARS = "일이삼사오육칠팔구천백십해경조억만".map { it.code }.toSet()
    private val NUMBER_LAST_CHARS = "일이삼사오육칠팔구천백십해경조억만원배분초".map { it.code }.toSet()

    /**
     * 문자열이 한글 수사 문자 집합으로만 구성되는지 확인합니다.
     *
     * ## 동작/계약
     * - 마지막 문자는 `원/배/분/초`를 포함한 확장 집합으로 판정한다.
     * - 마지막 이전 문자는 기본 수사 문자 집합으로 판정한다.
     *
     * ```kotlin
     * val number = KoreanSubstantive.isKoreanNumber("천이백만이십오")
     * // number == true
     * ```
     */
    fun isKoreanNumber(chunk: CharSequence): Boolean =
        (0 until chunk.length).fold(true) { output, i ->
            if (i < chunk.length - 1) {
                output && NUMBER_CHARS.contains(chunk[i].code)
            } else {
                output && NUMBER_LAST_CHARS.contains(chunk[i].code)
            }
        }

    /**
     * 이름의 종성/초성 변형(예: 우혀니)을 원형 이름으로 복원 가능한지 판정합니다.
     *
     * ## 동작/계약
     * - 길이 3..5가 아니면 `false`를 반환한다.
     * - 마지막 글자가 `ㅇ` 초성 생략 패턴(`*히/니`류) 조건을 만족할 때만 복원 시도를 한다.
     * - 복원 문자열과 마지막 글자 제거 문자열 둘 중 하나가 `isName`이면 `true`다.
     *
     * ```kotlin
     * val variation = KoreanSubstantive.isKoreanNameVariation("호혀니")
     * // variation == true
     * ```
     */
    fun isKoreanNameVariation(chunk: CharSequence): Boolean {
        // val nounDict = KoreanDictionaryProvider.koreanDictionary[Noun]!!

        if (isName(chunk)) return true

        val s = chunk.toString()
        if (s.length !in 3..5) {
            return false
        }

        val decomposed: List<Hangul.HangulChar> = s.map(Hangul::decomposeHangul)
        val lastChar = decomposed.last()
        if (lastChar.onset !in Hangul.CODA_MAP.keys) return false
        if (lastChar.onset == 'ㅇ' || lastChar.vowel != 'ㅣ' || lastChar.coda != ' ') return false
        if (decomposed.init().last().coda != ' ') return false

        // Recover missing 'ㅇ' (우혀니 -> 우현, 우현이, 빠순이 -> 빠순, 빠순이)
        val recovered: String = decomposed.mapIndexed { i, hc ->
            when (i) {
                s.lastIndex -> '이'
                s.lastIndex - 1 -> composeHangul(hc.copy(coda = decomposed.last().onset))
                else        -> composeHangul(hc)
            }
        }.joinToString("")

        return listOf(recovered, recovered.init()).any { isName(it) }
    }

    /**
     * 연속된 1글자 명사 토큰을 하나의 unknown 명사 토큰으로 병합합니다.
     *
     * ## 동작/계약
     * - `Noun`이면서 길이 1인 토큰이 연속되면 첫 토큰에 텍스트를 이어 붙여 병합한다.
     * - 병합된 토큰은 `unknown = true`로 설정한다.
     * - 비명사 또는 길이 1이 아닌 토큰을 만나면 병합 상태를 종료한다.
     *
     * ```kotlin
     * val merged = KoreanSubstantive.collapseNouns(
     *     listOf(KoreanToken("마", Noun, 0, 1), KoreanToken("코", Noun, 1, 1), KoreanToken("토", Noun, 2, 1))
     * )
     * // merged.first().text == "마코토"
     * ```
     */
    fun collapseNouns(posNodes: Iterable<KoreanToken>): List<KoreanToken> {
        val nodes = mutableListOf<KoreanToken>()
        var collapsing = false

        posNodes.forEach {
            if (it.pos == Noun && it.text.length == 1) {
                if (collapsing) {
                    val text = nodes[0].text + it.text
                    val offset = nodes[0].offset
                    nodes[0] = KoreanToken(text, Noun, offset, text.length, unknown = true)
                    // collapsing = true
                } else {
                    nodes.add(0, it)
                    collapsing = true
                }
            } else {
                nodes.add(0, it)
                collapsing = false
            }
        }
        return nodes.reversed()
    }
}
