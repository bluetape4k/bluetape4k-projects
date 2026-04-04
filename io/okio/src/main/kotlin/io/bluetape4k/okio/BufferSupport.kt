package io.bluetape4k.okio

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.Sink
import okio.Source
import java.io.InputStream

/**
 * Buffer의 내용을 `cursor`를 통해 읽기 작업([block]) 수행합니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello")
 * val result = buffer.readUnsafeAndClose { cursor ->
 *     cursor.seek(0)
 *     cursor.end - cursor.start // 5
 * }
 * // result == 5
 * ```
 */
inline fun <T> Buffer.readUnsafeAndClose(
    cursor: Buffer.UnsafeCursor = Buffer.UnsafeCursor(),
    block: (cursor: Buffer.UnsafeCursor) -> T,
): T =
    readUnsafe(cursor).use { block(it) }

/**
 * Buffer의 내용을 `cursor`를 통해 읽기/쓰기 작업([block]) 수행합니다.
 *
 * ```kotlin
 * val buffer = Buffer()
 * buffer.readAndWriteUnsafeAndClose { cursor ->
 *     cursor.expandBuffer(5)
 *     cursor.data?.set(cursor.start, 'H'.code.toByte())
 *     cursor.resizeBuffer(1)
 * }
 * // buffer.size == 1
 * ```
 */
inline fun <T> Buffer.readAndWriteUnsafeAndClose(
    cursor: Buffer.UnsafeCursor = Buffer.UnsafeCursor(),
    block: (cursor: Buffer.UnsafeCursor) -> T,
): T =
    readAndWriteUnsafe(cursor).use { block(it) }

/**
 * [Buffer] 를 [BufferedSource] 로 변환합니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello")
 * val source: BufferedSource = buffer.asBufferedSource()
 * val text = source.readUtf8()
 * // text == "hello"
 * ```
 */
fun Buffer.asBufferedSource(): BufferedSource = (this as Source).buffered()

/**
 * [Buffer]를 [BufferedSink]로 변환합니다.
 *
 * ```kotlin
 * val buffer = Buffer()
 * val sink: BufferedSink = buffer.asBufferedSink()
 * sink.writeUtf8("world")
 * sink.flush()
 * val text = buffer.readUtf8()
 * // text == "world"
 * ```
 */
fun Buffer.asBufferedSink(): BufferedSink = (this as Sink).buffered()

/**
 * [text]를 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello")
 * val text = buffer.readUtf8()
 * // text == "hello"
 * ```
 *
 * @param text Buffer에 쓸 UTF-8 텍스트
 * @return [Buffer] 인스턴스
 */
fun bufferOf(text: String): Buffer = Buffer().writeUtf8(text)

/**
 * [texts]를 새로운 [Buffer]에 순서대로 쓴 후 반환합니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello", " ", "world")
 * val text = buffer.readUtf8()
 * // text == "hello world"
 * ```
 */
fun bufferOf(vararg texts: String): Buffer {
    return Buffer().apply {
        texts.forEach { writeUtf8(it) }
    }
}

/**
 * [lines]를 새로운 [Buffer]에 순서대로 쓴 후 반환합니다.
 *
 * ```kotlin
 * val buffer = bufferOf(listOf("line1\n", "line2\n"))
 * val text = buffer.readUtf8()
 * // text == "line1\nline2\n"
 * ```
 */
fun bufferOf(lines: Iterable<String>): Buffer {
    return Buffer().apply {
        lines.forEach { writeUtf8(it) }
    }
}

/**
 * [bytes]를 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val bytes = byteArrayOf(1, 2, 3)
 * val buffer = bufferOf(bytes)
 * val size = buffer.size
 * // size == 3L
 * ```
 *
 * @param bytes Buffer에 쓸 [ByteArray]
 * @return [Buffer] 인스턴스
 */
@JvmName("bufferOfByteArray")
fun bufferOf(bytes: ByteArray): Buffer = Buffer().write(bytes)

/**
 * [bytes]를 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val buffer = bufferOf(0x48.toByte(), 0x69.toByte())
 * val size = buffer.size
 * // size == 2L
 * ```
 */
@JvmName("bufferOfBytes")
fun bufferOf(vararg bytes: Byte): Buffer = Buffer().write(bytes)

/**
 * [input] InputStream의 모든 내용을 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val bytes = "hello".toByteArray()
 * val input = bytes.inputStream()
 * val buffer = bufferOf(input)
 * val text = buffer.readUtf8()
 * // text == "hello"
 * ```
 */
fun bufferOf(input: InputStream): Buffer =
    Buffer().readFrom(input)

/**
 * [input] InputStream에서 최대 [byteCount] 바이트를 읽어 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val bytes = "hello world".toByteArray()
 * val input = bytes.inputStream()
 * val buffer = bufferOf(input, 5L)
 * val text = buffer.readUtf8()
 * // text == "hello"
 * ```
 */
fun bufferOf(input: InputStream, byteCount: Long): Buffer =
    Buffer().readFrom(input, byteCount)

/**
 * [byteString]을 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val byteString = ByteString.encodeUtf8("hi")
 * val buffer = bufferOf(byteString)
 * val size = buffer.size
 * // size == 2L
 * ```
 *
 * @param byteString Buffer에 쓸 [ByteString]
 * @return [Buffer] 인스턴스
 */
fun bufferOf(byteString: ByteString): Buffer = Buffer().write(byteString)

/**
 * [source] 내용을 복사한 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val source = bufferOf("hello world")
 * val copy = bufferOf(source, offset = 6L, size = 5L)
 * val text = copy.readUtf8()
 * // text == "world"
 * ```
 */
fun bufferOf(source: Buffer, offset: Long = 0L, size: Long = source.size): Buffer {
    return Buffer().apply {
        source.copyTo(this, offset, size)
    }
}

/**
 * [source]의 모든 내용을 읽어 담은 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val original = bufferOf("hello")
 * val buffer = bufferOf(original as Source)
 * val text = buffer.readUtf8()
 * // text == "hello"
 * ```
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
 * [source]에서 최대 [byteCount] 바이트를 읽어 [Buffer]를 생성합니다.
 *
 * ```kotlin
 * val source = bufferOf("hello world")
 * val buffer = bufferOf(source as Source, byteCount = 5L)
 * val text = buffer.readUtf8()
 * // text == "hello"
 * ```
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
