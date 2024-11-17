package io.bluetape4k.io.okio

import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.support.toUtf8Bytes
import okio.Buffer
import okio.BufferedSink
import okio.sink
import okio.source
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.StandardOpenOption
import kotlin.test.Test

@TempFolderTest
class NioTest {

    @Test
    fun `source is open`() {
        val source = Buffer().asBufferedSource()
        source.isOpen.shouldBeTrue()

        source.close()
        source.isOpen.shouldBeFalse()
    }

    @Test
    fun `sink is open`() {
        val sink = Buffer().asBufferedSink()
        sink.isOpen.shouldBeTrue()

        sink.close()
        sink.isOpen.shouldBeFalse()
    }

    @Test
    fun `writable channel nio file`(tempFolder: TempFolder) {
        val file = tempFolder.createFile()
        val fileChannel: FileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)

        testWritableByteChannel(fileChannel)

        // 파일을 Source로 읽어들여서 확인
        file.source().buffered().use { emitted ->
            emitted.readUtf8() shouldBeEqualTo "defghijklmnopqrstuvw"
        }
    }

    @Test
    fun `writable channel buffer`() {
        val buffer = Buffer()

        testWritableByteChannel(buffer)

        buffer.readUtf8() shouldBeEqualTo "defghijklmnopqrstuvw"
    }

    @Test
    fun `writable channel buffered sink`() {
        val buffer = Buffer()
        val bufferedSink: BufferedSink = buffer.asBufferedSink()

        testWritableByteChannel(bufferedSink)

        buffer.readUtf8() shouldBeEqualTo "defghijklmnopqrstuvw"
    }

    @Test
    fun `readable channel nio file`(tempFolder: TempFolder) {
        val file = tempFolder.createFile()
        // 파일에 데이터를 쓴다.
        file.sink().buffered().use { initialData ->
            initialData.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        }

        // 파일에서 데이터를 읽어온다
        val fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
        testReadableByteChannel(fileChannel)
    }

    @Test
    fun `readable channel buffer`() {
        val buffer = bufferOf("abcdefghijklmnopqrstuvwxyz")
        testReadableByteChannel(buffer)
    }

    @Test
    fun `readable channel with buffered source`() {
        val buffer = Buffer()
        buffer.writeUtf8("abcdefghijklmnopqrstuvwxyz")

        val bufferedSource = buffer.asBufferedSource()
        testReadableByteChannel(bufferedSource)
    }

    private fun testWritableByteChannel(channel: WritableByteChannel) {
        channel.isOpen.shouldBeTrue()

        val byteBuffer = ByteBuffer.allocate(1024)
        byteBuffer.put("abcdefghijklmnopqrstuvwxyz".toUtf8Bytes())
        byteBuffer.flip()
        byteBuffer.position(3)
        byteBuffer.limit(23)

        val byteCount = channel.write(byteBuffer)
        byteCount shouldBeEqualTo 20
        byteBuffer.position() shouldBeEqualTo 23
        byteBuffer.limit() shouldBeEqualTo 23

        channel.close()
        channel.isOpen shouldBeEqualTo (channel is Buffer)
    }

    private fun testReadableByteChannel(channel: ReadableByteChannel) {
        channel.isOpen.shouldBeTrue()

        val byteBuffer = ByteBuffer.allocate(1024)
        byteBuffer.position(3)
        byteBuffer.limit(23)

        // channel로부터 데이터를 읽어 byteBuffer에 저장한다.
        val byteCount = channel.read(byteBuffer)
        byteCount shouldBeEqualTo 20
        byteBuffer.position() shouldBeEqualTo 23
        byteBuffer.limit() shouldBeEqualTo 23

        channel.close()

        channel.isOpen shouldBeEqualTo (channel is Buffer)
        byteBuffer.flip()
        byteBuffer.position(3)

        val data = ByteArray(byteBuffer.remaining())
        byteBuffer.get(data)
        String(data, Charsets.UTF_8) shouldBeEqualTo "abcdefghijklmnopqrst"
    }
}
