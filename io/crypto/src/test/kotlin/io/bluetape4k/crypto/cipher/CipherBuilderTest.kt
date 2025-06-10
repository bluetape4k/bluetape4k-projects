package io.bluetape4k.crypto.cipher

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

class CipherBuilderTest: AbstractCipherTest() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `create AES cipher for encryption`() {
        repeat(100) {
            val content = Fakers.randomString(128, 1024)

            val encryptedBytes = encryptCipher.doFinal(content.toUtf8Bytes())
            val decryptedBytes = decryptCipher.update(encryptedBytes) +
                    runCatching { decryptCipher.doFinal() }.getOrDefault(emptyByteArray)
            val decryptedContent = decryptedBytes.toUtf8String()

            log.debug { "content: $content, decrypted content=${decryptedContent}" }
            decryptedContent shouldBeEqualTo content
        }
    }

    @Test
    fun `create AES cipher for encryption in multi threading`() {
        MultithreadingTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(4)
            .add {
                val encryptCipher = builder.build(Cipher.ENCRYPT_MODE)
                val decryptCipher = builder.build(Cipher.DECRYPT_MODE)

                val content = Fakers.randomString(128, 1024)

                val encryptedBytes = encryptCipher.doFinal(content.toUtf8Bytes())
                val decryptedBytes = decryptCipher.update(encryptedBytes) +
                        runCatching { decryptCipher.doFinal() }.getOrDefault(emptyByteArray)
                val decryptedContent = decryptedBytes.toUtf8String()

                log.debug { "content: $content, decrypted content=${decryptedContent}" }
                decryptedContent shouldBeEqualTo content
            }
            .run()
    }

    @Test
    fun `create AES cipher for encryption in virtual threads`() {
        StructuredTaskScopeTester()
            .roundsPerTask(4 * 2 * Runtimex.availableProcessors)
            .add {
                val encryptCipher = builder.build(Cipher.ENCRYPT_MODE)
                val decryptCipher = builder.build(Cipher.DECRYPT_MODE)

                val content = Fakers.randomString(128, 1024)

                val encryptedBytes = encryptCipher.doFinal(content.toUtf8Bytes())
                val decryptedBytes = decryptCipher.update(encryptedBytes) +
                        runCatching { decryptCipher.doFinal() }.getOrDefault(emptyByteArray)
                val decryptedContent = decryptedBytes.toUtf8String()

                log.debug { "content: $content, decrypted content=${decryptedContent}" }
                decryptedContent shouldBeEqualTo content
            }
            .run()
    }

    @Test
    fun `create AES cipher for encryption in suspended jobs`() = runTest {
        SuspendedJobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(4 * 2 * Runtimex.availableProcessors)
            .add {
                val encryptCipher = builder.build(Cipher.ENCRYPT_MODE)
                val decryptCipher = builder.build(Cipher.DECRYPT_MODE)

                val content = Fakers.randomString(128, 1024)

                val encryptedBytes = encryptCipher.doFinal(content.toUtf8Bytes())
                val decryptedBytes = decryptCipher.update(encryptedBytes) +
                        runCatching { decryptCipher.doFinal() }.getOrDefault(emptyByteArray)
                val decryptedContent = decryptedBytes.toUtf8String()

                log.debug { "content: $content, decrypted content=${decryptedContent}" }
                decryptedContent shouldBeEqualTo content
            }
            .run()
    }
}
