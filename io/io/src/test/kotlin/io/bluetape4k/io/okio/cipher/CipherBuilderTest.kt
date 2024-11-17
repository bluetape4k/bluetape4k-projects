package io.bluetape4k.io.okio.cipher

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

class CipherBuilderTest: AbstractCipherTest() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `create AES cipher for encryption`() {
        val content = Fakers.randomString(128, 256)

        val encryptedBytes = encryptCipher.doFinal(content.toUtf8Bytes())
        val decryptedBytes = decryptCipher.update(encryptedBytes) +
                runCatching { decryptCipher.doFinal() }.getOrDefault(emptyByteArray)

        decryptedBytes.toUtf8String() shouldBeEqualTo content
    }
}
