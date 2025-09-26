package io.bluetape4k.io.okio

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import net.datafaker.Faker
import okio.Buffer
import okio.appendingSink
import okio.blackholeSink
import okio.sink
import okio.source
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.*

@TempFolderTest
class OkioTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Faker(Locale.getDefault())
    }

    private lateinit var temp: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        temp = tempFolder
    }

    @Test
    fun `read write file`() {
        val file = temp.createFile()
        val content = Fakers.randomString()

        file.sink().buffered().use { sink ->
            sink.writeUtf8(content)
        }
        file.exists().shouldBeTrue()
        file.length() shouldBeEqualTo content.length.toLong()

        file.source().buffered().use { source ->
            source.readUtf8() shouldBeEqualTo content
        }
    }

    @Test
    fun `append file by appendingSink`() {
        val file = temp.createFile()
        val content1 = Fakers.randomString()
        val content2 = Fakers.randomString()

        file.appendingSink().buffered().use { sink ->
            sink.writeUtf8(content1)
        }
        file.exists().shouldBeTrue()
        file.length() shouldBeEqualTo content1.length.toLong()

        // appending 을 위한 sink
        file.appendingSink().buffered().use { sink ->
            sink.writeUtf8(content2)
        }
        file.length() shouldBeEqualTo (content1.length + content2.length).toLong()

        file.source().buffered().use { source ->
            source.readUtf8() shouldBeEqualTo content1 + content2
        }
    }

    @Test
    fun `read write path`() {
        val path = temp.createFile().toPath()
        val content = Fakers.randomString()

        path.sink().buffered().use { sink ->
            sink.writeUtf8(content)
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeEqualTo content.length.toLong()

        path.source().buffered().use { source ->
            source.readUtf8() shouldBeEqualTo content
        }
    }

    @Test
    fun `sink from output stream`() {
        val content = "a" + "b".repeat(9998) + "c"
        val data = Buffer().apply {
            writeUtf8(content)
        }

        val out = ByteArrayOutputStream()
        val sink = out.sink()

        // sink 에 data 를 쓰되, 3개의 문자만 쓴다.
        sink.write(data, 3)
        out.toString(Charsets.UTF_8) shouldBeEqualTo "abb"

        // sink 에 data 의 현재 position 부터 끝까지 쓴다.
        sink.write(data, data.size)
        out.toString(Charsets.UTF_8) shouldBeEqualTo content
    }

    @Test
    fun `source from InputStream`() {
        val content = "a" + "b".repeat(TestUtil.SEGMENT_SIZE * 2) + "c"
        val inputStream = ByteArrayInputStream(content.toUtf8Bytes())

        // Source: ab....bc
        val source = inputStream.source()
        val sink = Buffer()

        // Source로 부터 읽어서 sink 에 저장한다 
        // Source: ab....bc. Sink: abb.
        source.read(sink, 3) shouldBeEqualTo 3
        sink.readUtf8(3) shouldBeEqualTo "abb"

        // Source: b...bc. Sink: b...b.
        source.read(sink, 20000) shouldBeEqualTo TestUtil.SEGMENT_SIZE.toLong()
        sink.readUtf8() shouldBeEqualTo "b".repeat(TestUtil.SEGMENT_SIZE)

        // Source: b...bc. Sink: b...bc.
        source.read(sink, 20000) shouldBeEqualTo TestUtil.SEGMENT_SIZE - 1L
        sink.readUtf8() shouldBeEqualTo "b".repeat(TestUtil.SEGMENT_SIZE - 2) + "c"

        // Source and sink are empty.
        source.read(sink, 1) shouldBeEqualTo -1
    }

    @Test
    fun `source from InputStream with Segment size`() {
        val inputStream = ByteArrayInputStream(ByteArray(TestUtil.SEGMENT_SIZE))
        val source = inputStream.source()
        val sink = Buffer()

        source.read(sink, TestUtil.SEGMENT_SIZE.toLong()) shouldBeEqualTo TestUtil.SEGMENT_SIZE.toLong()
        source.read(sink, TestUtil.SEGMENT_SIZE.toLong()) shouldBeEqualTo -1L
    }

    @Test
    fun `source from input stream bounds`() {
        val source = ByteArrayInputStream(ByteArray(100)).source()

        assertFailsWith<IllegalArgumentException> {
            source.read(Buffer(), -1L)
        }
    }

    @Test
    fun `use blackhole sink`() {
        val data = Buffer()
        data.writeUtf8("blackhole")

        // blackhole sink 는 데이터를 버린다.
        val blackhole = blackholeSink()

        blackhole.write(data, 5L)
        data.readUtf8() shouldBeEqualTo "hole"
    }
}
