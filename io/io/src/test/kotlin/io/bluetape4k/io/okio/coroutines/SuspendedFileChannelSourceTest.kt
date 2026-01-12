package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@TempFolderTest
class SuspendedFileChannelSourceTest: AbstractOkioTest() {

    companion object: KLoggingChannel() {
        private const val MESSAGE = "동해물과 백두산이 마르고 닳도록"
    }

    private lateinit var tempFolder: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `read should return correct bytes`() = runSuspendIO {
        val buffer = Buffer()

        val source = createSuspendedSource()
        source.read(buffer, Int.MAX_VALUE.toLong())

        buffer.readUtf8() shouldBeEqualTo MESSAGE
    }

    @Test
    fun `read should return -1 at EOF`() = runSuspendIO {
        val buffer = Buffer()

        // Read all bytes
        val source = createSuspendedSource()
        source.read(buffer, Int.MAX_VALUE.toLong())

        // Try to read again, should return -1
        val eof = source.read(buffer, 1)
        eof shouldBeEqualTo -1L
    }

    private fun createSuspendedSource(): SuspendedFileChannelSource {
        val tempFile = tempFolder.createFile().toPath()
        Files.write(tempFile, MESSAGE.toByteArray())

        val channel = AsynchronousFileChannel.open(tempFile, StandardOpenOption.READ)
        return SuspendedFileChannelSource(channel)
    }
}
