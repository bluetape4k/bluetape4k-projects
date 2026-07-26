package io.bluetape4k.redis.lettuce.lock;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LettuceLockJavaDocumentationTest {

    @Test
    void identitiesAndResultVariantsAreVisibleFromJava() {
        assertNotNull(LockOwnerId.random());
        assertNotNull(LockRequestId.random());
        assertNotNull(LockAcquireResult.Closed.INSTANCE);
        assertNotNull(LockAcquireResult.TimedOut.INSTANCE);
        assertNotNull(LockMutationResult.AlreadyReleased.INSTANCE);
        assertNotNull(LockMutationResult.OwnershipLost.INSTANCE);
    }

    /**
     * Compile fixture for the standalone and Cluster factory/config shapes of all six blocking object families.
     * It is deliberately not executed because the connections are supplied by application infrastructure.
     */
    @SuppressWarnings("unused")
    static void compileFactories(
        StatefulRedisConnection<String, String> standalone,
        StatefulRedisClusterConnection<String, String> cluster,
        LockConfig lockConfig,
        FairLockConfig fairConfig,
        FencedLockConfig fencedConfig,
        ReadWriteLockConfig readWriteConfig,
        SpinLockConfig spinConfig,
        MultiLockConfig multiConfig
    ) {
        LettuceDistributedLock.create(standalone, "orders");
        LettuceDistributedLock.create(standalone, "orders", lockConfig);
        LettuceDistributedLock.create(cluster, "orders");
        LettuceDistributedLock.create(cluster, "orders", lockConfig);

        LettuceFairLock.create(standalone, "orders");
        LettuceFairLock.create(standalone, "orders", fairConfig);
        LettuceFairLock.create(cluster, "orders");
        LettuceFairLock.create(cluster, "orders", fairConfig);

        LettuceFencedLock.create(standalone, "orders", fencedConfig);
        LettuceFencedLock.create(cluster, "orders", fencedConfig);

        LettuceReadWriteLock.create(standalone, "orders");
        LettuceReadWriteLock.create(standalone, "orders", readWriteConfig);
        LettuceReadWriteLock.create(cluster, "orders");
        LettuceReadWriteLock.create(cluster, "orders", readWriteConfig);

        LettuceSpinLock.create(standalone, "orders");
        LettuceSpinLock.create(standalone, "orders", spinConfig);
        LettuceSpinLock.create(cluster, "orders");
        LettuceSpinLock.create(cluster, "orders", spinConfig);

        LettuceMultiLock.create(standalone, List.of("inventory-a", "inventory-b"));
        LettuceMultiLock.create(standalone, List.of("inventory-a", "inventory-b"), multiConfig);
        LettuceMultiLock.create(cluster, List.of("inventory-a", "inventory-b"));
        LettuceMultiLock.create(cluster, List.of("inventory-a", "inventory-b"), multiConfig);

        LettuceSuspendDistributedLock.create(standalone, "orders", lockConfig).close();
        LettuceSuspendFairLock.create(cluster, "orders", fairConfig).close();
        LettuceSuspendFencedLock.create(standalone, "orders", fencedConfig).close();
        LettuceSuspendReadWriteLock.create(cluster, "orders", readWriteConfig).close();
        LettuceSuspendSpinLock.create(standalone, "orders", spinConfig).close();
        LettuceSuspendMultiLock.create(cluster, List.of("inventory-a", "inventory-b"), multiConfig).close();
    }

    /**
     * Compile fixture for typed results, specialized handles, async futures, negative/closed variants, and
     * handle-based release.
     */
    @SuppressWarnings({"unused", "ConstantValue"})
    static void compileLifecycle(
        LettuceDistributedLock distributed,
        LettuceFencedLock fenced,
        LettuceReadWriteLock readWrite,
        LettuceMultiLock multi
    ) {
        LockOwnerId ownerId = LockOwnerId.random();
        LockRequestId requestId = LockRequestId.random();
        LeasePolicy lease = new LeasePolicy.Fixed(Duration.ofSeconds(15));

        LockAcquireResult<LockHandle> result = distributed.tryAcquire(ownerId, requestId, lease);
        if (result instanceof LockAcquireResult.Acquired<?> acquired) {
            LockHandle handle = (LockHandle) acquired.getHandle();
            distributed.release(handle);
        } else if (result instanceof LockAcquireResult.Reentered<?> reentered) {
            LockHandle handle = (LockHandle) reentered.getHandle();
            distributed.release(handle);
        } else if (result instanceof LockAcquireResult.Ambiguous ambiguous) {
            LockReconcileResult<LockHandle> reconciled =
                distributed.reconcile(ambiguous.getOwnerId(), ambiguous.getRequestId());
            if (reconciled instanceof LockReconcileResult.Owned<?> owned) {
                distributed.release((LockHandle) owned.getHandle());
            }
        } else if (result instanceof LockAcquireResult.Contended contended) {
            long remainingTtlMillis = contended.getRemainingTtlMillis();
        } else if (result == LockAcquireResult.Closed.INSTANCE) {
            distributed.close();
        }

        CompletableFuture<LockAcquireResult<LockHandle>> async =
            distributed.acquireAsync(ownerId, requestId, Duration.ofSeconds(2), lease);
        async.thenCompose(acquired -> {
            CompletableFuture<?> completion;
            if (acquired instanceof LockAcquireResult.Acquired<?> success) {
                completion = distributed.releaseAsync((LockHandle) success.getHandle());
            } else if (acquired instanceof LockAcquireResult.Reentered<?> reentered) {
                completion = distributed.releaseAsync((LockHandle) reentered.getHandle());
            } else if (acquired instanceof LockAcquireResult.Ambiguous ambiguous) {
                completion = distributed.reconcileAsync(ambiguous.getOwnerId(), ambiguous.getRequestId())
                    .thenCompose(reconciled -> {
                        if (reconciled instanceof LockReconcileResult.Owned<?> owned) {
                            return distributed.releaseAsync((LockHandle) owned.getHandle());
                        }
                        return CompletableFuture.completedFuture(null);
                    });
            } else {
                completion = CompletableFuture.completedFuture(null);
            }
            return completion.thenAccept(ignored -> {});
        });

        LockAcquireResult<FencedLockHandle> fencedResult = fenced.tryAcquire(ownerId, requestId, lease);
        if (fencedResult instanceof LockAcquireResult.Acquired<?> acquired) {
            FencedLockHandle handle = (FencedLockHandle) acquired.getHandle();
            fenced.release(handle);
        }

        LockAcquireResult<ReadLockHandle> readResult = readWrite.readLock().tryAcquire(ownerId, requestId, lease);
        if (readResult instanceof LockAcquireResult.Acquired<?> acquired) {
            ReadLockHandle handle = (ReadLockHandle) acquired.getHandle();
            readWrite.readLock().release(handle);
        }

        LockAcquireResult<WriteLockHandle> writeResult = readWrite.writeLock().tryAcquire(ownerId, requestId, lease);
        if (writeResult instanceof LockAcquireResult.Acquired<?> acquired) {
            WriteLockHandle handle = (WriteLockHandle) acquired.getHandle();
            readWrite.downgrade(handle);
        }

        LockAcquireResult<MultiLockHandle> multiResult = multi.tryAcquire(ownerId, requestId, lease);
        if (multiResult instanceof LockAcquireResult.Acquired<?> acquired) {
            MultiLockHandle handle = (MultiLockHandle) acquired.getHandle();
            multi.release(handle);
        }
    }
}
