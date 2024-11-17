package io.bluetape4k.io.okio.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DecryptSourceTest: AbstractEncryptTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource("getEncryptors")
    fun `decrypt by source`(encryptor: Encryptor) {
        val expected = faker.lorem().paragraph()
        log.debug { "expected=$expected" }

        val encryptedSource = bufferOf(encryptor.encrypt(expected.toUtf8Bytes()))
        val decoded = DecryptSource(encryptedSource, encryptor)
        val output = bufferOf(decoded)

        output.readUtf8() shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("getEncryptors")
    fun `decrypt by source with chunked`(encryptor: Encryptor) {
        val expected = faker.lorem().paragraph()
        log.debug { "expected=$expected" }

        val encryptedSource = bufferOf(encryptor.encrypt(expected.toUtf8Bytes()))
        val decoded = DecryptSource(encryptedSource, encryptor)

        // DecryptSource 는 chunked 로 읽을 수 없음
        assertFailsWith<UnsupportedOperationException> {
            val output = Buffer()
            decoded.read(output, 5)
            output.readByteArray() shouldBeEqualTo expected.toUtf8Bytes().copyOfRange(0, 5)
        }
    }
}
