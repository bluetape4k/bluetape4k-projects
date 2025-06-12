package io.bluetape4k.io.okio.coroutines

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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [AsynchronousFileChannel]을 [SuspendSink]로 변환합니다.
 */
fun AsynchronousSocketChannel.asSuspendSink(coroutineContext: CoroutineContext = Dispatchers.IO): SuspendSink {
    val channel = this

    return object: SuspendSink, KLoggingChannel() {
        val cursor = Buffer.UnsafeCursor()
        val timeout = Timeout.NONE

        override suspend fun write(source: Buffer, byteCount: Long) {
            source.readUnsafe()
        }

        override suspend fun flush() {
            // Nothing to do
        }

        override suspend fun close() {
            withContext(coroutineContext) {
                channel.close()
            }
        }

        override suspend fun timeout(): Timeout {
            return timeout
        }
    }
}

suspend fun AsynchronousSocketChannel.suspendRead(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont ->
        read(buffer, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

suspend fun AsynchronousFileChannel.suspendRead(buffer: ByteBuffer, position: Long): Int {
    return suspendCancellableCoroutine { cont ->
        read(buffer, position, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

suspend fun AsynchronousSocketChannel.suspendWrite(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont ->
        write(buffer, cont, ChannelCompletionHandler)
        cont.invokeOnCancellation { close() }
    }
}

suspend fun AsynchronousFileChannel.suspendWrite(buffer: ByteBuffer, position: Long): Int {
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
