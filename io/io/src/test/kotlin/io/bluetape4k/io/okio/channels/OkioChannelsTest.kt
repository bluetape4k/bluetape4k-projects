package io.bluetape4k.io.okio.channels

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.Timeout
import okio.buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
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
                + "there it is."
                + "동해물과 백두산이 마르고 닳도록"
                + "하느님이 보우하사 우리나라 만세")

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

        val source = channel.asSource().buffer()

        // source에서 모든 데이터를 읽어서 buffer에 저장
        source.readUtf8() shouldBeEqualTo QUOTE
    }

    @Test
    fun `write channel`() {
        val channel = Buffer()

        val sink = channel.asSink().buffer()
        sink.write(Buffer().writeUtf8(QUOTE), 75)
        sink.flush()

        channel.readUtf8() shouldBeEqualTo QUOTE.substring(0, 75)
    }

    @Test
    fun `read and write file`() {
        val path = tempFolder.createFile().toPath()

        FileChannel.open(path, w).asSink().use { sink ->
            sink.write(Buffer().writeUtf8(QUOTE), QUOTE.length.toLong())
            sink.flush()
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeEqualTo QUOTE.length.toLong()

        val buffer = Buffer()
        FileChannel.open(path, r).asSource().use { source ->
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
        FileChannel.open(path, w).asSink().use { sink ->
            sink.write(buffer, 75)
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeEqualTo 75L

        FileChannel.open(path, r).asSource().buffer().use { source ->
            source.readUtf8() shouldBeEqualTo QUOTE.substring(0, 75)
        }

        // 나머지 부분을 추가로 쓴다
        FileChannel.open(path, append).asSink().use { sink ->
            sink.write(buffer, buffer.size)
        }
        Files.exists(path).shouldBeTrue()
        Files.size(path) shouldBeGreaterOrEqualTo QUOTE.length.toLong()

        FileChannel.open(path, r).asSource().buffer().use { source ->
            source.readUtf8() shouldBeEqualTo QUOTE
        }
    }
}
