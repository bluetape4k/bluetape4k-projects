package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveLatchKeys
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import java.time.Duration

class SynchronizerCancellationTest: AbstractLettuceTest() {

    @Test
    fun `cancelling suspend await preserves count and removes its waiter`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-cancel-${randomName().substringAfter(':')}"
        val keys = deriveLatchKeys(name, LatchConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceSuspendCountDownLatch.create(connection, name)
        try {
            val generation = latch.trySetCount(1, LatchRequestId.random())
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
                .generation
            val waiter = launch {
                latch.await(generation, LatchRequestId.from("cancelled-waiter"), Duration.ofSeconds(5))
            }
            delay(100)
            waiter.cancelAndJoin()

            latch.getCount(generation).shouldBeInstanceOf<LatchCountResult.Active>().count shouldBeEqualTo 1
            latch.delete(generation, LatchRequestId.random()) shouldBeEqualTo LatchMutationResult.Deleted
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `cancellation during waiter registration propagates and cleans any dispatched waiter`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "latch-register-cancel-${randomName().substringAfter(':')}"
        val keys = deriveLatchKeys(name, LatchConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val latch = LettuceSuspendCountDownLatch.create(connection, name)
        try {
            val generation = latch.trySetCount(1, LatchRequestId.from("create"))
                .shouldBeInstanceOf<LatchSetCountResult.Created>()
                .generation
            val waiter = launch(start = CoroutineStart.UNDISPATCHED) {
                latch.await(generation, LatchRequestId.from("registration-cancel"), Duration.ofSeconds(5))
            }
            waiter.cancelAndJoin()
            waiter.isCancelled shouldBeEqualTo true
            latch.delete(generation, LatchRequestId.from("delete")) shouldBeEqualTo LatchMutationResult.Deleted
        } finally {
            latch.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }
}
