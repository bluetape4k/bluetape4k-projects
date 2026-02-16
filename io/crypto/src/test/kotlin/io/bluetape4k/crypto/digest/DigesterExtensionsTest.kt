package io.bluetape4k.crypto.digest

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.params.provider.FieldSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments

class DigesterExtensionsTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val digesters: List<Arguments> = listOf(
        Digesters.SHA256,
        Digesters.SHA512,
        Digesters.MD5,
        Digesters.KECCAK256,
    ).map { Arguments.of(it) }

    @ParameterizedTest(name = "String.digest by {0}")
    @FieldSource("digesters")
    fun `String digest extension function`(digester: Digester) {
        repeat(REPEAT_SIZE) {
            val message = Fakers.randomString(256, 1024)
            val digest = message.digest(digester)
            message.matchesDigest(digest, digester).shouldBeTrue()
        }
    }

    @ParameterizedTest(name = "ByteArray.digest by {0}")
    @FieldSource("digesters")
    fun `ByteArray digest extension function`(digester: Digester) {
        repeat(REPEAT_SIZE) {
            val message = Fakers.randomString(256, 1024).toUtf8Bytes()
            val digest = message.digest(digester)
            message.matchesDigest(digest, digester).shouldBeTrue()
        }
    }
}
