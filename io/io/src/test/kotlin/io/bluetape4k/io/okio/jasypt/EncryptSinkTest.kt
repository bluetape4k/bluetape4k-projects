package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class EncryptSinkTest: AbstractEncryptTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt random string`(encryptor: Encryptor) {
        val plainText = Fakers.randomString(1024, 8192)
        val source = bufferOf(plainText)

        val sink = Buffer()
        val encryptSink = sink.asEncryptSink(encryptor)
        encryptSink.write(source, source.size)

        val encryptedBytes = sink.readByteArray()
        encryptor.decrypt(encryptedBytes) shouldBeEqualTo plainText.toByteArray()
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt random string with large size`(encryptor: Encryptor) {
        val plainText = Fakers.randomString(8192, 16384)
        log.debug { "plainText=$plainText" }
        val source = bufferOf(plainText)

        val sink = Buffer()
        val encryptSink = sink.asEncryptSink(encryptor)
        encryptSink.write(source, source.size)

        val encryptedBytes = sink.readByteArray()
        encryptor.decrypt(encryptedBytes) shouldBeEqualTo plainText.toByteArray()
    }

    @Test
    fun `write should consume only requested bytes`() {
        val plainBytes = ByteArray(2048) { (it % 251).toByte() }
        val source = bufferOf(plainBytes)
        val sink = Buffer()
        val encryptSink = sink.asEncryptSink(Encryptors.AES)

        val byteCount = 1024L
        encryptSink.write(source, byteCount)

        source.size shouldBeEqualTo plainBytes.size.toLong() - byteCount
        Encryptors.AES.decrypt(sink.readByteArray()) shouldBeEqualTo plainBytes.copyOfRange(0, byteCount.toInt())
    }

    @Test
    fun `write with invalid byteCount behavior`() {
        val source = bufferOf(byteArrayOf(1, 2, 3, 4))
        val encrypted = Buffer()
        val sink = encrypted.asEncryptSink(Encryptors.AES)

        sink.write(source, -1L)
        source.size shouldBeEqualTo 4L
        encrypted.size shouldBeEqualTo 0L

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, source.size + 1L)
        }
    }
}
