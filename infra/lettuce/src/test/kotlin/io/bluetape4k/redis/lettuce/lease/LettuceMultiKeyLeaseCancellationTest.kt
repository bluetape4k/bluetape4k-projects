package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.RedisClient
import io.lettuce.core.event.command.CommandListener
import io.lettuce.core.event.command.CommandStartedEvent
import io.lettuce.core.protocol.CommandType
import io.lettuce.core.resource.DefaultClientResources
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail

internal class LettuceMultiKeyLeaseCancellationTest {

    @Test
    fun `future cancellation after dispatch permits full ownership or full absence but never partial state`() {
        val resources = DefaultClientResources.create()
        val redis = LettuceTestUtils.redis
        val client = RedisClient.create(resources, LettuceClients.getRedisURI(redis.host, redis.port))
        val dispatched = CountDownLatch(1)
        val listener = object : CommandListener {
            override fun commandStarted(event: CommandStartedEvent) {
                if (event.command.type == CommandType.EVALSHA || event.command.type == CommandType.EVAL) {
                    dispatched.countDown()
                }
            }
        }

        val tag = LettuceTestUtils.randomName()
        val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
        val token = "owner-$tag"
        try {
            client.connect().use { observerConnection ->
                try {
                    val observerLease = LettuceMultiKeyLease(observerConnection)
                    observerLease.acquire(keys, token, Duration.ofSeconds(5)) shouldBeEqualTo
                        MultiKeyAcquireResult.Acquired
                    observerLease.release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released

                    client.addListener(listener)
                    client.connect().use { commandConnection ->
                        observerConnection.sync().clientPause(SERVER_PAUSE_MILLIS) shouldBeEqualTo "OK"
                        val cancelledWait = LettuceMultiKeyLease(commandConnection)
                            .acquireAsync(keys, token, Duration.ofSeconds(5))

                        dispatched.await(5, TimeUnit.SECONDS).shouldBeTrue()
                        cancelledWait.cancel(true).shouldBeTrue()

                        val commandFence = commandConnection.async().ping()
                        commandFence.get(5, TimeUnit.SECONDS) shouldBeEqualTo "PONG"

                        when (val settled = observerLease.inspect(keys, token)) {
                            is MultiKeyInspectResult.Owned,
                            MultiKeyInspectResult.Lost,
                            -> Unit

                            is MultiKeyInspectResult.PartialOwnership ->
                                fail("Cancellation exposed partial ownership: ${settled.counts}")

                            is MultiKeyInspectResult.Conflicted ->
                                fail("Cancellation exposed conflicting ownership: ${settled.counts}")
                        }
                    }
                } finally {
                    client.removeListener(listener)
                    observerConnection.sync().del(*keys.toTypedArray())
                }
            }
        } finally {
            client.shutdown()
            resources.shutdown()
        }
    }

    private companion object {
        const val SERVER_PAUSE_MILLIS = 500L
    }
}
