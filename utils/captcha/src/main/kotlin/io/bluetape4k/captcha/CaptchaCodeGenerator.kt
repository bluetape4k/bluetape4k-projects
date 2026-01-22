package io.bluetape4k.captcha

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.support.requirePositiveNumber
import kotlin.random.Random

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
        operator fun invoke(
            symbols: String = UPPER_DIGITS,
        ): CaptchaCodeGenerator {
            symbols.requireNotEmpty("symbols")
            return CaptchaCodeGenerator(symbols)
        }

        @JvmStatic
        operator fun invoke(digitOnly: Boolean): CaptchaCodeGenerator {
            val symbols = if (digitOnly) DIGITS else ALPHA_DIGITS
            return invoke(symbols = symbols)
        }
    }

    /**
     * Captcha에 쓰일 랜덤 문자열을 생성합니다.
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
