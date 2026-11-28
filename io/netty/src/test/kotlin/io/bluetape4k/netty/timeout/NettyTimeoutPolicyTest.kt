package io.bluetape4k.netty.timeout

import io.bluetape4k.assertions.shouldBeEqualTo
import io.netty.channel.ChannelHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.junit.jupiter.api.Test
import reactor.netty.Connection
import java.lang.reflect.Proxy
import java.time.Duration

class NettyTimeoutPolicyTest {

    @Test
    fun `default policy is ten seconds for both directions`() {
        NettyTimeoutPolicy.DEFAULT.readTimeout shouldBeEqualTo Duration.ofSeconds(10)
        NettyTimeoutPolicy.DEFAULT.writeTimeout shouldBeEqualTo Duration.ofSeconds(10)
    }

    @Test
    fun `websocket policy can preserve a thirty second timeout`() {
        NettyTimeoutPolicy(Duration.ofSeconds(30), Duration.ofSeconds(30))
            .readTimeout shouldBeEqualTo Duration.ofSeconds(30)
    }

    @Test
    fun `handler adapter installs read and write timeout handlers`() {
        val handlers = mutableListOf<ChannelHandler>()
        lateinit var connection: Connection
        connection = Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
        ) { _, method, args ->
            when (method.name) {
                "addHandlerLast" -> {
                    handlers += requireNotNull(args).first() as ChannelHandler
                    connection
                }

                else -> error("unexpected Connection method: ${method.name}")
            }
        } as Connection

        NettyTimeoutHandlers.install(
            connection,
            NettyTimeoutPolicy(Duration.ofSeconds(2), Duration.ofSeconds(3)),
        )

        handlers.map { it::class.java } shouldBeEqualTo listOf(
            ReadTimeoutHandler::class.java,
            WriteTimeoutHandler::class.java,
        )
    }

    @Test
    fun `zero and negative timeout are rejected`() {
        assertIllegalArgument { NettyTimeoutPolicy(Duration.ZERO, Duration.ofSeconds(1)) }
        assertIllegalArgument { NettyTimeoutPolicy(Duration.ofSeconds(1), Duration.ofSeconds(-1)) }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
