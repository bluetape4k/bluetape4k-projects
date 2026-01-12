package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.toUtf8String
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.StandardOpenOption
import kotlin.test.assertFailsWith

@TempFolderTest
class SuspendedFileChannelSinkTest: AbstractOkioTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    private lateinit var tempFolder: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `write and read back data`() = runSuspendIO {
        val tempFile = tempFolder.createFile().toPath()
        val channel = AsynchronousFileChannel.open(
            tempFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        val message = faker.lorem().paragraph(10).repeat(100)
        val sink: SuspendedSink = channel.asSuspendedSink()
        val buffer = Buffer().writeUtf8(message)

        sink.write(buffer, buffer.size)
        sink.flush()
        sink.close()

        val readChannel = AsynchronousFileChannel.open(
            tempFile,
            StandardOpenOption.READ
        )

        val readBuffer = ByteBuffer.allocate(readChannel.size().toInt())
        readChannel.read(readBuffer, 0).get()
        readBuffer.flip()

        val result = ByteArray(readBuffer.remaining())
        readBuffer.get(result)
        result.toUtf8String() shouldBeEqualTo message

        readChannel.close()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `write after close throws`() = runSuspendIO {
        val tempFile = tempFolder.createFile().toPath()
        val channel = AsynchronousFileChannel.open(
            tempFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        val sink: SuspendedSink = channel.asSuspendedSink()
        sink.close()

        val buffer = Buffer().writeUtf8("fail")
        assertFailsWith<IllegalStateException> {
            sink.write(buffer, buffer.size)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `write and read by buffered suspended sink`() = runSuspendIO {
        val tempFile = tempFolder.createFile().toPath()
        val channel = AsynchronousFileChannel.open(
            tempFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        val message = faker.lorem().paragraph(10).repeat(100)
        val sink: BufferedSuspendedSink = channel.asSuspendedSink().buffered()
        val buffer = Buffer().writeUtf8(message)

        sink.write(buffer, buffer.size)
        sink.flush()
        sink.close()

        val readChannel = AsynchronousFileChannel.open(
            tempFile,
            StandardOpenOption.READ
        )

        val readBuffer = ByteBuffer.allocate(readChannel.size().toInt())
        readChannel.read(readBuffer, 0).get()
        readBuffer.flip()

        val result = ByteArray(readBuffer.remaining())
        readBuffer.get(result)
        result.toUtf8String() shouldBeEqualTo message

        readChannel.close()
    }
}
