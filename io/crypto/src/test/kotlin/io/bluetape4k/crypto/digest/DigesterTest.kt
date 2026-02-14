package io.bluetape4k.crypto.digest

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.params.provider.FieldSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments

class DigesterTest {

    companion object: KLogging() {

        private fun getRandomString() =
            Fakers.randomString(256, 1024)

        private const val REPEAT_SIZE = 10
    }

    private val digesters: List<Arguments> = listOf(
        Digesters.KECCAK256,
        Digesters.KECCAK384,
        Digesters.KECCAK512,
        Digesters.MD5,
        Digesters.SHA1,
        Digesters.SHA256,
        Digesters.SHA384,
        Digesters.SHA512
    ).map { Arguments.of(it) }

    @ParameterizedTest(name = "digest byte array by {0}")
    @FieldSource("digesters")
    fun `digest message as byte array`(digester: Digester) {
        repeat(REPEAT_SIZE) {
            val message = getRandomString().toUtf8Bytes()
            val digested = digester.digest(message)
            digester.matches(message, digested).shouldBeTrue()
        }
    }

    @ParameterizedTest(name = "digest string by {0}")
    @FieldSource("digesters")
    fun `digest message as string`(digester: Digester) {
        repeat(REPEAT_SIZE) {
            val message = getRandomString()
            val digested = digester.digest(message)
            digester.matches(message, digested).shouldBeTrue()
        }
    }

    @ParameterizedTest(name = "digest char array by {0}")
    @FieldSource("digesters")
    fun `digest message with char array`(digester: Digester) {
        repeat(REPEAT_SIZE) {
            val message = getRandomString().toCharArray()
            val digested = digester.digest(message)
            digester.matches(message, digested).shouldBeTrue()
        }
    }

    @ParameterizedTest(name = "digest string by {0}")
    @FieldSource("digesters")
    fun `digest message as string in multi threadings`(digester: Digester) {
        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                val message = getRandomString()
                val digested = digester.digest(message)
                digester.matches(message, digested).shouldBeTrue()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @ParameterizedTest(name = "digest string by {0}")
    @FieldSource("digesters")
    fun `digest message as string in virtual threads`(digester: Digester) {
        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val message = getRandomString()
                val digested = digester.digest(message)
                digester.matches(message, digested).shouldBeTrue()
            }
            .run()
    }

    @ParameterizedTest(name = "digest string by {0}")
    @FieldSource("digesters")
    fun `digest message as string in suspend jobs`(digester: Digester) = runTest {
        SuspendedJobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(16 * 2 * Runtimex.availableProcessors)
            .add {
                withContext(Dispatchers.Default) {
                    val message = getRandomString()
                    val digested = digester.digest(message)
                    digester.matches(message, digested).shouldBeTrue()
                }
            }
            .run()
    }
}
