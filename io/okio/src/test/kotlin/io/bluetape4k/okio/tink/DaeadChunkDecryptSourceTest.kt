package io.bluetape4k.okio.tink

import io.bluetape4k.tink.daead.TinkDaeads
import okio.Buffer
import okio.EOFException
import okio.ForwardingSource
import okio.Source
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

class DaeadChunkDecryptSourceTest: AbstractTinkEncryptTest() {

    private val daead = TinkDaeads.AES256_SIV

    @Test
    fun `decrypts single chunk`() {
        val expected = "hello daead chunk"
        val encrypted = encrypt(expected.toByteArray(), chunkSize = DEFAULT_DAEAD_CHUNK_SIZE)

        val decrypted = Buffer()
        encrypted.asDaeadChunkDecryptSource(daead).readAllTo(decrypted)

        decrypted.readUtf8() shouldBeEqualTo expected
    }

    @Test
    fun `decrypts multiple chunks with incremental reads`() {
        val expected = ByteArray(257) { (it % 127).toByte() }
        val encrypted = encrypt(expected, chunkSize = 17)
        val source = encrypted.asDaeadChunkDecryptSource(daead)
        val decrypted = Buffer()

        while (source.read(decrypted, 5L) >= 0L) {
            // 반복 read 계약 검증
        }

        decrypted.readByteArray() shouldBeEqualTo expected
        source.read(Buffer(), 1L) shouldBeEqualTo -1L
    }

    @Test
    fun `decrypts data written by multiple small writes`() {
        val encrypted = Buffer()

        encrypted.asDaeadChunkEncryptSink(daead, chunkSize = 8).use { sink ->
            listOf("a", "bc", "def", "ghij", "klmno").forEach { text ->
                val source = Buffer().writeUtf8(text)
                sink.write(source, source.size)
            }
        }

        val decrypted = Buffer()
        encrypted.asDaeadChunkDecryptSource(daead).readAllTo(decrypted)

        decrypted.readUtf8() shouldBeEqualTo "abcdefghijklmno"
    }

    @Test
    fun `read with zero byteCount returns zero`() {
        val encrypted = encrypt("abc".toByteArray(), chunkSize = 4)

        encrypted.asDaeadChunkDecryptSource(daead).read(Buffer(), 0L) shouldBeEqualTo 0L
    }

    @Test
    fun `read rejects negative byteCount`() {
        val encrypted = encrypt("abc".toByteArray(), chunkSize = 4)

        assertFailsWith<IllegalArgumentException> {
            encrypted.asDaeadChunkDecryptSource(daead).read(Buffer(), -1L)
        }
    }

    @Test
    fun `empty input returns eof`() {
        Buffer().asDaeadChunkDecryptSource(daead).read(Buffer(), 1L) shouldBeEqualTo -1L
    }

    @Test
    fun `truncated header throws eof exception`() {
        assertFailsWith<EOFException> {
            Buffer().writeByte(1).asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `truncated ciphertext throws eof exception`() {
        val malformed = Buffer().writeLong(16L).write(ByteArray(15))

        assertFailsWith<EOFException> {
            malformed.asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `invalid ciphertext length throws io exception`() {
        assertFailsWith<IOException> {
            Buffer().writeLong(0L).asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `wrong associated data fails to decrypt`() {
        val encrypted = encrypt("secret".toByteArray(), chunkSize = 4, associatedData = byteArrayOf(1))

        assertFailsWith<Exception> {
            encrypted.asDaeadChunkDecryptSource(daead, associatedData = byteArrayOf(2)).read(Buffer(), 1L)
        }
    }

    @Test
    fun `associated data is defensively copied by decrypt source`() {
        val encrypted = encrypt("secret".toByteArray(), chunkSize = 4, associatedData = byteArrayOf(1))
        val associatedData = byteArrayOf(1)
        val source = encrypted.asDaeadChunkDecryptSource(daead, associatedData = associatedData)

        associatedData[0] = 2

        val decrypted = Buffer()
        source.readAllTo(decrypted)

        decrypted.readUtf8() shouldBeEqualTo "secret"
    }

    @Test
    fun `first read consumes only the next encrypted chunk`() {
        val plaintext = ByteArray(512) { (it % 64).toByte() }
        val encrypted = encrypt(plaintext, chunkSize = 64)
        val encryptedSize = encrypted.size
        val countingSource = CountingSource(encrypted)
        val source = countingSource.asDaeadChunkDecryptSource(daead)
        val decrypted = Buffer()

        source.read(decrypted, 1L) shouldBeEqualTo 1L

        (countingSource.bytesRead < encryptedSize) shouldBeEqualTo true
    }

    private fun encrypt(
        plaintext: ByteArray,
        chunkSize: Int,
        associatedData: ByteArray = ByteArray(0),
    ): Buffer {
        val encrypted = Buffer()
        encrypted.asDaeadChunkEncryptSink(daead, chunkSize = chunkSize, associatedData = associatedData).use { sink ->
            sink.write(Buffer().write(plaintext), plaintext.size.toLong())
        }
        return encrypted
    }

    private fun Source.readAllTo(sink: Buffer): Long {
        var total = 0L
        while (true) {
            val bytesRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead < 0L) {
                return total
            }
            total += bytesRead
        }
    }

    private class CountingSource(delegate: Source): ForwardingSource(delegate) {
        var bytesRead: Long = 0L
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = super.read(sink, byteCount)
            if (read > 0L) {
                bytesRead += read
            }
            return read
        }
    }
}
