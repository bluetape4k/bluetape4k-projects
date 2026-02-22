package io.bluetape4k.io.okio.cipher

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
class DeprecatedCipherAliasTest: AbstractCipherTest() {

    @Test
    fun `CipherSink 는 FinalizingCipherSink 동작을 유지한다`() {
        val plainText = "cipher-deprecated-sink"
        val source = bufferOf(plainText)
        val output = Buffer()

        CipherSink(output, encryptCipher).use { sink ->
            sink.write(source, source.size)
        }

        output.readByteArray() shouldBeEqualTo encryptCipher.doFinal(plainText.toUtf8Bytes())
    }

    @Test
    fun `CipherSource 는 StreamingCipherSource 동작을 유지한다`() {
        val plainText = "cipher-deprecated-source"
        val encryptedSource = bufferOf(encryptCipher.doFinal(plainText.toUtf8Bytes()))

        val source = CipherSource(encryptedSource, decryptCipher)
        val output = bufferOf(source)

        output.readUtf8() shouldBeEqualTo plainText
    }
}
