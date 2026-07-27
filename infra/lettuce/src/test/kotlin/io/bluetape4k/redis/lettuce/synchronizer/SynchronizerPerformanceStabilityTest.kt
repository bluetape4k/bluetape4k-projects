package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.time.Duration

class SynchronizerPerformanceStabilityTest: AbstractLettuceTest() {

    @Test
    @Timeout(30)
    fun `one hundred contenders respect capacity across repeated object lifecycles`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val executor = Executors.newFixedThreadPool(16)
        try {
            repeat(5) { lifecycle ->
                val name = "semaphore-load-$lifecycle-${randomName().substringAfter(':')}"
                val keys = deriveSemaphoreKeys(name, SemaphoreConfig(), StringCodec.UTF8)
                connection.sync().del(*keys.all.toTypedArray())
                LettuceDistributedSemaphore.create(connection, name).use { semaphore ->
                    semaphore.trySetPermits(10)
                    val results = (1..100).map { contender ->
                        CompletableFuture.supplyAsync(
                            {
                                semaphore.tryAcquire(
                                    SemaphoreOwnerId.from("owner-$contender"),
                                    SemaphoreRequestId.from("request-$contender"),
                                )
                            },
                            executor,
                        )
                    }.map(CompletableFuture<PermitAcquireResult<PermitHandle>>::join)
                    val handles = results.filterIsInstance<PermitAcquireResult.Acquired<PermitHandle>>()
                        .map { it.handle }
                    handles.size shouldBeEqualTo 10
                    semaphore.availablePermits() shouldBeEqualTo 0
                    handles.forEach(semaphore::release)
                    semaphore.availablePermits() shouldBeEqualTo 10
                }
                connection.sync().del(*keys.all.toTypedArray())
            }
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
            connection.close()
        }
    }

    @Test
    @Timeout(30)
    fun `one hundred async waiters are connection runtime owned and close promptly`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "semaphore-async-${randomName().substringAfter(':')}"
        val keys = deriveSemaphoreKeys(name, SemaphoreConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettuceDistributedSemaphore.create(connection, name)
        try {
            semaphore.trySetPermits(1)
            semaphore.tryAcquire(SemaphoreOwnerId.from("holder"), SemaphoreRequestId.from("holder"))
            val waiters = (1..100).map {
                semaphore.acquireAsync(
                    SemaphoreOwnerId.from("owner-$it"),
                    SemaphoreRequestId.from("request-$it"),
                    1,
                    Duration.ofSeconds(10),
                )
            }
            semaphore.close()
            CompletableFuture.allOf(*waiters.toTypedArray()).get(5, TimeUnit.SECONDS)
            waiters.forEach { it.join() shouldBeEqualTo PermitAcquireResult.Closed }
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    @Timeout(30)
    fun `sub millisecond suspend polling remains bounded`() = runSuspendIO {
        val config = SemaphoreConfig(pollInterval = Duration.ofNanos(1))
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "semaphore-submillis-${randomName().substringAfter(':')}"
        val keys = deriveSemaphoreKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettuceSuspendDistributedSemaphore.create(connection, name, config)
        try {
            semaphore.trySetPermits(1)
            semaphore.tryAcquire(SemaphoreOwnerId.from("holder"), SemaphoreRequestId.from("holder"))
            semaphore.acquire(
                SemaphoreOwnerId.from("waiter"),
                SemaphoreRequestId.from("waiter"),
                1,
                Duration.ofMillis(5),
            ) shouldBeEqualTo PermitAcquireResult.TimedOut
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }
}
