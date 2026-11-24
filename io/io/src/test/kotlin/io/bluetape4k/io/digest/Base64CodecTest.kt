package io.bluetape4k.io.digest

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContentEqual
import org.junit.jupiter.api.Test

class Base64CodecTest {

    private val input = byteArrayOf(0xfb.toByte(), 0xff.toByte())

    @Test
    fun `default codec uses URL-safe padded Base64`() {
        Base64Codec.encode(input) shouldBeEqualTo "-_8="
        Base64Codec.decode("-_8=") shouldContentEqual input
    }

    @Test
    fun `all formats round trip with their alphabet and padding`() {
        val expected = mapOf(
            Base64Format.STANDARD_PADDED to "+/8=",
            Base64Format.STANDARD_UNPADDED to "+/8",
            Base64Format.URL_SAFE_PADDED to "-_8=",
            Base64Format.URL_SAFE_UNPADDED to "-_8",
        )

        for ((format, encoded) in expected) {
            Base64Codec.encode(input, format) shouldBeEqualTo encoded
            Base64Codec.decode(encoded, format) shouldContentEqual input
        }
    }

    @Test
    fun `decoder rejects wrong alphabet padding whitespace and malformed input`() {
        assertFailsWith<IllegalArgumentException> {
            Base64Codec.decode("-_8=", Base64Format.STANDARD_PADDED)
        }
        assertFailsWith<IllegalArgumentException> {
            Base64Codec.decode("+/8=", Base64Format.URL_SAFE_PADDED)
        }
        assertFailsWith<IllegalArgumentException> {
            Base64Codec.decode("+/8", Base64Format.STANDARD_PADDED)
        }
        assertFailsWith<IllegalArgumentException> {
            Base64Codec.decode("+/8=", Base64Format.STANDARD_UNPADDED)
        }
        assertFailsWith<IllegalArgumentException> {
            Base64Codec.decode("+/8=\n", Base64Format.STANDARD_PADDED)
        }
        assertFailsWith<IllegalArgumentException> {
            Base64Codec.decode("not-base64-!", Base64Format.URL_SAFE_PADDED)
        }
    }

    @Test
    fun `decoder rejects noncanonical pad bits for every format`() {
        val values = mapOf(
            Base64Format.STANDARD_PADDED to "+/9=",
            Base64Format.STANDARD_UNPADDED to "+/9",
            Base64Format.URL_SAFE_PADDED to "-_9=",
            Base64Format.URL_SAFE_UNPADDED to "-_9",
        )

        for ((format, encoded) in values) {
            assertFailsWith<IllegalArgumentException> {
                Base64Codec.decode(encoded, format)
            }
        }
    }
}
