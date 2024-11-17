package io.bluetape4k.io.okio.encrypt

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
    @MethodSource("getEncryptors")
    fun `encrypt random string`(encryptor: Encryptor) {
        val plainText = Fakers.randomString(1024)
        val source = bufferOf(plainText)

        val output = Buffer()
        val sink = EncryptSink(output, encryptor)

        sink.write(source, source.size)

        output.readByteArray() shouldBeEqualTo encryptor.encrypt(plainText.toByteArray())
    }
}
