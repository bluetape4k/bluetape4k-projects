package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveLatchKeys
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

class LettuceCountDownLatchTest: AbstractLettuceTest() {

    @Test
    fun `zero count and stale generation preserve lifecycle`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-boundaries-${randomName().substringAfter(':')}"
        val config = LatchConfig(maxCount = 2)
        val keys = deriveLatchKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceCountDownLatch.create(connection, name, config)
        val suspending = LettuceSuspendCountDownLatch.create(connection, name, config)
        try {
            latch.trySetCount(-1, LatchRequestId.random()) shouldBeEqualTo LatchSetCountResult.InvalidCount
            latch.trySetCountAsync(3, LatchRequestId.random()).get(5, TimeUnit.SECONDS) shouldBeEqualTo
                LatchSetCountResult.InvalidCount

            val completed = latch.trySetCount(0, LatchRequestId.from("zero-count"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
            latch.getCount(completed.generation)
                .shouldBeInstanceOf<LatchCountResult.Completed>()
            latch.await(completed.generation, LatchRequestId.from("zero-await"), Duration.ofMillis(20)) shouldBeEqualTo
                LatchAwaitResult.Completed
            latch.countDown(completed.generation, LatchRequestId.from("zero-down")) shouldBeEqualTo
                LatchMutationResult.AlreadyCompleted
            latch.delete(completed.generation, LatchRequestId.from("zero-delete")) shouldBeEqualTo
                LatchMutationResult.Deleted

            val active = latch.trySetCount(1, LatchRequestId.from("active-count"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
            latch.await(active.generation, LatchRequestId.from("sync-timeout"), Duration.ofMillis(30)) shouldBeEqualTo
                LatchAwaitResult.TimedOut
            latch.awaitAsync(
                active.generation,
                LatchRequestId.from("async-timeout"),
                Duration.ofMillis(30),
            ).get(5, TimeUnit.SECONDS) shouldBeEqualTo LatchAwaitResult.TimedOut
            suspending.await(
                active.generation,
                LatchRequestId.from("suspend-timeout"),
                Duration.ofMillis(30),
            ) shouldBeEqualTo LatchAwaitResult.TimedOut

            val stale = LatchGeneration(active.generation.value + 1)
            latch.getCount(stale) shouldBeEqualTo LatchCountResult.StaleGeneration
            latch.getCountAsync(stale).get(5, TimeUnit.SECONDS) shouldBeEqualTo LatchCountResult.StaleGeneration
            latch.await(stale, LatchRequestId.from("stale-await"), Duration.ofMillis(20)) shouldBeEqualTo
                LatchAwaitResult.StaleGeneration
            latch.awaitAsync(
                stale,
                LatchRequestId.from("stale-await-async"),
                Duration.ofMillis(20),
            ).get(5, TimeUnit.SECONDS) shouldBeEqualTo
                LatchAwaitResult.StaleGeneration
            latch.countDown(stale, LatchRequestId.from("stale-down")) shouldBeEqualTo
                LatchMutationResult.StaleGeneration
            latch.delete(stale, LatchRequestId.from("stale-delete")) shouldBeEqualTo
                LatchMutationResult.StaleGeneration

            latch.countDown(active.generation, LatchRequestId.from("complete")) shouldBeEqualTo
                LatchMutationResult.Completed
            latch.delete(active.generation, LatchRequestId.from("delete")) shouldBeEqualTo
                LatchMutationResult.Deleted

            latch.close()
            latch.trySetCount(1, LatchRequestId.random()) shouldBeEqualTo LatchSetCountResult.Closed
            latch.getCount(active.generation) shouldBeEqualTo LatchCountResult.Closed
            latch.getCountAsync(active.generation).get(5, TimeUnit.SECONDS) shouldBeEqualTo LatchCountResult.Closed
            latch.await(active.generation, LatchRequestId.random(), Duration.ofMillis(20)) shouldBeEqualTo
                LatchAwaitResult.Closed
            latch.delete(active.generation, LatchRequestId.random()) shouldBeEqualTo LatchMutationResult.Closed
        } finally {
            suspending.close()
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `generation increases across delete and recreate while count stays at zero`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-${randomName().substringAfter(':')}"
        val keys = deriveLatchKeys(name, LatchConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceCountDownLatch.create(connection, name)
        try {
            val first = latch.trySetCount(2, LatchRequestId.from("set-1"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
            latch.countDown(first.generation, LatchRequestId.from("down-1"))
                .shouldBeInstanceOf<LatchMutationResult.Decremented>()
                .remaining shouldBeEqualTo 1
            latch.countDown(first.generation, LatchRequestId.from("down-2")) shouldBeEqualTo
                LatchMutationResult.Completed
            latch.countDown(first.generation, LatchRequestId.from("down-3")) shouldBeEqualTo
                LatchMutationResult.AlreadyCompleted
            latch.await(first.generation, LatchRequestId.from("await"), Duration.ofMillis(100)) shouldBeEqualTo
                LatchAwaitResult.Completed
            latch.delete(first.generation, LatchRequestId.from("delete")) shouldBeEqualTo
                LatchMutationResult.Deleted
            val second = latch.trySetCount(1, LatchRequestId.from("set-2"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
            (second.generation.value > first.generation.value) shouldBeEqualTo true
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `generation bound results preserve completed stale and not found states`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-contract-${randomName().substringAfter(':')}"
        val keys = deriveLatchKeys(name, LatchConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceCountDownLatch.create(connection, name)
        try {
            val request = LatchRequestId.from("create")
            val created = latch.trySetCount(1, request).shouldBeInstanceOf<LatchSetCountResult.Created>()
            latch.trySetCount(9, LatchRequestId.from("other"))
                .shouldBeInstanceOf<LatchSetCountResult.ActiveGeneration>()
                .count shouldBeEqualTo 1
            latch.trySetCount(1, request).shouldBeInstanceOf<LatchSetCountResult.Created>()

            latch.getCount(created.generation).shouldBeInstanceOf<LatchCountResult.Active>().waiters shouldBeEqualTo 0
            latch.countDown(created.generation, LatchRequestId.from("down")) shouldBeEqualTo
                LatchMutationResult.Completed
            latch.inspect(created.generation).shouldBeInstanceOf<LatchCountResult.Completed>()
            latch.getCount(LatchGeneration(created.generation.value + 1)) shouldBeEqualTo
                LatchCountResult.StaleGeneration
            latch.delete(created.generation, LatchRequestId.from("delete")) shouldBeEqualTo
                LatchMutationResult.Deleted
            latch.delete(created.generation, LatchRequestId.from("unknown-delete")) shouldBeEqualTo
                LatchMutationResult.NotFound
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `waiter cap fails closed and async completion waits for durable cleanup`() {
        val config = LatchConfig(maxWaiters = 1, waiterCleanupGrace = Duration.ofMillis(50))
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-waiters-${randomName().substringAfter(':')}"
        val keys = deriveLatchKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceCountDownLatch.create(connection, name, config)
        try {
            val generation = latch.trySetCount(1, LatchRequestId.from("create"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
                .generation
            val first = latch.awaitAsync(generation, LatchRequestId.from("wait-1"), Duration.ofSeconds(2))
            await atMost Duration.ofSeconds(2) until {
                (latch.inspect(generation) as? LatchCountResult.Active)?.waiters == 1
            }
            latch.inspect(generation).shouldBeInstanceOf<LatchCountResult.Active>().waiters shouldBeEqualTo 1
            latch.delete(generation, LatchRequestId.from("blocked-delete")) shouldBeEqualTo
                LatchMutationResult.ActiveWaiters(1)
            latch.await(generation, LatchRequestId.from("wait-2"), Duration.ofMillis(100)) shouldBeEqualTo
                LatchAwaitResult.CapacityExceeded

            first.cancel(false)
            assertFailsWith<CancellationException> { first.get(2, TimeUnit.SECONDS) }
            first.isDone shouldBeEqualTo true
            latch.delete(generation, LatchRequestId.from("delete")) shouldBeEqualTo LatchMutationResult.Deleted
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `delete removes an expired waiter left by a failed client cleanup`() {
        val config = LatchConfig(waiterCleanupGrace = Duration.ofMillis(10))
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-stale-waiter-${randomName().substringAfter(':')}"
        val keys = deriveLatchKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceCountDownLatch.create(connection, name, config)
        try {
            val generation = latch.trySetCount(1, LatchRequestId.from("create"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
                .generation
            connection.sync().zadd(keys.waiters, 1.0, "abandoned")
            latch.delete(generation, LatchRequestId.from("delete")) shouldBeEqualTo LatchMutationResult.Deleted
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `late cleanup from an old generation cannot remove a current waiter`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-generation-waiter-${randomName().substringAfter(':')}"
        val config = LatchConfig()
        val keys = deriveLatchKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceCountDownLatch.create(connection, name, config)
        try {
            val first = latch.trySetCount(1, LatchRequestId.from("create-1"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
            latch.delete(first.generation, LatchRequestId.from("delete-1")) shouldBeEqualTo
                LatchMutationResult.Deleted
            val second = latch.trySetCount(1, LatchRequestId.from("create-2"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
            val reusedRequest = LatchRequestId.from("reused-waiter")
            val current = latch.awaitAsync(second.generation, reusedRequest, Duration.ofSeconds(2))
            await atMost Duration.ofSeconds(2) until {
                (latch.inspect(second.generation) as? LatchCountResult.Active)?.waiters == 1
            }

            RedisScriptRunner.run<List<String>>(
                connection.sync(),
                LatchScripts.UNREGISTER_WAITER_SCRIPT,
                ScriptOutputType.MULTI,
                keys.array,
                first.generation.value.toString(),
                reusedRequest.value,
            )
            latch.inspect(second.generation).shouldBeInstanceOf<LatchCountResult.Active>().waiters shouldBeEqualTo 1

            current.cancel(false)
            assertFailsWith<CancellationException> { current.get(2, TimeUnit.SECONDS) }
            latch.delete(second.generation, LatchRequestId.from("delete-2")) shouldBeEqualTo
                LatchMutationResult.Deleted
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `async and suspend modes preserve the generation result matrix`() = runTest {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-modes-${randomName().substringAfter(':')}"
        val config = LatchConfig()
        val keys = deriveLatchKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val future = LettuceCountDownLatch.create(connection, name, config)
        val suspending = LettuceSuspendCountDownLatch.create(connection, name, config)
        try {
            val futureGeneration = future.trySetCountAsync(2, LatchRequestId.from("future-create"))
                .get(5, TimeUnit.SECONDS)
                .shouldBeInstanceOf<LatchSetCountResult.Created>().generation
            future.getCountAsync(futureGeneration).get(5, TimeUnit.SECONDS)
                .shouldBeInstanceOf<LatchCountResult.Active>().count shouldBeEqualTo 2
            val awaited = future.awaitAsync(
                futureGeneration,
                LatchRequestId.from("future-await"),
                Duration.ofSeconds(2),
            )
            future.countDownAsync(futureGeneration, LatchRequestId.from("future-down-1")).get(5, TimeUnit.SECONDS)
                .shouldBeInstanceOf<LatchMutationResult.Decremented>().remaining shouldBeEqualTo 1
            future.countDownAsync(futureGeneration, LatchRequestId.from("future-down-2")).get(5, TimeUnit.SECONDS) shouldBeEqualTo
                LatchMutationResult.Completed
            awaited.get(2, TimeUnit.SECONDS) shouldBeEqualTo LatchAwaitResult.Completed
            future.inspectAsync(futureGeneration).get(5, TimeUnit.SECONDS)
                .shouldBeInstanceOf<LatchCountResult.Completed>()
            future.deleteAsync(futureGeneration, LatchRequestId.from("future-delete")).get(5, TimeUnit.SECONDS) shouldBeEqualTo
                LatchMutationResult.Deleted

            val suspendGeneration = suspending.trySetCount(1, LatchRequestId.from("suspend-create"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>().generation
            suspending.getCount(suspendGeneration)
                .shouldBeInstanceOf<LatchCountResult.Active>().count shouldBeEqualTo 1
            suspending.countDown(suspendGeneration, LatchRequestId.from("suspend-down")) shouldBeEqualTo
                LatchMutationResult.Completed
            suspending.await(
                suspendGeneration,
                LatchRequestId.from("suspend-await"),
                Duration.ofSeconds(1),
            ) shouldBeEqualTo LatchAwaitResult.Completed
            suspending.inspect(suspendGeneration)
                .shouldBeInstanceOf<LatchCountResult.Completed>()
            suspending.delete(suspendGeneration, LatchRequestId.from("suspend-delete")) shouldBeEqualTo
                LatchMutationResult.Deleted
        } finally {
            future.close()
            suspending.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }
}
