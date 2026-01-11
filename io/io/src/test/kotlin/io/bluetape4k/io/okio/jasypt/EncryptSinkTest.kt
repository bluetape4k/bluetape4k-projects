package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
        val source = bufferOf(plainText)

        val sink = Buffer()
        val encryptSink = sink.asEncryptSink(encryptor)
        encryptSink.write(source, source.size)

        val encryptedBytes = sink.readByteArray()
        encryptor.decrypt(encryptedBytes) shouldBeEqualTo plainText.toByteArray()
    }
}
