package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.IOException
import okio.Timeout
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * 취소가 가능한 코루틴 방식의 파이프를 구현한다.
 *
 * 이 파이프는 내부 버퍼를 사용하여 데이터를 비동기적으로 읽고 쓸 수 있으며,
 * 취소 시 모든 입출력 작업이 즉시 중단된다.
 */
class SuspendedPipe(internal val maxBufferSize: Long) {

    internal val buffer = Buffer()
    internal var canceled = false
    internal var sinkClosed = false
    internal var sourceClosed = false
    internal var foldedSink: SuspendedSink? = null

    val lock: ReentrantLock = ReentrantLock()
    val condition: Condition = lock.newCondition()

    init {
        require(maxBufferSize >= 1L) { "maxBufferSize[$maxBufferSize] >= 1" }
    }

    @get:JvmName("sink")
    val sink = object: SuspendedSink {
        private val timeout = Timeout()

        override suspend fun write(source: Buffer, byteCount: Long) {
            var byteCount = byteCount
            var delegate: SuspendedSink? = null
            lock.withLock {
                check(!sinkClosed) { "closed" }
                if (canceled) throw IOException("canceled")

                while (byteCount > 0) {
                    foldedSink?.let {
                        delegate = it
                        return@withLock
                    }

                    if (sourceClosed) throw IOException("source is closed")

                    val bufferSpaceAvailable = maxBufferSize - buffer.size
                    if (bufferSpaceAvailable == 0L) {
                        timeout.awaitSignal(condition) // Wait until the source drains the buffer.
                        if (canceled) throw IOException("canceled")
                        continue
                    }

                    val bytesToWrite = minOf(bufferSpaceAvailable, byteCount)
                    buffer.write(source, bytesToWrite)
                    byteCount -= bytesToWrite
                    condition.signalAll() // Notify the source that it can resume reading.
                }
            }

            delegate?.forward { write(source, byteCount) }
        }

        override suspend fun flush() {
            var delegate: SuspendedSink? = null
            lock.withLock {
                check(!sinkClosed) { "closed" }
                if (canceled) throw IOException("canceled")

                foldedSink?.let {
                    delegate = it
                    return@withLock
                }

                if (sourceClosed && buffer.size > 0L) {
                    throw IOException("source is closed")
                }
            }

            delegate?.forward { flush() }
        }

        override suspend fun close() {
            var delegate: SuspendedSink? = null
            lock.withLock {
                if (sinkClosed) return

                foldedSink?.let {
                    delegate = it
                    return@withLock
                }

                if (sourceClosed && buffer.size > 0L) throw IOException("source is closed")
                sinkClosed = true
                condition.signalAll() // Notify the source that no more bytes are coming.
            }

            delegate?.forward { close() }
        }

        override fun timeout(): Timeout = timeout
    }

    @get:JvmName("source")
    val source = object: SuspendedSource {
        private val timeout = Timeout()

        override suspend fun read(sink: Buffer, byteCount: Long): Long {
            lock.withLock {
                check(!sourceClosed) { "closed" }
                if (canceled) throw IOException("canceled")

                while (buffer.size == 0L) {
                    if (sinkClosed) return -1L
                    timeout.awaitSignal(condition) // Wait until the sink fills the buffer.
                    if (canceled) throw IOException("canceled")
                }

                val result = buffer.read(sink, byteCount)
                condition.signalAll() // Notify the sink that it can resume writing.
                return result
            }
        }

        override suspend fun close() {
            lock.withLock {
                sourceClosed = true
                condition.signalAll() // Notify the sink that no more bytes are desired.
            }
        }

        override fun timeout(): Timeout = timeout
    }

    /**
     * Writes any buffered contents of this pipe to `sink`, then replace this pipe's source with
     * `sink`. This pipe's source is closed and attempts to read it will throw an
     * [IllegalStateException].
     *
     * This method must not be called while concurrently accessing this pipe's source. It is safe,
     * however, to call this while concurrently writing this pipe's sink.
     */
    @Throws(IOException::class)
    suspend fun fold(sink: SuspendedSink) {
        while (true) {
            // Either the buffer is empty and we can swap and return. Or the buffer is non-empty and we
            // must copy it to sink without holding any locks, then try it all again.
            var closed = false
            var done = false
            lateinit var sinkBuffer: Buffer
            lock.withLock {
                check(foldedSink == null) { "sink already folded" }

                if (canceled) {
                    foldedSink = sink
                    throw IOException("canceled")
                }

                closed = sinkClosed
                if (buffer.exhausted()) {
                    sourceClosed = true
                    foldedSink = sink
                    done = true
                    return@withLock
                }

                sinkBuffer = Buffer()
                sinkBuffer.write(buffer, buffer.size)
                condition.signalAll() // Notify the sink that it can resume writing.
            }

            if (done) {
                if (closed) {
                    sink.close()
                }
                return
            }

            var success = false
            try {
                sink.write(sinkBuffer, sinkBuffer.size)
                sink.flush()
                success = true
            } finally {
                if (!success) {
                    lock.withLock {
                        sourceClosed = true
                        condition.signalAll() // Notify the sink that it can resume writing.
                    }
                }
            }
        }
    }

    private suspend inline fun SuspendedSink.forward(block: suspend SuspendedSink.() -> Unit) {
        this.timeout().intersectWith(this@SuspendedPipe.sink.timeout()) { this.block() }
    }

    /**
     * Fail any in-flight and future operations. After canceling:
     *
     *  * Any attempt to write or flush [sink] will fail immediately with an [IOException].
     *  * Any attempt to read [source] will fail immediately with an [IOException].
     *  * Any attempt to [fold] will fail immediately with an [IOException].
     *
     * Closing the source and the sink will complete normally even after a pipe has been canceled. If
     * this sink has been folded, closing it will close the folded sink. This operation may block.
     *
     * This operation may be called by any thread at any time. It is safe to call concurrently while
     * operating on the source or the sink.
     */
    fun cancel() {
        lock.withLock {
            canceled = true
            buffer.clear()
            condition.signalAll() // Notify the source and sink that they're canceled.
        }
    }
}
