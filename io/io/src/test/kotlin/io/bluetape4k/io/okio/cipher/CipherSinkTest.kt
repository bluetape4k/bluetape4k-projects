package io.bluetape4k.io.okio.cipher

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

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
}
