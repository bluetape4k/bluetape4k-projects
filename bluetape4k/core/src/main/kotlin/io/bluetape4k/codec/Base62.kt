package io.bluetape4k.codec

import io.bluetape4k.codec.Base62.DEFAULT_BIT_LIMIT
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.bluetape4k.support.toBigInt
import io.bluetape4k.support.toUuid
import java.math.BigInteger
import java.util.*

/**
 * Base62 인코딩 및 디코딩을 수행하는 객체입니다.
 * UUID 를 Base62로 인코딩하여 문자열로 변환하거나, Base62로 인코딩된 문자열을 UUID로 디코딩할 수 있습니다.
 * 이렇게 하면 문자열 길이를 줄이고, URL-safe 하게 만들 수 있습니다.
 *
 * ```
 * // UUID -> Base62 encoded string
 * 7baf3680-eafb-4194-a1c9-4d85f15de186, encoded=3lO7ysTzNOGrjT4vadTPio
 * c5123c89-e02b-41e1-a375-ae012d05c967, encoded=5zrozjHIxuZmgxWUOyUJmh
 * ```
 *
 * 참고: [opencoinage Base62.java](https://github.com/opencoinage/opencoinage/blob/master/src/java/org/opencoinage/util/Base62.java)
 */
object Base62: KLogging() {

    private const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(62)
    private val PATTERN = "^[0-9A-Za-z]+$".toRegex()
    const val DEFAULT_BIT_LIMIT = 128

    /**
     * Base62 알고리즘을 이용하여 [BigInteger] 값을 문자열로 인코딩합니다.
     *
     * @param number 인코딩할 [BigInteger] 값
     * @return Base62로 인코딩된 문자열
     */
    fun encode(number: BigInteger): String {
        number.requireZeroOrPositiveNumber("number")
        val result = StringBuilder()

        var current = number
        while (current > BigInteger.ZERO) {
            val (quantient, remainder) = current.divideAndRemainder(BASE)
            current = quantient
            val digit = remainder.intValueExact()
            result.insert(0, DIGITS[digit])
        }
        return if (result.isEmpty()) DIGITS.substring(0, 1) else result.toString()
    }

    /**
     * Base62 알고리즘으로 인코딩된 문자열을 디코딩하여 [BigInteger] 값을 반환합니다.
     */
    fun decode(text: String, bitLimit: Int = DEFAULT_BIT_LIMIT): BigInteger {
        text.requireNotBlank("text")

        require(PATTERN.matches(text)) {
            "Text `$text` contains illegal characters, only [$DIGITS] are allowed."
        }

        var sum = BigInteger.ZERO
        text.forEachIndexed { index, _ ->
            sum += DIGITS.indexOf(text[text.length - index - 1]).toBigInteger() * BASE.pow(index)
            if (bitLimit > 0 && sum.bitLength() > bitLimit) {
                throw IllegalArgumentException("Text [$text] contains more than ${bitLimit}bit information")
            }
        }
        return sum
    }
}

/**
 * Base62 알고리즘을 이용하여 UUID 값을 문자열로 인코딩합니다.
 *
 * @return Base62로 인코딩된 문자열
 */
fun <T: Number> T.encodeBase62(): String = Base62.encode(this.toBigInt())

/**
 * Base62 알고리즘으로 인코딩된 문자열을 디코딩하여 [BigInteger] 값을 반환합니다.
 */
fun String.decodeBase62(bitLimit: Int = DEFAULT_BIT_LIMIT): BigInteger =
    Base62.decode(this, bitLimit)

/**
 * Base62 알고리즘을 이용하여 UUID 값을 문자열로 인코딩합니다.
 *
 * ```
 * // UUID -> Base62 encoded string
 * UUID.randomUUID().encodeBase62() // "3lO7ysTzNOGrjT4vadTPio"
 * ```
 *
 * @return Base62로 인코딩된 문자열
 */
fun UUID.encodeBase62(): String = Base62.encode(this.toBigInt())

/**
 * Base62 알고리즘으로 인코딩된 문자열을 디코딩하여 UUID 값을 반환합니다.
 *
 * ```
 * // Base62 encoded string -> UUID
 * "3lO7ysTzNOGrjT4vadTPio".decodeBase62AsUuid() // UUID.randomUUID()
 * ```
 */
fun String.decodeBase62AsUuid(bitLimit: Int = DEFAULT_BIT_LIMIT): UUID =
    Base62.decode(this, bitLimit).toUuid()
