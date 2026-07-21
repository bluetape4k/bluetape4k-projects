package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Provides stateless, atomic Redis operations for one ownership lease spanning multiple keys.
 *
 * Every operation executes one Lua script, but this primitive is an advisory coordination boundary rather than a
 * distributed transaction or durable source of truth. All keys must resolve to one Redis Cluster slot. A shared hash
 * tag such as `ticket:{sale-42}:ip` and `ticket:{sale-42}:user` is the usual way to preserve that routing constraint.
 *
 * Generate a high-entropy owner token for each logical acquisition and reuse that same token for inspection,
 * renewal, release, and ambiguous-completion recovery. Do not reuse credentials, JWTs, personal identifiers, or
 * session tokens, and do not place lease keys or owner tokens in logs or metric labels.
 *
 * Cancelling or timing out a returned [CompletableFuture] only cancels the caller's wait. It does not prove that the
 * Redis script did not execute, and it does not guarantee cancellation of the upstream Redis command. Recover an
 * ambiguous mutation with the same owner token and an authoritative state check.
 */
class LettuceMultiKeyLease private constructor(
    private val syncCommands: RedisScriptingCommands<String, String>,
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    private val codec: RedisCodec<String, String>,
    private val config: LettuceMultiKeyLeaseConfig,
) {

    /** Creates a lease facade for a standalone Redis connection. */
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.sync(), connection.async(), connection.codec, config)

    /** Creates a lease facade for a Redis Cluster connection. */
    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.sync(), connection.async(), connection.codec, config)

    init {
        require(config.maxKeys > 0) { "maxKeys must be positive." }
    }

    /** Acquires every [keys] entry for [ownerToken] with [leaseTime], or reports the observed ownership state. */
    fun acquire(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyAcquireResult = runAcquire(
        syncCommands,
        validateLeaseInput(keys, ownerToken, leaseTime, config, codec),
        ownerToken,
    )

    /** Asynchronously acquires every [keys] entry for [ownerToken] with [leaseTime]. */
    fun acquireAsync(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): CompletableFuture<MultiKeyAcquireResult> = runAcquireAsync(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, leaseTime, config, codec),
        ownerToken,
    )

    /** Inspects whether [ownerToken] owns every [keys] entry without changing Redis state. */
    fun inspect(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyInspectResult = runInspect(
        syncCommands,
        validateLeaseInput(keys, ownerToken, null, config, codec),
        ownerToken,
    )

    /** Asynchronously inspects whether [ownerToken] owns every [keys] entry without changing Redis state. */
    fun inspectAsync(
        keys: Collection<String>,
        ownerToken: String,
    ): CompletableFuture<MultiKeyInspectResult> = runInspectAsync(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, null, config, codec),
        ownerToken,
    )

    /** Renews matching [keys] entries owned by [ownerToken] to [leaseTime]. */
    fun renew(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyRenewResult = runRenew(
        syncCommands,
        validateLeaseInput(keys, ownerToken, leaseTime, config, codec),
        ownerToken,
    )

    /** Asynchronously renews matching [keys] entries owned by [ownerToken] to [leaseTime]. */
    fun renewAsync(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): CompletableFuture<MultiKeyRenewResult> = runRenewAsync(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, leaseTime, config, codec),
        ownerToken,
    )

    /** Releases matching [keys] entries owned by [ownerToken], including persistent same-token recovery keys. */
    fun release(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyReleaseResult = runRelease(
        syncCommands,
        validateLeaseInput(keys, ownerToken, null, config, codec),
        ownerToken,
    )

    /** Asynchronously releases matching [keys] entries owned by [ownerToken]. */
    fun releaseAsync(
        keys: Collection<String>,
        ownerToken: String,
    ): CompletableFuture<MultiKeyReleaseResult> = runReleaseAsync(
        asyncCommands,
        validateLeaseInput(keys, ownerToken, null, config, codec),
        ownerToken,
    )
}
