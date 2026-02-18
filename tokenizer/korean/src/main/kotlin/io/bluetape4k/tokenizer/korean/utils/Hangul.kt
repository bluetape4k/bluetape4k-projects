package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.Hangul.DOUBLE_CODAS
import java.io.Serializable

/**
 * 한글을 초성, 중성, 종성으로 분해하고 조합하는 유틸리티 클래스
 */
object Hangul: KLogging() {

    /**
     * 한글 문자를 초성, 중성, 종성으로 분해한 정보 클래스
     */
    data class HangulChar(val onset: Char, val vowel: Char, val coda: Char): Serializable {
        val codaIsEmpty: Boolean get() = coda == ' '
        val hasCoda: Boolean get() = coda != ' '
    }

    /**
     * 밭침에 두 개의 자음이 있는 경우를 나타내는 클래스
     *
     * @property first 첫 번째 자음
     * @property second 두 번째 자음
     *
     * @see DOUBLE_CODAS
     */
    data class DoubleCoda(val first: Char, val second: Char): Serializable

    private const val HANGUL_BASE: Int = 0xAC00
    private const val ONSET_BASE: Int = 21 * 28
    private const val VOWEL_BASE: Int = 28

    private val ONSET_LIST: CharArray = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
        'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )
    private val VOWEL_LIST: CharArray = charArrayOf(
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ',
        'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ',
        'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ',
        'ㅡ', 'ㅢ', 'ㅣ'
    )
    private val CODA_LIST: CharArray = charArrayOf(
        ' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ',
        'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ',
        'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ',
        'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )

    private val ONSET_MAP: Map<Char, Int> = ONSET_LIST.mapIndexed { index, c -> c to index }.toMap()
    private val VOWEL_MAP: Map<Char, Int> = VOWEL_LIST.mapIndexed { index, c -> c to index }.toMap()

    @PublishedApi
    internal val CODA_MAP: Map<Char, Int> = CODA_LIST.mapIndexed { index, c -> c to index }.toMap()

    @PublishedApi
    internal val DOUBLE_CODAS: Map<Char, DoubleCoda> = mapOf(
        'ㄳ' to DoubleCoda('ㄱ', 'ㅅ'),
        'ㄵ' to DoubleCoda('ㄴ', 'ㅈ'),
        'ㄶ' to DoubleCoda('ㄴ', 'ㅎ'),
        'ㄺ' to DoubleCoda('ㄹ', 'ㄱ'),
        'ㄻ' to DoubleCoda('ㄹ', 'ㅁ'),
        'ㄼ' to DoubleCoda('ㄹ', 'ㅂ'),
        'ㄽ' to DoubleCoda('ㄹ', 'ㅅ'),
        'ㄾ' to DoubleCoda('ㄹ', 'ㅌ'),
        'ㄿ' to DoubleCoda('ㄹ', 'ㅍ'),
        'ㅀ' to DoubleCoda('ㄹ', 'ㅎ'),
        'ㅄ' to DoubleCoda('ㅂ', 'ㅅ')
    )

    /**
     *  한글을 초성, 중성, 종성으로 분해합니다.
     *
     *  ```
     *  val (onset, vowel, coda) = Hangul.decomposeHangul('한')  // ('ㅎ', 'ㅏ', 'ㄴ')
     *  val (onset, vowel, coda) = Hangul.decomposeHangul('하')  // ('ㅎ', 'ㅏ', ' ')
     *  ```
     *
     *  @param c Korean Character
     *  @return (onset:Char, vowel:Char, coda:Char)
     */
    fun decomposeHangul(c: Char): HangulChar {
        assert(!(ONSET_MAP.containsKey(c) || VOWEL_MAP.containsKey(c) || CODA_MAP.containsKey(c))) {
            "Input character is not a valid Korean character"
        }
        val u = (c - HANGUL_BASE).code
        return HangulChar(
            ONSET_LIST[u / ONSET_BASE],
            VOWEL_LIST[(u % ONSET_BASE) / VOWEL_BASE],
            CODA_LIST[u % VOWEL_BASE]
        )
    }

    /**
     * 한글에 종성(받침)이 있는지 검사
     *
     * ```
     * Hangul.hasCoda('한')  // true
     * Hangul.hasCoda('하')  // false
     * ```
     *
     * @param c 한글 문자
     */
    fun hasCoda(c: Char): Boolean = (c.code - HANGUL_BASE) % VOWEL_BASE > 0

    /**
     * 초,중,종성의 char 로 한글을 조홥합니다.
     *
     * ```
     * Hangul.composeHangul('ㅎ', 'ㅏ', 'ㄴ')  // '한'
     * Hangul.composeHangul('ㄱ', 'ㅏ')  // '가'
     * ```
     *
     * @param onset 초성
     * @param vowel 중성
     * @param coda 종성
     */
    fun composeHangul(onset: Char, vowel: Char, coda: Char = ' '): Char {
        assert(onset != ' ' && vowel != ' ') { "Input characters are not valid" }

        return (HANGUL_BASE +
                ((ONSET_MAP[onset] ?: 0) * ONSET_BASE) +
                ((VOWEL_MAP[vowel] ?: 0) * VOWEL_BASE) +
                (CODA_MAP[coda] ?: 0)).toChar()
    }

    /**
     * [HangulChar] 의 초성, 중성, 종성으로 한글을 조합합니다.
     *
     * ```
     * val hc = HangulChar('ㅎ', 'ㅏ', 'ㄴ')
     * Hangul.composeHangul(hc)  // '한'
     * ```
     *
     * @param hc [HangulChar] 인스턴스
     * @return 조합된 한글 문자
     */
    fun composeHangul(hc: HangulChar): Char =
        composeHangul(hc.onset, hc.vowel, hc.coda)
}
