package io.bluetape4k.io.okio

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.Sink
import okio.Source
import java.io.InputStream

/**
 * [Buffer]를 [BufferedSource]로 변환합니다.
 */
fun Buffer.asBufferedSource(): BufferedSource = (this as Source).buffered()

/**
 * [Buffer]를 [BufferedSink]로 변환합니다.
 */
fun Buffer.asBufferedSink(): BufferedSink = (this as Sink).buffered()

/**
 * [text]를 담은 [Buffer]를 생성합니다.
 *
 * @param text Buffer에 쓸 UTF-8 텍스트
 * @return [Buffer] 인스턴스
 */
fun bufferOf(text: String): Buffer = Buffer().writeUtf8(text)

/**
 * [texts]를 새로운 [Buffer]에 순서대로 쓴 후 반환합니다.
 */
fun bufferOf(vararg texts: String): Buffer {
    return Buffer().apply {
        texts.forEach { writeUtf8(it) }
    }
}

/**
 * [bytes]를 담은 [Buffer]를 생성합니다.
 *
 * @param bytes Buffer에 쓸 [ByteArray]
 * @return [Buffer] 인스턴스
 */
@JvmName("bufferOfByteArray")
fun bufferOf(bytes: ByteArray): Buffer = Buffer().write(bytes)

/**
 *  [bytes]를 담은 [Buffer]를 생성합니다.
 */
@JvmName("bufferOfBytes")
fun bufferOf(vararg bytes: Byte): Buffer = Buffer().write(bytes)

fun bufferOf(input: InputStream, byteCount: Long = input.available().toLong()): Buffer =
    Buffer().readFrom(input, byteCount)

/**
 * [byteString]을 담은 [Buffer]를 생성합니다.
 *
 * @param byteString Buffer에 쓸 [ByteString]
 * @return [Buffer] 인스턴스
 */
fun bufferOf(byteString: ByteString): Buffer = Buffer().write(byteString)

/**
 * [source] 내용을 복사한 [Buffer]를 생성합니다.
 */
fun bufferOf(source: Buffer, offset: Long = 0L, size: Long = source.size): Buffer {
    return Buffer().apply {
        source.clone().copyTo(this, offset, size)
    }
}

/**
 * [source] 내용을 복사한 [Buffer]를 생성합니다.
 *
 * @param source 복사할 [Source]
 * @return [Buffer] 인스턴스
 */
fun bufferOf(source: Source): Buffer {
    return Buffer().apply {
        writeAll(source)
    }
}

/**
 * [source]에서 최대 `byteCount` 바이트를 읽어 [Buffer]를 생성합니다.
 *
 * @param source 읽을 [Source]
 * @param byteCount 읽을 바이트 수
 * @return [Buffer] 인스턴스
 */
fun bufferOf(source: Source, byteCount: Long): Buffer {
    return Buffer().apply {
        write(source, byteCount)
    }
}
