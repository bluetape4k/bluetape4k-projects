package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import java.time.Duration

/**
 * Provides suspending, stateless Redis operations for one ownership lease spanning multiple keys.
 *
 * Every operation executes one Lua script, but this primitive is an advisory coordination boundary rather than a
 * distributed transaction or durable source of truth. All keys must resolve to one Redis Cluster slot. A shared hash
 * tag such as `ticket:{sale-42}:ip` and `ticket:{sale-42}:user` is the usual way to preserve that routing constraint.
 *
 * Generate a high-entropy owner token for each logical acquisition and reuse that same token for inspection,
 * renewal, release, and ambiguous-completion recovery. Do not reuse credentials, JWTs, personal identifiers, or
 * session tokens, and do not place lease keys or owner tokens in logs or metric labels.
 *
 * Coroutine cancellation cancels a pending Redis future, but it does not prove that a dispatched script did not
 * execute. Recover an ambiguous mutation with the same owner token and an authoritative state check.
 */
class LettuceSuspendMultiKeyLease private constructor(
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    private val codec: RedisCodec<String, String>,
    private val config: LettuceMultiKeyLeaseConfig,
) {

    /** Creates a suspending lease facade for a standalone Redis connection. */
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.async(), connection.codec, config)

    /** Creates a suspending lease facade for a Redis Cluster connection. */
    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.async(), connection.codec, config)

    init {
        require(config.maxKeys > 0) { "maxKeys must be positive." }
    }

    /** Acquires every [keys] entry for [ownerToken] with [leaseTime], or reports the observed ownership state. */
    suspend fun acquire(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyAcquireResult = runAcquireSuspending(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, leaseTime, config, codec),
        ownerToken,
    )

    /** Inspects whether [ownerToken] owns every [keys] entry without changing Redis state. */
    suspend fun inspect(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyInspectResult = runInspectSuspending(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, null, config, codec),
        ownerToken,
    )

    /** Renews matching [keys] entries owned by [ownerToken] to [leaseTime], without repairing missing keys. */
    suspend fun renew(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyRenewResult = runRenewSuspending(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, leaseTime, config, codec),
        ownerToken,
    )

    /** Releases matching [keys] entries owned by [ownerToken], including persistent same-token recovery keys. */
    suspend fun release(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyReleaseResult = runReleaseSuspending(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, null, config, codec),
        ownerToken,
    )
}
