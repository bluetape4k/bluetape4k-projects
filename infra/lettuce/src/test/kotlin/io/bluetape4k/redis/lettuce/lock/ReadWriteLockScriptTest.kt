package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveReadWriteLockKeys
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal class ReadWriteLockScriptTest : AbstractLettuceTest() {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var lock: LettuceReadWriteLock
    private lateinit var lockName: String

    private val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))

    @BeforeEach
    fun setUp() {
        connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        lockName = "read-write-script-${randomName().substringAfter(':')}"
        lock = LettuceReadWriteLock.create(connection, lockName)
    }

    @AfterEach
    fun tearDown() {
        lock.close()
        connection.close()
    }

    @Test
    fun `compatible readers share a phase while writer remains exclusive`() {
        val first = acquireRead("reader-1", "read-1")
        val second = acquireRead("reader-2", "read-2")

        lock.writeLock().tryAcquire(owner("writer"), request("write"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()

        lock.readLock().release(first) shouldBeEqualTo LockMutationResult.Released(0)
        lock.readLock().release(second) shouldBeEqualTo LockMutationResult.Released(0)
        val writer = acquireWrite("writer", "write")

        lock.readLock().tryAcquire(owner("reader-3"), request("read-3"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()
        lock.writeLock().release(writer) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `writer boundary closes the current reader phase and admits one writer`() {
        val activeReader = acquireRead("active-reader", "active-read")
        val firstWriter = acquireWriteAsync("writer-1", "write-1")
        awaitQueued(lock.writeLock(), "writer-1", "write-1")
        val lateReader = acquireReadAsync("late-reader", "late-read")
        awaitQueued(lock.readLock(), "late-reader", "late-read")
        val secondWriter = acquireWriteAsync("writer-2", "write-2")
        awaitQueued(lock.writeLock(), "writer-2", "write-2")

        lock.readLock().release(activeReader) shouldBeEqualTo LockMutationResult.Released(0)
        val firstWriterHandle = firstWriter.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle
        lock.readLock().reconcile(owner("late-reader"), request("late-read"))
            .shouldBeInstanceOf<LockReconcileResult.Queued>()
        lock.writeLock().reconcile(owner("writer-2"), request("write-2"))
            .shouldBeInstanceOf<LockReconcileResult.Queued>()

        lock.writeLock().release(firstWriterHandle) shouldBeEqualTo LockMutationResult.Released(0)
        val lateReaderHandle = lateReader.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            .handle
        secondWriter.isDone shouldBeEqualTo false
        lock.readLock().release(lateReaderHandle) shouldBeEqualTo LockMutationResult.Released(0)
        val secondWriterHandle = secondWriter.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle
        lock.writeLock().release(secondWriterHandle) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `queued readers before a writer boundary form one bounded reader phase`() {
        val activeWriter = acquireWrite("active-writer", "active-write")
        val firstReader = acquireReadAsync("reader-1", "read-1")
        awaitQueued(lock.readLock(), "reader-1", "read-1")
        val secondReader = acquireReadAsync("reader-2", "read-2")
        awaitQueued(lock.readLock(), "reader-2", "read-2")
        val nextWriter = acquireWriteAsync("next-writer", "next-write")
        awaitQueued(lock.writeLock(), "next-writer", "next-write")
        val lateReader = acquireReadAsync("late-reader", "late-read")
        awaitQueued(lock.readLock(), "late-reader", "late-read")

        lock.writeLock().release(activeWriter) shouldBeEqualTo LockMutationResult.Released(0)
        val first = firstReader.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            .handle
        val second = secondReader.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            .handle
        nextWriter.isDone shouldBeEqualTo false
        lateReader.isDone shouldBeEqualTo false

        lock.readLock().release(first) shouldBeEqualTo LockMutationResult.Released(0)
        lock.readLock().release(second) shouldBeEqualTo LockMutationResult.Released(0)
        val writer = nextWriter.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle
        lock.writeLock().release(writer) shouldBeEqualTo LockMutationResult.Released(0)
        val late = lateReader.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            .handle
        lock.readLock().release(late) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `same owner reenters only within its current mode`() {
        val first = acquireRead("reader", "read-1")
        val second = lock.readLock().tryAcquire(owner("reader"), request("read-2"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<ReadLockHandle>>()
        second.holdCount shouldBeEqualTo 2

        lock.writeLock().tryAcquire(owner("reader"), request("write"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()
        lock.readLock().release(second.handle) shouldBeEqualTo LockMutationResult.Released(1)
        lock.readLock().release(first) shouldBeEqualTo LockMutationResult.Released(0)

        val writer = acquireWrite("writer", "write-1")
        val reentered = lock.writeLock().tryAcquire(owner("writer"), request("write-2"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<WriteLockHandle>>()
        reentered.holdCount shouldBeEqualTo 2
        lock.writeLock().release(reentered.handle) shouldBeEqualTo LockMutationResult.Released(1)
        lock.writeLock().release(writer) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `queued writer closes the reader boundary even for a new request from the active reader owner`() {
        val reader = acquireRead("reader", "read-1")
        val writer = acquireWriteAsync("writer", "write")
        awaitQueued(lock.writeLock(), "writer", "write")

        lock.readLock().tryAcquire(owner("reader"), request("read-2"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()
        lock.readLock().release(reader) shouldBeEqualTo LockMutationResult.Released(0)

        val writerHandle = writer.get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle
        lock.writeLock().release(writerHandle) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `downgrade is atomic idempotent and stale generations cannot consume current ownership`() {
        val writer = acquireWrite("writer", "write")
        val first = lock.downgrade(writer)
            .shouldBeInstanceOf<DowngradeResult.Downgraded>()
            .handle
        val replay = lock.downgrade(writer)
            .shouldBeInstanceOf<DowngradeResult.Downgraded>()
            .handle
        replay shouldBeEqualTo first

        val peer = acquireRead("peer", "peer-read")
        lock.readLock().release(first) shouldBeEqualTo LockMutationResult.Released(0)
        lock.readLock().release(peer) shouldBeEqualTo LockMutationResult.Released(0)

        val stale = acquireWrite("stale-writer", "stale-write")
        lock.writeLock().release(stale) shouldBeEqualTo LockMutationResult.Released(0)
        val next = acquireWrite("next-writer", "next-write")
        lock.downgrade(stale) shouldBeEqualTo DowngradeResult.StaleGeneration
        lock.writeLock().release(next) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `downgrade replay consumes one write hold and never increments its read hold twice`() {
        val firstWrite = acquireWrite("writer", "write-1")
        val secondWrite = lock.writeLock().tryAcquire(owner("writer"), request("write-2"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<WriteLockHandle>>()
            .handle

        val downgraded = lock.downgrade(firstWrite)
            .shouldBeInstanceOf<DowngradeResult.Downgraded>()
            .handle
        lock.downgrade(firstWrite) shouldBeEqualTo DowngradeResult.Downgraded(downgraded)
        lock.readLock().inspect(downgraded)
            .shouldBeInstanceOf<LockInspectResult.Owned<ReadLockHandle>>()
            .holdCount shouldBeEqualTo 1
        lock.readLock().tryAcquire(owner("peer"), request("peer-read"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()

        lock.writeLock().release(secondWrite) shouldBeEqualTo LockMutationResult.Released(0)
        val peer = acquireRead("peer", "peer-read")
        lock.readLock().release(peer) shouldBeEqualTo LockMutationResult.Released(0)
        lock.readLock().release(downgraded) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `expired reader is cleaned without deleting a newer writer generation`() {
        lock.close()
        lock = LettuceReadWriteLock.create(
            connection,
            "read-write-expiry-${randomName().substringAfter(':')}",
            ReadWriteLockConfig(),
        )
        acquireRead(
            "expiring-reader",
            "expiring-read",
            LeasePolicy.Fixed(Duration.ofMillis(100)),
        )

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            val writer = lock.writeLock().tryAcquire(owner("writer"), request("write"), lease)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            lock.writeLock().inspect(writer)
                .shouldBeInstanceOf<LockInspectResult.Owned<WriteLockHandle>>()
            lock.writeLock().release(writer) shouldBeEqualTo LockMutationResult.Released(0)
        }
    }

    @Test
    fun `renew release replay and inspection preserve terminal result distinctions`() {
        val reader = acquireRead("renew-reader", "renew-read")
        lock.readLock().renew(reader, Duration.ofSeconds(1))
            .shouldBeInstanceOf<LockMutationResult.Renewed<ReadLockHandle>>()
        lock.readLock().release(reader) shouldBeEqualTo LockMutationResult.Released(0)
        lock.readLock().release(reader) shouldBeEqualTo LockMutationResult.AlreadyReleased
        lock.readLock().inspect(reader) shouldBeEqualTo LockInspectResult.Released
        lock.readLock().renew(reader, Duration.ofSeconds(1)) shouldBeEqualTo LockMutationResult.AlreadyReleased

        val expiring = acquireWrite("expiring-writer", "expiring-write", LeasePolicy.Fixed(Duration.ofMillis(100)))
        Thread.sleep(150L)
        lock.writeLock().inspect(expiring) shouldBeEqualTo LockInspectResult.Expired
        lock.writeLock().release(expiring) shouldBeEqualTo LockMutationResult.Expired
    }

    @Test
    fun `reconcile and closed surfaces return typed terminal outcomes`() {
        lock.readLock().reconcile(owner("missing-owner"), request("missing-request")) shouldBeEqualTo
            LockReconcileResult.NotFound

        val reader = acquireRead("close-reader", "close-read")
        lock.close()

        lock.readLock().tryAcquire(owner("after-close"), request("after-close"), lease) shouldBeEqualTo
            LockAcquireResult.Closed
        lock.readLock().inspect(reader) shouldBeEqualTo LockInspectResult.Closed
        lock.readLock().release(reader) shouldBeEqualTo LockMutationResult.Closed
    }

    @Test
    fun `bounded acquire paths reject foreign handles and time out`() = runSuspendIO {
        val writer = acquireWrite("bounded-writer", "bounded-write")
        val other = LettuceReadWriteLock.create(connection, "read-write-foreign-${randomName().substringAfter(':')}")
        val foreignHandle = other.writeLock().tryAcquire(owner("foreign-owner"), request("foreign-write"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle
        try {
            assertFailsWith<IllegalArgumentException> {
                lock.writeLock().inspect(foreignHandle)
            }
            assertFailsWith<IllegalArgumentException> {
                lock.writeLock().release(foreignHandle)
            }

            lock.readLock().acquire(
                owner("sync-timeout"),
                request("sync-timeout"),
                Duration.ofMillis(35),
                lease,
            ) shouldBeEqualTo LockAcquireResult.TimedOut
            lock.readLock().acquireAsync(
                owner("async-timeout"),
                request("async-timeout"),
                Duration.ofMillis(35),
                lease,
            ).get(2, TimeUnit.SECONDS) shouldBeEqualTo LockAcquireResult.TimedOut

            val suspendLock = LettuceSuspendReadWriteLock.create(
                connection,
                lockName,
            )
            try {
                suspendLock.readLock().acquire(
                    owner("suspend-timeout"),
                    request("suspend-timeout"),
                    Duration.ofMillis(35),
                    lease,
                ) shouldBeEqualTo LockAcquireResult.TimedOut
            } finally {
                suspendLock.close()
            }

            assertFailsWith<IllegalArgumentException> {
                lock.readLock().acquire(
                    owner("too-long"),
                    request("too-long"),
                    Duration.ofHours(24).plusMillis(1),
                    lease,
                )
            }
        } finally {
            other.writeLock().release(foreignHandle)
            other.close()
        }

        lock.writeLock().release(writer) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `async and suspend views preserve read write lifecycle parity`() = runSuspendIO {
        val asyncReader = lock.readLock().tryAcquireAsync(owner("async-reader"), request("async-read"), lease)
            .get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            .handle
        lock.readLock().inspectAsync(asyncReader).get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockInspectResult.Owned<ReadLockHandle>>()
        lock.readLock().reconcileAsync(owner("async-reader"), request("async-read"))
            .get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockReconcileResult.Owned<ReadLockHandle>>()
        lock.readLock().renewAsync(asyncReader, Duration.ofSeconds(1)).get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockMutationResult.Renewed<ReadLockHandle>>()
        lock.readLock().releaseAsync(asyncReader).get(2, TimeUnit.SECONDS) shouldBeEqualTo
            LockMutationResult.Released(0)

        val asyncWriter = lock.writeLock().tryAcquireAsync(owner("async-writer"), request("async-write"), lease)
            .get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle
        lock.writeLock().inspectAsync(asyncWriter).get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockInspectResult.Owned<WriteLockHandle>>()
        lock.writeLock().reconcileAsync(owner("async-writer"), request("async-write"))
            .get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockReconcileResult.Owned<WriteLockHandle>>()
        lock.writeLock().renewAsync(asyncWriter, Duration.ofSeconds(1)).get(2, TimeUnit.SECONDS)
            .shouldBeInstanceOf<LockMutationResult.Renewed<WriteLockHandle>>()
        lock.writeLock().releaseAsync(asyncWriter).get(2, TimeUnit.SECONDS) shouldBeEqualTo
            LockMutationResult.Released(0)

        val suspendName = "read-write-suspend-${randomName().substringAfter(':')}"
        val suspendLock = LettuceSuspendReadWriteLock.create(connection, suspendName)
        try {
            val suspendReader = suspendLock.readLock().tryAcquire(
                owner("suspend-reader"),
                request("suspend-read"),
                lease,
            )
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            suspendLock.readLock().inspect(suspendReader)
                .shouldBeInstanceOf<LockInspectResult.Owned<ReadLockHandle>>()
            suspendLock.readLock().reconcile(owner("suspend-reader"), request("suspend-read"))
                .shouldBeInstanceOf<LockReconcileResult.Owned<ReadLockHandle>>()
            suspendLock.readLock().renew(suspendReader, Duration.ofSeconds(1))
                .shouldBeInstanceOf<LockMutationResult.Renewed<ReadLockHandle>>()
            suspendLock.readLock().release(suspendReader) shouldBeEqualTo LockMutationResult.Released(0)

            val suspendWriter = suspendLock.writeLock().tryAcquire(
                owner("suspend-writer"),
                request("suspend-write"),
                lease,
            )
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            suspendLock.writeLock().inspect(suspendWriter)
                .shouldBeInstanceOf<LockInspectResult.Owned<WriteLockHandle>>()
            suspendLock.writeLock().reconcile(owner("suspend-writer"), request("suspend-write"))
                .shouldBeInstanceOf<LockReconcileResult.Owned<WriteLockHandle>>()
            suspendLock.writeLock().renew(suspendWriter, Duration.ofSeconds(1))
                .shouldBeInstanceOf<LockMutationResult.Renewed<WriteLockHandle>>()
            suspendLock.writeLock().release(suspendWriter) shouldBeEqualTo LockMutationResult.Released(0)
        } finally {
            suspendLock.close()
        }
    }

    @Test
    fun `bounded stale waiter cleanup fails closed without bypassing the remaining boundary`() {
        lock.close()
        val name = "read-write-cleanup-${randomName().substringAfter(':')}"
        val config = ReadWriteLockConfig(cleanupBatchSize = 1)
        lock = LettuceReadWriteLock.create(connection, name, config)
        val keys = deriveReadWriteLockKeys(name, config, connection.codec)
        val commands = connection.sync()
        commands.zadd(keys.queue, 1.0, "stale-writer-1")
        commands.zadd(keys.queue, 2.0, "stale-writer-2")
        commands.hset(
            keys.waiters,
            "stale-writer-1",
            "1|0|1|W|stale-owner-1|stale-request-1|F:3000|3000",
        )
        commands.hset(
            keys.waiters,
            "stale-writer-2",
            "2|0|1|W|stale-owner-2|stale-request-2|F:3000|3000",
        )
        commands.set(keys.sequence, "2")

        lock.readLock().tryAcquire(owner("candidate"), request("candidate-read"), lease) shouldBeEqualTo
            LockAcquireResult.CleanupPending
        commands.zcard(keys.queue) shouldBeEqualTo 1L

        val acquired = acquireRead("candidate", "candidate-read")
        commands.zcard(keys.queue) shouldBeEqualTo 0L
        lock.readLock().release(acquired) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `public and internal surfaces expose no read-to-write upgrade`() {
        LettuceReadWriteLock::class.java.methods.none {
            it.name.contains("upgrade", ignoreCase = true)
        } shouldBeEqualTo true
        LettuceReadWriteLock.ReadLockView::class.java.methods
            .none { it.name.contains("upgrade", ignoreCase = true) } shouldBeEqualTo true
        LettuceSuspendReadWriteLock::class.java.methods
            .none { it.name.contains("upgrade", ignoreCase = true) } shouldBeEqualTo true
    }

    private fun acquireRead(
        owner: String,
        request: String,
        leasePolicy: LeasePolicy = lease,
    ): ReadLockHandle =
        lock.readLock().tryAcquire(owner(owner), request(request), leasePolicy)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            .handle

    private fun acquireWrite(
        owner: String,
        request: String,
        leasePolicy: LeasePolicy = lease,
    ): WriteLockHandle =
        lock.writeLock().tryAcquire(owner(owner), request(request), leasePolicy)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
            .handle

    private fun acquireReadAsync(
        owner: String,
        request: String,
    ): CompletableFuture<LockAcquireResult<ReadLockHandle>> =
        lock.readLock().acquireAsync(
            owner(owner),
            request(request),
            Duration.ofSeconds(2),
            lease,
        )

    private fun acquireWriteAsync(
        owner: String,
        request: String,
    ): CompletableFuture<LockAcquireResult<WriteLockHandle>> =
        lock.writeLock().acquireAsync(
            owner(owner),
            request(request),
            Duration.ofSeconds(2),
            lease,
        )

    private fun awaitQueued(
        view: LettuceReadWriteLock.ReadLockView,
        owner: String,
        request: String,
    ) {
        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            view.reconcile(owner(owner), request(request))
                .shouldBeInstanceOf<LockReconcileResult.Queued>()
        }
    }

    private fun awaitQueued(
        view: LettuceReadWriteLock.WriteLockView,
        owner: String,
        request: String,
    ) {
        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            view.reconcile(owner(owner), request(request))
                .shouldBeInstanceOf<LockReconcileResult.Queued>()
        }
    }

    private fun owner(value: String): LockOwnerId = LockOwnerId.from(value)

    private fun request(value: String): LockRequestId = LockRequestId.from(value)
}
