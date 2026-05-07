package io.bluetape4k.okio.tink

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.okio.bufferOf
import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors
import okio.Buffer
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.bluetape4k.assertions.assertFailsWith

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
        TinkEncryptors.AES256_GCM.decrypt(sink.readByteArray()) shouldBeEqualTo plainBytes.copyOfRange(
            0,
            byteCount.toInt()
        )
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

    @Test
    fun `write empty source produces no output`() {
        val source = bufferOf(ByteArray(0))
        val encrypted = Buffer()
        val sink = encrypted.asTinkEncryptSink(TinkEncryptors.AES256_GCM)

        sink.write(source, 0L)
        encrypted.size shouldBeEqualTo 0L
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `encrypt sink close delegates to underlying sink`(encryptor: TinkEncryptor) {
        val underlying = Buffer()
        val encryptSink = underlying.asTinkEncryptSink(encryptor)

        // close를 호출해도 예외가 발생하지 않아야 합니다.
        encryptSink.close()
    }

    @ParameterizedTest
    @MethodSource("encryptors")
    fun `multiple writes are independently encrypted`(encryptor: TinkEncryptor) {
        val plainText1 = "first chunk"
        val plainText2 = "second chunk"

        val output = Buffer()
        val encryptSink = output.asTinkEncryptSink(encryptor)

        // 두 번 쓰면 두 개의 독립적인 암호문이 누적된다.
        encryptSink.write(bufferOf(plainText1), plainText1.length.toLong())
        val encrypted1Size = output.size

        encryptSink.write(bufferOf(plainText2), plainText2.length.toLong())
        val totalEncryptedSize = output.size

        // 두 번째 write 후에도 데이터가 누적되어야 한다.
        (totalEncryptedSize > encrypted1Size) shouldBeEqualTo true
    }
}
