package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

internal class LockProductionObservationTest {

    @Test
    fun `every lock family publishes runtime and Redis latency observations through its public sink`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            Executors.newSingleThreadScheduledExecutor().use { scheduler ->
                val observations = CopyOnWriteArrayList<LockObservation>()
                val sink = LockObservationSink(observations::add)
                val suffix = System.nanoTime()
                val owner = LockOwnerId.from("observation-owner")
                val lease = LeasePolicy.Fixed(Duration.ofSeconds(2))
                val waitTime = Duration.ofMillis(100)

                LettuceDistributedLock.create(
                    connection,
                    "observed-distributed-$suffix",
                    LockConfig(),
                    scheduler,
                    sink,
                ).use { lock ->
                    connection.sync().scriptFlush()
                    val handle = lock.acquire(owner, LockRequestId.from("distributed-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                    lock.acquire(
                        LockOwnerId.from("contending-owner"),
                        LockRequestId.from("contending-request"),
                        Duration.ofMillis(25),
                        lease,
                    ) shouldBeEqualTo LockAcquireResult.TimedOut
                    lock.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                }
                LettuceFairLock.create(
                    connection,
                    "observed-fair-$suffix",
                    FairLockConfig(),
                    scheduler,
                    sink,
                ).use { lock ->
                    connection.sync().scriptFlush()
                    val handle = lock.acquire(owner, LockRequestId.from("fair-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                    lock.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                }
                LettuceFencedLock.create(
                    connection,
                    "observed-fenced-$suffix",
                    FencedLockConfig(epoch = 1L),
                    scheduler,
                    sink,
                ).use { lock ->
                    connection.sync().scriptFlush()
                    lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                    val handle = lock.acquire(owner, LockRequestId.from("fenced-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                    lock.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                }
                LettuceReadWriteLock.create(
                    connection,
                    "observed-read-write-$suffix",
                    ReadWriteLockConfig(),
                    scheduler,
                    sink,
                ).use { lock ->
                    connection.sync().scriptFlush()
                    val read = lock.readLock().acquire(owner, LockRequestId.from("read-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>().handle
                    lock.readLock().release(read) shouldBeEqualTo LockMutationResult.Released(0)
                    connection.sync().scriptFlush()
                    val write = lock.writeLock().acquire(owner, LockRequestId.from("write-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>().handle
                    lock.writeLock().release(write) shouldBeEqualTo LockMutationResult.Released(0)
                }
                LettuceSpinLock.create(
                    connection,
                    "observed-spin-$suffix",
                    SpinLockConfig(),
                    scheduler,
                    sink,
                ).use { lock ->
                    connection.sync().scriptFlush()
                    val handle = lock.acquire(owner, LockRequestId.from("spin-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                    lock.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                }
                LettuceMultiLock.create(
                    connection,
                    listOf("observed-multi-a-$suffix", "observed-multi-b-$suffix"),
                    MultiLockConfig(),
                    scheduler,
                    sink,
                ).use { lock ->
                    connection.sync().scriptFlush()
                    val handle = lock.acquire(owner, LockRequestId.from("multi-request"), waitTime, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>().handle
                    lock.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                }

                fun histogramKinds(name: LockHistogramName): Set<LockKind> =
                    observations.filterIsInstance<LockObservation.Histogram>()
                        .filter { it.name == name }
                        .map { it.dimensions.objectKind }
                        .toSet()

                fun gaugeKinds(name: LockGaugeName): Set<LockKind> =
                    observations.filterIsInstance<LockObservation.Gauge>()
                        .filter { it.name == name }
                        .map { it.dimensions.objectKind }
                        .toSet()

                histogramKinds(LockHistogramName.REDIS_COMMAND_LATENCY_MILLIS) shouldBeEqualTo LockKind.entries.toSet()
                histogramKinds(LockHistogramName.CALLER_WAIT_LATENCY_MILLIS) shouldBeEqualTo LockKind.entries.toSet()
                histogramKinds(LockHistogramName.RETRY_COUNT) shouldBeEqualTo LockKind.entries.toSet()
                gaugeKinds(LockGaugeName.COORDINATION_OBJECTS) shouldBeEqualTo LockKind.entries.toSet()
                gaugeKinds(LockGaugeName.ACTIVE_REQUEST_HOLDS) shouldBeEqualTo LockKind.entries.toSet()
                observations.filterIsInstance<LockObservation.Counter>()
                    .filter { it.name == LockCounterName.NOSCRIPT_FALLBACK_TOTAL }
                    .map { it.dimensions.objectKind }
                    .toSet() shouldBeEqualTo LockKind.entries.toSet()

                observations.filterIsInstance<LockObservation.Gauge>()
                    .filter {
                        it.name == LockGaugeName.QUEUED_WAITERS &&
                            it.dimensions.objectKind == LockKind.DISTRIBUTED
                    }
                    .map { it.value }
                    .toSet() shouldBeEqualTo setOf(0L, 1L)

                observations.filterIsInstance<LockObservation.Gauge>()
                    .filter {
                        it.name == LockGaugeName.COORDINATION_OBJECTS &&
                            it.dimensions.objectKind == LockKind.SPIN
                    }
                    .maxOf { it.value } shouldBeEqualTo 1L
            }
        }
    }
}
