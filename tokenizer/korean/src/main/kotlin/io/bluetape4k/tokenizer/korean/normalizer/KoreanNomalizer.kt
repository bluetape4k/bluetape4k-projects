package io.bluetape4k.tokenizer.korean.normalizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.sliding
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.utils.Hangul
import io.bluetape4k.tokenizer.korean.utils.Hangul.composeHangul
import io.bluetape4k.tokenizer.korean.utils.Hangul.decomposeHangul
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx
import io.bluetape4k.tokenizer.korean.utils.koreanContains
import java.util.regex.Matcher

/**
 * 한글 구어체의 의미 없는 글자들을 맞춤법에 맞게 정규화 합니다.
 *
 * ```
 * val expected = "안돼ㅋㅋㅋ내 심장을 가격했어ㅋㅋㅋ"
 * val actual = normalize("안됔ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ내 심장을 가격했엌ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ")
 * actual shouldBeEqualTo expected
 *
 * normalize("무의식중에 손들어버려섴ㅋㅋㅋㅋ") shouldBeEqualTo "무의식중에 손들어버려서ㅋㅋㅋ"
 * normalize("기억도 나지아낳ㅎㅎㅎ") shouldBeEqualTo "기억도 나지아나ㅎㅎㅎ"
 * normalize("근데비싸서못머구뮤ㅠㅠ") shouldBeEqualTo "근데비싸서못먹음ㅠㅠ"
 *
 * normalize("미친 존잘니뮤ㅠㅠㅠㅠ") shouldBeEqualTo "미친 존잘님ㅠㅠㅠ"
 * normalize("만나무ㅜㅜㅠ") shouldBeEqualTo "만남ㅜㅜㅠ"
 * normalize("가루ㅜㅜㅜㅜ") shouldBeEqualTo "가루ㅜㅜㅜ"
 *
 * normalize("유성우ㅠㅠㅠ") shouldBeEqualTo "유성우ㅠㅠㅠ"
 *
 * normalize("예뿌ㅠㅠ") shouldBeEqualTo "예뻐ㅠㅠ"
 * normalize("고수야고수ㅠㅠㅠ") shouldBeEqualTo "고수야고수ㅠㅠㅠ"
 *
 * normalize("안돼ㅋㅋㅋㅋㅋ") shouldBeEqualTo "안돼ㅋㅋㅋ"
 * ```
 *
 * ```
 * normalize("사브작사브작사브작사브작사브작사브작사브작사브작") shouldBeEqualTo "사브작사브작"
 * normalize("ㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎ") shouldBeEqualTo "ㅋㅋㅎㅋㅋㅎ"
 * ```
 *
 * ```
 * normalize("가쟝 용기있는 사람이 머굼 되는거즤") shouldBeEqualTo "가장 용기있는 사람이 먹음 되는거지"
 * ```
 *
 * ```
 * normalizeCodaN("버슨가") shouldBeEqualTo "버스인가"
 * normalizeCodaN("보슨지") shouldBeEqualTo "보스인지"
 * normalizeCodaN("쵸킨데") shouldBeEqualTo "쵸킨데"
 * ```
 */
object KoreanNormalizer: KLogging() {

    private val EXTENTED_KOREAN_REGEX: Regex = """([ㄱ-ㅣ가-힣]+)""".toRegex()
    private val KOREAN_TO_NORMALIZE_REGEX: Regex = """([가-힣]+)(ㅋ+|ㅎ+|[ㅠㅜ]+)""".toRegex()
    private val REPEATING_CHAR_REGEX: Regex = """(.)\1{3,}|[ㅠㅜ]{3,}""".toRegex()
    private val REPEATING_2CHAR_REGEX: Regex = """(..)\1{2,}""".toRegex()
    private val REPEATING_3CHAR_REGEX: Regex = """(...)\1{2,}""".toRegex()

    private val WHITESPACE_REGEX: Regex = """\s+""".toRegex()

    private val CODA_N_EXCPETION: CharArray = "은는운인텐근른픈닌든던".toCharArray()
    private val CODA_N_LAST_CHAR: CharArray = charArrayOf('데', '가', '지')

    private data class Segment(val text: String, val matchData: MatchResult?)

    /**
     * 한글 구어체를 맞춤법에 맞게 정규화합니다.
     *
     * ```
     * val expected = "안돼ㅋㅋㅋ내 심장을 가격했어ㅋㅋㅋ"
     * val actual = normalize("안됔ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ내 심장을 가격했엌ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ")
     * actual shouldBeEqualTo expected
     *
     * normalize("무의식중에 손들어버려섴ㅋㅋㅋㅋ") shouldBeEqualTo "무의식중에 손들어버려서ㅋㅋㅋ"
     * normalize("기억도 나지아낳ㅎㅎㅎ") shouldBeEqualTo "기억도 나지아나ㅎㅎㅎ"
     * normalize("근데비싸서못머구뮤ㅠㅠ") shouldBeEqualTo "근데비싸서못먹음ㅠㅠ"
     *
     * normalize("미친 존잘니뮤ㅠㅠㅠㅠ") shouldBeEqualTo "미친 존잘님ㅠㅠㅠ"
     * normalize("만나무ㅜㅜㅠ") shouldBeEqualTo "만남ㅜㅜㅠ"
     * normalize("가루ㅜㅜㅜㅜ") shouldBeEqualTo "가루ㅜㅜㅜ"
     *
     * normalize("유성우ㅠㅠㅠ") shouldBeEqualTo "유성우ㅠㅠㅠ"
     *
     * normalize("예뿌ㅠㅠ") shouldBeEqualTo "예뻐ㅠㅠ"
     * normalize("고수야고수ㅠㅠㅠ") shouldBeEqualTo "고수야고수ㅠㅠㅠ"
     *
     * normalize("안돼ㅋㅋㅋㅋㅋ") shouldBeEqualTo "안돼ㅋㅋㅋ"
     * ```
     *
     * ```
     * normalize("사브작사브작사브작사브작사브작사브작사브작사브작") shouldBeEqualTo "사브작사브작"
     * normalize("ㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎ") shouldBeEqualTo "ㅋㅋㅎㅋㅋㅎ"
     * ```
     *
     * ```
     * normalize("가쟝 용기있는 사람이 머굼 되는거즤") shouldBeEqualTo "가장 용기있는 사람이 먹음 되는거지"
     * ```
     *
     * ```
     * normalizeCodaN("버슨가") shouldBeEqualTo "버스인가"
     * normalizeCodaN("보슨지") shouldBeEqualTo "보스인지"
     * normalizeCodaN("쵸킨데") shouldBeEqualTo "쵸킨데"
     * ```
     *
     * @param input 입력 문자열
     * @return 정규화된 문자열
     */
    fun normalize(input: CharSequence): CharSequence {
        if (input.isBlank()) return input
        var match: MatchResult = EXTENTED_KOREAN_REGEX.find(input) ?: return input

        var lastStart = 0
        val length = input.length
        return buildString(length) {
            do {
                val foundMatch: MatchResult = match
                append(input, lastStart, foundMatch.range.first)
                append(normalizeKoreanChunk(foundMatch.groupValues.first()))
                lastStart = foundMatch.range.last + 1
                match = foundMatch.next() ?: break
            } while (lastStart < length)

            if (lastStart < length) {
                append(input, lastStart, length)
            }
        }
    }

    private fun normalizeKoreanChunk(input: CharSequence): CharSequence {

        // Normalize endings: 안됔ㅋㅋㅋ -> 안돼ㅋㅋ
        val endingNormalized = KOREAN_TO_NORMALIZE_REGEX.replace(input) {
            processNormalizationCandidate(it).toString()
        }

        // Normalize repeating chars: ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ -> ㅋㅋㅋ
        val exclamationNormalized = REPEATING_CHAR_REGEX.replace(endingNormalized) {
            Matcher.quoteReplacement(it.groupValues[0].take(3))
        }

        // Normalize repeating 2 chars: 훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍 -> 훌쩍훌쩍
        val repeatingNormalized2 = REPEATING_2CHAR_REGEX.replace(exclamationNormalized) {
            Matcher.quoteReplacement(it.groupValues[0].take(4))
        }

        // Normalize repeating 3 chars: 사브작사브작사브작사브작 -> 사브작사브작
        val repeatingNormalized3 = REPEATING_3CHAR_REGEX.replace(repeatingNormalized2) {
            Matcher.quoteReplacement(it.groupValues[0].take(6))
        }

        // Coda normalization (명사 + ㄴ 첨가 정규화): 소린가 -> 소리인가
        val codaNNormalized = normalizeCodaN(repeatingNormalized3)

        // Typo correction: 하겟다 -> 하겠다
        val typoCorrected = correctTypo(codaNNormalized)

        // Spaces, tabs, new lines are replaced with a single space.
        return WHITESPACE_REGEX.replace(typoCorrected, " ")
    }

    fun correctTypo(chunk: CharSequence): CharSequence {
        var output = chunk.toString()

        KoreanDictionaryProvider.typoDictionaryByLength.entries
            .forEach { (wordLen: Int, typoMap: Map<String, String>) ->
                output.sliding(wordLen)
                    .forEach { slice ->
                        if (typoMap.containsKey(slice)) {
                            log.trace { "Typo check: $slice -> ${typoMap[slice]}" }
                            output = output.replace(slice, typoMap[slice].toString(), ignoreCase = true)
                        }
                    }
            }

        return output
    }

    fun normalizeCodaN(chunk: CharSequence): CharSequence {
        if (chunk.length < 2)
            return chunk

        val lastTwo = chunk.subSequence(chunk.length - 2, chunk.length)
        val last = chunk[chunk.lastIndex]
        val lastTwoHead = lastTwo[0]

        fun isExceptional(): Boolean =
            koreanContains(KoreanPos.Noun, chunk) ||
                    koreanContains(KoreanPos.Conjunction, chunk) ||
                    koreanContains(KoreanPos.Adverb, chunk) ||
                    koreanContains(KoreanPos.Noun, lastTwo) ||
                    lastTwoHead < '가' ||
                    lastTwoHead > '힣' ||
                    CODA_N_EXCPETION.contains(lastTwoHead)

        if (isExceptional()) {
            return chunk
        }

        val tokens = KoreanTokenizer.tokenize(chunk)
        if (tokens.isNotEmpty() && KoreanPosx.Predicates.contains(tokens.first().pos)) {
            return chunk
        }

        val hc = decomposeHangul(lastTwoHead)
        val newHead = StringBuilder()
            .append(chunk.subSequence(0, chunk.length - 2))
            .append(composeHangul(hc.onset, hc.vowel))

        val needNewHead = hc.coda == 'ㄴ' && last in CODA_N_LAST_CHAR && koreanContains(KoreanPos.Noun, newHead)
        return if (needNewHead) {
            newHead.append("인").append(last).toString()
        } else {
            chunk
        }
    }

    private fun processNormalizationCandidate(m: kotlin.text.MatchResult): CharSequence {
        val chunk = m.groupValues[1]
        val toNormalize = m.groupValues[2]

        val isNormalized = koreanContains(KoreanPos.Noun, chunk) ||
                koreanContains(KoreanPos.Eomi, chunk.takeLast(1)) ||
                koreanContains(KoreanPos.Eomi, chunk.takeLast(2))

        val normalizedChunk = if (isNormalized) chunk
        else normalizeEmotionAttachedChunk(chunk, toNormalize)

        return normalizedChunk.toString() + toNormalize
    }

    private fun normalizeEmotionAttachedChunk(s: CharSequence, toNormalize: CharSequence): CharSequence {

        val init = s.take(s.length - 1)
        val secondToLastDecomposed: Hangul.HangulChar? by lazy {
            if (init.isNotEmpty()) {
                val hc = decomposeHangul(init.last())
                if (hc.codaIsEmpty) hc else null
            } else {
                null
            }
        }

        val hc = decomposeHangul(s.last())

        val hasSecondToLastDecomposed: Boolean by lazy {
            hc.codaIsEmpty &&
                    secondToLastDecomposed != null &&
                    hc.vowel == toNormalize[0] &&
                    Hangul.CODA_MAP.containsKey(hc.onset)
        }

        if (hc.coda in charArrayOf('ㅋ', 'ㅎ')) {
            return buildString {
                append(init)
                append(composeHangul(hc.onset, hc.vowel))
            }
        } else if (hasSecondToLastDecomposed) {
            val shc = secondToLastDecomposed!!
            return buildString {
                append(init.subSequence(0, init.length - 1))
                append(composeHangul(shc.onset, shc.vowel, hc.onset))
            }
        }
        return s
    }
}
