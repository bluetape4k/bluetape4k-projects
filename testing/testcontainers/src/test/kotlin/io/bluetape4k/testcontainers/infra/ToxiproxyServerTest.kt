package io.bluetape4k.testcontainers.infra

import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.Network
import kotlin.system.measureTimeMillis
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [ToxiproxyServer] 테스트입니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToxiproxyServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `create toxiproxy server with default port`() {
            ToxiproxyServer(useDefaultPort = true).use { server ->
                server.start()
                server.isRunning.shouldBeTrue()
                server.port shouldBeEqualTo ToxiproxyServer.CONTROL_PORT
                server.controlPort shouldBeEqualTo ToxiproxyServer.CONTROL_PORT
            }
        }
    }

    @Nested
    inner class UseDockerPort {
        @Test
        fun `create toxiproxy server`() {
            ToxiproxyServer().use { server ->
                server.start()
                server.isRunning.shouldBeTrue()
            }
        }
    }

    @Nested
    inner class WithRedisAndLettuce {
        @Test
        fun `redis upstream 을 프록시하고 lettuce 로 latency toxic 을 검증한다`() {
            Network.newNetwork().use { network ->
                RedisServer()
                    .withNetwork(network)
                    .withNetworkAliases("redis")
                    .use { redis ->
                        ToxiproxyServer()
                            .withNetwork(network)
                            .use { toxiproxy ->
                                redis.start()
                                toxiproxy.start()

                                val toxiproxyClient = ToxiproxyClient(toxiproxy.host, toxiproxy.controlPort)
                                val proxy = toxiproxyClient.createProxy(
                                    "redis-primary",
                                    "0.0.0.0:8666",
                                    "redis:${RedisServer.PORT}",
                                )
                                val proxyPort = toxiproxy.getMappedPort(8666)
                                val redisClient = RedisServer.Launcher.LettuceLib.getRedisClient(toxiproxy.host, proxyPort)

                                try {
                                    redisClient.connect().use { connection ->
                                        val commands = connection.sync()

                                        commands.ping() shouldBeEqualTo "PONG"
                                        commands.set("toxiproxy:key", "value") shouldBeEqualTo "OK"
                                        commands.get("toxiproxy:key") shouldBeEqualTo "value"

                                        val latency = proxy.toxics().latency(
                                            "redis-latency",
                                            ToxicDirection.DOWNSTREAM,
                                            250,
                                        )

                                        val delayedElapsed = measureTimeMillis {
                                            commands.get("toxiproxy:key") shouldBeEqualTo "value"
                                        }
                                        assertTrue(
                                            delayedElapsed >= 150,
                                            "latency toxic 이후 GET 응답은 지연되어야 한다. actual=${delayedElapsed}ms"
                                        )

                                        latency.remove()

                                        val recoveredElapsed = measureTimeMillis {
                                            commands.get("toxiproxy:key") shouldBeEqualTo "value"
                                        }
                                        assertTrue(
                                            recoveredElapsed < delayedElapsed,
                                            "latency toxic 제거 후 응답 시간은 감소해야 한다. delayed=${delayedElapsed}ms, recovered=${recoveredElapsed}ms"
                                        )
                                    }
                                } finally {
                                    runCatching { proxy.delete() }
                                    runCatching { redisClient.shutdown() }
                                }
                            }
                    }
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ToxiproxyServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ToxiproxyServer(tag = " ") }
    }

    @Test
    fun `toxiproxy 서버가 정상적으로 시작된다`() {
        val server = ToxiproxyServer()
        server.start()
        try {
            server.isRunning.shouldBeTrue()
            log.debug { "ToxiproxyServer control port: ${server.controlPort}" }
        } finally {
            server.stop()
        }
    }
}
