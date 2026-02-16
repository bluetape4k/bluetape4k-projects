package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.test.assertFailsWith

class SuspendedSocketChannelTest: AbstractOkioTest() {

    companion object: KLoggingChannel() {
        private const val DEFAULT_TIMEOUT_MS = 25_000L
    }

    @Test
    fun `use suspended async socket channel`() = runAsyncSocketTest { client, server ->
        val clientSource = client.asSuspendedSource().buffered()
        val clientSink = client.asSuspendedSink().buffered()

        val serverSource = server.asSuspendedSource().buffered()
        val serverSink = server.asSuspendedSink().buffered()

        val message = "a".repeat((SEGMENT_SIZE * 3 + 17).toInt())
        clientSink.writeUtf8(message).flush()
        serverSource.request(message.length.toLong())
        serverSource.readUtf8(message.length.toLong()) shouldBeEqualTo message

        val reply = "b".repeat((SEGMENT_SIZE * 2 + 3).toInt())
        serverSink.writeUtf8(reply).flush()
        clientSource.request(reply.length.toLong())
        clientSource.readUtf8(reply.length.toLong()) shouldBeEqualTo reply
    }

    @Test
    fun `read until eof async socket channel`() = runAsyncSocketTest { client, server ->
        val serverSink = client.asSuspendedSink().buffered()
        val clientSource = server.asSuspendedSource().buffered()

        val message = Fakers.randomString()
        serverSink.writeUtf8(message)
        serverSink.close()

        clientSource.readUtf8() shouldBeEqualTo message
    }

    @Test
    fun `suspended source read with negative byteCount throws`() = runAsyncSocketTest { _, server ->
        val source = server.asSuspendedSource()
        assertFailsWith<IllegalArgumentException> {
            source.read(okio.Buffer(), -1L)
        }
    }

    private fun runAsyncSocketTest(
        block: suspend (client: AsynchronousSocketChannel, server: AsynchronousSocketChannel) -> Unit,
    ) = runSuspendTest {
        kotlinx.coroutines.withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
            AsynchronousServerSocketChannel.open().use { serverSocketChannel ->
                serverSocketChannel.bind(InetSocketAddress("127.0.0.1", 0))
                val address = serverSocketChannel.localAddress as InetSocketAddress

                AsynchronousSocketChannel.open().use { client ->
                    client.connect(address).awaitSuspending()
                    val server = serverSocketChannel.accept().awaitSuspending()
                    server.use {
                        block(client, server)
                    }
                }
            }
        } ?: fail("test timeout")
    }
}
