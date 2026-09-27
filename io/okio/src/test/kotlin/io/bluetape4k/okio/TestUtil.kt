package io.bluetape4k.okio

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.ByteString
import kotlin.random.Random

object TestUtil: KLogging() {

    const val SEGMENT_POOL_MAX_SIZE = 64 * 1024
    const val SEGMENT_SIZE = 8192

    const val REPLACEMENT_BYTE: Byte = '?'.code.toByte()
    const val REPLACEMENT_CHARACTER: Char = '\ufffd'
    const val REPLACEMENT_CODE_POINT: Int = REPLACEMENT_CHARACTER.code


    fun assertByteArraysEquals(a: ByteArray, b: ByteArray) {
        a.contentToString() shouldBeEqualTo b.contentToString()
    }

    fun randomBytes(length: Int): ByteString {
        val randomBytes = ByteArray(length)
        Random.nextBytes(randomBytes)
        return byteStringOf(*randomBytes)
    }

    /**
     * Returns a new buffer containing the contents of [strs], attempting to isolate each
     * string to its own segment in the returned buffer. This clones buffers so that segments are
     * shared, preventing compaction from occurring.
     */
    fun bufferWithSegments(vararg strs: String): Buffer {
        val result = Buffer()
        for (str in strs) {
            val offsetInSegment = if (str.length < SEGMENT_SIZE) (SEGMENT_SIZE - str.length) / 2 else 0
            val buffer = Buffer().apply {
                writeUtf8("_".repeat(offsetInSegment))
                writeUtf8(str)
                skip(offsetInSegment.toLong())
            }
            log.debug { "Buffer with segments. buffer=$buffer, buffer.size=${buffer.size}" }
            result.write(buffer.clone(), buffer.size)
        }
        return result
    }

    fun bufferWithRandomSegmentLayout(data: ByteArray): Buffer {
        val result = Buffer()

        // Writing to result directly will yield packed segments. Instead, write to
        // other buffers, then write those buffers to result.
        var pos = 0
        var byteCount: Int
        while (pos < data.size) {
            byteCount = SEGMENT_SIZE / 2 + Random.nextInt(SEGMENT_SIZE / 2)
            if (byteCount > data.size - pos) byteCount = data.size - pos
            val offset = Random.nextInt(SEGMENT_SIZE - byteCount)

            val buffer = Buffer().apply {
                write(ByteArray(offset))
                write(data, pos, byteCount)
                skip(offset.toLong())
            }

            log.debug { "Buffer with random segment layout. buffer=$buffer" }
            result.write(buffer, byteCount.toLong())
            pos += byteCount
        }
        return result
    }
}
