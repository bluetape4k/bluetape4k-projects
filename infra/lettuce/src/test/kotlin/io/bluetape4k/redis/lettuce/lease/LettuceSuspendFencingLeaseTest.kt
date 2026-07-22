package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import java.time.Duration

internal class LettuceSuspendFencingLeaseTest : FencingLeaseContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): FencingLeaseAdapter {
        val lease = LettuceSuspendFencingLease(connection, config)
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
