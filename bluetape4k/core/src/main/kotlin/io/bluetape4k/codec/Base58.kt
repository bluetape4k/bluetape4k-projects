package io.bluetape4k.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.isNullOrEmpty
import io.bluetape4k.support.requireGt
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import java.security.SecureRandom


/**
 * Base58 인코딩 및 디코딩을 수행하는 객체입니다.
 *
 * ```
 * val expected = "Hello, World!"
 * val encoded = Base58.encode(expected.toUtf8Bytes())   // "JxF12TrwUP45BMd"
 * val decoded = Base58.decode(encoded).toUtf8String()   // "Hello, World!"
 * ```
 */
object Base58: KLogging() {

    private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
    private val ENCODED_ZERO = ALPHABET[0]
    private val INDEXES = IntArray(128) { -1 }.apply {
        ALPHABET.indices.forEach {
            this[ALPHABET[it].code] = it
        }
    }
    private val RANDOM = SecureRandom()

    /**
     * [length] 크기를 가진 랜덤 문자열을 만듭니다.
     *
     * ```
     * val random = Base58.randomString(10)  // "1a2B3c4D5e"
     * ```
     *
     * @param length 생성할 문자열의 길이
     * @return 랜덤 문자열
     */
    fun randomString(length: Int): String {
        length.requireGt(0, "length")

        val result = CharArray(length)
        repeat(length) { index ->
            val pick = ALPHABET[RANDOM.nextInt(ALPHABET.size)]
            result[index] = pick
        }

        return String(result)
    }

    /**
     * 주어진 바이트를 base58 문자열로 인코딩합니다(체크섬은 추가되지 않습니다).
     *
     * ```
     * val encoded = Base58.encode("Hello, World!".toUtf8Bytes())   // "JxF12TrwUP45BMd"
     * ```
     *
     * @param source 인코딩할 바이트 배열
     * @return Base58로 인코딩된 문자열
     */
    fun encode(source: ByteArray): String {
        if (source.isNullOrEmpty()) {
            return ""
        }

        // Count leading zeros.
        var zeros = 0
        while (zeros < source.size && source[zeros].toInt() == 0) {
            zeros++
        }
        log.trace { "leading zeros count=$zeros" }

        // Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
        val input = source.copyOf(source.size)
        val encoded = CharArray(input.size * 2)
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < input.size) {
            encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58).toInt()]
            if (input[inputStart].toInt() == 0) {
                ++inputStart
            }
        }

        // 입력에서 선행하는 0의 개수만큼 출력에서 선행하는 0을 보존합니다.
        while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
            ++outputStart
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ENCODED_ZERO
        }
        // 인코딩된 문자열을 반환합니다(선행하는 0을 포함합니다).
        log.trace { "outputStart=$outputStart, encoded.size=${encoded.size}" }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    /**
     * 주어진 문자열을 base58 문자열로 인코딩합니다.
     */
    fun encode(source: String) = encode(source.toUtf8Bytes())

    /**
     * 주어진 base58 문자열을 원래 데이터 바이트로 디코딩합니다.
     *
     * ```
     * val decoded = Base58.decode("JxF12TrwUP45BMd").toUtf8String()   // "Hello, World!"
     * ```
     *
     * @param source 디코딩할 base58 인코딩 문자열
     * @return 디코딩된 데이터 바이트
     */
    fun decode(source: String): ByteArray {
        if (source.isBlank()) {
            return ByteArray(0)
        }

        // Base58 ASCII 문자열을 Base58 바이트 배열로 변환합니다.
        val input58 = ByteArray(source.length) {
            val c = source[it]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            if (digit < 0) {
                throw IllegalArgumentException("Illegal character in Base58: `$c` at position $it")
            }
            digit.toByte()
        }

        // 선행하는 0의 개수를 세어봅니다.
        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) {
            zeros++
        }
        log.trace { "leading zeros count=$zeros" }

        // Base-58 숫자를 Base-256 숫자로 변환합니다.
        val decoded = ByteArray(source.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256)
            if (input58[inputStart].toInt() == 0) {
                ++inputStart
            }
        }

        // 계산 중에 추가된 선행하는 0을 무시합니다.
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
            ++outputStart
        }
        // 디코딩된 바이트 배열을 반환합니다.
        log.trace { "outputStart=$outputStart, zeros=$zeros, decoded.size=${decoded.size}" }
        return decoded.copyOfRange(outputStart - zeros, decoded.size)
    }

    /**
     * Base58로 인코딩된 문자열을 디코딩해서 문자열로 반환합니다.
     *
     * @param source 디코딩할 base58 인코딩 문자열
     * @return Base58로 디코딩된 문자열
     */
    fun decodeAsString(source: String): String = decode(source).toUtf8String()

    /**
     * 지정된 진법에서 각각 한 자리를 가진 바이트 배열로 표현된 숫자를 주어진 나눗수로 나눕니다.
     * 주어진 숫자는 그 자리에서 수정되어 몫을 포함하게 되며, 나머지를 반환합니다.
     *
     * @param number the number to divide
     * @param firstDigit the index within the array of the first non-zero digit
     *        (this is used for optimization by skipping the leading zeros)
     * @param base the base in which the number's digits are represented (up to 256)
     * @param divisor the number to divide by (up to 256)
     * @return the remainder of the division operation
     */
    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
        // 이것은 입력 숫자의 기수를 고려하는 일반적인 나눗셈입니다.
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}

/**
 * Base58 알고리즘을 이용하여 [ByteArray] 값을 문자열로 인코딩합니다.
 *
 * @return Base58로 인코딩된 문자열
 */
fun ByteArray.encodeBase58(): String = Base58.encode(this)

/**
 * Base58 알고리즘으로 인코딩된 문자열을 디코딩하여 [ByteArray] 값을 반환합니다.
 *
 * @return Base58로 디코딩된 바이트 배열
 */
fun String.encodeBase58(): String = Base58.encode(this.toUtf8Bytes())

/**
 * Base58 알고리즘으로 인코딩된 문자열을 디코딩하여 [String] 값을 반환합니다.
 *
 * @return Base58로 디코딩된 문자열
 */
fun String.decodeBase58(): ByteArray = Base58.decode(this)

/**
 * Base58 알고리즘으로 인코딩된 문자열을 디코딩하여 [String] 값을 반환합니다.
 *
 * @return Base58로 디코딩된 문자열
 */
fun String.decodeBase58AsString(): String = Base58.decodeAsString(this)
