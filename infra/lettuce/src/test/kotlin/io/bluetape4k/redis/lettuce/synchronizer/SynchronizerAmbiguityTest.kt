package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class SynchronizerAmbiguityTest {

    @Test
    fun `post dispatch timeout returns request-bound ambiguous outcomes`() {
        val connection = mockk<StatefulRedisConnection<String, String>>()
        val sync = mockk<RedisCommands<String, String>>()
        val async = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        every { connection.codec } returns StringCodec.UTF8
        every { connection.sync() } returns sync
        every { connection.async() } returns async
        every {
            sync.evalsha<List<String>>(
                any<String>(),
                any<io.lettuce.core.ScriptOutputType>(),
                any<Array<String>>(),
                *anyVararg<String>(),
            )
        } throws RedisCommandTimeoutException("post-dispatch timeout")

        val owner = SemaphoreOwnerId.from("owner")
        val request = SemaphoreRequestId.from("request")
        val semaphore = LettuceDistributedSemaphore.create(connection, "ambiguous-semaphore")
        val expirable = LettucePermitExpirableSemaphore.create(connection, "ambiguous-expirable")
        val latch = LettuceCountDownLatch.create(connection, "ambiguous-latch")
        try {
            semaphore.tryAcquire(owner, request)
                .shouldBeInstanceOf<PermitAcquireResult.Ambiguous>()
            val keys = deriveSemaphoreKeys("ambiguous-semaphore", SemaphoreConfig(), StringCodec.UTF8)
            semaphore.release(
                PermitHandle(keys.fingerprint, owner, 1, request, 1, "token"),
            ).shouldBeInstanceOf<PermitMutationResult.Ambiguous>()
            expirable.renew(
                ExpirablePermitHandle(
                    PermitHandle(
                        deriveSemaphoreKeys("ambiguous-expirable", SemaphoreConfig(), StringCodec.UTF8).fingerprint,
                        owner,
                        1,
                        request,
                        1,
                        "allocation",
                    ),
                    listOf(ExpirablePermitLease("lease", 1)),
                ),
                java.time.Duration.ofSeconds(1),
            ).shouldBeInstanceOf<PermitRenewResult.Ambiguous>()
            latch.countDown(LatchGeneration(1), LatchRequestId.from("latch-request"))
                .shouldBeInstanceOf<LatchMutationResult.Ambiguous>()
            latch.await(
                LatchGeneration(1),
                LatchRequestId.from("latch-await-request"),
                java.time.Duration.ofSeconds(1),
            ).shouldBeInstanceOf<LatchAwaitResult.Ambiguous>()
        } finally {
            semaphore.close()
            expirable.close()
            latch.close()
        }
    }
}
