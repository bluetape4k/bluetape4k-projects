package io.bluetape4k.captcha

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.support.requirePositiveNumber
import kotlin.random.Random

/**
 * 지정된 심볼 집합에서 CAPTCHA 코드를 랜덤 생성하는 생성기입니다.
 *
 * ## 동작/계약
 * - [symbols]는 비어 있을 수 없으며 생성 시 검증됩니다.
 * - 코드 생성 시마다 새 문자열을 할당합니다.
 *
 * ```kotlin
 * val generator = CaptchaCodeGenerator()
 * val code = generator.next(6)
 * // code.length == 6
 * ```
 *
 * @property symbols 코드 생성에 사용할 문자 집합
 */
class CaptchaCodeGenerator private constructor(
    val symbols: String,
) {

    companion object: KLogging() {
        const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWER = "abcdefghijklmnopqrstuvwxyz"
        const val DIGITS = "0123456789"

        const val UPPER_DIGITS = UPPER + DIGITS
        const val ALPHA_DIGITS = UPPER + LOWER + DIGITS

        @JvmStatic
        val DEFAULT: CaptchaCodeGenerator = CaptchaCodeGenerator(symbols = UPPER_DIGITS)

        @JvmStatic
        /**
         * 사용자 지정 심볼 집합으로 생성기를 만듭니다.
         *
         * ## 동작/계약
         * - [symbols]가 비어 있으면 [IllegalArgumentException]이 발생합니다.
         * - 전달된 심볼 집합을 그대로 보관하는 새 생성기를 반환합니다.
         *
         * ```kotlin
         * val gen = CaptchaCodeGenerator("ABC123")
         * // gen.next(5).length == 5
         * ```
         */
        operator fun invoke(
            symbols: String = UPPER_DIGITS,
        ): CaptchaCodeGenerator {
            symbols.requireNotEmpty("symbols")
            return CaptchaCodeGenerator(symbols)
        }

        @JvmStatic
        /**
         * 숫자 전용 또는 영숫자 코드 생성기를 만듭니다.
         *
         * ## 동작/계약
         * - [digitOnly]가 `true`면 숫자(`0-9`)만 사용합니다.
         * - `false`면 영문 대/소문자와 숫자를 사용합니다.
         *
         * ```kotlin
         * val numeric = CaptchaCodeGenerator(digitOnly = true).next(4)
         * // numeric.all { it.isDigit() } == true
         * ```
         */
        operator fun invoke(digitOnly: Boolean): CaptchaCodeGenerator {
            val symbols = if (digitOnly) DIGITS else ALPHA_DIGITS
            return invoke(symbols = symbols)
        }
    }

    /**
     * Captcha에 쓰일 랜덤 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - [length]가 0 이하이면 [IllegalArgumentException]이 발생합니다.
     * - [symbols]에서 임의 인덱스를 선택해 길이 [length]의 새 문자열을 생성합니다.
     * - 입력 [symbols]를 변경하지 않습니다.
     *
     * ```kotlin
     * val code = CaptchaCodeGenerator.DEFAULT.next(6)
     * // code.length == 6
     * ```
     *
     * @param length 생성할 문자열의 길이, 0보다 커야 합니다. 보통 6자리를 사용합니다.
     */
    fun next(length: Int): String {
        length.requirePositiveNumber("length")

        val buf = CharArray(length) {
            symbols[Random.nextInt(symbols.length)]
        }
        return String(buf)
    }
}
