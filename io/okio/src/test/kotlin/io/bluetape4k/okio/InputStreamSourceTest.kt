package io.bluetape4k.okio

import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test

/**
 * [InputStreamSource] 및 [InputStream.asSource] 확장 함수를 검증합니다.
 */
class InputStreamSourceTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `asSource는 InputStream을 Source로 변환한다`() {
        val bytes = "hello world".toByteArray()
        val source = bytes.inputStream().asSource()
        val sink = Buffer()
        val read = source.read(sink, bytes.size.toLong())
        source.close()
        read shouldBeEqualTo bytes.size.toLong()
        sink.readUtf8() shouldBeEqualTo "hello world"
    }

    @Test
    fun `read는 0 byteCount 요청 시 0을 반환한다`() {
        val source = "data".toByteArray().inputStream().asSource()
        val sink = Buffer()
        val read = source.read(sink, 0L)
        source.close()
        read shouldBeEqualTo 0L
    }

    @Test
    fun `read는 EOF에서 -1을 반환한다`() {
        val source = ByteArray(0).inputStream().asSource()
        val sink = Buffer()
        val read = source.read(sink, 10L)
        source.close()
        read shouldBeEqualTo -1L
    }

    @Test
    fun `readAll은 InputStream의 모든 내용을 읽는다`() {
        val text = "hello okio"
        val inputStreamSource = InputStreamSource(text.toByteArray().inputStream())
        val sink = Buffer()
        val total = inputStreamSource.readAll(sink)
        inputStreamSource.close()
        total shouldBeGreaterThan 0L
        sink.readUtf8() shouldBeEqualTo text
    }

    @Test
    fun `timeout은 설정된 Timeout을 반환한다`() {
        val source = "test".toByteArray().inputStream().asSource()
        val timeout = source.timeout()
        source.close()
        timeout shouldBeEqualTo okio.Timeout.NONE
    }

    @Test
    fun `close는 예외 없이 InputStream을 닫는다`() {
        val source = "test".toByteArray().inputStream().asSource()
        source.close()
<<<<<<< feat/coverage-improvement
        source.close()
=======
>>>>>>> develop
        // close 후에 다시 close 해도 예외가 없어야 함
    }

    @Test
    fun `큰 데이터를 분할 읽기한다`() {
        val size = 8192
        val bytes = ByteArray(size) { it.toByte() }
        val source = bytes.inputStream().asSource()
        val sink = Buffer()
        var total = 0L
        while (true) {
            val read = source.read(sink, 1024L)
            if (read < 0) break
            total += read
        }
        source.close()
        total shouldBeEqualTo size.toLong()
    }
}
