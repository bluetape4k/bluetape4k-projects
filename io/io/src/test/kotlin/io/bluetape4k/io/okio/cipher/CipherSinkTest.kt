package io.bluetape4k.io.okio.cipher

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CipherSinkTest: AbstractCipherTest() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `encrypt random string`() {
        val plainText = Fakers.randomString(1024)
        val source = bufferOf(plainText)

        val output = Buffer()
        val sink = CipherSink(output, encryptCipher)

        sink.write(source, source.size)

        output.readByteArray() shouldBeEqualTo encryptCipher.doFinal(plainText.toByteArray())
    }

    @Test
    fun `write with zero byteCount does nothing`() {
        val plainText = "cipher"
        val source = bufferOf(plainText)
        val output = Buffer()
        val sink = CipherSink(output, encryptCipher)

        sink.write(source, 0L)

        output.size shouldBeEqualTo 0L
        source.readUtf8() shouldBeEqualTo plainText
    }

    @Test
    fun `write with invalid byteCount behavior`() {
        val source = bufferOf("cipher")
        val output = Buffer()
        val sink = CipherSink(output, encryptCipher)

        sink.write(source, -1L)
        output.size shouldBeEqualTo 0L
        source.readUtf8() shouldBeEqualTo "cipher"

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, source.size + 1L)
        }
    }
}
