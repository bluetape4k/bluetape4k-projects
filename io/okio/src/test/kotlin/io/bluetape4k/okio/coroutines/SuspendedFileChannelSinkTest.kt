package io.bluetape4k.okio.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.okio.AbstractOkioTest
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.Buffer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.InterruptedIOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

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

    @Test
    fun `write ignores non-positive byteCount and validates upper range`() = runSuspendIO {
        val tempFile = tempFolder.createFile().toPath()
        val channel = AsynchronousFileChannel.open(
            tempFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        val sink: SuspendedSink = channel.asSuspendedSink()
        val negativeSource = Buffer().writeUtf8("hello")
        val zeroSource = Buffer().writeUtf8("world")
        val overflowSource = Buffer().writeUtf8("ok")

        sink.write(negativeSource, -1L)
        sink.write(zeroSource, 0L)
        negativeSource.readUtf8() shouldBeEqualTo "hello"
        zeroSource.readUtf8() shouldBeEqualTo "world"

        assertFailsWith<IllegalArgumentException> {
            sink.write(overflowSource, overflowSource.size + 1L)
        }

        sink.close()
    }

    @Test
    fun `flush interrupts blocking force when cancelled`() = runSuspendIO {
        withTimeout(5.seconds) {
            val channel = InterruptibleFileChannel()
            val sink = channel.asSuspendedSink()

            val job = launch {
                try {
                    sink.flush()
                } catch (e: InterruptedIOException) {
                    // Expected when cancellation interrupts the blocking force() call.
                }
            }
            channel.forceEntered.await()
            job.cancelAndJoin()

            channel.forceInterrupted.get() shouldBeEqualTo true
        }
    }

    private class InterruptibleFileChannel: AsynchronousFileChannel() {
        val forceEntered = CompletableDeferred<Unit>()
        val forceInterrupted = AtomicBoolean(false)

        private val open = AtomicBoolean(true)

        override fun isOpen(): Boolean = open.get()

        override fun close() {
            open.set(false)
        }

        override fun size(): Long = 0L

        override fun truncate(size: Long): AsynchronousFileChannel = this

        override fun force(metaData: Boolean) {
            forceEntered.complete(Unit)
            blockUntilInterrupted(forceInterrupted)
        }

        override fun <A: Any?> lock(
            position: Long,
            size: Long,
            shared: Boolean,
            attachment: A,
            handler: CompletionHandler<FileLock, in A>,
        ) {
            throw UnsupportedOperationException()
        }

        override fun lock(position: Long, size: Long, shared: Boolean): Future<FileLock> {
            throw UnsupportedOperationException()
        }

        override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock {
            throw UnsupportedOperationException()
        }

        override fun <A: Any?> read(
            dst: ByteBuffer,
            position: Long,
            attachment: A,
            handler: CompletionHandler<Int, in A>,
        ) {
            throw UnsupportedOperationException()
        }

        override fun read(dst: ByteBuffer, position: Long): Future<Int> {
            throw UnsupportedOperationException()
        }

        override fun <A: Any?> write(
            src: ByteBuffer,
            position: Long,
            attachment: A,
            handler: CompletionHandler<Int, in A>,
        ) {
            throw UnsupportedOperationException()
        }

        override fun write(src: ByteBuffer, position: Long): Future<Int> {
            throw UnsupportedOperationException()
        }

        private fun blockUntilInterrupted(interrupted: AtomicBoolean): Nothing {
            try {
                while (true) {
                    Thread.sleep(1_000L)
                }
            } catch (e: InterruptedException) {
                interrupted.set(true)
                Thread.currentThread().interrupt()
                throw InterruptedIOException("blocking call interrupted").apply {
                    initCause(e)
                }
            }
        }
    }
}
