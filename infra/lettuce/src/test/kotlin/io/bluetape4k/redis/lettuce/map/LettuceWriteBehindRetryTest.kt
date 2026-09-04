package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.LinkedBlockingDeque
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

internal class LettuceWriteBehindRetryTest: AbstractLettuceTest() {

    @Test
    fun `blocking write-behind는 entry별 retry count를 보존한다`() {
        val prefix = "entry-retry-loaded:${randomName()}"
        val writer = object: MapWriter<String, String> {
            override fun write(map: Map<String, String>) = error("simulated write failure")

            override fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 2,
        )

        LettuceLoadedMap(client = client, writer = writer, config = config).use { map ->
            map.writeBehindQueue().apply {
                add(Triple("retried-key", "retried-value", 2))
                add(Triple("fresh-key", "fresh-value", 0))
            }

            map.flushWriteBehindQueue()

            map.writeBehindQueue().toList() shouldBeEqualTo
                listOf(Triple("fresh-key", "fresh-value", 1))
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo listOf("retried-key")
            }
        }
    }

    @Test
    fun `suspend write-behind는 entry별 retry count를 보존한다`() = runSuspendIO {
        val prefix = "entry-retry-suspended:${randomName()}"
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) = error("simulated write failure")

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val cancelledScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { it.cancel() }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 2,
        )

        LettuceSuspendedLoadedMap(
            client = client,
            writer = writer,
            config = config,
            scope = cancelledScope,
        ).use { map ->
            map.flushBatch(
                listOf(
                    Triple("retried-key", "retried-value", 2),
                    Triple("fresh-key", "fresh-value", 0),
                )
            )

            map.writeBehindChannel().tryReceive().getOrNull() shouldBeEqualTo
                Triple("fresh-key", "fresh-value", 1)
            map.writeBehindChannel().tryReceive().getOrNull().shouldBeNull()
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo listOf("retried-key")
            }
        }
    }

    @Test
    fun `blocking write-behind는 retry queue 포화 시 entry를 dead-letter에 보존한다`() {
        val prefix = "entry-retry-loaded-full:${randomName()}"
        lateinit var loadedMap: LettuceLoadedMap<String, String>
        val writer = object: MapWriter<String, String> {
            override fun write(map: Map<String, String>) {
                loadedMap.writeBehindQueue().add(Triple("queue-blocker", "blocker-value", 0))
                error("simulated write failure")
            }

            override fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 1,
            writeBehindQueueCapacity = 1,
        )

        loadedMap = LettuceLoadedMap(client = client, writer = writer, config = config)
        loadedMap.use { map ->
            map.writeBehindQueue().add(Triple("failed-key", "failed-value", 0))

            map.flushWriteBehindQueue()

            map.writeBehindQueue().toList() shouldBeEqualTo
                listOf(Triple("queue-blocker", "blocker-value", 0))
            map.writeBehindQueue().clear()
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo listOf("failed-key")
            }
            client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
                connection.sync().hget("$prefix:dead-letter:values", "failed-key") shouldBeEqualTo "failed-value"
            }
        }
    }

    @Test
    fun `suspend write-behind는 retry channel 포화 시 entry를 dead-letter에 보존한다`() = runSuspendIO {
        val prefix = "entry-retry-suspended-full:${randomName()}"
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) = error("simulated write failure")

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val cancelledScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { it.cancel() }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 1,
            writeBehindQueueCapacity = 1,
        )

        LettuceSuspendedLoadedMap(
            client = client,
            writer = writer,
            config = config,
            scope = cancelledScope,
        ).use { map ->
            map.writeBehindChannel().trySend(Triple("channel-blocker", "blocker-value", 0)).isSuccess shouldBeEqualTo true

            map.flushBatch(listOf(Triple("failed-key", "failed-value", 0)))

            map.writeBehindChannel().tryReceive().getOrNull() shouldBeEqualTo
                Triple("channel-blocker", "blocker-value", 0)
            map.writeBehindChannel().tryReceive().getOrNull().shouldBeNull()
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo listOf("failed-key")
            }
            client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
                connection.sync().hget("$prefix:dead-letter:values", "failed-key") shouldBeEqualTo "failed-value"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun LettuceLoadedMap<String, String>.writeBehindQueue(): LinkedBlockingDeque<Triple<String, String, Int>> =
        javaClass.getDeclaredField("writeBehindQueue")
            .apply { isAccessible = true }
            .get(this) as LinkedBlockingDeque<Triple<String, String, Int>>

    private fun LettuceLoadedMap<String, String>.flushWriteBehindQueue() {
        javaClass.getDeclaredMethod("flushWriteBehindQueue")
            .apply { isAccessible = true }
            .invoke(this)
    }

    @Suppress("UNCHECKED_CAST")
    private fun LettuceSuspendedLoadedMap<String, String>.writeBehindChannel(): Channel<Triple<String, String, Int>> =
        javaClass.getDeclaredField("writeBehindChannel")
            .apply { isAccessible = true }
            .get(this) as Channel<Triple<String, String, Int>>

    private suspend fun LettuceSuspendedLoadedMap<String, String>.flushBatch(
        entries: List<Triple<String, String, Int>>,
    ) {
        val method = this::class.declaredMemberFunctions.single { it.name == "flushBatch" }
        method.isAccessible = true
        method.callSuspend(this, entries)
    }
}
