package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveLatchKeys
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class LettuceCountDownLatchTest: AbstractLettuceTest() {

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
            var waiters = 0
            repeat(40) {
                waiters = latch.inspect(generation).shouldBeInstanceOf<LatchCountResult.Active>().waiters
                if (waiters == 1) return@repeat
                Thread.sleep(5)
            }
            waiters shouldBeEqualTo 1
            latch.delete(generation, LatchRequestId.from("blocked-delete")) shouldBeEqualTo
                LatchMutationResult.ActiveWaiters(1)
            latch.await(generation, LatchRequestId.from("wait-2"), Duration.ofMillis(100)) shouldBeEqualTo
                LatchAwaitResult.CapacityExceeded

            first.cancel(false)
            runCatching { first.get(2, TimeUnit.SECONDS) }
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
            repeat(40) {
                if (latch.inspect(second.generation).shouldBeInstanceOf<LatchCountResult.Active>().waiters == 0) {
                    Thread.sleep(5)
                }
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
            runCatching { current.get(2, TimeUnit.SECONDS) }
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
            val futureGeneration = future.trySetCountAsync(2, LatchRequestId.from("future-create")).get()
                .shouldBeInstanceOf<LatchSetCountResult.Created>().generation
            future.getCountAsync(futureGeneration).get()
                .shouldBeInstanceOf<LatchCountResult.Active>().count shouldBeEqualTo 2
            val awaited = future.awaitAsync(
                futureGeneration,
                LatchRequestId.from("future-await"),
                Duration.ofSeconds(2),
            )
            future.countDownAsync(futureGeneration, LatchRequestId.from("future-down-1")).get()
                .shouldBeInstanceOf<LatchMutationResult.Decremented>().remaining shouldBeEqualTo 1
            future.countDownAsync(futureGeneration, LatchRequestId.from("future-down-2")).get() shouldBeEqualTo
                LatchMutationResult.Completed
            awaited.get(2, TimeUnit.SECONDS) shouldBeEqualTo LatchAwaitResult.Completed
            future.inspectAsync(futureGeneration).get()
                .shouldBeInstanceOf<LatchCountResult.Completed>()
            future.deleteAsync(futureGeneration, LatchRequestId.from("future-delete")).get() shouldBeEqualTo
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
