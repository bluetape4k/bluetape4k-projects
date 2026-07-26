package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.lang.reflect.Modifier
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import kotlin.coroutines.Continuation

internal class LockApiSurfaceTest {

    private data class MethodShape(val parameterTypes: List<Class<*>>)

    private val companionFactories: Map<Class<*>, Set<List<Class<*>>>> = mapOf(
        LettuceDistributedLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, LockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, LockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                LockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                LockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSuspendDistributedLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, LockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, LockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                LockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                LockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceFairLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, FairLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, FairLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                FairLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                FairLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSuspendFairLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, FairLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, FairLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                FairLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                FairLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceFencedLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java, FencedLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, FencedLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                FencedLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                FencedLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSuspendFencedLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java, FencedLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, FencedLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                FencedLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                FencedLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceReadWriteLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, ReadWriteLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, ReadWriteLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                ReadWriteLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                ReadWriteLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSuspendReadWriteLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, ReadWriteLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, ReadWriteLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                ReadWriteLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                ReadWriteLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSpinLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, SpinLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, SpinLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                SpinLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                SpinLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSuspendSpinLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, String::class.java),
            listOf(StatefulRedisConnection::class.java, String::class.java, SpinLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java),
            listOf(StatefulRedisClusterConnection::class.java, String::class.java, SpinLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                SpinLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                String::class.java,
                SpinLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceMultiLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, Collection::class.java),
            listOf(StatefulRedisConnection::class.java, Collection::class.java, MultiLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, Collection::class.java),
            listOf(StatefulRedisClusterConnection::class.java, Collection::class.java, MultiLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                Collection::class.java,
                MultiLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                Collection::class.java,
                MultiLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
        LettuceSuspendMultiLock::class.java to setOf(
            listOf(StatefulRedisConnection::class.java, Collection::class.java),
            listOf(StatefulRedisConnection::class.java, Collection::class.java, MultiLockConfig::class.java),
            listOf(StatefulRedisClusterConnection::class.java, Collection::class.java),
            listOf(StatefulRedisClusterConnection::class.java, Collection::class.java, MultiLockConfig::class.java),
            listOf(
                StatefulRedisConnection::class.java,
                Collection::class.java,
                MultiLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
            listOf(
                StatefulRedisClusterConnection::class.java,
                Collection::class.java,
                MultiLockConfig::class.java,
                ScheduledExecutorService::class.java,
                LockObservationSink::class.java,
            ),
        ),
    )

    @Test
    fun `lock companion factories keep expected public static signatures`() {
        companionFactories.forEach { (type, expectedSignatures) ->
            val createMethods = type.methods
                .asSequence()
                .filter { it.name == "create" && Modifier.isStatic(it.modifiers) }
                .map { it.parameterTypes.toList() }
                .toSet()
                .map(::MethodShape)
                .toSet()

            createMethods.size shouldBeEqualTo expectedSignatures.size
            expectedSignatures.map(::MethodShape).toSet() shouldBeEqualTo createMethods
        }
    }

    @Test
    fun `compatibility lock constructors keep legacy shape`() {
        legacyCreateConstructors(LettuceLock::class.java).contains(
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                Duration::class.java,
            ),
        ).shouldBeTrue()
        legacyCreateConstructors(LettuceSuspendLock::class.java).contains(
            listOf(
                StatefulRedisConnection::class.java,
                String::class.java,
                Duration::class.java,
            ),
        ).shouldBeTrue()
    }

    @Test
    fun `blocking families keep typed handle lifecycle and async counterparts`() {
        val families = listOf(
            Triple(LettuceDistributedLock::class.java, LockHandle::class.java, "distributed"),
            Triple(LettuceFairLock::class.java, LockHandle::class.java, "fair"),
            Triple(LettuceFencedLock::class.java, FencedLockHandle::class.java, "fenced"),
            Triple(LettuceSpinLock::class.java, LockHandle::class.java, "spin"),
            Triple(LettuceMultiLock::class.java, MultiLockHandle::class.java, "multi"),
        )

        families.forEach { (type, handleType, label) ->
            assertGenericHandle(
                type.getMethod(
                    "tryAcquire",
                    LockOwnerId::class.java,
                    LockRequestId::class.java,
                    LeasePolicy::class.java,
                ),
                LockAcquireResult::class.java,
                handleType,
                "$label tryAcquire",
            )
            assertGenericHandle(
                type.getMethod(
                    "acquireAsync",
                    LockOwnerId::class.java,
                    LockRequestId::class.java,
                    Duration::class.java,
                    LeasePolicy::class.java,
                ),
                CompletableFuture::class.java,
                handleType,
                "$label acquireAsync",
            )
            assertGenericHandle(
                type.getMethod("inspect", handleType),
                LockInspectResult::class.java,
                handleType,
                "$label inspect",
            )
            assertGenericHandle(
                type.getMethod("release", handleType),
                LockMutationResult::class.java,
                handleType,
                "$label release",
            )
            val close = type.getMethod("close")
            close.parameterCount shouldBeEqualTo 0
            close.returnType shouldBeEqualTo Void.TYPE
        }
    }

    @Test
    fun `fenced and read write specialized surfaces remain explicit`() {
        LettuceFencedLock::class.java.getMethod("bootstrapFencing")
            .returnType shouldBeEqualTo FencedBootstrapResult::class.java
        LettuceFencedLock::class.java.getMethod("bootstrapFencingAsync")
            .returnType shouldBeEqualTo CompletableFuture::class.java

        LettuceReadWriteLock::class.java.getMethod("readLock")
            .returnType shouldBeEqualTo LettuceReadWriteLock.ReadLockView::class.java
        LettuceReadWriteLock::class.java.getMethod("writeLock")
            .returnType shouldBeEqualTo LettuceReadWriteLock.WriteLockView::class.java
        LettuceReadWriteLock::class.java.getMethod("downgrade", WriteLockHandle::class.java)
            .returnType shouldBeEqualTo DowngradeResult::class.java
        LettuceReadWriteLock::class.java.getMethod("downgradeAsync", WriteLockHandle::class.java)
            .returnType shouldBeEqualTo CompletableFuture::class.java

        assertGenericHandle(
            LettuceReadWriteLock.ReadLockView::class.java.getMethod(
                "tryAcquire",
                LockOwnerId::class.java,
                LockRequestId::class.java,
                LeasePolicy::class.java,
            ),
            LockAcquireResult::class.java,
            ReadLockHandle::class.java,
            "read view",
        )
        assertGenericHandle(
            LettuceReadWriteLock.WriteLockView::class.java.getMethod(
                "tryAcquire",
                LockOwnerId::class.java,
                LockRequestId::class.java,
                LeasePolicy::class.java,
            ),
            LockAcquireResult::class.java,
            WriteLockHandle::class.java,
            "write view",
        )
        assertReadWriteViewLifecycle(
            LettuceReadWriteLock.ReadLockView::class.java,
            ReadLockHandle::class.java,
            "read view",
        )
        assertReadWriteViewLifecycle(
            LettuceReadWriteLock.WriteLockView::class.java,
            WriteLockHandle::class.java,
            "write view",
        )
    }

    @Test
    fun `suspend counterparts expose suspending lifecycle and non suspending close`() {
        val families = listOf(
            Triple(LettuceSuspendDistributedLock::class.java, LockHandle::class.java, "distributed"),
            Triple(LettuceSuspendFairLock::class.java, LockHandle::class.java, "fair"),
            Triple(LettuceSuspendFencedLock::class.java, FencedLockHandle::class.java, "fenced"),
            Triple(LettuceSuspendSpinLock::class.java, LockHandle::class.java, "spin"),
            Triple(LettuceSuspendMultiLock::class.java, MultiLockHandle::class.java, "multi"),
        )

        families.forEach { (type, handleType, label) ->
            type.getMethod(
                "tryAcquire",
                LockOwnerId::class.java,
                LockRequestId::class.java,
                LeasePolicy::class.java,
                Continuation::class.java,
            ).returnType shouldBeEqualTo Any::class.java
            type.getMethod(
                "release",
                handleType,
                Continuation::class.java,
            ).returnType shouldBeEqualTo Any::class.java
            val close = type.getMethod("close")
            close.parameterCount shouldBeEqualTo 0
            close.returnType shouldBeEqualTo Void.TYPE
            label.isNotBlank().shouldBeTrue()
        }

        LettuceSuspendFencedLock::class.java.getMethod(
            "bootstrapFencing",
            Continuation::class.java,
        ).returnType shouldBeEqualTo Any::class.java
        LettuceSuspendReadWriteLock::class.java.getMethod("readLock")
            .returnType shouldBeEqualTo LettuceSuspendReadWriteLock.ReadLockView::class.java
        LettuceSuspendReadWriteLock::class.java.getMethod("writeLock")
            .returnType shouldBeEqualTo LettuceSuspendReadWriteLock.WriteLockView::class.java
        LettuceSuspendReadWriteLock::class.java.getMethod(
            "downgrade",
            WriteLockHandle::class.java,
            Continuation::class.java,
        ).returnType shouldBeEqualTo Any::class.java
        LettuceSuspendReadWriteLock::class.java.getMethod("close")
            .returnType shouldBeEqualTo Void.TYPE
        assertSuspendReadWriteViewLifecycle(
            LettuceSuspendReadWriteLock.ReadLockView::class.java,
            ReadLockHandle::class.java,
        )
        assertSuspendReadWriteViewLifecycle(
            LettuceSuspendReadWriteLock.WriteLockView::class.java,
            WriteLockHandle::class.java,
        )
    }

    private fun assertReadWriteViewLifecycle(
        type: Class<*>,
        handleType: Class<out Serializable>,
        label: String,
    ) {
        val acquisitionParameters = arrayOf(
            LockOwnerId::class.java,
            LockRequestId::class.java,
            LeasePolicy::class.java,
        )
        assertGenericHandle(
            type.getMethod("tryAcquire", *acquisitionParameters),
            LockAcquireResult::class.java,
            handleType,
            "$label tryAcquire",
        )
        assertGenericHandle(
            type.getMethod("tryAcquireAsync", *acquisitionParameters),
            CompletableFuture::class.java,
            handleType,
            "$label tryAcquireAsync",
        )
        val waitingParameters = arrayOf(
            LockOwnerId::class.java,
            LockRequestId::class.java,
            Duration::class.java,
            LeasePolicy::class.java,
        )
        assertGenericHandle(
            type.getMethod("acquire", *waitingParameters),
            LockAcquireResult::class.java,
            handleType,
            "$label acquire",
        )
        assertGenericHandle(
            type.getMethod("acquireAsync", *waitingParameters),
            CompletableFuture::class.java,
            handleType,
            "$label acquireAsync",
        )
        listOf("inspect" to LockInspectResult::class.java, "release" to LockMutationResult::class.java).forEach {
            (methodName, resultType) ->
            assertGenericHandle(
                type.getMethod(methodName, handleType),
                resultType,
                handleType,
                "$label $methodName",
            )
            assertGenericHandle(
                type.getMethod("${methodName}Async", handleType),
                CompletableFuture::class.java,
                handleType,
                "$label ${methodName}Async",
            )
        }
        assertGenericHandle(
            type.getMethod("reconcile", LockOwnerId::class.java, LockRequestId::class.java),
            LockReconcileResult::class.java,
            handleType,
            "$label reconcile",
        )
        assertGenericHandle(
            type.getMethod("reconcileAsync", LockOwnerId::class.java, LockRequestId::class.java),
            CompletableFuture::class.java,
            handleType,
            "$label reconcileAsync",
        )
        assertGenericHandle(
            type.getMethod("renew", handleType, Duration::class.java),
            LockMutationResult::class.java,
            handleType,
            "$label renew",
        )
        assertGenericHandle(
            type.getMethod("renewAsync", handleType, Duration::class.java),
            CompletableFuture::class.java,
            handleType,
            "$label renewAsync",
        )
    }

    private fun assertSuspendReadWriteViewLifecycle(
        type: Class<*>,
        handleType: Class<out Serializable>,
    ) {
        val lifecycleSignatures = listOf(
            "tryAcquire" to listOf(
                LockOwnerId::class.java,
                LockRequestId::class.java,
                LeasePolicy::class.java,
                Continuation::class.java,
            ),
            "acquire" to listOf(
                LockOwnerId::class.java,
                LockRequestId::class.java,
                Duration::class.java,
                LeasePolicy::class.java,
                Continuation::class.java,
            ),
            "inspect" to listOf(handleType, Continuation::class.java),
            "reconcile" to listOf(
                LockOwnerId::class.java,
                LockRequestId::class.java,
                Continuation::class.java,
            ),
            "renew" to listOf(handleType, Duration::class.java, Continuation::class.java),
            "release" to listOf(handleType, Continuation::class.java),
        )
        lifecycleSignatures.forEach { (methodName, parameterTypes) ->
            type.getMethod(methodName, *parameterTypes.toTypedArray())
                .returnType shouldBeEqualTo Any::class.java
        }
    }

    private fun assertGenericHandle(
        method: java.lang.reflect.Method,
        rawReturnType: Class<*>,
        handleType: Class<out Serializable>,
        label: String,
    ) {
        method.returnType shouldBeEqualTo rawReturnType
        method.genericReturnType.typeName.contains(handleType.name).shouldBeTrue()
        label.isNotBlank().shouldBeTrue()
    }

    private fun legacyCreateConstructors(type: Class<*>): List<List<Class<*>>> =
        type.declaredConstructors
            .asSequence()
            .map { it.parameterTypes.toList() }
            .filter { it.isNotEmpty() && it.size == 3 }
            .filter { it[0] == StatefulRedisConnection::class.java }
            .filter { it[1] == String::class.java }
            .filter { it[2] == Duration::class.java }
            .toList()

    @Suppress("unused")
    private fun compileBlockingAndAsyncExamples(
        lock: LettuceDistributedLock,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ) {
        val result = lock.tryAcquire(ownerId, requestId, LeasePolicy.Fixed(Duration.ofSeconds(10)))
        if (result is LockAcquireResult.Acquired) {
            lock.inspect(result.handle)
            lock.release(result.handle)
        }

        lock.tryAcquireAsync(ownerId, requestId, LeasePolicy.Fixed(Duration.ofSeconds(10)))
            .thenApply { asyncResult ->
                when (asyncResult) {
                    is LockAcquireResult.Acquired -> lock.releaseAsync(asyncResult.handle)
                    else -> CompletableFuture.completedFuture(null)
                }
            }
            .thenCompose { it }

        val outer = lock.tryAcquire(
            ownerId,
            LockRequestId.random(),
            LeasePolicy.Fixed(Duration.ofSeconds(10)),
        ) as LockAcquireResult.Acquired
        val inner = lock.tryAcquire(
            ownerId,
            LockRequestId.random(),
            LeasePolicy.Fixed(Duration.ofSeconds(10)),
        ) as LockAcquireResult.Reentered
        lock.release(inner.handle)
        lock.release(outer.handle)
        lock.release(outer.handle)
    }

    @Suppress("unused")
    private suspend fun compileSuspendExample(
        lock: LettuceSuspendDistributedLock,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ) {
        val result = lock.tryAcquire(ownerId, requestId, LeasePolicy.Fixed(Duration.ofSeconds(10)))
        if (result is LockAcquireResult.Acquired) {
            lock.inspect(result.handle)
            lock.release(result.handle)
        }
        lock.close()
    }
}
