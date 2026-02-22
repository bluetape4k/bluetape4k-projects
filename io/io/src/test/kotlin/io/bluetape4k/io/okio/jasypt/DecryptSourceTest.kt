package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.io.okio.compress.asCompressSink
import io.bluetape4k.io.okio.compress.asDecompressSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import okio.Source
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DecryptSourceTest: AbstractEncryptTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `decrypt by source`(encryptor: Encryptor) {
        val expected = faker.lorem().paragraph().repeat(10)

        val encryptedSource = bufferOf(encryptor.encrypt(expected.toUtf8Bytes()))
        val decryptedSource = encryptedSource.asDecryptSource(encryptor)

        val decryptedBuffer = Buffer()
        decryptedSource.readAllTo(decryptedBuffer)

        decryptedBuffer.readUtf8() shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt and decrypt`(encryptor: Encryptor) {
        val expectedText = faker.lorem().paragraph().repeat(10)
        val buffer = bufferOf(expectedText)

        val sink = Buffer()
        val encryptedSink = sink.asEncryptSink(encryptor)

        encryptedSink.write(buffer, buffer.size)

        val source = Buffer()
        val decryptedSource = sink.asDecryptSource(encryptor)
        decryptedSource.readAllTo(source)

        source.readUtf8() shouldBeEqualTo expectedText
    }

    /**
     * 압축 -> 암호화 -> 복호화 -> 압축 해제
     */
    @ParameterizedTest
    @MethodSource("encryptors")
    fun `compress and encrypt`(encryptor: Encryptor) {
        compressors().forEach { compressor ->
            val expectedText = faker.lorem().paragraph().repeat(10)
            val buffer = bufferOf(expectedText)

            val sink = Buffer()
            sink.asEncryptSink(encryptor).asCompressSink(compressor).use { compressAndEncryptSink ->
                compressAndEncryptSink.write(buffer, buffer.size)
            }


            val source = Buffer()
            val decryptAndDecompressSource = sink.asDecryptSource(encryptor).asDecompressSource(compressor)
            decryptAndDecompressSource.readAllTo(source)

            source.readUtf8() shouldBeEqualTo expectedText
        }
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `decrypt source should support incremental reads and eof`(encryptor: Encryptor) {
        val expected = faker.lorem().paragraph().repeat(5)
        val encryptedSource = bufferOf(encryptor.encrypt(expected.toUtf8Bytes()))
        val decryptedSource = encryptedSource.asDecryptSource(encryptor)

        val sink = Buffer()
        val firstRead = decryptedSource.read(sink, 17L)
        firstRead shouldBeEqualTo 17L

        decryptedSource.readAllTo(sink, 31L)
        decryptedSource.read(Buffer(), 32L) shouldBeEqualTo -1L
        sink.readUtf8() shouldBeEqualTo expected
    }

    @Test
    fun `decrypt source should throw for negative byteCount`() {
        val expected = faker.lorem().paragraph()
        val encryptedSource = bufferOf(Encryptors.AES.encrypt(expected.toUtf8Bytes()))
        val decryptedSource = encryptedSource.asDecryptSource(Encryptors.AES)

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            decryptedSource.read(Buffer(), -1L)
        }
    }

    private fun Source.readAllTo(sink: Buffer, chunkSize: Long = DEFAULT_BUFFER_SIZE.toLong()): Long {
        var total = 0L
        while (true) {
            val bytesRead = read(sink, chunkSize)
            if (bytesRead < 0L) {
                break
            }
            total += bytesRead
        }
        return total
    }
}
