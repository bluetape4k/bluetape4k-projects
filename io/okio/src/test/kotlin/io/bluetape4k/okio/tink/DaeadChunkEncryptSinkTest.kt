package io.bluetape4k.okio.tink

import io.bluetape4k.tink.daead.TinkDaeads
import okio.Buffer
import okio.Sink
import okio.Timeout
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.io.IOException
import io.bluetape4k.assertions.assertFailsWith

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
        encrypted.size shouldBeGreaterThan 0L
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

    @Test
    fun `write after close throws IOException`() {
        val sink = Buffer().asDaeadChunkEncryptSink(daead)
        sink.close()

        assertFailsWith<IOException> {
            sink.write(Buffer().writeUtf8("x"), 1L)
        }
    }

    @Test
    fun `close is idempotent`() {
        val encrypted = Buffer()
        val sink = encrypted.asDaeadChunkEncryptSink(daead)
        sink.write(Buffer().writeUtf8("hello"), 5L)

        sink.close()
        val sizeAfterFirstClose = encrypted.size
        sink.close()

        encrypted.size shouldBeEqualTo sizeAfterFirstClose
    }

    @Test
    fun `flush after close is no-op`() {
        val encrypted = Buffer()
        val sink = encrypted.asDaeadChunkEncryptSink(daead)
        sink.write(Buffer().writeUtf8("hello"), 5L)
        sink.close()
        val sizeAfterClose = encrypted.size

        sink.flush()

        encrypted.size shouldBeEqualTo sizeAfterClose
    }

    @Test
    fun `write zero bytes is no-op`() {
        val encrypted = Buffer()
        val sink = encrypted.asDaeadChunkEncryptSink(daead, chunkSize = 4)

        sink.write(Buffer(), 0L)
        sink.flush()

        encrypted.size shouldBeEqualTo 0L
        sink.close()
    }

    @Test
    fun `round-trip with payload size exact multiple of chunkSize`() {
        val chunkSize = 16
        listOf(1, 2, 4).forEach { multiple ->
            val plaintext = ByteArray(chunkSize * multiple) { (it % 127).toByte() }
            val encrypted = Buffer()

            encrypted.asDaeadChunkEncryptSink(daead, chunkSize = chunkSize).use { sink ->
                sink.write(Buffer().write(plaintext), plaintext.size.toLong())
            }

            val decrypted = Buffer()
            encrypted.asDaeadChunkDecryptSource(daead).readAllTo(decrypted)

            decrypted.readByteArray() shouldBeEqualTo plaintext
        }
    }

    @Test
    fun `round-trip with payload sizes around chunk boundaries`() {
        val chunkSize = 16
        listOf(chunkSize - 1, chunkSize, chunkSize + 1, 2 * chunkSize - 1, 2 * chunkSize, 2 * chunkSize + 1).forEach { size ->
            val plaintext = ByteArray(size) { (it % 251).toByte() }
            val encrypted = Buffer()

            encrypted.asDaeadChunkEncryptSink(daead, chunkSize = chunkSize).use { sink ->
                sink.write(Buffer().write(plaintext), plaintext.size.toLong())
            }

            val decrypted = Buffer()
            encrypted.asDaeadChunkDecryptSource(daead).readAllTo(decrypted)

            decrypted.readByteArray() shouldBeEqualTo plaintext
        }
    }

    @Test
    fun `close propagates exception from delegate sink`() {
        val throwingDelegate = ThrowingDelegateSink(throwOnWrite = true, throwOnClose = false)
        val sink = throwingDelegate.asDaeadChunkEncryptSink(daead, chunkSize = 100)

        sink.write(Buffer().writeUtf8("hello"), 5L)

        val ex = assertFailsWith<IOException> {
            sink.close()
        }
        ex.message shouldBeEqualTo "delegate write failed"
    }

    @Test
    fun `close suppresses delegate close exception when emit also throws`() {
        val throwingDelegate = ThrowingDelegateSink(throwOnWrite = true, throwOnClose = true)
        val sink = throwingDelegate.asDaeadChunkEncryptSink(daead, chunkSize = 100)

        sink.write(Buffer().writeUtf8("hello"), 5L)

        val ex = assertFailsWith<IOException> {
            sink.close()
        }
        ex.message shouldBeEqualTo "delegate write failed"
        ex.suppressed.shouldNotBeEmpty()
        ex.suppressed[0].message shouldBeEqualTo "delegate close failed"
    }

    private class ThrowingDelegateSink(
        private val throwOnWrite: Boolean,
        private val throwOnClose: Boolean,
    ): Sink {
        override fun write(source: Buffer, byteCount: Long) {
            if (throwOnWrite) throw IOException("delegate write failed")
        }
        override fun flush() {}
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() {
            if (throwOnClose) throw IOException("delegate close failed")
        }
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
