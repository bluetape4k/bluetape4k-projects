package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.Hangul.composeHangul
import io.bluetape4k.tokenizer.korean.utils.Hangul.hasCoda
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun

/**
 * 한글 명사와 조사를 위한 Helper Class
 */
object KoreanSubstantive: KLogging() {

    private val JOSA_HEAD_FOR_CODA = setOf('은', '이', '을', '과', '아')
    private val JOSA_HEAD_FOR_NO_CODA = setOf('는', '가', '를', '와', '야', '여', '라')

    /**
     * [prevChar] 다음에 [headChar]이 오면 조사를 붙일 수 있는지 확인합니다.
     *
     * ```
     * isJosaAttachable('플', '은') == true // 애플은
     * isJosaAttachable('플', '는') == false // 애플는
     *
     * isJosaAttachable('프', '은') == false // 애프은
     * isJosaAttachable('프', '는') == true // 애프는
     * ```
     *
     * @param prevChar 이전 글자
     * @param headChar 다음 글자
     * @return 조사를 붙일 수 있는지 여부
     */
    fun isJosaAttachable(prevChar: Char, headChar: Char): Boolean {
        return (hasCoda(prevChar) && headChar !in JOSA_HEAD_FOR_NO_CODA) ||
                (!hasCoda(prevChar) && headChar !in JOSA_HEAD_FOR_CODA)
    }

    //  fun isName(str: String): Boolean = isName(str as CharSequence)

    /**
     * [chunk] 가 한글 이름인지 확인합니다.
     *
     * ```
     * isName("문재인") == true
     * isName("강철중") == true
     * isName("사다리") == false
     * ```
     * @param chunk 검사할 글자
     * @return 이름인지 여부
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
     * 한글 숫자 텍스트인지 확인합니다.
     *
     * ```
     * isKoreanNumber("천이백만이십오") == true
     * isKoreanNumber("이십") == true
     * isKoreanNumber("일천").shouldBeTrue()
     * ```
     *
     * @param chunk 검사할 글자
     * @return 한글 숫자인지 여부
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
     * 명사의 'ㅇ' 생략 변형인지 확인합니다.
     *
     * ```
     * 우혀니 -> 우현, 우현이       // true
     * 빠순이 -> 빠순, 빠순이       // false
     * ```
     *
     * ```
     * isKoreanNameVariation("호혀니").shouldBeTrue()
     * isKoreanNameVariation("혜지니").shouldBeTrue()
     * isKoreanNameVariation("빠수니").shouldBeTrue()
     * ```
     *
     * ```
     * isKoreanNameVariation("가라찌").shouldBeFalse()
     * isKoreanNameVariation("귀요미").shouldBeFalse()
     * isKoreanNameVariation("사람이").shouldBeFalse()
     * ```
     *
     * @param chunk 검사할 글자
     * @return `ㅇ` 이 빠진 변형이면 true
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
     * [posNodes]의 모든 단어를 하나의 알 수 없는 명사로 합칩니다.
     *
     * ```
     * val tokens = collapseNouns('마', '코', '토')  // KoreanToken("마코토", Noun, 0, 3, unknown = true)
     * ```
     *
     * @param posNodes 한글 토큰 컬렉션
     * @return 알 수 없는 명사로 합쳐진 토큰 컬렉션
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
