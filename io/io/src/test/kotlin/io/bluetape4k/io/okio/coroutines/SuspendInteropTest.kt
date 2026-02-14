package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.asBufferedSink
import io.bluetape4k.io.okio.asBufferedSource
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

class SuspendInteropTest: AbstractOkioTest() {

    @Test
    fun `Buffer suspendReadAll은 모든 바이트를 SuspendedSink에 쓴다`() = runSuspendIO {
        val buffer = Buffer().apply { writeUtf8("테스트") }
        val sink = mockk<SuspendedSink>(relaxed = true)

        val written = buffer.suspendReadAll(sink)

        written shouldBeEqualTo 9L // "테스트"의 UTF-8 바이트 길이
        coVerify { sink.write(buffer, 9L) }
    }

    @Test
    fun `BufferedSource suspendReadAll은 모든 바이트를 SuspendedSink에 쓴다`() = runSuspendIO {
        val source = Buffer().apply { writeUtf8("데이터") }
        val bufferedSource: BufferedSource = source // sealed interface이므로 직접 사용
        val sink = mockk<SuspendedSink>(relaxed = true)

        val written = bufferedSource.suspendReadAll(sink)

        written shouldBeEqualTo 9L
        coVerify { sink.write(any(), any()) }
    }

    @Test
    fun `BufferedSink suspendWriteAll은 SuspendedSource에서 모든 바이트를 읽는다`() = runSuspendIO {
        val sink = Buffer()
        val bufferedSink: BufferedSink = sink // sealed interface이므로 직접 사용
        val source = mockk<SuspendedSource>()
        coEvery { source.read(any(), any()) } returnsMany listOf(5L, 4L, -1L)

        val read = bufferedSink.suspendWriteAll(source)

        read shouldBeEqualTo 9L
        coVerify(exactly = 3) { source.read(any(), any()) }
    }

    @Test
    fun `BufferedSuspendedSource suspendReadAll은 모든 바이트를 Sink에 쓴다`() = runSuspendIO {
        val buffer = Buffer().apply { writeUtf8("소스테스트") }
        val bufferSize = buffer.size
        // val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        val source: BufferedSuspendedSource = buffer.asBufferedSource().asSuspended().buffered()
        val sink: Sink = mockk<Sink>(relaxed = true)

        val written = source.suspendReadAll(sink)

        written shouldBeEqualTo bufferSize
        coVerify { sink.write(any(), any()) }
    }

    @Test
    fun `BufferedSuspendedSink suspendWriteAll은 Source에서 모든 바이트를 읽는다`() = runSuspendIO {
        val sink = Buffer().asBufferedSink().asSuspended().buffered()
        val source = mockk<Source>()
        coEvery { source.read(any(), any()) } returnsMany listOf(3L, 2L, -1L)

        val read = sink.suspendWriteAll(source)

        read shouldBeEqualTo 5L
        coVerify(exactly = 3) { source.read(any(), any()) }
    }

    @Test
    fun `BufferedSuspendedSink suspendWrite는 Source에서 byteCount만큼 읽는다`() = runSuspendIO {
        val sink = RealBufferedSuspendedSink(FakeSuspendedSink())

        val source = mockk<Source>()
        coEvery { source.read(any(), any()) } returnsMany listOf(2L, 3L)

        val result = sink.suspendWrite(source, 5L)

        result shouldBeEqualTo sink
        coVerify(exactly = 2) { source.read(any(), any()) }
    }

    @Test
    fun `BufferedSuspendedSink suspendWrite는 non-positive byteCount면 즉시 반환한다`() = runSuspendIO {
        val sink = RealBufferedSuspendedSink(FakeSuspendedSink())
        val source = mockk<Source>()

        sink.suspendWrite(source, -1L) shouldBeEqualTo sink
        sink.suspendWrite(source, 0L) shouldBeEqualTo sink

        coVerify(exactly = 0) { source.read(any(), any()) }
    }

    @Test
    fun `BufferedSink suspendWriteAll은 no progress가 반복되면 예외를 던진다`() = runSuspendIO {
        val sink = Buffer().asBufferedSink()
        val source = mockk<SuspendedSource>()
        coEvery { source.read(any(), any()) } returns 0L

        assertFailsWith<IOException> {
            sink.suspendWriteAll(source)
        }
    }

    @Test
    fun `BufferedSuspendedSink suspendWriteAll은 no progress가 반복되면 예외를 던진다`() = runSuspendIO {
        val sink = Buffer().asBufferedSink().asSuspended().buffered()
        val source = mockk<Source>()
        every { source.read(any(), any()) } returns 0L

        assertFailsWith<IOException> {
            sink.suspendWriteAll(source)
        }
    }

    @Test
    fun `BufferedSuspendedSink suspendWrite는 no progress가 반복되면 예외를 던진다`() = runSuspendIO {
        val sink = RealBufferedSuspendedSink(FakeSuspendedSink())
        val source = mockk<Source>()
        every { source.read(any(), any()) } returns 0L

        assertFailsWith<IOException> {
            sink.suspendWrite(source, 1L)
        }
    }

    @Test
    fun `SuspendedSource readAll은 no progress가 반복되면 예외를 던진다`() = runSuspendIO {
        val noProgressSource = object: SuspendedSource {
            override suspend fun read(sink: Buffer, byteCount: Long): Long = 0L
            override suspend fun close() {}
            override fun timeout() = Timeout.NONE
        }

        assertFailsWith<IOException> {
            noProgressSource.readAll(Buffer())
        }
    }

    // 테스트용 FakeSuspendedSink
    private class FakeSuspendedSink: SuspendedSink {
        val buffer = Buffer()
        var closed = false
        override suspend fun write(source: Buffer, byteCount: Long) {
            buffer.write(source, byteCount)
        }

        override suspend fun flush() {}
        override suspend fun close() {
            closed = true
        }

        override fun timeout() = Timeout.NONE
    }

    // 테스트용 FakeSuspendedSource
    // 실제로는 BufferedSource를 사용하지만, 여기서는 간단한 예시로 FakeSuspendedSource를 사용합니다.
    private class FakeSuspendedSource(private val data: Buffer): SuspendedSource {
        var closed = false
        override suspend fun read(sink: Buffer, byteCount: Long): Long {
            return data.read(sink, byteCount)
        }

        override suspend fun close() {
            closed = true
        }

        override fun timeout() = Timeout.NONE
    }
}
