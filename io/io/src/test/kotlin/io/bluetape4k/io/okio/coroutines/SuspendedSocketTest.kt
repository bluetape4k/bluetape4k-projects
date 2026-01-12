package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class SuspendedSocketTest: AbstractOkioTest() {

    companion object: KLoggingChannel() {
        private const val DEFAULT_TIMEOUT_MS = 25_000L
    }

    @Test
    fun `use suspend socket`() = runSocketTest { client, server ->
        val clientSource = client.asSuspendedSource().buffered()
        val clientSink = client.asSuspendedSink().buffered()

        val serverSource = server.asSuspendedSource().buffered()
        val serverSink = server.asSuspendedSink().buffered()

        clientSink.writeUtf8("abc").flush()
        serverSource.request(3)
        serverSource.readUtf8(3) shouldBeEqualTo "abc"

        serverSink.writeUtf8("def").flush()
        clientSource.request(3)
        clientSource.readUtf8(3) shouldBeEqualTo "def"
    }

    @Test
    fun `read until eof`() = runSocketTest { client, server ->
        val serverSink = client.asSuspendedSink().buffered()
        val clientSource = server.asSuspendedSource().buffered()

        val message = Fakers.randomString()
        serverSink.writeUtf8(message)
        serverSink.close()

        clientSource.readUtf8() shouldBeEqualTo message
    }

    @Test
    fun `read fails because the socket is already closed`() = runSocketTest { _, server ->
        val serverSource = server.asSuspendedSource().buffered()
        server.close()

        assertFailsWith<IOException> {
            serverSource.readUtf8()
        }
    }

    @Test
    fun `write fails because the socket is already closed`() = runSocketTest { _, server ->
        val serverSink = server.asSuspendedSink().buffered()
        server.close()

        assertFailsWith<IOException> {
            serverSink.writeUtf8(Fakers.randomString())
            serverSink.flush()
        }
    }

    @Test
    fun `blocked read fails due to close`() = runSocketTest { _, server ->
        val serverSource = server.asSuspendedSource().buffered()

        coroutineScope {
            launch {
                delay(500)
                server.close()
            }

            assertFailsWith<IOException> {
                serverSource.request(1L)
            }
        }
    }

    @Test
    fun `blocked write fails due to close`() = runSocketTest { client, server ->
        val clientSink = client.asSuspendedSink().buffered()

        coroutineScope {
            launch {
                delay(500)
                server.close()
            }

            assertFailsWith<IOException> {
                while (true) {
                    clientSink.writeUtf8(Fakers.randomString())
                }
            }
        }
    }

    private fun runSocketTest(block: suspend (client: Socket, server: Socket) -> Unit) = runSuspendTest {
        withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
            ServerSocketChannel.open().use { serverSocketChannel ->
                val serverSocket = serverSocketChannel.socket()
                serverSocket.reuseAddress = true
                serverSocketChannel.bind(InetSocketAddress(0))
                serverSocketChannel.configureBlocking(false)

                SocketChannel.open().use { clientChannel ->
                    clientChannel.configureBlocking(false)
                    clientChannel.connect(InetSocketAddress(serverSocket.inetAddress, serverSocket.localPort))
                    clientChannel.await(SelectionKey.OP_CONNECT)
                    if (!clientChannel.finishConnect()) throw IOException("connect failed")

                    clientChannel.socket().use { client ->
                        serverSocketChannel.await(SelectionKey.OP_ACCEPT)
                        val serverChannel = serverSocketChannel.accept()
                        serverChannel.configureBlocking(false)

                        serverChannel.socket().use { server ->
                            block(client, server)
                        }
                    }
                }
            }
        } ?: fail("test timeout")
    }
}
