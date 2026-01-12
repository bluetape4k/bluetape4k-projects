package io.bluetape4k.io.okio.channels

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.Timeout
import okio.buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

@TempFolderTest
class OkioChannelsTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val QUOTE: String = ("John, the kind of control you're attempting simply is... it's not "
                + "possible. If there is one thing the history of evolution has "
                + "taught us it's that life will not be contained. Life breaks "
                + "free, it expands to new territories and crashes through "
                + "barriers, painfully, maybe even dangerously, but, uh... well, "
                + "there it is.")

        private val r = EnumSet.of(StandardOpenOption.READ)
        private val w = EnumSet.of(StandardOpenOption.WRITE)
        private val append = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND)
    }

    private lateinit var tempFolder: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `read channel`() {
        val channel: ReadableByteChannel = Buffer().writeUtf8(QUOTE)

        // channel -> source -> buffer 로 데이터 전달
        val source = ByteChannelSource(channel, Timeout.NONE)
        val buffer = Buffer()

        // source에서 75바이트 읽어서 buffer에 저장
        source.read(buffer, 75)

        buffer.readUtf8() shouldBeEqualTo QUOTE.substring(0, 75)
    }

    @Test
    fun `read channel fully`() {
        val channel = Buffer().writeUtf8(QUOTE)

        val source = ByteChannelSource(channel, Timeout.NONE).buffer()

        // source에서 모든 데이터를 읽어서 buffer에 저장
        source.readUtf8() shouldBeEqualTo QUOTE
    }

    @Test
    fun `write channel`() {
        val channel = Buffer()

        val sink = ByteChannelSink(channel, Timeout.NONE)
        sink.write(Buffer().writeUtf8(QUOTE), 75)

        channel.readUtf8() shouldBeEqualTo QUOTE.substring(0, 75)
    }

    @Test
    fun `read and write file`() {
        val path = tempFolder.createFile().toPath()

        FileChannelSink(FileChannel.open(path, w), Timeout.NONE).use { sink ->
            sink.write(Buffer().writeUtf8(QUOTE), QUOTE.length.toLong())
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeEqualTo QUOTE.length.toLong()

        val buffer = Buffer()
        FileChannelSource(FileChannel.open(path, r), Timeout.NONE).use { source ->
            source.read(buffer, 44)
            buffer.readUtf8() shouldBeEqualTo QUOTE.substring(0, 44)

            source.read(buffer, 31)
            buffer.readUtf8() shouldBeEqualTo QUOTE.substring(44, 75)
        }
    }

    @Test
    fun `append to file`() {
        val path = tempFolder.createFile().toPath()

        val buffer = Buffer().writeUtf8(QUOTE)

        // 75바이트를 쓴다
        FileChannelSink(FileChannel.open(path, w), Timeout.NONE).use { sink ->
            sink.write(buffer, 75)
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeEqualTo 75L

        FileChannelSource(FileChannel.open(path, r), Timeout.NONE).buffer().use { source ->
            source.readUtf8() shouldBeEqualTo QUOTE.substring(0, 75)
        }

        // 나머지 부분을 추가로 쓴다
        FileChannelSink(FileChannel.open(path, append), Timeout.NONE).use { sink ->
            sink.write(buffer, buffer.size)
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeEqualTo QUOTE.length.toLong()

        FileChannelSource(FileChannel.open(path, r), Timeout.NONE).buffer().use { source ->
            source.readUtf8() shouldBeEqualTo QUOTE
        }
    }
}
