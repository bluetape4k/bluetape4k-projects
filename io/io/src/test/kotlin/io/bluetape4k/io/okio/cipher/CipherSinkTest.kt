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
        FinalizingCipherSink(output, encryptCipher).use { sink ->
            sink.write(source, source.size)
        }

        output.readByteArray() shouldBeEqualTo encryptCipher.doFinal(plainText.toByteArray())
    }

    @Test
    fun `write with zero byteCount does nothing`() {
        val plainText = "cipher"
        val source = bufferOf(plainText)
        val output = Buffer()
        val sink = FinalizingCipherSink(output, encryptCipher)

        sink.write(source, 0L)

        output.size shouldBeEqualTo 0L
        source.readUtf8() shouldBeEqualTo plainText
    }

    @Test
    fun `write with invalid byteCount behavior`() {
        val source = bufferOf("cipher")
        val output = Buffer()
        val sink = FinalizingCipherSink(output, encryptCipher)

        sink.write(source, -1L)
        output.size shouldBeEqualTo 0L
        source.readUtf8() shouldBeEqualTo "cipher"

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, source.size + 1L)
        }
    }

    @Test
    fun `여러 번 write 후 StreamingCipherSource 로 복호화할 수 있다`() {
        val part1 = "streaming-"
        val part2 = "cipher-"
        val part3 = "sink"
        val plainText = part1 + part2 + part3

        val output = Buffer()
        FinalizingCipherSink(output, encryptCipher).use { sink ->
            sink.write(bufferOf(part1), part1.length.toLong())
            sink.write(bufferOf(part2), part2.length.toLong())
            sink.write(bufferOf(part3), part3.length.toLong())
        }

        val decoded = StreamingCipherSource(output, decryptCipher)
        val restored = bufferOf(decoded).readUtf8()
        restored shouldBeEqualTo plainText
    }
}
