package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.io.okio.compress.asCompressSink
import io.bluetape4k.io.okio.compress.asDecompressSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
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
        decryptedSource.read(decryptedBuffer, Long.MAX_VALUE)

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
        decryptedSource.read(source, Long.MAX_VALUE)

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
            val compressAndEncryptSink = sink.asEncryptSink(encryptor).asCompressSink(compressor)
            compressAndEncryptSink.write(buffer, buffer.size)


            val source = Buffer()
            val decryptAndDecompressSource = sink.asDecryptSource(encryptor).asDecompressSource(compressor)
            decryptAndDecompressSource.read(source, sink.size)

            source.readUtf8() shouldBeEqualTo expectedText
        }
    }
}
