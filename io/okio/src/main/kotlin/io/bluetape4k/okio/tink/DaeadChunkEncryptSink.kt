package io.bluetape4k.okio.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.tink.daead.TinkDeterministicAead
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import java.io.IOException

/**
 * Encrypts plaintext into a DAEAD chunk stream and writes it to a delegate [Sink].
 *
 * Input is split into plaintext chunks of [chunkSize]. Each encrypted frame is
 * written as `[1-byte flags][8-byte big-endian ciphertext length][ciphertext]`.
 * The last partial chunk is finalized on [close], so callers must use `use {}`
 * or close this sink explicitly. The chunk index and final-frame flag are bound
 * into DAEAD associated data, so chunk reorder, deletion, and duplicate replay
 * fail during decryption.
 *
 * [TinkDeterministicAead] produces the same ciphertext for the same key,
 * plaintext, and associated data. This can reveal repeated plaintext chunks.
 * Use `TinkEncryptSink` when deterministic encryption is not required.
 *
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val output = Buffer()
 *
 * output.asDaeadChunkEncryptSink(daead).use { sink ->
 *     val plaintext = Buffer().writeUtf8("large streaming plaintext")
 *     sink.write(plaintext, plaintext.size)
 * }
 * // output now contains DAEAD chunk frames.
 * // Decrypt with DaeadChunkDecryptSource to restore the plaintext.
 * ```
 *
 * @param delegate target [Sink] for ciphertext frames.
 * @param daead DAEAD wrapper used to encrypt each chunk.
 * @param chunkSize plaintext chunk size. Defaults to [DEFAULT_DAEAD_CHUNK_SIZE].
 * @param associatedData caller-provided associated data. It is authenticated but not encrypted.
 * @see DaeadChunkDecryptSource
 * @see TinkEncryptSink
 */
class DaeadChunkEncryptSink(
    delegate: Sink,
    private val daead: TinkDeterministicAead,
    private val chunkSize: Int = DEFAULT_DAEAD_CHUNK_SIZE,
    associatedData: ByteArray = ByteArray(0),
): ForwardingSink(delegate) {

    companion object: KLogging()

    private val associatedData: ByteArray = associatedData.copyOf()
    private val plainBuffer = Buffer()
    private var closed = false
    private var chunkIndex = 0L

    init {
        chunkSize.requirePositiveNumber("chunkSize")
    }

    /**
     * Reads [byteCount] bytes from [source], buffers them, and emits complete encrypted chunks.
     *
     * @param source plaintext buffer.
     * @param byteCount number of bytes to consume.
     * @throws IOException after close or when encryption/write fails.
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (closed) {
            throw IOException("closed")
        }
        byteCount.requireInRange(0L, source.size, "byteCount")
        if (byteCount == 0L) {
            return
        }

        plainBuffer.write(source, byteCount)
        emitCompleteChunks()
    }

    /**
     * Emits only chunks known to be non-final and keeps the last possible final chunk until [close].
     *
     * Note: `flush()` also keeps a buffered chunk of exactly [chunkSize] bytes because
     * it may be the final chunk unless more plaintext arrives.
     */
    override fun flush() {
        if (!closed) {
            emitCompleteChunks()
            super.flush()
        }
    }

    /**
     * Encrypts the remaining final chunk and closes the delegate [Sink].
     */
    override fun close() {
        if (closed) {
            return
        }

        var thrown: Throwable? = null
        try {
            emitFinalChunk()
            super.flush()
        } catch (e: Throwable) {
            thrown = e
        } finally {
            closed = true
            try {
                super.close()
            } catch (closeException: Throwable) {
                if (thrown == null) {
                    thrown = closeException
                } else {
                    thrown.addSuppressed(closeException)
                }
            }
            try {
                plainBuffer.close()
            } catch (bufferException: Throwable) {
                if (thrown == null) {
                    thrown = bufferException
                } else {
                    thrown.addSuppressed(bufferException)
                }
            }
        }

        if (thrown == null) {
            return
        }
        throw thrown
    }

    private fun emitCompleteChunks() {
        while (plainBuffer.size > chunkSize) {
            emitChunk(chunkSize.toLong(), finalChunk = false)
        }
    }

    private fun emitFinalChunk() {
        emitChunk(plainBuffer.size, finalChunk = true)
    }

    private fun emitChunk(byteCount: Long, finalChunk: Boolean) {
        val plaintext = plainBuffer.readByteArray(byteCount)
        // Bind stream position into AD so whole-frame reorder/drop/duplicate attacks fail authentication.
        val ciphertext = daead.encryptDeterministically(
            plaintext,
            daeadChunkAssociatedData(associatedData, chunkIndex, finalChunk)
        )
        val frame = Buffer()
            .writeByte(if (finalChunk) DAEAD_CHUNK_FINAL_FLAG else DAEAD_CHUNK_NON_FINAL_FLAG)
            .writeLong(ciphertext.size.toLong())
            .write(ciphertext)

        super.write(frame, frame.size)
        chunkIndex++
    }
}

/**
 * Wraps this [Sink] as a DAEAD chunk encryption sink.
 *
 * ```kotlin
 * val daead = TinkDaeads.AES256_SIV
 * val output = Buffer()
 *
 * output.asDaeadChunkEncryptSink(daead, associatedData = "context".toByteArray()).use { sink ->
 *     sink.write(Buffer().writeUtf8("hello"), 5L)
 * }
 * // Decrypt output with DaeadChunkDecryptSource and the same associatedData.
 * ```
 *
 * @param daead DAEAD wrapper used to encrypt each chunk.
 * @param chunkSize plaintext chunk size. Defaults to [DEFAULT_DAEAD_CHUNK_SIZE] (64 KiB).
 * @param associatedData caller-provided associated data. Decryption must use the same value.
 * @return DAEAD chunk encryption [Sink].
 * @see DaeadChunkDecryptSource
 */
fun Sink.asDaeadChunkEncryptSink(
    daead: TinkDeterministicAead,
    chunkSize: Int = DEFAULT_DAEAD_CHUNK_SIZE,
    associatedData: ByteArray = ByteArray(0),
): DaeadChunkEncryptSink =
    DaeadChunkEncryptSink(this, daead, chunkSize, associatedData)

/**
 * Default plaintext chunk size for DAEAD chunk encryption (64 KiB).
 *
 * Override this with the [chunkSize] parameter when constructing the encryption sink.
 * [DaeadChunkDecryptSource] reads ciphertext lengths from frame headers, so the
 * decrypting side does not need the plaintext chunk size.
 */
const val DEFAULT_DAEAD_CHUNK_SIZE: Int = 64 * 1024

internal const val DAEAD_CHUNK_NON_FINAL_FLAG: Int = 0
internal const val DAEAD_CHUNK_FINAL_FLAG: Int = 1

private const val DAEAD_CHUNK_ASSOCIATED_DATA_DOMAIN = "bluetape4k-okio-daead-chunk-v2"

// Domain separation + caller AD + frame metadata form the authenticated stream context.
internal fun daeadChunkAssociatedData(
    associatedData: ByteArray,
    chunkIndex: Long,
    finalChunk: Boolean,
): ByteArray =
    Buffer()
        .writeUtf8(DAEAD_CHUNK_ASSOCIATED_DATA_DOMAIN)
        .writeByte(0)
        .writeInt(associatedData.size)
        .write(associatedData)
        .writeLong(chunkIndex)
        .writeByte(if (finalChunk) DAEAD_CHUNK_FINAL_FLAG else DAEAD_CHUNK_NON_FINAL_FLAG)
        .readByteArray()
