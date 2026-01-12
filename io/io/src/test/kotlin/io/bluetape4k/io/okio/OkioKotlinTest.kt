package io.bluetape4k.io.okio

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import okio.sink
import okio.source
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.nio.file.StandardOpenOption
import kotlin.test.Test

@TempFolderTest
class OkioKotlinTest: AbstractOkioTest() {

    companion object: KLogging() {
        val faker = Fakers.faker
    }

    private lateinit var temp: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        temp = tempFolder
    }

    @Test
    fun `output stream as sink`() {
        ByteArrayOutputStream().use { bos ->
            val sink = bos.sink()
            sink.write(Buffer().writeUtf8("a"), 1L)
            bos.toByteArray() shouldBeEqualTo byteArrayOf(0x61)
        }
    }

    @Test
    fun `input stream as source`() {
        ByteArrayInputStream(byteArrayOf(0x61)).use { bis ->
            val source = bis.source()
            val buffer = Buffer()
            source.read(buffer, 1)
            buffer.readUtf8() shouldBeEqualTo "a"
        }
    }

    @Test
    fun `file as sink for writing`() {
        val content = Fakers.randomString()
        val file = temp.createFile()
        file.sink().use { sink ->
            sink.write(Buffer().writeUtf8(content), content.length.toLong())
        }
        file.readText() shouldBeEqualTo content
        file.delete()
    }

    @Test
    fun `file as sink for appending`() {
        val content = Fakers.randomString()

        val file = temp.createFile()
        file.writeText("a")
        file.sink(append = true).use { sink ->
            sink.write(Buffer().writeUtf8(content), content.length.toLong())
        }
        file.readText() shouldBeEqualTo "a$content"
        file.delete()
    }

    @Test
    fun `file as source for reading`() {
        val file = temp.createFile()
        file.writeText("a")

        file.source().use { source ->
            val buffer = Buffer()
            source.read(buffer, 1L)
            buffer.readUtf8() shouldBeEqualTo "a"
        }

        file.delete()
    }

    @Test
    fun `path as sink`() {
        val file = temp.createFile()

        file.toPath().sink().use { sink ->
            sink.write(Buffer().writeUtf8("a"), 1L)
        }
        file.readText() shouldBeEqualTo "a"

        file.delete()
    }

    @Test
    fun `path as sink with options`() {
        val file = temp.createFile()
        file.writeText("a")

        file.toPath().sink(StandardOpenOption.APPEND).use { sink ->
            sink.write(Buffer().writeUtf8("b"), 1L)
        }

        file.readText() shouldBeEqualTo "ab"
    }

    @Test
    fun `path as source`() {
        val file = temp.createFile()
        val content = Fakers.randomString()
        file.writeText(content)

        file.toPath().source().use { source ->
            val buffer = Buffer()
            source.read(buffer, content.length.toLong())
            buffer.readUtf8() shouldBeEqualTo content
        }

        file.delete()
    }

    @Test
    fun `path as source with options`() {
        val content = Fakers.randomString()

        val file = temp.createFile()
        file.writeText(content)

        file.toPath().source(StandardOpenOption.READ).use { source ->
            val buffer = Buffer()
            source.read(buffer, content.length.toLong())
            buffer.readUtf8() shouldBeEqualTo content
        }
        file.delete()   // Clean up after test
    }

    @Test
    fun `socket as Sink`() {
        val content = Fakers.randomString()

        val bos = ByteArrayOutputStream()
        val socket = object: Socket() {
            override fun getOutputStream() = bos
        }
        val sink = socket.sink()
        sink.write(Buffer().writeUtf8(content), content.length.toLong())

        bos.toByteArray() shouldBeEqualTo content.toUtf8Bytes()
        bos.close()
    }

    @Test
    fun `socket as Source`() {
        val content = Fakers.randomString()
        val contentBytes = content.toUtf8Bytes()
        val bis = ByteArrayInputStream(contentBytes)
        val socket = object: Socket() {
            override fun getInputStream() = bis
        }
        socket.source().use { source ->
            val buffer = Buffer()
            source.read(buffer, contentBytes.size.toLong())
            buffer.readUtf8() shouldBeEqualTo content
        }
    }
}
