package io.bluetape4k.okio

import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * [OutputStreamSink] 및 [OutputStream.asSink] 확장 함수를 검증합니다.
 */
class OutputStreamSinkTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `asSink는 OutputStream을 Sink로 변환한다`() {
        val baos = java.io.ByteArrayOutputStream()
        val sink = baos.asSink()
        val source = bufferOf("hello world")
        sink.write(source, source.size)
        sink.flush()
        sink.close()
        baos.toString(Charsets.UTF_8) shouldBeEqualTo "hello world"
    }

    @Test
    fun `write는 지정된 byteCount만큼 데이터를 기록한다`() {
        val baos = java.io.ByteArrayOutputStream()
        val sink = OutputStreamSink(baos)
        val source = bufferOf("hello world")
        sink.write(source, 5L)
        sink.flush()
        baos.toString(Charsets.UTF_8) shouldBeEqualTo "hello"
    }

    @Test
    fun `flush는 버퍼 데이터를 OutputStream에 반영한다`() {
        val baos = java.io.ByteArrayOutputStream()
        val sink = OutputStreamSink(baos)
        val source = bufferOf("test")
        sink.write(source, source.size)
        sink.flush()
        baos.size() shouldBeEqualTo 4
    }

    @Test
    fun `timeout은 설정된 Timeout을 반환한다`() {
        val baos = java.io.ByteArrayOutputStream()
        val sink = baos.asSink()
        val timeout = sink.timeout()
        sink.close()
        timeout shouldBeEqualTo okio.Timeout.NONE
    }

    @Test
    fun `toString은 OutputStreamSink 설명 문자열을 반환한다`() {
        val baos = java.io.ByteArrayOutputStream()
        val sink = OutputStreamSink(baos)
        val str = sink.toString()
        sink.close()
        str shouldBeEqualTo "OutputStreamSink($baos)"
    }

    @Test
    fun `큰 데이터를 분할해서 기록한다`() {
        val size = 8192
        val data = ByteArray(size) { it.toByte() }
        val baos = java.io.ByteArrayOutputStream()
        val sink = OutputStreamSink(baos)
        val source = Buffer().write(data)
        sink.write(source, data.size.toLong())
        sink.flush()
        sink.close()
        baos.toByteArray() shouldBeEqualTo data
    }
}
