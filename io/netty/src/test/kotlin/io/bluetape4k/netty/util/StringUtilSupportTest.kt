package io.bluetape4k.netty.util

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class StringUtilSupportTest {

    @Test
    fun `byte array to hex string`() {
        val bytes = byteArrayOf(0x30, 0x31, 0x32, 0x33)
        bytes.toHexString() shouldBeEqualTo "30313233"
    }

    @Test
    fun `decode hex bytes`() {
        "30313233".decodeHexDump() shouldBeEqualTo byteArrayOf(0x30, 0x31, 0x32, 0x33)
    }

    @Test
    fun `char is double quote`() {
        '"'.isDoubleQuote.shouldBeTrue()
        'a'.isDoubleQuote.shouldBeFalse()
    }

    @Test
    fun `index of white space`() {
        "abc".indexOfWhiteSpace() shouldBeEqualTo -1
        "abc def".indexOfWhiteSpace() shouldBeEqualTo 3
        "abc\tdef".indexOfWhiteSpace() shouldBeEqualTo 3
    }

    @Test
    fun `index of non white space`() {
        " abc".indexOfNonWhiteSpace() shouldBeEqualTo 1
        "\t abc def".indexOfNonWhiteSpace() shouldBeEqualTo 2
        "\t\tabc\tdef".indexOfNonWhiteSpace() shouldBeEqualTo 2
    }

    @Test
    fun `trim optional whtespace character`() {
        "\tabc".trimOws() shouldBeEqualTo "abc"
        "abc\t".trimOws() shouldBeEqualTo "abc"

        // 중간에 있는 whitespace 를 제거하지는 못합니다.
        "abc\tdef".trimOws() shouldBeEqualTo "abc\tdef"
    }

    @Test
    fun `join string list`() {
        val strs = listOf("a", "b", "c")

        strs.join().toString() shouldBeEqualTo "a,b,c"
        strs.join("|").toString() shouldBeEqualTo "a|b|c"
    }
}
