package io.bluetape4k.okio.tink

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.okio.bufferOf
import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class TinkEncryptSinkTest: AbstractTinkEncryptTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt random string`(encryptor: TinkEncryptor) {
        val plainText = Fakers.randomString(1024, 8192)
        val source = bufferOf(plainText)

        val sink = Buffer()
        val encryptSink = sink.asTinkEncryptSink(encryptor)
        encryptSink.write(source, source.size)

        val encryptedBytes = sink.readByteArray()
        encryptor.decrypt(encryptedBytes) shouldBeEqualTo plainText.toByteArray()
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt random string with large size`(encryptor: TinkEncryptor) {
        val plainText = Fakers.randomString(8192, 16384)
        log.debug { "plainText length=${plainText.length}" }
        val source = bufferOf(plainText)

        val sink = Buffer()
        val encryptSink = sink.asTinkEncryptSink(encryptor)
        encryptSink.write(source, source.size)

        val encryptedBytes = sink.readByteArray()
        encryptor.decrypt(encryptedBytes) shouldBeEqualTo plainText.toByteArray()
    }

    @Test
    fun `write should consume only requested bytes`() {
        val plainBytes = ByteArray(2048) { (it % 251).toByte() }
        val source = bufferOf(plainBytes)
        val sink = Buffer()
        val encryptSink = sink.asTinkEncryptSink(TinkEncryptors.AES256_GCM)

        val byteCount = 1024L
        encryptSink.write(source, byteCount)

        source.size shouldBeEqualTo plainBytes.size.toLong() - byteCount
        TinkEncryptors.AES256_GCM.decrypt(sink.readByteArray()) shouldBeEqualTo plainBytes.copyOfRange(0, byteCount.toInt())
    }

    @Test
    fun `write with invalid byteCount behavior`() {
        val source = bufferOf(byteArrayOf(1, 2, 3, 4))
        val encrypted = Buffer()
        val sink = encrypted.asTinkEncryptSink(TinkEncryptors.AES256_GCM)

        sink.write(source, -1L)
        source.size shouldBeEqualTo 4L
        encrypted.size shouldBeEqualTo 0L

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, source.size + 1L)
        }
    }
}
