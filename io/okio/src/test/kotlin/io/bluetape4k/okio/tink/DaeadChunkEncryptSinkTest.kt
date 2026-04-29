package io.bluetape4k.okio.tink

import io.bluetape4k.tink.daead.TinkDaeads
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DaeadChunkEncryptSinkTest: AbstractTinkEncryptTest() {

    private val daead = TinkDaeads.AES256_SIV

    @Test
    fun `empty input writes no chunk`() {
        val encrypted = Buffer()

        encrypted.asDaeadChunkEncryptSink(daead).use {
            it.flush()
        }

        encrypted.size shouldBeEqualTo 0L
    }

    @Test
    fun `single chunk uses big endian ciphertext length header`() {
        val plaintext = byteArrayOf(1, 2, 3)
        val expectedCiphertext = daead.encryptDeterministically(plaintext)
        val encrypted = Buffer()

        encrypted.asDaeadChunkEncryptSink(daead, chunkSize = plaintext.size).use { sink ->
            sink.write(Buffer().write(plaintext), plaintext.size.toLong())
        }

        encrypted.readLong() shouldBeEqualTo expectedCiphertext.size.toLong()
        encrypted.readByteArray() shouldBeEqualTo expectedCiphertext
    }

    @Test
    fun `multiple small writes are combined into configured chunks`() {
        val encrypted = Buffer()
        val chunks = mutableListOf<Long>()

        encrypted.asDaeadChunkEncryptSink(daead, chunkSize = 4).use { sink ->
            listOf("ab", "cd", "ef", "gh", "i").forEach { text ->
                val source = Buffer().writeUtf8(text)
                sink.write(source, source.size)
            }
        }

        val framed = encrypted.clone()
        while (framed.size > 0L) {
            val ciphertextLength = framed.readLong()
            chunks += ciphertextLength
            framed.skip(ciphertextLength)
        }

        chunks.size shouldBeEqualTo 3
    }

    @Test
    fun `flush does not write partial chunk`() {
        val encrypted = Buffer()
        val sink = encrypted.asDaeadChunkEncryptSink(daead, chunkSize = 8)

        sink.write(Buffer().writeUtf8("abc"), 3L)
        sink.flush()

        encrypted.size shouldBeEqualTo 0L

        sink.close()
        (encrypted.size > 0L) shouldBeEqualTo true
    }

    @Test
    fun `write rejects invalid byteCount`() {
        val encrypted = Buffer()
        val sink = encrypted.asDaeadChunkEncryptSink(daead)

        assertFailsWith<IllegalArgumentException> {
            sink.write(Buffer().writeUtf8("abc"), 4L)
        }
    }

    @Test
    fun `constructor rejects non positive chunk size`() {
        assertFailsWith<IllegalArgumentException> {
            Buffer().asDaeadChunkEncryptSink(daead, chunkSize = 0)
        }
    }

    @Test
    fun `associated data is defensively copied by encrypt sink`() {
        val associatedData = byteArrayOf(1)
        val encrypted = Buffer()
        val sink = encrypted.asDaeadChunkEncryptSink(daead, chunkSize = 4, associatedData = associatedData)

        associatedData[0] = 2
        sink.use {
            it.write(Buffer().writeUtf8("secret"), 6L)
        }

        val decrypted = Buffer()
        encrypted.asDaeadChunkDecryptSource(daead, associatedData = byteArrayOf(1)).readAllTo(decrypted)

        decrypted.readUtf8() shouldBeEqualTo "secret"
    }

    private fun DaeadChunkDecryptSource.readAllTo(sink: Buffer): Long {
        var total = 0L
        while (true) {
            val bytesRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead < 0L) {
                return total
            }
            total += bytesRead
        }
    }
}
