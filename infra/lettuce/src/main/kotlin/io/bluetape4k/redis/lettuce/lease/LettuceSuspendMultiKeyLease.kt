package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.support.requirePositiveNumber
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import java.time.Duration

/**
 * Provides suspending, stateless Redis operations for one ownership lease spanning multiple keys.
 *
 * Every operation executes one Lua script, but this primitive is a single-writer advisory coordination boundary,
 * not a distributed transaction or durable source of truth. All keys must resolve to one Redis Cluster slot. A
 * shared hash tag such as `ticket:{sale-42}:ip` and `ticket:{sale-42}:user` preserves that routing constraint.
 * Validation calculates the slot from the actual wire bytes produced by the connection's [RedisCodec.encodeKey], so
 * a custom String key codec is checked with the same bytes that Lettuce uses for client-side routing.
 *
 * Generate an external high-entropy owner token once per logical acquisition and reuse it for every attempt,
 * inspection, renewal, release, and ambiguous-completion recovery. Only acquire supports deterministic same-token
 * replay, which returns [MultiKeyAcquireResult.AlreadyOwned]; renew and release must recover an ambiguous completion
 * by inspecting with the same token first. Inspect is read-only and may be retried under a bounded transport policy.
 * Partial ownership, partial release, or ownership mismatch requires
 * reconciliation against a durable authority. A persistent same-token key is an integrity failure reported as
 * [MultiKeyLeaseIntegrityException], not a normal lease.
 *
 * The owner token is not an authentication credential. Never reuse a JWT, session token, user identifier, or PII,
 * and never place lease keys or tokens in logs or metric labels. Redis stores the token as plaintext; Redis ACLs and
 * TLS are the actual confidentiality and access-control boundary.
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
        config.maxKeys.requirePositiveNumber("config.maxKeys")
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
