package io.bluetape4k.okio

import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [BufferSupport.kt]의 `bufferOf`, `readUnsafeAndClose`, `readAndWriteUnsafeAndClose`,
 * `asBufferedSource`, `asBufferedSink` 함수들을 검증합니다.
 */
class BufferSupportTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `bufferOf는 UTF-8 문자열을 담은 Buffer를 생성한다`() {
        val buffer = bufferOf("hello")
        buffer.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `bufferOf vararg strings는 여러 문자열을 순서대로 담은 Buffer를 생성한다`() {
        val buffer = bufferOf("hello", " ", "world")
        buffer.readUtf8() shouldBeEqualTo "hello world"
    }

    @Test
    fun `bufferOf Iterable는 문자열 목록을 순서대로 담은 Buffer를 생성한다`() {
        val buffer = bufferOf(listOf("line1\n", "line2\n"))
        buffer.readUtf8() shouldBeEqualTo "line1\nline2\n"
    }

    @Test
    fun `bufferOf ByteArray는 바이트 배열을 담은 Buffer를 생성한다`() {
        val bytes = byteArrayOf(1, 2, 3)
        val buffer = bufferOf(bytes)
        buffer.size shouldBeEqualTo 3L
    }

    @Test
    fun `bufferOf vararg bytes는 바이트를 담은 Buffer를 생성한다`() {
        val buffer = bufferOf(0x48.toByte(), 0x69.toByte())
        buffer.size shouldBeEqualTo 2L
        buffer.readUtf8() shouldBeEqualTo "Hi"
    }

    @Test
    fun `bufferOf InputStream은 InputStream을 읽어 Buffer를 생성한다`() {
        val bytes = "hello".toByteArray()
        val buffer = bufferOf(bytes.inputStream())
        buffer.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `bufferOf InputStream byteCount는 지정된 바이트 수만 읽어 Buffer를 생성한다`() {
        val bytes = "hello world".toByteArray()
        val buffer = bufferOf(bytes.inputStream(), 5L)
        buffer.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `bufferOf ByteString은 ByteString을 담은 Buffer를 생성한다`() {
        val byteString = "hi".encodeUtf8()
        val buffer = bufferOf(byteString)
        buffer.size shouldBeEqualTo 2L
        buffer.readUtf8() shouldBeEqualTo "hi"
    }

    @Test
    fun `bufferOf source Buffer는 부분 복사를 지원한다`() {
        val source = bufferOf("hello world")
        val copy = bufferOf(source, offset = 6L, size = 5L)
        copy.readUtf8() shouldBeEqualTo "world"
    }

    @Test
    fun `bufferOf Source는 Source의 모든 내용을 읽어 Buffer를 생성한다`() {
        val original = bufferOf("hello")
        val buffer = bufferOf(original as okio.Source)
        buffer.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `bufferOf Source byteCount는 지정된 바이트 수만 읽는다`() {
        val original = bufferOf("hello world")
        val buffer = bufferOf(original as okio.Source, byteCount = 5L)
        buffer.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `readUnsafeAndClose는 Buffer 내용을 안전하게 읽는다`() {
        val buffer = bufferOf("hello")
        val result = buffer.readUnsafeAndClose { cursor ->
            cursor.seek(0)
            cursor.end - cursor.start
        }
        result shouldBeEqualTo 5
    }

    @Test
    fun `readAndWriteUnsafeAndClose는 Buffer에 읽기 쓰기 작업을 수행한다`() {
        val buffer = Buffer()
        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.expandBuffer(5)
            cursor.data?.set(cursor.start, 'H'.code.toByte())
            cursor.resizeBuffer(1)
        }
        buffer.size shouldBeEqualTo 1L
    }

    @Test
    fun `asBufferedSource는 Buffer를 BufferedSource로 변환한다`() {
        val buffer = bufferOf("hello")
        val source = buffer.asBufferedSource()
        source.shouldNotBeNull()
        source.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `asBufferedSink는 Buffer를 BufferedSink로 변환한다`() {
        val buffer = Buffer()
        val sink = buffer.asBufferedSink()
        sink.shouldNotBeNull()
        sink.writeUtf8("world")
        sink.flush()
        buffer.readUtf8() shouldBeEqualTo "world"
    }
}
