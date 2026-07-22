package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.future.await
import java.time.Duration

internal class LettuceFencingLeaseTest : FencingLeaseContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): FencingLeaseAdapter {
        val lease = LettuceFencingLease(connection, config)
        return object: FencingLeaseAdapter {
            override suspend fun bootstrap(): FencingBootstrapResult = lease.bootstrap()
            override suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult =
                lease.acquire(ownerId, leaseTime)
            override suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult = lease.inspect(ownerId)
            override suspend fun renew(
                ownerId: FencingOwnerId,
                token: FencingToken,
                leaseTime: Duration,
            ): FencingRenewResult = lease.renew(ownerId, token, leaseTime)
            override suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult =
                lease.release(ownerId, token)
        }
    }
}

internal class LettuceFencingLeaseFutureTest : FencingLeaseContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): FencingLeaseAdapter {
        val lease = LettuceFencingLease(connection, config)
        return object: FencingLeaseAdapter {
            override suspend fun bootstrap(): FencingBootstrapResult = lease.bootstrapAsync().await()
            override suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult =
                lease.acquireAsync(ownerId, leaseTime).await()
            override suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult = lease.inspectAsync(ownerId).await()
            override suspend fun renew(
                ownerId: FencingOwnerId,
                token: FencingToken,
                leaseTime: Duration,
            ): FencingRenewResult = lease.renewAsync(ownerId, token, leaseTime).await()
            override suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult =
                lease.releaseAsync(ownerId, token).await()
        }
    }
}

@Suppress("unused")
private suspend fun compileFencingLeasePublicApi(
    standalone: StatefulRedisConnection<String, String>,
    cluster: StatefulRedisClusterConnection<String, String>,
    config: LettuceFencingLeaseConfig,
    ownerId: FencingOwnerId,
    token: FencingToken,
    leaseTime: Duration,
) {
    listOf(
        LettuceFencingLease(standalone, config),
        LettuceFencingLease(cluster, config),
    ).forEach { lease ->
        lease.bootstrap()
        lease.bootstrapAsync()
        lease.acquire(ownerId, leaseTime)
        lease.acquireAsync(ownerId, leaseTime)
        lease.inspect(ownerId)
        lease.inspectAsync(ownerId)
        lease.renew(ownerId, token, leaseTime)
        lease.renewAsync(ownerId, token, leaseTime)
        lease.release(ownerId, token)
        lease.releaseAsync(ownerId, token)
    }
    listOf(
        LettuceSuspendFencingLease(standalone, config),
        LettuceSuspendFencingLease(cluster, config),
    ).forEach { lease ->
        lease.bootstrap()
        lease.acquire(ownerId, leaseTime)
        lease.inspect(ownerId)
        lease.renew(ownerId, token, leaseTime)
        lease.release(ownerId, token)
    }
}
