package io.bluetape4k.io.okio.base64

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireGe
import okio.Buffer
import okio.ByteString
import okio.ForwardingSource
import okio.Source
import java.io.IOException

/**
 * 데이터를 Apache Commons의 Base64로 인코딩하여 [Source]에 쓰는 [Source] 구현체.
 * NOTE: Apache Commons의 Base64 인코딩은 okio의 Base64 인코딩과 다르다. (특히 한글의 경우)
 *
 * @see ApacheBase64Sink
 * @see ApacheBase64Source
 * @see OkioBase64Sink
 * @see OkioBase64Source
 */
abstract class AbstractBase64Source(delegate: Source): ForwardingSource(delegate) {

    companion object: KLogging() {
        const val BASE64_BLOCK = 4 // 4바이트 블록을 읽어 3바이트 디코딩
        const val MAX_NO_PROGRESS_READS = 8
    }

    private val sourceBuffer = okio.Buffer()
    private val decodedBuffer = okio.Buffer()

    /**
     * Base64로 인코딩된 문자열을 디코딩하여 바이트 배열로 변환합니다.
     *
     * @param encodedString Base64로 인코딩된 문자열
     * @return 디코딩된 바이트 배열
     */
    protected abstract fun decodeBase64Bytes(encodedString: String): ByteString?

    /**
     * Okio Base64에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireGe(0L, "byteCount")
        if (byteCount == 0L) return 0L

        // 요청한 바이트가 이미 버퍼에 있으면 바로 반환
        if (decodedBuffer.size >= byteCount) {
            sink.write(decodedBuffer, byteCount)  // decodedBuffer를 읽어 sink에 byteCount만큼 쓴다
            return byteCount
        }

        var streamEnded = false
        var noProgressCount = 0
        while (decodedBuffer.size < byteCount && !streamEnded) {
            val sourceSizeBefore = sourceBuffer.size
            val decodedSizeBefore = decodedBuffer.size

            val bytesRead = super.read(sourceBuffer, byteCount - decodedBuffer.size)
            if (bytesRead < 0) {
                streamEnded = true
            }

            // 모든 가능한 block을 Base64 디코딩
            val decodeLength = if (streamEnded) {
                sourceBuffer.size
            } else {
                BASE64_BLOCK * (sourceBuffer.size / BASE64_BLOCK)
            }
            if (decodeLength > 0) {
                val decoded = decodeBase64Bytes(sourceBuffer.readUtf8(decodeLength))
                check(decoded != null) { "base64 decode failed. decoded is null." }
                if (decoded.size > 0) {
                    log.debug { "decoded: $decoded" }
                }

                decodedBuffer.write(decoded)
            }

            val madeProgress = sourceBuffer.size != sourceSizeBefore || decodedBuffer.size != decodedSizeBefore
            if (!streamEnded && !madeProgress) {
                noProgressCount++
                if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to decode base64 source: no progress.")
                }
            } else {
                noProgressCount = 0
            }
        }

        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        val bytesToReturn = byteCount.coerceAtMost(decodedBuffer.size)
        sink.write(decodedBuffer, bytesToReturn)

        return when {
            bytesToReturn > 0 -> bytesToReturn
            streamEnded       -> -1L
            else              -> 0L
        }
    }
}
