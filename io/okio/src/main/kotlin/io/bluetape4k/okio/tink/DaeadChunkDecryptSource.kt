package io.bluetape4k.okio.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.bluetape4k.tink.daead.TinkDeterministicAead
import okio.Buffer
import okio.EOFException
import okio.ForwardingSource
import okio.Source
import java.io.IOException

/**
 * Decrypts a DAEAD chunk stream and exposes plaintext through [Source].
 *
 * Input must be a sequence of `[1-byte flags][8-byte big-endian ciphertext length][ciphertext]`
 * frames. [read] decrypts at most the next encrypted chunk when the internal
 * plaintext buffer is empty, so it never loads the whole ciphertext stream into memory.
 * The chunk index and final-frame flag are bound into DAEAD associated data, so
 * chunk reorder, deletion, and duplicate replay fail with authentication or EOF errors.
 *
 * [TinkDeterministicAead] produces the same ciphertext for the same key,
 * plaintext, and associated data. This can reveal repeated plaintext chunks.
 * [associatedData] is authenticated but not encrypted and must match the value
 * used during encryption.
 *
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val output = Buffer()
 *
 * // Encrypt
 * output.asDaeadChunkEncryptSink(daead).use { sink ->
 *     sink.write(Buffer().writeUtf8("streaming plaintext"), 19L)
 * }
 *
 * // Decrypt — only one chunk is loaded at a time.
 * val decrypted = Buffer()
 * output.asDaeadChunkDecryptSource(daead).use { source ->
 *     source.read(decrypted, Long.MAX_VALUE)
 * }
 * val plaintext = decrypted.readUtf8() // "streaming plaintext"
 * ```
 *
 * @param delegate source of DAEAD chunk ciphertext frames.
 * @param daead DAEAD wrapper used to decrypt each chunk.
 * @param associatedData caller-provided associated data. It must match the encryption value.
 * @param maxCiphertextLength maximum allowed ciphertext length per frame.
 *   Defaults to [DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH] to limit memory allocation from untrusted sources.
 * @see DaeadChunkEncryptSink
 * @see TinkDecryptSource
 */
class DaeadChunkDecryptSource(
    delegate: Source,
    private val daead: TinkDeterministicAead,
    associatedData: ByteArray = ByteArray(0),
    private val maxCiphertextLength: Long = DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH,
): ForwardingSource(delegate) {

    companion object: KLogging() {
        private const val CHUNK_HEADER_SIZE = 1L + Long.SIZE_BYTES
        private const val TRAILING_PROBE_BYTE_COUNT = 64L
        private const val MAX_NO_PROGRESS_READS = 8
    }

    private val associatedData: ByteArray = associatedData.copyOf()
    private val plainBuffer = Buffer()
    private var closed = false
    private var finished = false
    private var chunkIndex = 0L

    /**
     * Reads up to [byteCount] decrypted plaintext bytes into [sink].
     *
     * When the internal plaintext buffer is empty, this source reads and decrypts
     * the next encrypted chunk. A single call returns at most one chunk of plaintext.
     *
     * @param sink target plaintext buffer.
     * @param byteCount maximum requested byte count.
     * @return number of bytes read, or `-1` after the authenticated final frame.
     * @throws IOException after close or when the stream format/authentication is invalid.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        if (closed) {
            throw IOException("closed")
        }
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) {
            return 0L
        }

        while (plainBuffer.size == 0L && !finished) {
            readNextChunk()
        }
        if (plainBuffer.size == 0L && finished) {
            return -1L
        }

        val bytesToRead = minOf(byteCount, plainBuffer.size)
        sink.write(plainBuffer, bytesToRead)
        return bytesToRead
    }

    /**
     * Closes the internal plaintext buffer and delegate [Source].
     */
    override fun close() {
        if (closed) {
            return
        }
        try {
            super.close()
        } finally {
            closed = true
            plainBuffer.close()
        }
    }

    private fun readNextChunk() {
        val header = readExactly(CHUNK_HEADER_SIZE)
        val flags = header.readByte().toInt()
        val ciphertextLength = header.readLong()
        val finalChunk = when (flags) {
            DAEAD_CHUNK_NON_FINAL_FLAG -> false
            DAEAD_CHUNK_FINAL_FLAG -> true
            else                   -> throw IOException("Invalid DAEAD chunk flags: $flags")
        }

        if (ciphertextLength <= 0L || ciphertextLength > Int.MAX_VALUE) {
            throw IOException("Invalid DAEAD chunk ciphertext length: $ciphertextLength")
        }
        if (ciphertextLength > maxCiphertextLength) {
            throw IOException(
                "DAEAD chunk ciphertext length $ciphertextLength exceeds maxCiphertextLength $maxCiphertextLength"
            )
        }

        val ciphertext = readExactly(ciphertextLength)

        // The flag is part of AD; changing final/non-final state invalidates the frame.
        val plaintext = daead.decryptDeterministically(
            ciphertext.readByteArray(),
            daeadChunkAssociatedData(associatedData, chunkIndex, finalChunk)
        )
        chunkIndex++
        plainBuffer.write(plaintext)
        if (finalChunk) {
            ensureNoTrailingData()
            finished = true
        }
    }

    private fun ensureNoTrailingData() {
        val probe = Buffer()
        var noProgressCount = 0

        while (true) {
            val bytesRead = super.read(probe, TRAILING_PROBE_BYTE_COUNT)
            when {
                bytesRead < 0L -> return

                bytesRead == 0L -> {
                    noProgressCount++
                    if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                        throw IOException("Unable to verify DAEAD chunk stream EOF: no progress.")
                    }
                }

                else           -> {
                    // Final frame authentication is only complete when no trailing frame bytes remain.
                    throw IOException("Trailing DAEAD chunk data after final frame.")
                }
            }
        }
    }

    private fun readExactly(byteCount: Long): Buffer {
        val buffer = Buffer()
        var noProgressCount = 0

        while (buffer.size < byteCount) {
            val bytesRead = super.read(buffer, byteCount - buffer.size)
            when {
                bytesRead < 0L -> {
                    throw EOFException("Truncated DAEAD chunk. expected=$byteCount actual=${buffer.size}")
                }

                bytesRead == 0L -> {
                    noProgressCount++
                    if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                        throw IOException("Unable to read DAEAD chunk bytes from source: no progress.")
                    }
                }

                else           -> noProgressCount = 0
            }
        }

        return buffer
    }
}

/**
 * Wraps this [Source] as a DAEAD chunk decryption source.
 *
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val decrypted = Buffer()
 *
 * encryptedSource.asDaeadChunkDecryptSource(
 *     daead,
 *     associatedData = "context".toByteArray(),
 * ).use { source ->
 *     while (source.read(decrypted, DEFAULT_BUFFER_SIZE.toLong()) >= 0L) { /* drain */ }
 * }
 * ```
 *
 * @param daead DAEAD wrapper used to decrypt each chunk.
 * @param associatedData caller-provided associated data. It must match the encryption value.
 * @param maxCiphertextLength maximum allowed ciphertext length per frame.
 *   Defaults to [DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH].
 * @return DAEAD chunk decryption [Source].
 * @see DaeadChunkEncryptSink
 */
fun Source.asDaeadChunkDecryptSource(
    daead: TinkDeterministicAead,
    associatedData: ByteArray = ByteArray(0),
    maxCiphertextLength: Long = DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH,
): DaeadChunkDecryptSource =
    DaeadChunkDecryptSource(this, daead, associatedData, maxCiphertextLength)

/**
 * Default maximum ciphertext length accepted by DAEAD chunk decryption (16 MiB).
 *
 * This bounds memory allocation when reading from untrusted sources. If the
 * encryption side uses a larger plaintext chunk size, pass an explicit
 * `maxCiphertextLength` when creating [DaeadChunkDecryptSource].
 */
const val DEFAULT_DAEAD_MAX_CIPHERTEXT_LENGTH: Long = 16L * 1024 * 1024
