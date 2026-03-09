package io.bluetape4k.okio.tink

import io.bluetape4k.okio.bufferOf
import io.bluetape4k.okio.compress.asCompressSink
import io.bluetape4k.okio.compress.asDecompressSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors
import okio.Buffer
import okio.Source
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TinkDecryptSourceTest: AbstractTinkEncryptTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `decrypt by source`(encryptor: TinkEncryptor) {
        val expected = faker.lorem().paragraph().repeat(10)

        val encryptedSource = bufferOf(encryptor.encrypt(expected.toUtf8Bytes()))
        val decryptedSource = encryptedSource.asTinkDecryptSource(encryptor)

        val decryptedBuffer = Buffer()
        decryptedSource.readAllTo(decryptedBuffer)

        decryptedBuffer.readUtf8() shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt and decrypt`(encryptor: TinkEncryptor) {
        val expectedText = faker.lorem().paragraph().repeat(10)
        val buffer = bufferOf(expectedText)

        val sink = Buffer()
        val encryptedSink = sink.asTinkEncryptSink(encryptor)

        encryptedSink.write(buffer, buffer.size)

        val source = Buffer()
        val decryptedSource = sink.asTinkDecryptSource(encryptor)
        decryptedSource.readAllTo(source)

        source.readUtf8() shouldBeEqualTo expectedText
    }

    /**
     * 압축 → 암호화 → 복호화 → 압축 해제 라운드트립 검증
     */
    @ParameterizedTest
    @MethodSource("encryptors")
    fun `compress and encrypt`(encryptor: TinkEncryptor) {
        compressors().forEach { compressor ->
            val expectedText = faker.lorem().paragraph().repeat(10)
            val buffer = bufferOf(expectedText)

            val sink = Buffer()
            sink.asTinkEncryptSink(encryptor).asCompressSink(compressor).use { compressAndEncryptSink ->
                compressAndEncryptSink.write(buffer, buffer.size)
            }

            val source = Buffer()
            val decryptAndDecompressSource = sink.asTinkDecryptSource(encryptor).asDecompressSource(compressor)
            decryptAndDecompressSource.readAllTo(source)

            source.readUtf8() shouldBeEqualTo expectedText
        }
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `decrypt source should support incremental reads and eof`(encryptor: TinkEncryptor) {
        val expected = faker.lorem().paragraph().repeat(5)
        val encryptedSource = bufferOf(encryptor.encrypt(expected.toUtf8Bytes()))
        val decryptedSource = encryptedSource.asTinkDecryptSource(encryptor)

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
        val encryptedSource = bufferOf(TinkEncryptors.AES256_GCM.encrypt(expected.toUtf8Bytes()))
        val decryptedSource = encryptedSource.asTinkDecryptSource(TinkEncryptors.AES256_GCM)

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
