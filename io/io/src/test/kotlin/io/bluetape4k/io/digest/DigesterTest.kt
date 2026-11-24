package io.bluetape4k.io.digest

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContentEqual
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import java.util.HexFormat

class DigesterTest {

    private val input = "abc".toByteArray(StandardCharsets.UTF_8)

    @Test
    fun `SHA-256 known vectors preserve raw hex and URL-safe padded output`() {
        val sha256 = Digesters.SHA256
        val expectedHex = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val expectedBase64 = "ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0="

        sha256.algorithm shouldBeEqualTo "SHA-256"
        sha256.digest(input) shouldContentEqual HexFormat.of().parseHex(expectedHex)
        sha256.digestUtf8("abc") shouldContentEqual HexFormat.of().parseHex(expectedHex)
        sha256.digestHex(input) shouldBeEqualTo expectedHex
        sha256.digestHexUtf8("abc") shouldBeEqualTo expectedHex
        sha256.digestBase64(input) shouldBeEqualTo expectedBase64
        sha256.digestBase64Utf8("abc") shouldBeEqualTo expectedBase64
    }

    @Test
    fun `Base64 digest and matcher honor each alphabet and padding format`() {
        val sha256 = Digesters.SHA256

        for (format in Base64Format.entries) {
            val encoded = sha256.digestBase64(input, format)

            sha256.matchesBase64(input, encoded, format).shouldBeTrue()
            sha256.matchesBase64(input, withWrongAlphabet(encoded, format), format).shouldBeFalse()
            sha256.matchesBase64(input, withWrongPadding(encoded, format), format).shouldBeFalse()
            sha256.matchesBase64(input, "$encoded ", format).shouldBeFalse()
            sha256.matchesBase64(input, withNonCanonicalPadBits(encoded, format), format).shouldBeFalse()
        }

        val defaultValue = sha256.digestBase64(input)
        sha256.matchesBase64(input, defaultValue).shouldBeTrue()
    }

    @Test
    fun `raw and hexadecimal matchers compare expected digest bytes`() {
        val sha256 = Digesters.SHA256
        val expected = sha256.digest(input)
        val expectedHex = sha256.digestHex(input)

        sha256.matches(input, expected).shouldBeTrue()
        sha256.matches("different".toByteArray(), expected).shouldBeFalse()
        sha256.matchesHex(input, expectedHex).shouldBeTrue()
        sha256.matchesHexUtf8("abc", expectedHex).shouldBeTrue()
        sha256.matchesHexUtf8("abc", expectedHex.uppercase()).shouldBeFalse()
        sha256.matchesHexUtf8("abc", expectedHex.dropLast(1)).shouldBeFalse()
    }

    @Test
    fun `malformed Base64 expected values return false`() {
        val sha256 = Digesters.SHA256

        sha256.matchesBase64(input, "not-base64-!").shouldBeFalse()
        sha256.matchesBase64(input, "not-base64-!", Base64Format.STANDARD_PADDED).shouldBeFalse()
    }

    @Test
    fun `provider failures propagate from session one-shot and matcher paths`() {
        val unsupported = JdkDigester("issue-1917-unsupported")

        unsupported.algorithm shouldBeEqualTo "issue-1917-unsupported"
        assertFailsWith<NoSuchAlgorithmException> { unsupported.newSession() }
        assertFailsWith<NoSuchAlgorithmException> { unsupported.digest(input) }
        assertFailsWith<NoSuchAlgorithmException> { unsupported.matches(input, ByteArray(32)) }
        assertFailsWith<NoSuchAlgorithmException> {
            unsupported.matchesBase64(input, "AA==", Base64Format.STANDARD_PADDED)
        }
    }

    @Test
    fun `known legacy algorithm names can be supplied by trusted configuration`() {
        JdkDigester("MD5").algorithm shouldBeEqualTo "MD5"
        JdkDigester("SHA-1").algorithm shouldBeEqualTo "SHA-1"
    }

    @Test
    fun `returned digest arrays are caller-owned`() {
        val sha256 = Digesters.SHA256
        val expected = sha256.digest(input)
        val returned = sha256.digest(input)

        returned.fill(0)

        sha256.digest(input) shouldContentEqual expected
    }

    private fun withWrongAlphabet(encoded: String, format: Base64Format): String =
        if (format.name.startsWith("STANDARD")) {
            encoded.replace('+', '-').replace('/', '_')
        } else {
            encoded.replace('-', '+').replace('_', '/')
        }

    private fun withWrongPadding(encoded: String, format: Base64Format): String =
        if (format.name.endsWith("PADDED")) encoded.dropLast(1) else "$encoded="

    private fun withNonCanonicalPadBits(encoded: String, format: Base64Format): String {
        val alphabet = if (format.name.startsWith("STANDARD")) {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        } else {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        }
        val lastDataIndex = encoded.indexOfLast { it != '=' }
        val lastIndex = alphabet.indexOf(encoded[lastDataIndex])
        val changedIndex = (lastIndex and 0b111100) or ((lastIndex and 0b11) xor 0b01)

        return encoded.substring(0, lastDataIndex) + alphabet[changedIndex] + encoded.substring(lastDataIndex + 1)
    }
}
