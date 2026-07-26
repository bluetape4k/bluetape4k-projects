package io.bluetape4k.okio.tink

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.tink.daead.TinkDaeads
import okio.Buffer
import okio.EOFException
import okio.ForwardingSource
import okio.Source
import org.junit.jupiter.api.Test
import java.io.IOException
import java.security.GeneralSecurityException

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
            // Exercise the repeated-read contract across decrypted chunk boundaries.
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
    fun `encrypted empty input returns eof`() {
        val encrypted = encrypt(ByteArray(0), chunkSize = 4)

        encrypted.asDaeadChunkDecryptSource(daead).read(Buffer(), 1L) shouldBeEqualTo -1L
    }

    @Test
    fun `raw empty input without final marker throws eof exception`() {
        assertFailsWith<EOFException> {
            Buffer().asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `truncated header throws eof exception`() {
        assertFailsWith<EOFException> {
            Buffer().writeByte(1).asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `truncated ciphertext throws eof exception`() {
        val malformed = Buffer()
            .writeByte(DAEAD_CHUNK_NON_FINAL_FLAG)
            .writeLong(16L)
            .write(ByteArray(15))

        assertFailsWith<EOFException> {
            malformed.asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `invalid ciphertext length zero throws io exception`() {
        assertFailsWith<IOException> {
            Buffer()
                .writeByte(DAEAD_CHUNK_NON_FINAL_FLAG)
                .writeLong(0L)
                .asDaeadChunkDecryptSource(daead)
                .read(Buffer(), 1L)
        }
    }

    @Test
    fun `negative ciphertext length throws io exception`() {
        assertFailsWith<IOException> {
            Buffer()
                .writeByte(DAEAD_CHUNK_NON_FINAL_FLAG)
                .writeLong(-1L)
                .asDaeadChunkDecryptSource(daead)
                .read(Buffer(), 1L)
        }
    }

    @Test
    fun `ciphertext length exceeding maxCiphertextLength throws io exception`() {
        val smallMax = 100L
        val header = Buffer()
            .writeByte(DAEAD_CHUNK_NON_FINAL_FLAG)
            .writeLong(smallMax + 1L)

        assertFailsWith<IOException> {
            header.asDaeadChunkDecryptSource(daead, maxCiphertextLength = smallMax).read(Buffer(), 1L)
        }
    }

    @Test
    fun `wrong associated data fails to decrypt`() {
        val encrypted = encrypt("secret".toByteArray(), chunkSize = 4, associatedData = byteArrayOf(1))

        assertFailsWith<GeneralSecurityException> {
            encrypted.asDaeadChunkDecryptSource(daead, associatedData = byteArrayOf(2)).read(Buffer(), 1L)
        }
    }

    @Test
    fun `reordered chunks fail to decrypt`() {
        val frames = encrypt("abcdefgh".toByteArray(), chunkSize = 4).readFrames()
        val reordered = Buffer()
            .write(frames[1])
            .write(frames[0])

        assertFailsWith<GeneralSecurityException> {
            reordered.asDaeadChunkDecryptSource(daead).readAllTo(Buffer())
        }
    }

    @Test
    fun `dropped final chunk fails to decrypt`() {
        val frames = encrypt("abcdefgh".toByteArray(), chunkSize = 4).readFrames()
        val truncated = Buffer().write(frames.first())

        assertFailsWith<EOFException> {
            truncated.asDaeadChunkDecryptSource(daead).readAllTo(Buffer())
        }
    }

    @Test
    fun `duplicated chunk fails to decrypt`() {
        val frames = encrypt("abcdefgh".toByteArray(), chunkSize = 4).readFrames()
        val duplicated = Buffer()
            .write(frames[0])
            .write(frames[0])
            .write(frames[1])

        assertFailsWith<GeneralSecurityException> {
            duplicated.asDaeadChunkDecryptSource(daead).readAllTo(Buffer())
        }
    }

    @Test
    fun `trailing data after final chunk fails to decrypt`() {
        val tampered = encrypt("secret".toByteArray(), chunkSize = 4)
            .writeByte(DAEAD_CHUNK_NON_FINAL_FLAG)

        assertFailsWith<IOException> {
            tampered.asDaeadChunkDecryptSource(daead).readAllTo(Buffer())
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

        countingSource.bytesRead shouldBeLessThan encryptedSize
    }

    @Test
    fun `read after close throws IOException`() {
        val source = encrypt("hello".toByteArray(), chunkSize = 4).asDaeadChunkDecryptSource(daead)
        source.close()

        assertFailsWith<IOException> {
            source.read(Buffer(), 1L)
        }
    }

    @Test
    fun `close is idempotent`() {
        val closeCountingSource = CloseCountingSource(encrypt("hello".toByteArray(), chunkSize = 4))
        val source = closeCountingSource.asDaeadChunkDecryptSource(daead)

        source.close()
        source.close()

        closeCountingSource.closeCount shouldBeEqualTo 1
    }

    @Test
    fun `stalled source throws IOException after no progress`() {
        val stalledSource = StalledSource()

        assertFailsWith<IOException> {
            stalledSource.asDaeadChunkDecryptSource(daead).read(Buffer(), 1L)
        }
    }

    @Test
    fun `round-trip with payload size exact multiple of chunkSize`() {
        val chunkSize = 16
        listOf(1, 2, 4).forEach { multiple ->
            val plaintext = ByteArray(chunkSize * multiple) { (it % 127).toByte() }
            val encrypted = encrypt(plaintext, chunkSize = chunkSize)

            val decrypted = Buffer()
            encrypted.asDaeadChunkDecryptSource(daead).readAllTo(decrypted)

            decrypted.readByteArray() shouldBeEqualTo plaintext
        }
    }

    @Test
    fun `round-trip with payload sizes around chunk boundaries`() {
        val chunkSize = 16
        listOf(
            chunkSize - 1,
            chunkSize,
            chunkSize + 1,
            2 * chunkSize - 1,
            2 * chunkSize,
            2 * chunkSize + 1
        ).forEach { size ->
            val plaintext = ByteArray(size) { (it % 251).toByte() }
            val encrypted = encrypt(plaintext, chunkSize = chunkSize)

            val decrypted = Buffer()
            encrypted.asDaeadChunkDecryptSource(daead).readAllTo(decrypted)

            decrypted.readByteArray() shouldBeEqualTo plaintext
        }
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

    private fun Buffer.readFrames(): List<ByteArray> {
        val frames = mutableListOf<ByteArray>()
        while (size > 0L) {
            val frame = Buffer()
            val flags = readByte()
            val ciphertextLength = readLong()
            frame.writeByte(flags.toInt())
            frame.writeLong(ciphertextLength)
            frame.write(this, ciphertextLength)
            frames += frame.readByteArray()
        }
        return frames
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

    private class CloseCountingSource(delegate: Source): ForwardingSource(delegate) {
        var closeCount: Int = 0
            private set

        override fun close() {
            closeCount++
            super.close()
        }
    }

    /** Simulates a source that repeatedly returns 0L without making progress. */
    private class StalledSource: Source {
        override fun read(sink: Buffer, byteCount: Long): Long = 0L
        override fun timeout() = okio.Timeout.NONE
        override fun close() = Unit
    }
}
