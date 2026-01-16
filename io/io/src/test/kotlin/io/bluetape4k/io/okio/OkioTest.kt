package io.bluetape4k.io.okio

import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import okio.blackholeSink
import okio.sink
import okio.source
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@TempFolderTest
class OkioTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private lateinit var temp: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        temp = tempFolder
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read write file`() {
        val file = temp.createFile()
        val content = faker.lorem().paragraph()

        file.sink().buffered().use { sink ->
            sink.writeUtf8(content)
        }
        log.debug { "Write content to file: $file" }
        file.exists().shouldBeTrue()

        file.source().buffered().use { source ->
            source.readUtf8() shouldBeEqualTo content
        }
    }

    @Test
    fun `output stream 을 sink 로 사용하기`() {
        val content = "a" + "b".repeat(9998) + "c"
        val data = bufferOf(content)

        val out = ByteArrayOutputStream()
        val sink = out.sink()

        // sink 에 data 를 쓰되, 3개의 문자만 쓴다.
        sink.write(data, 3)
        out.toString(Charsets.UTF_8) shouldBeEqualTo "abb"

        // sink 에 data의 현재 위치부터 끝까지 쓴다.
        sink.write(data, data.size)
        out.toString(Charsets.UTF_8) shouldBeEqualTo content

        out.close()
    }

    @Test
    fun `input stream 을 source 로 사용하기`() {
        val content = "a" + "b".repeat(TestUtil.SEGMENT_SIZE * 2) + "c"
        val inputStream = ByteArrayInputStream(content.toUtf8Bytes())

        val source = inputStream.source()
        val sink = Buffer()

        // Source로 부터 3개의 문자를 읽어서 sink 에 저장한다
        // Source: ab....bc. Sink: abb.
        source.read(sink, 3) shouldBeEqualTo 3L
        sink.readUtf8() shouldBeEqualTo "abb"

        // Source: b...bc. Sink: b...b.
        source.read(sink, 20000) shouldBeEqualTo TestUtil.SEGMENT_SIZE.toLong()
        sink.readUtf8() shouldBeEqualTo "b".repeat(TestUtil.SEGMENT_SIZE)

        // Source: b...bc. Sink: b...bc.
        source.read(sink, 20000) shouldBeEqualTo TestUtil.SEGMENT_SIZE - 1L
        sink.readUtf8() shouldBeEqualTo "b".repeat(TestUtil.SEGMENT_SIZE - 2) + "c"

        // Source and sink are empty
        source.read(sink, 1) shouldBeEqualTo -1L
        sink.exhausted().shouldBeTrue()
        inputStream.close()
    }

    @Test
    fun `input stream을 source로 이용하고, segment size 만큼 읽어오기`() {
        val inputStream = ByteArrayInputStream(ByteArray(TestUtil.SEGMENT_SIZE))
        val source = inputStream.source()
        val sink = Buffer()

        source.read(sink, TestUtil.SEGMENT_SIZE.toLong()) shouldBeEqualTo TestUtil.SEGMENT_SIZE.toLong()
        source.read(sink, TestUtil.SEGMENT_SIZE.toLong()) shouldBeEqualTo -1L
    }

    @Test
    fun `input stream 을 source로 이용할 때, byteCount를 음수로 사용하면 예외가 발생한다`() {
        val source = ByteArrayInputStream(ByteArray(100)).source()

        assertFailsWith<IllegalArgumentException> {
            source.read(Buffer(), -1L)
        }
    }

    @Test
    fun `전달된 데이터를 단순히 버리는 blackhole sink 사용하기`() {
        val data = bufferOf("blackhole")

        // blackhole sink 는 데이터를 버린다. (테스트를 위해 사용한다)
        val blackhole = blackholeSink()
        // blackhole sink 에 5개의 문자를 쓴다
        blackhole.write(data, 5L)

        // 남은 4개의 데이터
        data.readUtf8() shouldBeEqualTo "hole"
    }
}
