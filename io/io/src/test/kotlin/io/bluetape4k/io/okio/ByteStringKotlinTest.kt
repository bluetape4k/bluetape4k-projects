package io.bluetape4k.io.okio

import okio.ByteString
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.readByteString
import okio.ByteString.Companion.toByteString
import org.amshove.kluent.shouldBeEqualTo
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import kotlin.test.Test

class ByteStringKotlinTest: AbstractOkioTest() {

    @Test
    fun `array to byte string`() {
        val actual = byteArrayOf(1, 2, 3, 4).toByteString()
        val expected = ByteString.of(1, 2, 3, 4)

        actual shouldBeEqualTo expected
    }

    @Test
    fun `byte buffer to byte string`() {
        val actual = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)).toByteString()
        val expected = ByteString.of(1, 2, 3, 4)

        actual shouldBeEqualTo expected
    }

    @Test
    fun `string encode byte string default charset`() {
        val actual = "a\uD83C\uDF69c".encode()
        val expected = "a\uD83C\uDF69c".encodeUtf8()

        actual shouldBeEqualTo expected
    }

    @Test
    fun `stream read byte string`() {
        val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        stream.readByteString(4) shouldBeEqualTo ByteString.of(1, 2, 3, 4)
        stream.readByteString(stream.available()) shouldBeEqualTo ByteString.of(5, 6, 7, 8)
    }

    @Test
    fun substring() {
        val byteString = "abcdef".encodeUtf8()

        byteString.substring() shouldBeEqualTo "abcdef".encodeUtf8()
        byteString.substring(endIndex = 3) shouldBeEqualTo "abc".encodeUtf8()
        byteString.substring(beginIndex = 3) shouldBeEqualTo "def".encodeUtf8()
        byteString.substring(beginIndex = 1, endIndex = 5) shouldBeEqualTo "bcde".encodeUtf8()
    }
}
