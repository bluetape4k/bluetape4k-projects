package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Timeout
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [AsynchronousSocketChannel]을 [AsyncSource]로 변환합니다.
 */
fun AsynchronousSocketChannel.asAsyncSource(): AsyncSource {
    val channel = this

    return object: AsyncSource, KLoggingChannel() {
        val buffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())
        val timeout = Timeout.NONE

        override suspend fun read(sink: Buffer, byteCount: Long): Long {
            buffer.clear()
            buffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())
            val read = channel.coRead(buffer)
            buffer.flip()
            if (read > 0) sink.write(buffer)
            return read.toLong()
        }

        override suspend fun close() {
            withContext(Dispatchers.IO) {
                channel.close()
            }
        }

        override suspend fun timeout(): Timeout {
            return timeout
        }
    }
}

/**
 * [AsynchronousFileChannel]을 [AsyncSink]로 변환합니다.
 */
fun AsynchronousSocketChannel.asAsyncSink(): AsyncSink {
    val channel = this

    return object: AsyncSink, KLoggingChannel() {
        val cursor = Buffer.UnsafeCursor()
        val timeout = Timeout.NONE

        override suspend fun write(source: Buffer, byteCount: Long) {
            source.readUnsafe()
        }

        override suspend fun flush() {
            // Nothing to do
        }

        override suspend fun close() {
            withContext(Dispatchers.IO) {
                channel.close()
            }
        }

        override suspend fun timeout(): Timeout {
            return timeout
        }
    }
}


suspend fun AsynchronousSocketChannel.coRead(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont ->
        read(buffer, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

suspend fun AsynchronousSocketChannel.coWrite(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont ->
        write(buffer, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

suspend fun AsynchronousFileChannel.coRead(buffer: ByteBuffer, position: Long): Int {
    return suspendCancellableCoroutine { cont ->
        read(buffer, position, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

suspend fun AsynchronousFileChannel.coWrite(buffer: ByteBuffer, position: Long): Int {
    return suspendCancellableCoroutine { cont ->
        write(buffer, position, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

internal object ChannelCompletionHandler: CompletionHandler<Int, CancellableContinuation<Int>> {

    override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
        attachment.resumeWithException(exc)
    }
}
