package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.SpinLockRetryPolicy
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

internal class LockConcurrencyTest {

    @Test
    @Timeout(30)
    fun `distributed and multi locks admit exactly one concurrent owner`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val suffix = System.nanoTime().toString()
            val distributed = LettuceDistributedLock.create(
                connection,
                "concurrent-resource",
                LockConfig(hashTag = "lock-concurrency-$suffix"),
            )
            val multi = LettuceMultiLock.create(
                connection,
                listOf("account", "inventory"),
                MultiLockConfig(lock = LockConfig(hashTag = "multi-concurrency-$suffix")),
            )
            try {
                verifyOneWinner { caller ->
                    distributed.tryAcquire(owner(caller), request(caller), LEASE)
                }.let { winner ->
                    val first = winner.shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                    val reentered = distributed.tryAcquire(
                        first.ownerId,
                        LockRequestId.from("distributed-reentry"),
                        LEASE,
                    ).shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()
                    reentered.holdCount shouldBeEqualTo 2
                    reentered.handle.generation shouldBeEqualTo first.generation
                    distributed.release(reentered.handle) shouldBeEqualTo LockMutationResult.Released(1)
                    distributed.release(first) shouldBeEqualTo LockMutationResult.Released(0)
                }

                verifyOneWinner { caller ->
                    multi.tryAcquire(owner(caller), request(caller), LEASE)
                }.let { winner ->
                    val first = winner.shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>().handle
                    val reentered = multi.tryAcquire(
                        first.lock.ownerId,
                        LockRequestId.from("multi-reentry"),
                        LEASE,
                    ).shouldBeInstanceOf<LockAcquireResult.Reentered<MultiLockHandle>>()
                    reentered.holdCount shouldBeEqualTo 2
                    reentered.handle.lock.generation shouldBeEqualTo first.lock.generation
                    multi.release(reentered.handle) shouldBeEqualTo LockMutationResult.Released(1)
                    multi.release(first) shouldBeEqualTo LockMutationResult.Released(0)
                }
            } finally {
                distributed.close()
                multi.close()
            }
        }
    }

    @Test
    fun `fair lock admits queued owners in Redis sequence order`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            LettuceFairLock.create(
                connection,
                "fair-concurrency",
                FairLockConfig(LockConfig(hashTag = "fair-concurrency-${System.nanoTime()}")),
            ).use { lock ->
                val holder = lock.tryAcquire(owner(0), request(0), LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
                    .handle
                val firstWaiter = lock.acquireAsync(
                    owner(1),
                    request(1),
                    Duration.ofSeconds(5),
                    LEASE,
                )
                awaitQueued { lock.reconcile(owner(1), request(1)) }
                val secondWaiter = lock.acquireAsync(
                    owner(2),
                    request(2),
                    Duration.ofSeconds(5),
                    LEASE,
                )
                awaitQueued { lock.reconcile(owner(2), request(2)) }

                lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
                val firstHandle = firstWaiter.get(2, TimeUnit.SECONDS)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
                    .handle
                secondWaiter.isDone shouldBeEqualTo false
                lock.release(firstHandle) shouldBeEqualTo LockMutationResult.Released(0)
                val secondHandle = secondWaiter.get(2, TimeUnit.SECONDS)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
                    .handle
                lock.release(secondHandle) shouldBeEqualTo LockMutationResult.Released(0)
            }
        }
    }

    @Test
    @Timeout(30)
    fun `read write lock advances writer boundary before a late reader`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            LettuceReadWriteLock.create(
                connection,
                "read-write-concurrency",
                ReadWriteLockConfig(LockConfig(hashTag = "read-write-concurrency-${System.nanoTime()}")),
            ).use { lock ->
                val activeReader = lock.readLock().tryAcquire(owner(0), request(0), LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                val writer = lock.writeLock().acquireAsync(
                    owner(1),
                    request(1),
                    Duration.ofSeconds(5),
                    LEASE,
                )
                awaitQueued { lock.writeLock().reconcile(owner(1), request(1)) }
                val lateReader = lock.readLock().acquireAsync(
                    owner(2),
                    request(2),
                    Duration.ofSeconds(5),
                    LEASE,
                )
                awaitQueued { lock.readLock().reconcile(owner(2), request(2)) }

                lock.readLock().release(activeReader) shouldBeEqualTo LockMutationResult.Released(0)
                val writerHandle = writer.get(2, TimeUnit.SECONDS)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                lateReader.isDone shouldBeEqualTo false
                lock.writeLock().release(writerHandle) shouldBeEqualTo LockMutationResult.Released(0)
                val readerHandle = lateReader.get(2, TimeUnit.SECONDS)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                lock.readLock().release(readerHandle) shouldBeEqualTo LockMutationResult.Released(0)
            }
        }
    }

    @Test
    fun `spin retry policy enforces the configured attempt ceiling`() {
        val policy = SpinLockRetryPolicy(
            SpinLockConfig(
                initialDelay = Duration.ofMillis(1),
                multiplier = 1.0,
                maxAttemptsPerSecond = 100,
                jitterRatio = 0.0,
            ),
        )

        policy.delay(1, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(10)
    }

    @Test
    fun `fenced lock generations remain strictly monotonic across owners`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceFencedLock.create(
                connection,
                "fenced-concurrency",
                FencedLockConfig(
                    lock = LockConfig(hashTag = "fenced-concurrency-${System.nanoTime()}"),
                    epoch = 1,
                ),
            )
            try {
                lock.bootstrapFencing().shouldBeInstanceOf<FencedBootstrapResult.Initialized>()
                val generations = (0 until CALLERS).map { caller ->
                    val acquired = lock.tryAcquire(owner(caller), request(caller), LEASE)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                        .handle
                    lock.release(acquired) shouldBeEqualTo LockMutationResult.Released(0)
                    acquired.lock.generation.value
                }

                generations shouldBeEqualTo generations.sorted()
                generations.distinct().size shouldBeEqualTo CALLERS
            } finally {
                lock.close()
            }
        }
    }

    private fun <H: java.io.Serializable> verifyOneWinner(
        attempt: (Int) -> LockAcquireResult<H>,
    ): LockAcquireResult<H> {
        val barrier = CyclicBarrier(CALLERS)
        val results = ConcurrentLinkedQueue<LockAcquireResult<H>>()
        val tasks = List(CALLERS) { caller ->
            val task: () -> Unit = {
                barrier.await()
                results += attempt(caller)
            }
            task
        }
        MultithreadingTester()
            .workers(CALLERS)
            .rounds(1)
            .addAll(tasks)
            .run()

        results.count { it is LockAcquireResult.Acquired } shouldBeEqualTo 1
        results.count { it is LockAcquireResult.Contended } shouldBeEqualTo CALLERS - 1
        return results.single { it is LockAcquireResult.Acquired }
    }

    private fun owner(caller: Int): LockOwnerId = LockOwnerId.from("concurrent-owner-$caller")

    private fun request(caller: Int): LockRequestId = LockRequestId.from("concurrent-request-$caller")

    private fun awaitQueued(reconcile: () -> LockReconcileResult<*>) {
        val deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos()
        while (System.nanoTime() < deadline) {
            if (reconcile() is LockReconcileResult.Queued) return
            Thread.sleep(5)
        }
        reconcile().shouldBeInstanceOf<LockReconcileResult.Queued>()
    }

    private companion object {
        const val CALLERS = 8
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(5))
    }
}
