package io.bluetape4k.io

import io.bluetape4k.support.assertPositiveNumber
import io.bluetape4k.support.assertZeroOrPositiveNumber
import io.bluetape4k.support.toUtf8String
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import kotlin.io.copyTo
import kotlin.text.Charsets.UTF_8

/**
 * 빈 [InputStream] 인스턴스
 */
@JvmField
val emptyInputStream = ByteArrayInputStream(ByteArray(0))

/**
 * 빈 [OutputStream] 인스턴스
 */
@JvmField
val emptyOutputStream = ByteArrayOutputStream(0)

const val DEFAULT_BUFFER_SIZE = 8192
const val DEFAULT_BLOCK_SIZE = 4096
const val MINIMAL_BLOCK_SIZE = 512

/**
 * [InputStream]을 읽어 [Writer]에 씁니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val writer = StringWriter()
 * inputStream.copyTo(writer)
 * println(writer.toString()) // Hello, World!
 * ```
 *
 * @param out         데이터를 쓸 대상 [Writer]
 * @param cs          Charset
 * @param bufferSize  buffer size
 * @return 복사한 데이터의 Byte 크기
 */
fun InputStream.copyTo(out: Writer, cs: Charset = UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long =
    this.reader(cs).buffered().copyTo(out, bufferSize)

/**
 * [InputStream]을 읽어 [OutputStream]에 씁니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val outputStream = ByteArrayOutputStream()
 * inputStream.copyTo(outputStream)
 * println(outputStream.toString()) // Hello, World!
 * ```
 *
 * @param out         데이터를 쓸 대상 [OutputStream]
 * @param bufferSize  buffer size
 * @return 복사한 데이터의 Byte 크기
 */
fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    bufferSize.assertPositiveNumber("bufferSize")

    var readBytes = 0L
    val buffer = ByteArray(bufferSize)
    do {
        val readCount = this.read(buffer)
        if (readCount > 0) {
            out.write(buffer, 0, readCount)
            readBytes += readCount
        }
    } while (readCount > 0)

    return readBytes
}

/**
 * [ReadableByteChannel]을 읽어 [WritableByteChannel]에 씁니다.
 *
 * ```
 * val channel = FileChannel.open(Paths.get("hello.txt"), StandardOpenOption.READ)
 * val outputChannel = FileChannel.open(Paths.get("world.txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE)
 * channel.copyTo(outputChannel)
 * ```
 *
 * @receiver ReadableByteChannel 읽어들일 대상
 * @param bufferSize buffer size
 * @return 복사한 데이터의 Byte 크기
 */
fun ReadableByteChannel.copyTo(out: WritableByteChannel, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    bufferSize.assertPositiveNumber("bufferSize")

    var readBytes = 0L
    val buffer = ByteBuffer.allocateDirect(bufferSize)

    while (this.read(buffer) > 0) {
        buffer.flip()
        readBytes += out.write(buffer)
        buffer.compact()
    }
    return readBytes
}

/**
 * [Reader]를 읽어 [OutputStream]에 씁니다.
 *
 * ```
 * val reader = StringReader("Hello, World!")
 * val outputStream = ByteArrayOutputStream()
 * reader.copyTo(outputStream)
 * println(outputStream.toString()) // Hello, World!
 * ```
 *
 * @param out         데이터를 쓸 대상 [Writer]
 * @param bufferSize  buffer size
 * @return 복사한 데이터의 Byte 크기
 */
fun Reader.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE, cs: Charset = UTF_8): Long {
    bufferSize.assertPositiveNumber("bufferSize")

    OutputStreamWriter(out, cs).buffered().use { writer ->
        val count = copyTo(writer, bufferSize)
        out.flush()
        return count
    }
}

/**
 * [Reader]를 읽어 문자열로 반환합니다.
 *
 * ```
 * val reader = StringReader("Hello, World!")
 * val text = reader.copyToString()
 * println(text) // Hello, World!
 * ```
 *
 * @param bufferSize  buffer size (see [DEFAULT_BUFFER_SIZE])
 * @return 읽어들인 문자열
 */
fun Reader.copyToString(bufferSize: Int = DEFAULT_BUFFER_SIZE): String {
    return StringWriter(bufferSize).use { writer ->
        this.copyTo(writer)
        writer.toString()
    }
}

/**
 * [ByteArray]를 읽어들이는 [InputStream]을 빌드합니다.
 *
 * ```
 * val byteArray = "Hello, World!".toByteArray()
 * val inputStream = byteArray.toInputStream()
 * val text = inputStream.toString()
 * println(text) // Hello, World!
 * ```
 */
fun ByteArray.toInputStream(): InputStream = ByteArrayInputStream(this).buffered()

/**
 * [String]를 읽어들이는 [InputStream]을 빌드합니다.
 *
 * ```
 * val text = "Hello, World!"
 * val inputStream = text.toInputStream()
 * val text = inputStream.toString()
 * println(text) // Hello, World!
 * ```
 */
fun String.toInputStream(cs: Charset = UTF_8): InputStream = toByteArray(cs).toInputStream()

/**
 * [InputStream]를 읽어 [ByteArrayOutputStream]에 씁니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val outputStream = inputStream.toOutputStream()
 * println(outputStream.toString()) // Hello, World!
 * ```
 */
fun InputStream.toOutputStream(blockSize: Int = DEFAULT_BLOCK_SIZE): ByteArrayOutputStream =
    ByteArrayOutputStream(DEFAULT_BUFFER_SIZE).apply {
        this@toOutputStream.copyTo(this, blockSize)
    }

/**
 * [ByteArray]를 읽어 [ByteArrayOutputStream]에 씁니다.
 *
 * ```
 * val byteArray = "Hello, World!".toByteArray()
 * val outputStream = byteArray.toOutputStream()
 * println(outputStream.toString()) // Hello, World!
 * ```
 */
fun ByteArray.toOutputStream(blockSize: Int = DEFAULT_BLOCK_SIZE): ByteArrayOutputStream =
    toInputStream().toOutputStream(blockSize)

/**
 * [String]를 읽어 [ByteArrayOutputStream]에 씁니다.
 *
 * ```
 * val text = "Hello, World!"
 * val outputStream = text.toOutputStream()
 * println(outputStream.toString()) // Hello, World!
 * ```
 */
fun String.toOutputStream(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): ByteArrayOutputStream =
    toByteArray(cs).toOutputStream(blockSize)

/**
 * [InputStream]의 available한 부분을 읽어 [ByteArray]로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val bytes = inputStream.availableBytes()
 * println(bytes.toString()) // Hello, World!
 * ```
 */
fun InputStream.availableBytes(): ByteArray = ByteArray(available()).also { read(it) }

/**
 * [InputStream]을 읽어 [ByteArray]로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val bytes = inputStream.toByteArray()
 * println(bytes.toString()) // Hello, World!
 * ```
 */
fun InputStream.toByteArray(blockSize: Int = DEFAULT_BLOCK_SIZE): ByteArray =
    toOutputStream(blockSize).use { it.toByteArray() }

/**
 * [InputStream]을 읽어 [CharArray]로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val chars = inputStream.toCharArray()
 * println(chars.toString()) // Hello, World!
 * ```
 */
fun InputStream.toCharArray(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): CharArray =
    reader(cs).buffered(blockSize).use { it.readText().toCharArray() }

/**
 * [InputStream]을 읽어 [ByteBuffer]로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val buffer = inputStream.toByteBuffer()
 * println(buffer.toString()) // Hello, World!
 * ```
 */
fun InputStream.toByteBuffer(blockSize: Int = DEFAULT_BLOCK_SIZE): ByteBuffer =
    ByteBuffer.wrap(this.toByteArray(blockSize))

/**
 * [InputStream]을 읽어 문자열로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val text = inputStream.toString()
 * println(text) // Hello, World!
 * ```
 * @param cs Charset (see [UTF_8])
 */
fun InputStream.toString(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): String =
    toByteArray(blockSize).toString(cs)

/**
 * [InputStream]을 읽어 UTF-8 문자열로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val text = inputStream.toUtf8String()
 * println(text) // Hello, World!
 * ```
 * @param blockSize block size
 */
fun InputStream.toUtf8String(blockSize: Int = DEFAULT_BLOCK_SIZE): String = toByteArray(blockSize).toUtf8String()

/**
 * [InputStream]을 라인 단위로 읽어 문자열 컬렉션으로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello\nWorld!".toByteArray())
 * val lines = inputStream.toStringList()
 * println(lines) // [Hello, World!]
 * ```
 */
fun InputStream.toStringList(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): List<String> =
    reader(cs)
        .buffered(blockSize)
        .useLines {
            it.toList()
        }

/**
 * [InputStream]을 라인 단위로 읽어 UTF-8 문자열 컬렉션으로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello\nWorld!".toByteArray())
 * val lines = inputStream.toUtf8StringList()
 * println(lines) // [Hello, World!]
 * ```
 */
fun InputStream.toUtf8StringList(blockSize: Int = DEFAULT_BLOCK_SIZE): List<String> =
    reader(UTF_8)
        .buffered(blockSize)
        .useLines {
            it.toList()
        }

/**
 * [InputStream]을 라인 단위로 읽어 문자열 시퀀스로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello\nWorld!".toByteArray())
 * val lines = inputStream.toLineSequence()
 * println(lines.toList()) // [Hello, World!]
 * ```
 */
fun InputStream.toLineSequence(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): Sequence<String> =
    sequence {
        reader(cs)
            .buffered(blockSize)
            .useLines { lines ->
                for (line in lines) {
                    yield(line)
                }
            }
    }

/**
 * [InputStream]을 라인 단위로 읽어 UTF-8 문자열의 시퀀스로 반환합니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello\nWorld!".toByteArray())
 * val lines = inputStream.toUtf8LineSequence()
 * println(lines.toList()) // [Hello, World!]
 * ```
 */
fun InputStream.toUtf8LineSequence(blockSize: Int = DEFAULT_BLOCK_SIZE): Sequence<String> =
    sequence {
        reader(UTF_8)
            .buffered(blockSize)
            .useLines { lines ->
                for (line in lines) {
                    yield(line)
                }
            }
    }

/**
 * [ByteArray]를 라인 단위로 읽어 문자열 컬렉션으로 변홥합니다.
 *
 * ```
 * val byteArray = "Hello\nWorld!".toByteArray()
 * val lines = byteArray.toStringList()
 * println(lines) // [Hello, World!]
 * ```
 */
fun ByteArray.toStringList(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): List<String> =
    toInputStream().toStringList(cs, blockSize)

/**
 * [ByteArray]를 라인 단위로 읽어 UTF-8 문자열 컬렉션으로 변홥합니다.
 *
 * ```
 * val byteArray = "Hello\nWorld!".toByteArray()
 * val lines = byteArray.toUtf8StringList()
 * println(lines) // [Hello, World!]
 * ```
 */
fun ByteArray.toUtf8StringList(blockSize: Int = DEFAULT_BLOCK_SIZE): List<String> =
    toInputStream().toUtf8StringList(blockSize)

/**
 * [ByteArray]를 라인 단위로 읽어 문자열 시퀀스로 변환합니다.
 *
 * ```
 * val byteArray = "Hello\nWorld!".toByteArray()
 * val lines = byteArray.toLineSequence()
 * println(lines.toList()) // [Hello, World!]
 * ```
 */
fun ByteArray.toLineSequence(cs: Charset = UTF_8, blockSize: Int = DEFAULT_BLOCK_SIZE): Sequence<String> =
    toInputStream().toLineSequence(cs, blockSize)

/**
 * [ByteArray]를 라인 단위로 읽어 UTF-8 문자열 시퀀스로 변환합니다.
 *
 * ```
 * val byteArray = "Hello\nWorld!".toByteArray()
 * val lines = byteArray.toUtf8LineSequence()
 * println(lines.toList()) // [Hello, World!]
 * ```
 */
fun ByteArray.toUtf8LineSequence(blockSize: Int = DEFAULT_BLOCK_SIZE): Sequence<String> =
    toInputStream().toUtf8LineSequence(blockSize)

/**
 * [InputStream]을 읽어 [dst]에 씁니다.
 *
 * ```
 * val inputStream = ByteArrayInputStream("Hello, World!".toByteArray())
 * val buffer = ByteBuffer.allocate(1024)
 * val readCount = inputStream.putTo(buffer)
 * println(buffer.toString()) // Hello, World!
 * ```
 *
 * @receiver 읽어들일 대상 [InputStream]
 * @param dst   쓸 대상 [ByteBuffer]
 * @param limit 쓸 데이터의 크기
 * @return 쓴 데이터의 크기
 */
fun InputStream.putTo(dst: ByteBuffer, limit: Int = dst.remaining()): Int {
    limit.assertZeroOrPositiveNumber("limit")

    val size = minOf(limit, dst.remaining())
    if (size == 0) {
        return 0
    }

    return if (dst.hasArray()) {
        val readCount = read(dst.array(), dst.arrayOffset() + dst.position(), size)
        if (readCount > 0) {
            dst.position(dst.position() + readCount)
        }
        readCount
    } else {
        val array = ByteArray(size)
        val readCount = read(array, 0, size)
        if (readCount > 0) {
            dst.put(array, 0, readCount)
        }
        readCount
    }
}
