package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveLatchKeys
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test

class SynchronizerProtocolTest {

    @Test
    fun `all semaphore and latch keys share their object slot`() {
        val semaphore = deriveSemaphoreKeys("orders", SemaphoreConfig(), StringCodec.UTF8)
        val latch = deriveLatchKeys("orders", LatchConfig(), StringCodec.UTF8)

        semaphore.all.map { SlotHash.getSlot(StringCodec.UTF8.encodeKey(it)) }.distinct().size shouldBeEqualTo 1
        latch.all.map { SlotHash.getSlot(StringCodec.UTF8.encodeKey(it)) }.distinct().size shouldBeEqualTo 1
    }

    @Test
    fun `latch delete preserves the generation counter`() {
        LatchScripts.DELETE_SCRIPT.source shouldContain "redis.call('del', KEYS[1], KEYS[3]"
        LatchScripts.DELETE_SCRIPT.source shouldNotContain "redis.call('del', KEYS[1], KEYS[2], KEYS[3]"
    }

    @Test
    fun `expirable cleanup restores one allocation total exactly once`() {
        SynchronizerScripts.EXPIRABLE_CLEANUP_SCRIPT.source shouldContain "allocationPermits"
        SynchronizerScripts.EXPIRABLE_CLEANUP_SCRIPT.source shouldContain "hdel"
        SynchronizerScripts.EXPIRABLE_CLEANUP_SCRIPT.source shouldNotContain "permitCount * permitCount"
    }
}
