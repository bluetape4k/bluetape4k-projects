package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.Hangul.DOUBLE_CODAS
import java.io.Serializable

/**
 * 한글 음절의 초성/중성/종성 분해와 조합을 제공합니다.
 *
 * ## 동작/계약
 * - `decomposeHangul`은 단일 음절을 `HangulChar`로 분해한다.
 * - `composeHangul`은 분해된 자모를 유니코드 한글 음절로 조합한다.
 * - 받침 판정은 유니코드 음절 인덱스 계산으로 수행한다.
 *
 * ```kotlin
 * val hc = Hangul.decomposeHangul('한')
 * // hc == Hangul.HangulChar('ㅎ', 'ㅏ', 'ㄴ')
 * ```
 */
object Hangul: KLogging() {

    /**
     * 한글 음절 한 글자의 초성/중성/종성 분해 결과입니다.
     *
     * ## 동작/계약
     * - `coda`가 공백 문자면 받침이 없는 음절이다.
     *
     * ```kotlin
     * val hc = Hangul.HangulChar('ㅎ', 'ㅏ', ' ')
     * // hc.codaIsEmpty == true
     * ```
     */
    data class HangulChar(val onset: Char, val vowel: Char, val coda: Char): Serializable {
        /**
         * 종성이 비어 있는지 여부입니다.
         *
         * ## 동작/계약
         * - `coda == ' '`일 때 `true`를 반환한다.
         *
         * ```kotlin
         * val empty = Hangul.HangulChar('ㅎ', 'ㅏ', ' ').codaIsEmpty
         * // empty == true
         * ```
         */
        val codaIsEmpty: Boolean get() = coda == ' '
        /**
         * 종성이 존재하는지 여부입니다.
         *
         * ## 동작/계약
         * - `coda != ' '`일 때 `true`를 반환한다.
         *
         * ```kotlin
         * val has = Hangul.HangulChar('ㅎ', 'ㅏ', 'ㄴ').hasCoda
         * // has == true
         * ```
         */
        val hasCoda: Boolean get() = coda != ' '
    }

    /**
     * 겹받침을 구성하는 두 자음을 표현합니다.
     *
     * ## 동작/계약
     * - `DOUBLE_CODAS` 매핑의 값 타입으로 사용된다.
     *
     * ```kotlin
     * val dc = Hangul.DoubleCoda('ㄱ', 'ㅅ')
     * // dc.first == 'ㄱ'
     * ```
     *
     * @property first 첫 번째 자음
     * @property second 두 번째 자음
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
     * 한글 음절을 초성/중성/종성으로 분해합니다.
     *
     * ## 동작/계약
     * - 자모 문자 입력은 허용하지 않으며 `assert` 실패 시 `AssertionError`가 발생한다(`-ea` 필요).
     * - 유니코드 한글 음절 인덱스로 초성/중성/종성을 계산한다.
     *
     * ```kotlin
     * val hc = Hangul.decomposeHangul('하')
     * // hc == Hangul.HangulChar('ㅎ', 'ㅏ', ' ')
     * ```
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
     * 한글 음절에 종성(받침)이 있는지 확인합니다.
     *
     * ## 동작/계약
     * - 유니코드 한글 음절 인덱스의 종성 영역 값으로 판정한다.
     *
     * ```kotlin
     * val has = Hangul.hasCoda('한')
     * // has == true
     * ```
     */
    fun hasCoda(c: Char): Boolean = (c.code - HANGUL_BASE) % VOWEL_BASE > 0

    /**
     * 초성/중성/종성으로 한글 음절을 조합합니다.
     *
     * ## 동작/계약
     * - 초성과 중성이 공백이면 `assert` 실패 시 `AssertionError`가 발생한다(`-ea` 필요).
     * - 종성은 기본값 공백(`' '`)을 사용하면 받침 없는 음절을 생성한다.
     *
     * ```kotlin
     * val h = Hangul.composeHangul('ㅎ', 'ㅏ', 'ㄴ')
     * // h == '한'
     * ```
     */
    fun composeHangul(onset: Char, vowel: Char, coda: Char = ' '): Char {
        assert(onset != ' ' && vowel != ' ') { "Input characters are not valid" }

        return (HANGUL_BASE +
                ((ONSET_MAP[onset] ?: 0) * ONSET_BASE) +
                ((VOWEL_MAP[vowel] ?: 0) * VOWEL_BASE) +
                (CODA_MAP[coda] ?: 0)).toChar()
    }

    /**
     * [HangulChar]를 한글 음절로 조합합니다.
     *
     * ## 동작/계약
     * - `composeHangul(hc.onset, hc.vowel, hc.coda)` 호출을 그대로 위임한다.
     *
     * ```kotlin
     * val c = Hangul.composeHangul(Hangul.HangulChar('ㄱ', 'ㅏ', ' '))
     * // c == '가'
     * ```
     */
    fun composeHangul(hc: HangulChar): Char =
        composeHangul(hc.onset, hc.vowel, hc.coda)
}
