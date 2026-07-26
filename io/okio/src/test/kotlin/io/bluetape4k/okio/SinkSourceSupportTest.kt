package io.bluetape4k.okio

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.junit.jupiter.api.Test

/**
 * [SinkSupport.kt]와 [SourceSupport.kt]의 `buffered` 확장 함수를 검증합니다.
 */
class SinkSourceSupportTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `Sink buffered는 Sink를 BufferedSink로 변환한다`() {
        val output = Buffer()
        val sink = (output as okio.Sink).buffered()
        sink.shouldNotBeNull()
        sink.writeUtf8("hello")
        sink.flush()
        output.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `Source buffered는 Source를 BufferedSource로 변환한다`() {
        val buffer = bufferOf("hello")
        val source = (buffer as okio.Source).buffered()
        source.shouldNotBeNull()
        source.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `Sink buffered는 데이터를 정상적으로 기록한다`() {
        val baos = java.io.ByteArrayOutputStream()
        val sink = baos.asSink().buffered()
        sink.writeUtf8("world")
        sink.flush()
        sink.close()
        baos.toString(Charsets.UTF_8) shouldBeEqualTo "world"
    }

    @Test
    fun `Source buffered는 InputStream Source를 통해 읽기를 지원한다`() {
        val bytes = "hello source".toByteArray()
        val source = bytes.inputStream().asSource().buffered()
        val text = source.readUtf8()
        source.close()
        text shouldBeEqualTo "hello source"
    }
}
