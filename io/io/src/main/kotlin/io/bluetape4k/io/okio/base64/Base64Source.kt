package io.bluetape4k.io.okio.base64

import io.bluetape4k.logging.KLogging
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.Source

/**
 * Base64로 인코딩된 [Source]를 읽어 디코딩하여 전달하는 [Source] 구현체.
 *
 * @see Base64Sink
 */
class Base64Source(delegate: Source): AbstractBase64Source(delegate) {

    companion object: KLogging()

    override fun decodeBase64Bytes(encodedString: String): ByteString? {
        return encodedString.decodeBase64()
    }

}

//    : ForwardingSource(delegate) {
//
//    companion object: KLogging() {
//        const val MAX_REQUEST_LENGTH = 9223372036854775804L // 4 * (Long.MAX_VALUE / 4)
//        const val BASE64_BLOCK = 4 // 4바이트 블록을 읽어 3바이트 디코딩
//    }
//
//    private val sourceBuffer: Buffer = Buffer()
//    private val decodeBuffer: Buffer = Buffer()
//
//    override fun read(sink: Buffer, byteCount: Long): Long {
//        byteCount.requireLt(MAX_REQUEST_LENGTH, "byteCount")
//
//        // 요청한 바이트가 이미 버퍼에 있으면 바로 반환
//        if (decodeBuffer.size >= byteCount) {
//            sink.write(decodeBuffer, byteCount)   // decodedBuffer를 읽어 sink에 byteCount만큼 쓴다
//            return byteCount
//        }
//
//        var streamEnded = false
//        while (decodeBuffer.size < byteCount && !streamEnded) {
//            val bytesRead = super.read(sourceBuffer, byteCount)
//            if (bytesRead < 0) {
//                streamEnded = true
//            }
//
//            // 모든 가능한 block을 Base64 디코딩
//            val allFullBlocks = BASE64_BLOCK * (sourceBuffer.size / BASE64_BLOCK)
//            val decoded = sourceBuffer.readUtf8(allFullBlocks).decodeBase64()
//            log.debug { "decoded: $decoded" }
//            check(decoded != null) { "base64 decode failed." }
//
//            decodeBuffer.write(decoded)
//        }
//
//        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
//        val bytesToReturn = byteCount.coerceAtMost(decodeBuffer.size)
//        sink.write(decodeBuffer, bytesToReturn)
//
//        return if (streamEnded) -1 else bytesToReturn
//    }
//}
