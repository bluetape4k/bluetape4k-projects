package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.SEGMENT_SIZE
import io.bluetape4k.io.okio.coroutines.internal.await
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import okio.Timeout
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

fun Socket.asAsyncSource(): AsyncSource {
    val channel = this.channel!!

    return object: AsyncSource {
        val timeout = Timeout()
        val buffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

        override suspend fun read(sink: Buffer, byteCount: Long): Long = coroutineScope {
            channel.await(SelectionKey.OP_READ)

            buffer.clear()
            buffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())
            val read = channel.read(buffer)
            buffer.flip()

            if (read > 0) sink.write(buffer)

            read.toLong()
        }

        override suspend fun close() = coroutineScope {
            channel.close()
        }

        override suspend fun timeout(): Timeout {
            return timeout
        }
    }
}

fun Socket.asAsyncSink(): AsyncSink {
    val channel = this.channel!!

    return object: AsyncSink {
        val timeout = Timeout()
        val cursor = Buffer.UnsafeCursor()

        override suspend fun write(source: Buffer, byteCount: Long) {
            channel.await(SelectionKey.OP_WRITE)

            source.readUnsafe(cursor).use { cur ->
                var remaining = byteCount
                while (remaining > 0) {
                    cur.seek(0)
                    val length = minOf(cur.end - cur.start, remaining.toInt())

                    val written = channel.write(ByteBuffer.wrap(cur.data, cur.start, length))
                    if (written == 0) channel.await(SelectionKey.OP_WRITE)

                    remaining -= written
                    source.skip(written.toLong())
                }
            }
        }

        override suspend fun flush() {
            // Nothing to do
        }

        override suspend fun close() = coroutineScope {
            channel.close()
        }

        override suspend fun timeout(): Timeout {
            return timeout
        }
    }
}
