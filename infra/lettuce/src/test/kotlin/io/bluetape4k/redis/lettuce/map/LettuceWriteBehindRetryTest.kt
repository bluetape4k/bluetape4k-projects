package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

internal class LettuceWriteBehindRetryTest: AbstractLettuceTest() {

    @Test
    fun `blocking write-behind는 실패 후에도 동일 키의 최신 값을 유지한다`() {
        val writes = mutableListOf<Map<String, String>>()
        val attempts = AtomicInteger()
        val writer = object: MapWriter<String, String> {
            override fun write(map: Map<String, String>) {
                writes += map
                if (attempts.getAndIncrement() == 0) {
                    error("simulated write failure")
                }
            }

            override fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = "latest-write-wins:${randomName()}",
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 2,
        )

        LettuceLoadedMap(client = client, writer = writer, config = config).use { map ->
            map.writeBehindQueue().apply {
                add(Triple("same-key", "old-value", 0))
                add(Triple("same-key", "latest-value", 0))
            }

            map.flushWriteBehindQueue()
            map.flushWriteBehindQueue()

            writes shouldBeEqualTo listOf(
                mapOf("same-key" to "latest-value"),
                mapOf("same-key" to "latest-value"),
            )
        }
    }

    @Test
    fun `blocking write-behind는 재시도 중 도착한 최신 값을 순서대로 처리한다`() {
        val writes = mutableListOf<Map<String, String>>()
        val attempts = AtomicInteger()
        lateinit var loadedMap: LettuceLoadedMap<String, String>
        val writer = object: MapWriter<String, String> {
            override fun write(map: Map<String, String>) {
                writes += map
                if (attempts.getAndIncrement() == 0) {
                    loadedMap.writeBehindQueue().add(Triple("same-key", "newest-value", 0))
                    error("simulated write failure")
                }
            }

            override fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = "concurrent-latest-write-wins:${randomName()}",
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 2,
        )

        loadedMap = LettuceLoadedMap(client = client, writer = writer, config = config)
        loadedMap.use { map ->
            map.writeBehindQueue().apply {
                add(Triple("same-key", "old-value", 0))
                add(Triple("same-key", "latest-retried-value", 0))
            }

            map.flushWriteBehindQueue()
            map.flushWriteBehindQueue()
            map.flushWriteBehindQueue()

            writes shouldBeEqualTo listOf(
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "newest-value"),
            )
        }
    }

    @Test
    fun `blocking write-behind는 retry 소진 시 동일 키의 최신 값을 dead-letter에 보존한다`() {
        val prefix = "latest-dead-letter:${randomName()}"
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
                add(Triple("same-key", "old-value", 2))
                add(Triple("same-key", "latest-value", 2))
            }

            map.flushWriteBehindQueue()

            map.writeBehindQueue().toList() shouldBeEqualTo emptyList()
            client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
                connection.sync().hget("$prefix:dead-letter:values", "same-key") shouldBeEqualTo "latest-value"
            }
        }
    }

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

            map.writeBehindRetryQueue().removeFirstOrNull() shouldBeEqualTo
                Triple("fresh-key", "fresh-value", 1)
            map.writeBehindRetryQueue().removeFirstOrNull().shouldBeNull()
            map.writeBehindChannel().tryReceive().getOrNull().shouldBeNull()
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo listOf("retried-key")
            }
        }
    }

    @Test
    fun `suspend write-behind는 동일 키 최신 값의 retry count만 적용한다`() = runSuspendIO {
        val prefix = "entry-retry-suspended-latest:${randomName()}"
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
                    Triple("same-key", "old-value", 2),
                    Triple("same-key", "latest-value", 0),
                )
            )

            map.writeBehindRetryQueue().toList() shouldBeEqualTo
                listOf(Triple("same-key", "latest-value", 1))
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo emptyList()
            }
            client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
                connection.sync().hget("$prefix:dead-letter:values", "same-key").shouldBeNull()
            }
        }
    }

    @Test
    fun `suspend write-behind는 실패 중 도착한 최신 값을 재시도 뒤에 처리한다`() = runTest {
        val writes = mutableListOf<Map<String, String>>()
        val attempts = AtomicInteger()
        lateinit var suspendedMap: LettuceSuspendedLoadedMap<String, String>
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                writes += map
                if (attempts.getAndIncrement() == 0) {
                    suspendedMap.writeBehindChannel()
                        .trySend(Triple("same-key", "newest-value", 0))
                        .isSuccess shouldBeEqualTo true
                    error("simulated write failure")
                }
            }

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val consumerScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = "suspended-latest-write-wins:${randomName()}",
            writeBehindDelay = Duration.ofMillis(1),
            writeBehindBatchSize = 2,
        )

        suspendedMap = LettuceSuspendedLoadedMap(
            client = client,
            writer = writer,
            config = config,
            scope = consumerScope,
        )
        try {
            suspendedMap.writeBehindChannel()
                .trySend(Triple("same-key", "latest-retried-value", 0))
                .isSuccess shouldBeEqualTo true

            runCurrent()
            advanceUntilIdle()

            writes shouldBeEqualTo listOf(
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "newest-value"),
            )
        } finally {
            suspendedMap.suspendClose()
            consumerScope.cancel()
        }
    }

    @Test
    fun `suspend write-behind는 retry 소진 뒤 도착한 최신 값을 처리한다`() = runSuspendIO {
        val prefix = "suspended-exhausted-latest:${randomName()}"
        val writes = mutableListOf<Map<String, String>>()
        val latestWritten = CompletableDeferred<Unit>()
        val attempts = AtomicInteger()
        lateinit var suspendedMap: LettuceSuspendedLoadedMap<String, String>
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                writes += map
                val attempt = attempts.incrementAndGet()
                if (attempt == 1) {
                    suspendedMap.writeBehindChannel()
                        .trySend(Triple("same-key", "newest-value", 0))
                        .isSuccess shouldBeEqualTo true
                }
                if (attempt <= 3) {
                    error("simulated write failure")
                }
                latestWritten.complete(Unit)
            }

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofMillis(1),
            writeBehindBatchSize = 2,
        )
        val executor = Executors.newSingleThreadExecutor()
        val consumerDispatcher = executor.asCoroutineDispatcher()
        val blockerStarted = CountDownLatch(1)
        val releaseConsumer = CountDownLatch(1)
        executor.submit {
            blockerStarted.countDown()
            releaseConsumer.await()
        }
        blockerStarted.await()
        val consumerScope = CoroutineScope(SupervisorJob() + consumerDispatcher)

        suspendedMap = LettuceSuspendedLoadedMap(
            client = client,
            writer = writer,
            config = config,
            scope = consumerScope,
        )
        try {
            suspendedMap.writeBehindChannel().apply {
                trySend(Triple("same-key", "old-value", 0)).isSuccess shouldBeEqualTo true
                trySend(Triple("same-key", "latest-retried-value", 0)).isSuccess shouldBeEqualTo true
            }

            releaseConsumer.countDown()
            kotlinx.coroutines.withTimeout(5_000L) {
                latestWritten.await()
            }

            writes shouldBeEqualTo listOf(
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "latest-retried-value"),
                mapOf("same-key" to "newest-value"),
            )
            client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
                connection.sync().hget("$prefix:dead-letter:values", "same-key") shouldBeEqualTo
                    "latest-retried-value"
            }
        } finally {
            releaseConsumer.countDown()
            suspendedMap.suspendClose()
            consumerScope.cancel()
            consumerDispatcher.close()
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
    fun `suspend write-behind는 channel 포화와 무관하게 accepted retry를 보존한다`() = runSuspendIO {
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
            map.writeBehindChannel()
                .trySend(Triple("channel-blocker", "blocker-value", 0))
                .isSuccess shouldBeEqualTo true

            map.flushBatch(listOf(Triple("failed-key", "failed-value", 0)))

            map.writeBehindChannel().tryReceive().getOrNull() shouldBeEqualTo
                Triple("channel-blocker", "blocker-value", 0)
            map.writeBehindChannel().tryReceive().getOrNull().shouldBeNull()
            map.writeBehindRetryQueue().toList() shouldBeEqualTo
                listOf(Triple("failed-key", "failed-value", 1))
            client.connect(StringCodec.UTF8).use { connection ->
                connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo emptyList()
            }
            client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
                connection.sync().hget("$prefix:dead-letter:values", "failed-key").shouldBeNull()
            }
        }
    }

    @Test
    fun `dead-letter HSET 실패 시 accepted entry를 retry queue에 보존한다`() = runSuspendIO {
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) = error("simulated write failure")

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val cancelledScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { it.cancel() }
        val map = LettuceSuspendedLoadedMap(
            client = client,
            writer = writer,
            config = LettuceCacheConfig.WRITE_BEHIND,
            scope = cancelledScope,
        )
        map.valueConnection().close()

        map.flushBatch(listOf(Triple("failed-key", "failed-value", 2)))

        map.writeBehindRetryQueue().toList() shouldBeEqualTo
            listOf(Triple("failed-key", "failed-value", 2))
    }

    @Test
    fun `dead-letter LPUSH 실패 시 accepted entry를 retry queue에 보존한다`() = runSuspendIO {
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) = error("simulated write failure")

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val cancelledScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { it.cancel() }
        val map = LettuceSuspendedLoadedMap(
            client = client,
            writer = writer,
            config = LettuceCacheConfig.WRITE_BEHIND,
            scope = cancelledScope,
        )
        map.stringConnection().close()

        map.flushBatch(listOf(Triple("failed-key", "failed-value", 2)))

        map.writeBehindRetryQueue().toList() shouldBeEqualTo
            listOf(Triple("failed-key", "failed-value", 2))
        map.valueConnection().close()
    }

    @Test
    fun `close와 suspendClose 동시 호출은 하나의 shutdown 결과를 공유한다`() = runSuspendIO {
        val prefix = "shared-shutdown:${randomName()}"
        val cancelledScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { it.cancel() }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindShutdownTimeout = Duration.ofSeconds(2),
        )
        val map = LettuceSuspendedLoadedMap<String, String>(
            client = client,
            config = config,
            scope = cancelledScope,
        )
        map.writeBehindChannel()
            .trySend(Triple("pending-key", "pending-value", 0))
            .isSuccess shouldBeEqualTo true

        val blockingClose = async(Dispatchers.IO) { map.close() }
        val suspendingClose = async { map.suspendClose() }
        blockingClose.await()
        suspendingClose.await()

        client.connect(StringCodec.UTF8).use { connection ->
            connection.sync().lrange("$prefix:dead-letter", 0L, -1L) shouldBeEqualTo listOf("pending-key")
        }
    }

    @Test
    fun `non-cooperative writer에서도 shutdown timeout 상한을 지킨다`() = runSuspendIO {
        val writerStarted = CompletableDeferred<Unit>()
        val releaseWriter = CompletableDeferred<Unit>()
        val writeCalls = AtomicInteger()
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                writeCalls.incrementAndGet()
                writerStarted.complete(Unit)
                withContext(NonCancellable) {
                    releaseWriter.await()
                }
            }

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = "bounded-shutdown:${randomName()}",
            writeBehindShutdownTimeout = Duration.ofMillis(100),
        )
        val map = LettuceSuspendedLoadedMap(client = client, writer = writer, config = config)
        map.set("in-flight", "value")
        writerStarted.await()

        val elapsedMillis = measureTimeMillis {
            assertFailsWith<TimeoutCancellationException> {
                map.suspendClose()
            }
        }

        (elapsedMillis < 1_000L) shouldBeEqualTo true
        assertFailsWith<TimeoutCancellationException> {
            map.suspendClose()
        }
        writeCalls.get() shouldBeEqualTo 1
        releaseWriter.complete(Unit)
        map.suspendClose()
        map.valueConnection().isOpen shouldBeEqualTo false
    }

    @Test
    fun `suspendClose timeout은 처리 중 entry와 channel 잔여분을 dead-letter에 보존한다`() = runSuspendIO {
        val prefix = "suspend-close-recovery:${randomName()}"
        val writerStarted = CompletableDeferred<Unit>()
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                writerStarted.complete(Unit)
                delay(Duration.ofDays(1).toMillis())
            }

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 1,
            writeBehindShutdownTimeout = Duration.ofMillis(500),
        )
        val map = LettuceSuspendedLoadedMap(client = client, writer = writer, config = config)

        map.set("in-flight", "first-value")
        writerStarted.await()
        map.set("queued", "second-value")
        map.suspendClose()

        client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
            connection.sync().hget("$prefix:dead-letter:values", "in-flight") shouldBeEqualTo "first-value"
            connection.sync().hget("$prefix:dead-letter:values", "queued") shouldBeEqualTo "second-value"
        }
    }

    @Test
    fun `close timeout은 처리 중 entry와 channel 잔여분을 dead-letter에 보존한다`() = runSuspendIO {
        val prefix = "blocking-close-recovery:${randomName()}"
        val writerStarted = CompletableDeferred<Unit>()
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                writerStarted.complete(Unit)
                delay(Duration.ofDays(1).toMillis())
            }

            override suspend fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofDays(1),
            writeBehindBatchSize = 1,
            writeBehindShutdownTimeout = Duration.ofMillis(500),
        )
        val map = LettuceSuspendedLoadedMap(client = client, writer = writer, config = config)

        map.set("in-flight", "first-value")
        writerStarted.await()
        map.set("queued", "second-value")
        map.close()

        client.connect(LettuceBinaryCodec<String>(BinarySerializers.LZ4Fory)).use { connection ->
            connection.sync().hget("$prefix:dead-letter:values", "in-flight") shouldBeEqualTo "first-value"
            connection.sync().hget("$prefix:dead-letter:values", "queued") shouldBeEqualTo "second-value"
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

    @Suppress("UNCHECKED_CAST")
    private fun LettuceSuspendedLoadedMap<String, String>.writeBehindRetryQueue():
        ArrayDeque<Triple<String, String, Int>> = javaClass.getDeclaredField("writeBehindRetryQueue")
            .apply { isAccessible = true }
            .get(this) as ArrayDeque<Triple<String, String, Int>>

    private suspend fun LettuceSuspendedLoadedMap<String, String>.flushBatch(
        entries: List<Triple<String, String, Int>>,
    ) {
        val method = this::class.declaredMemberFunctions.single { it.name == "flushBatch" }
        method.isAccessible = true
        method.callSuspend(this, entries)
    }

    @Suppress("UNCHECKED_CAST")
    private fun LettuceSuspendedLoadedMap<String, String>.valueConnection():
        StatefulRedisConnection<String, String> = javaClass.getDeclaredField("connection")
            .apply { isAccessible = true }
            .get(this) as StatefulRedisConnection<String, String>

    @Suppress("UNCHECKED_CAST")
    private fun LettuceSuspendedLoadedMap<String, String>.stringConnection():
        StatefulRedisConnection<String, String> {
        val connection = javaClass.getDeclaredField("lazyStrConnection")
            .apply { isAccessible = true }
            .get(this) as Lazy<StatefulRedisConnection<String, String>>
        return connection.value
    }
}
