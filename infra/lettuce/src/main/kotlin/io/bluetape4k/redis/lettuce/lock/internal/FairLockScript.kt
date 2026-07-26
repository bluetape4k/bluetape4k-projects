package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocol
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.FairLockConfig
import io.bluetape4k.redis.lettuce.lock.FairWaiterState
import io.bluetape4k.redis.lettuce.lock.FairWaiterStatus
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockBackendFailure
import io.bluetape4k.redis.lettuce.lock.LockBackendFailureKind
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailureKind
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockObservation
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRecoveryAction
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal val FAIR_LOCK_SCRIPT = RedisScript(
    """
    local operation = ARGV[1]

    local function key_type(key)
        local result = redis.call('TYPE', key)
        if type(result) == 'table' then
            return result.ok
        end
        return result
    end

    local function positive_decimal(value)
        return type(value) == 'string'
            and string.len(value) <= 16
            and string.match(value, '^[1-9][0-9]*$') ~= nil
    end

    local function non_negative_decimal(value)
        return value == '0' or positive_decimal(value)
    end

    local function compare_decimal(left, right)
        if string.len(left) ~= string.len(right) then
            return string.len(left) < string.len(right) and -1 or 1
        end
        if left == right then
            return 0
        end
        return left < right and -1 or 1
    end

    local function parse_hold(value)
        if type(value) ~= 'string' or string.len(value) > 128 then
            return nil, nil, nil
        end
        local generation, acquisition, policy = string.match(value, '^([1-9][0-9]*)|([AR])|(.+)$')
        if not positive_decimal(generation) or acquisition == nil or policy == nil then
            return nil, nil, nil
        end
        return generation, acquisition, policy
    end

    local function parse_waiter(value)
        if type(value) ~= 'string' or string.len(value) > 57 then
            return nil, nil, nil
        end
        local sequence, generation, deadline =
            string.match(value, '^([1-9][0-9]*)|([0-9]+)|([1-9][0-9]*)$')
        if not positive_decimal(sequence) or not non_negative_decimal(generation) or not positive_decimal(deadline)
            or compare_decimal(sequence, '9007199254740991') > 0
            or compare_decimal(generation, '9007199254740991') > 0
            or compare_decimal(deadline, '9007199254740991') > 0 then
            return nil, nil, nil
        end
        return sequence, generation, deadline
    end

    local function now_millis()
        local time = redis.call('TIME')
        return tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
    end

    local function validate()
        local generation_type = key_type(KEYS[2])
        if generation_type ~= 'none' and generation_type ~= 'string' then
            return false, false
        end
        if generation_type == 'string' then
            local counter = redis.call('GET', KEYS[2])
            if not positive_decimal(counter) or redis.call('PTTL', KEYS[2]) ~= -1 then
                return false, false
            end
        end

        local sequence_type = key_type(KEYS[7])
        if sequence_type ~= 'none' and sequence_type ~= 'string' then
            return false, false
        end
        if sequence_type == 'string' then
            local sequence = redis.call('GET', KEYS[7])
            if not positive_decimal(sequence) or redis.call('PTTL', KEYS[7]) ~= -1 then
                return false, false
            end
        end

        local queue_type = key_type(KEYS[5])
        local waiters_type = key_type(KEYS[6])
        if queue_type == 'none' and waiters_type ~= 'none' then
            return false, false
        end
        if queue_type ~= 'none' and queue_type ~= 'zset' then
            return false, false
        end
        if waiters_type ~= 'none' and waiters_type ~= 'hash' then
            return false, false
        end
        if redis.call('ZCARD', KEYS[5]) ~= redis.call('HLEN', KEYS[6]) then
            return false, false
        end

        local terminal_type = key_type(KEYS[4])
        if terminal_type ~= 'none' then
            if terminal_type ~= 'hash' or redis.call('PTTL', KEYS[4]) <= 0 then
                return false, false
            end
            local terminal_owner = redis.call('HGET', KEYS[4], 'owner')
            local terminal_generation = redis.call('HGET', KEYS[4], 'generation')
            local terminal_request = redis.call('HGET', KEYS[4], 'request')
            if terminal_owner == false or string.len(terminal_owner) < 1 or string.len(terminal_owner) > 256
                or not positive_decimal(terminal_generation)
                or terminal_request == false or string.len(terminal_request) < 1 or string.len(terminal_request) > 256
                or redis.call('HLEN', KEYS[4]) ~= 3 then
                return false, false
            end
        end

        local state_type = key_type(KEYS[1])
        local holds_type = key_type(KEYS[3])
        if state_type == 'none' then
            return holds_type == 'none', false
        end
        if state_type ~= 'hash' or holds_type ~= 'hash' then
            return false, false
        end
        if redis.call('PTTL', KEYS[1]) <= 0 or redis.call('PTTL', KEYS[3]) <= 0 then
            return false, false
        end
        local owner = redis.call('HGET', KEYS[1], 'owner')
        local generation = redis.call('HGET', KEYS[1], 'generation')
        local hold_count = redis.call('HGET', KEYS[1], 'holdCount')
        local counter = redis.call('GET', KEYS[2])
        if owner == false or string.len(owner) < 1 or string.len(owner) > 256
            or not positive_decimal(generation)
            or not positive_decimal(hold_count)
            or not positive_decimal(counter)
            or compare_decimal(generation, counter) > 0
            or tonumber(hold_count) ~= redis.call('HLEN', KEYS[3])
            or redis.call('HLEN', KEYS[1]) ~= 3 then
            return false, false
        end
        return true, true
    end

    local function cleanup_stale(batch, now)
        local queue_size = redis.call('ZCARD', KEYS[5])
        local candidates = redis.call('ZRANGE', KEYS[5], 0, batch - 1, 'WITHSCORES')
        local stale = {}
        for index = 1, #candidates, 2 do
            local member = candidates[index]
            local score = candidates[index + 1]
            local value = redis.call('HGET', KEYS[6], member)
            local sequence, generation, deadline = parse_waiter(value)
            if sequence == nil or sequence ~= score then
                return nil
            end
            if tonumber(deadline) <= now then
                stale[#stale + 1] = member
            else
                break
            end
        end
        for index = 1, #stale do
            redis.call('ZREM', KEYS[5], stale[index])
            redis.call('HDEL', KEYS[6], stale[index])
        end
        return #stale == batch and queue_size > batch
    end

    local valid, active = validate()
    if not valid then
        return {'INTEGRITY'}
    end

    if operation == 'ACQUIRE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        local member = ARGV[4]
        local policy = ARGV[5]
        local ttl = tonumber(ARGV[6])
        local maximum = tonumber(ARGV[7])
        local wait_millis = tonumber(ARGV[8])
        local cleanup_batch = tonumber(ARGV[9])
        local maximum_queue = tonumber(ARGV[10])
        local now = now_millis()

        if active and redis.call('HGET', KEYS[1], 'owner') == owner then
            local generation = redis.call('HGET', KEYS[1], 'generation')
            local hold_count = tonumber(redis.call('HGET', KEYS[1], 'holdCount'))
            local queued = redis.call('HGET', KEYS[6], member)
            if queued ~= false then
                local queued_sequence = parse_waiter(queued)
                if queued_sequence == nil or redis.call('ZSCORE', KEYS[5], member) ~= queued_sequence then
                    return {'INTEGRITY'}
                end
            end
            local existing = redis.call('HGET', KEYS[3], request)
            if existing ~= false then
                local existing_generation, acquisition, existing_policy = parse_hold(existing)
                if existing_generation == nil or existing_generation ~= generation then
                    return {'INTEGRITY'}
                end
                if queued ~= false then
                    redis.call('ZREM', KEYS[5], member)
                    redis.call('HDEL', KEYS[6], member)
                end
                local tag = acquisition == 'A' and 'ACQUIRED' or 'REENTERED'
                return {tag, generation, tostring(hold_count), tostring(redis.call('PTTL', KEYS[1])), existing_policy}
            end
            if hold_count >= maximum then
                return {'CAPACITY'}
            end
            if queued ~= false then
                redis.call('ZREM', KEYS[5], member)
                redis.call('HDEL', KEYS[6], member)
            end
            hold_count = hold_count + 1
            redis.call('HSET', KEYS[1], 'holdCount', hold_count)
            redis.call('HSET', KEYS[3], request, generation .. '|R|' .. policy)
            redis.call('PEXPIRE', KEYS[1], ttl)
            redis.call('PEXPIRE', KEYS[3], ttl)
            return {'REENTERED', generation, tostring(hold_count), tostring(ttl), policy}
        end

        local cleanup_pending = cleanup_stale(cleanup_batch, now)
        if cleanup_pending == nil then
            return {'INTEGRITY'}
        end
        if cleanup_pending then
            return {'CLEANUP_PENDING'}
        end

        local waiter = redis.call('HGET', KEYS[6], member)
        local waiter_sequence = nil
        local waiter_generation = nil
        local waiter_deadline = nil
        if waiter ~= false then
            waiter_sequence, waiter_generation, waiter_deadline = parse_waiter(waiter)
            if waiter_sequence == nil or redis.call('ZSCORE', KEYS[5], member) ~= waiter_sequence then
                return {'INTEGRITY'}
            end
        elseif wait_millis > 0 then
            if redis.call('ZCARD', KEYS[5]) >= maximum_queue then
                return {'CAPACITY'}
            end
            local current_sequence = redis.call('GET', KEYS[7])
            if current_sequence ~= false and compare_decimal(current_sequence, '9007199254740990') > 0 then
                return {'CAPACITY'}
            end
            redis.call('INCR', KEYS[7])
            waiter_sequence = redis.call('GET', KEYS[7])
            waiter_generation = redis.call('GET', KEYS[2]) or '0'
            waiter_deadline = tostring(now + wait_millis)
            redis.call('ZADD', KEYS[5], waiter_sequence, member)
            redis.call(
                'HSET',
                KEYS[6],
                member,
                waiter_sequence .. '|' .. waiter_generation .. '|' .. waiter_deadline
            )
        end

        if not active then
            local head = redis.call('ZRANGE', KEYS[5], 0, 0)
            if #head == 0 or head[1] == member then
                if #head == 1 then
                    redis.call('ZREM', KEYS[5], member)
                    redis.call('HDEL', KEYS[6], member)
                end
                local previous_generation = redis.call('GET', KEYS[2])
                if previous_generation ~= false and compare_decimal(previous_generation, '9007199254740990') > 0 then
                    return {'CAPACITY'}
                end
                redis.call('INCR', KEYS[2])
                local generation = redis.call('GET', KEYS[2])
                redis.call('DEL', KEYS[4])
                redis.call('HSET', KEYS[1], 'owner', owner, 'generation', generation, 'holdCount', 1)
                redis.call('HSET', KEYS[3], request, generation .. '|A|' .. policy)
                redis.call('PEXPIRE', KEYS[1], ttl)
                redis.call('PEXPIRE', KEYS[3], ttl)
                return {'ACQUIRED', generation, '1', tostring(ttl), policy}
            end
        end

        local remaining = active and redis.call('PTTL', KEYS[1]) or 0
        if waiter_sequence ~= nil then
            local wait_remaining = math.max(0, tonumber(waiter_deadline) - now)
            return {'QUEUED', waiter_sequence, waiter_generation, tostring(wait_remaining), tostring(remaining)}
        end
        return {'CONTENDED', tostring(remaining)}
    end

    if operation == 'RECONCILE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        local member = ARGV[4]
        if active and redis.call('HGET', KEYS[1], 'owner') == owner then
            local hold = redis.call('HGET', KEYS[3], request)
            if hold ~= false then
                local generation, acquisition, policy = parse_hold(hold)
                if generation == nil or generation ~= redis.call('HGET', KEYS[1], 'generation') then
                    return {'INTEGRITY'}
                end
                return {'OWNED', generation, redis.call('HGET', KEYS[1], 'holdCount'), tostring(redis.call('PTTL', KEYS[1])), policy}
            end
        end
        local value = redis.call('HGET', KEYS[6], member)
        if value ~= false then
            local sequence, generation, deadline = parse_waiter(value)
            if sequence == nil or redis.call('ZSCORE', KEYS[5], member) ~= sequence then
                return {'INTEGRITY'}
            end
            local remaining = tonumber(deadline) - now_millis()
            if remaining <= 0 then
                if redis.call('HGET', KEYS[6], member) == value then
                    redis.call('HDEL', KEYS[6], member)
                    redis.call('ZREM', KEYS[5], member)
                    return {'REMOVED'}
                end
                return {'STALE'}
            end
            return {'QUEUED', sequence, generation, tostring(remaining)}
        end
        if key_type(KEYS[4]) == 'hash'
            and redis.call('HGET', KEYS[4], 'owner') == owner
            and redis.call('HGET', KEYS[4], 'request') == request then
            return {'RELEASED'}
        end
        return {'NOT_FOUND'}
    end

    if operation == 'REMOVE' then
        local member = ARGV[4]
        local expected_sequence = ARGV[5]
        local expected_generation = ARGV[6]
        local value = redis.call('HGET', KEYS[6], member)
        if value == false then
            return {'NOT_FOUND'}
        end
        local sequence, generation, deadline = parse_waiter(value)
        if sequence == nil or redis.call('ZSCORE', KEYS[5], member) ~= sequence then
            return {'INTEGRITY'}
        end
        if sequence ~= expected_sequence or generation ~= expected_generation then
            return {'STALE'}
        end
        if redis.call('HGET', KEYS[6], member) ~= value then
            return {'STALE'}
        end
        redis.call('HDEL', KEYS[6], member)
        redis.call('ZREM', KEYS[5], member)
        return {'REMOVED'}
    end
    return {'INTEGRITY'}
    """.trimIndent(),
)

internal enum class FairLockOperation(val wireValue: String) {
    ACQUIRE("ACQUIRE"),
    RECONCILE("RECONCILE"),
    REMOVE("REMOVE"),
}

internal data class FairLockKeys(
    val state: String,
    val generation: String,
    val holds: String,
    val terminal: String,
    val queue: String,
    val waiters: String,
    val sequence: String,
    val fingerprint: String,
) {
    val all: Array<String> = arrayOf(state, generation, holds, terminal, queue, waiters, sequence)

    val distributed: DistributedLockKeys =
        DistributedLockKeys(state, generation, holds, terminal, fingerprint)
}

internal fun deriveFairLockKeys(
    name: String,
    config: FairLockConfig,
    codec: RedisCodec<String, String>,
): FairLockKeys {
    val lock = config.lock
    val resource = lock.validateResourceName(name)
    val hashTag = lock.hashTag ?: resource
    val prefix = "${lock.namespace}:{$hashTag}:lock:$resource"
    val keys = listOf(
        lock.validateDerivedKey("$prefix:state"),
        lock.validateDerivedKey("$prefix:generation"),
        lock.validateDerivedKey("$prefix:holds"),
        lock.validateDerivedKey("$prefix:terminal"),
        lock.validateDerivedKey("$prefix:queue"),
        lock.validateDerivedKey("$prefix:waiters"),
        lock.validateDerivedKey("$prefix:queue-sequence"),
    )
    require(keys.map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet().size == 1) {
        "Derived fair lock keys must share one Redis Cluster slot."
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(codec.encodeKey(keys[0]).toByteArray())
    val fingerprint = digest.take(FAIR_FINGERPRINT_BYTES).joinToString("") { byte -> "%02x".format(byte) }
    return FairLockKeys(
        state = keys[0],
        generation = keys[1],
        holds = keys[2],
        terminal = keys[3],
        queue = keys[4],
        waiters = keys[5],
        sequence = keys[6],
        fingerprint = fingerprint,
    )
}

private interface FairLockCommandExecutor {
    fun run(operation: FairLockOperation, keys: FairLockKeys, args: List<String>): List<String>
    fun runAsync(
        operation: FairLockOperation,
        keys: FairLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>>
    suspend fun runSuspending(operation: FairLockOperation, keys: FairLockKeys, args: List<String>): List<String>
}

private class DefaultFairLockCommandExecutor(
    private val sync: RedisScriptingCommands<String, String>,
    private val async: RedisScriptingAsyncCommands<String, String>,
): FairLockCommandExecutor {
    override fun run(operation: FairLockOperation, keys: FairLockKeys, args: List<String>): List<String> =
        RedisScriptRunner.run(
            sync,
            FAIR_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.wireValue,
            *args.toTypedArray(),
        )

    override fun runAsync(
        operation: FairLockOperation,
        keys: FairLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>> =
        RedisScriptRunner.runAsync(
            async,
            FAIR_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.wireValue,
            *args.toTypedArray(),
        )

    override suspend fun runSuspending(
        operation: FairLockOperation,
        keys: FairLockKeys,
        args: List<String>,
    ): List<String> =
        RedisScriptRunner.runSuspending(
            async,
            FAIR_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.wireValue,
            *args.toTypedArray(),
        )
}

private sealed interface FairAttempt {
    data class Result(val value: LockAcquireResult<LockHandle>): FairAttempt
    data class Queued(
        val waiter: FairWaiterState,
        val identity: FairWaiterIdentity,
        val lockTtlMillis: Long,
    ): FairAttempt
}

internal data class FairWaiterIdentity(
    val sequence: Long,
    val generation: Long,
)

internal class FairLockClient private constructor(
    private val keys: FairLockKeys,
    private val config: FairLockConfig,
    private val executor: FairLockCommandExecutor,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val distributed: DistributedLockClient,
) {
    private val closed = AtomicBoolean()
    private val waitSupport = LockWaitSupport(registration, closed::get)

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, Duration.ZERO)
        if (closed.get()) return LockAcquireResult.Closed
        return fairClassified(
            backend = { LockAcquireResult.BackendFailure(it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            materializeAttempt(decodeAttempt(executor.run(FairLockOperation.ACQUIRE, keys, args), keys, ownerId, requestId))
        }
    }

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, Duration.ZERO)
        if (closed.get()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        return executor.runAsync(FairLockOperation.ACQUIRE, keys, args)
            .fairMap(
                decode = { decodeAttempt(it, keys, ownerId, requestId) },
                backend = { FairAttempt.Result(LockAcquireResult.BackendFailure(it)) },
                integrity = { FairAttempt.Result(LockAcquireResult.IntegrityFailure(it)) },
                action = LockRecoveryAction.RECONCILE_REQUEST,
            ).thenCompose(::materializeAttemptAsync)
    }

    suspend fun tryAcquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, Duration.ZERO)
        if (closed.get()) return LockAcquireResult.Closed
        return fairClassifiedSuspending(
            backend = { LockAcquireResult.BackendFailure(it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            materializeAttempt(
                decodeAttempt(executor.runSuspending(FairLockOperation.ACQUIRE, keys, args), keys, ownerId, requestId),
            )
        }
    }

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        validateWait(waitTime)
        val waiterIdentity = AtomicReference<FairWaiterIdentity?>()
        val result = waitSupport.acquire(waitTime) {
            acquireAttempt(ownerId, requestId, leasePolicy, waitTime, waiterIdentity)
        }
        return cleanupTimedOut(result, ownerId, requestId, waiterIdentity.get())
    }

    internal fun enqueueOnce(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        validateWait(waitTime)
        return acquireAttempt(ownerId, requestId, leasePolicy, waitTime, AtomicReference())
    }

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        validateWait(waitTime)
        val waiterIdentity = AtomicReference<FairWaiterIdentity?>()
        val pending = waitSupport.acquireAsync(waitTime) {
            acquireAttemptAsync(ownerId, requestId, leasePolicy, waitTime, waiterIdentity)
        }
        val result = CompletableFuture<LockAcquireResult<LockHandle>>()
        pending.whenComplete { acquired, error ->
            if (result.isDone) return@whenComplete
            if (error != null) {
                result.completeExceptionally(error)
            } else if (acquired == LockAcquireResult.TimedOut && waiterIdentity.get() != null) {
                removeWaiterAsync(ownerId, requestId, waiterIdentity.get()!!).whenComplete { removed, removeError ->
                    if (removeError != null) {
                        result.completeExceptionally(removeError)
                    } else {
                        result.complete(cleanupResult(removed, ownerId, requestId))
                    }
                }
            } else {
                result.complete(acquired)
            }
        }
        result.whenComplete { _, _ ->
            if (result.isCancelled) {
                pending.cancel(false)
                waiterIdentity.get()?.let { removeWaiterAsync(ownerId, requestId, it) }
            }
        }
        return result
    }

    suspend fun acquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        validateWait(waitTime)
        val waiterIdentity = AtomicReference<FairWaiterIdentity?>()
        return try {
            val result = waitSupport.acquireSuspending(waitTime) {
                acquireAttemptSuspending(ownerId, requestId, leasePolicy, waitTime, waiterIdentity)
            }
            if (result == LockAcquireResult.TimedOut && waiterIdentity.get() != null) {
                cleanupResult(
                    removeWaiterSuspending(ownerId, requestId, waiterIdentity.get()!!),
                    ownerId,
                    requestId,
                )
            } else {
                result
            }
        } catch (cancelled: CancellationException) {
            val identity = waiterIdentity.get()
            if (identity != null) {
                withContext(NonCancellable) {
                    removeWaiterSuspending(ownerId, requestId, identity)
                }
            }
            throw cancelled
        }
    }

    fun inspect(handle: LockHandle): LockInspectResult<LockHandle> =
        mapInspect(distributed.inspect(handle.toDistributed()))

    fun inspectAsync(handle: LockHandle): CompletableFuture<LockInspectResult<LockHandle>> =
        distributed.inspectAsync(handle.toDistributed()).thenApply(::mapInspect)

    suspend fun inspectSuspending(handle: LockHandle): LockInspectResult<LockHandle> =
        mapInspect(distributed.inspectSuspending(handle.toDistributed()))

    fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId): LockReconcileResult<LockHandle> {
        val args = listOf(ownerId.value, requestId.value, fairWaiterMember(ownerId, requestId))
        if (closed.get()) return LockReconcileResult.Closed
        return fairClassified(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            materializeReconcile(
                decodeFairReconcile(executor.run(FairLockOperation.RECONCILE, keys, args), keys, ownerId, requestId),
            )
        }
    }

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> {
        val args = listOf(ownerId.value, requestId.value, fairWaiterMember(ownerId, requestId))
        if (closed.get()) return CompletableFuture.completedFuture(LockReconcileResult.Closed)
        return executor.runAsync(FairLockOperation.RECONCILE, keys, args)
            .fairMap(
                decode = { decodeFairReconcile(it, keys, ownerId, requestId) },
                backend = { LockReconcileResult.BackendFailure(it) },
                integrity = { LockReconcileResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RECONCILE_REQUEST,
            ).thenCompose(::materializeReconcileAsync)
    }

    suspend fun reconcileSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        val args = listOf(ownerId.value, requestId.value, fairWaiterMember(ownerId, requestId))
        if (closed.get()) return LockReconcileResult.Closed
        return fairClassifiedSuspending(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            materializeReconcile(
                decodeFairReconcile(
                    executor.runSuspending(FairLockOperation.RECONCILE, keys, args),
                    keys,
                    ownerId,
                    requestId,
                ),
            )
        }
    }

    fun renew(handle: LockHandle, extension: Duration): LockMutationResult<LockHandle> =
        mapMutation(distributed.renew(handle.toDistributed(), extension))

    fun renewAsync(
        handle: LockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> =
        distributed.renewAsync(handle.toDistributed(), extension).thenApply(::mapMutation)

    suspend fun renewSuspending(handle: LockHandle, extension: Duration): LockMutationResult<LockHandle> =
        mapMutation(distributed.renewSuspending(handle.toDistributed(), extension))

    fun release(handle: LockHandle): LockMutationResult<LockHandle> =
        mapMutation(distributed.release(handle.toDistributed()))

    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> =
        distributed.releaseAsync(handle.toDistributed()).thenApply(::mapMutation)

    suspend fun releaseSuspending(handle: LockHandle): LockMutationResult<LockHandle> =
        mapMutation(distributed.releaseSuspending(handle.toDistributed()))

    fun close() {
        if (closed.compareAndSet(false, true)) {
            waitSupport.close()
            registration.close()
            distributed.close()
        }
    }

    private fun acquireAttempt(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
        waiterIdentity: AtomicReference<FairWaiterIdentity?>,
    ): LockAcquireResult<LockHandle> =
        fairClassified(
            backend = { LockAcquireResult.BackendFailure(it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAttempt(
                executor.run(FairLockOperation.ACQUIRE, keys, acquireArgs(ownerId, requestId, leasePolicy, waitTime)),
                keys,
                ownerId,
                requestId,
            ).toPublic(waiterIdentity)
        }

    private fun acquireAttemptAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
        waiterIdentity: AtomicReference<FairWaiterIdentity?>,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        executor.runAsync(
            FairLockOperation.ACQUIRE,
            keys,
            acquireArgs(ownerId, requestId, leasePolicy, waitTime),
        ).fairMap(
            decode = { decodeAttempt(it, keys, ownerId, requestId) },
            backend = { FairAttempt.Result(LockAcquireResult.BackendFailure(it)) },
            integrity = { FairAttempt.Result(LockAcquireResult.IntegrityFailure(it)) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ).thenCompose { attempt ->
            when (attempt) {
                is FairAttempt.Queued ->
                    CompletableFuture.completedFuture(attempt.toPublic(waiterIdentity))
                is FairAttempt.Result ->
                    materializeAttemptAsync(attempt).thenApply { it }
            }
        }

    private suspend fun acquireAttemptSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
        waiterIdentity: AtomicReference<FairWaiterIdentity?>,
    ): LockAcquireResult<LockHandle> =
        fairClassifiedSuspending(
            backend = { LockAcquireResult.BackendFailure(it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            when (val attempt = decodeAttempt(
                executor.runSuspending(
                    FairLockOperation.ACQUIRE,
                    keys,
                    acquireArgs(ownerId, requestId, leasePolicy, waitTime),
                ),
                keys,
                ownerId,
                requestId,
            )) {
                is FairAttempt.Queued -> attempt.toPublic(waiterIdentity)
                is FairAttempt.Result -> materializeAttempt(attempt)
            }
        }

    private fun FairAttempt.toPublic(
        waiterIdentity: AtomicReference<FairWaiterIdentity?>,
    ): LockAcquireResult<LockHandle> =
        when (this) {
            is FairAttempt.Result -> value
            is FairAttempt.Queued -> {
                waiterIdentity.set(identity)
                LockAcquireResult.Contended(lockTtlMillis)
            }
        }

    private fun materializeAttempt(attempt: FairAttempt): LockAcquireResult<LockHandle> {
        val value = (attempt as? FairAttempt.Result)?.value
            ?: return (attempt as FairAttempt.Queued).toPublic(AtomicReference())
        val handle = when (value) {
            is LockAcquireResult.Acquired -> value.handle
            is LockAcquireResult.Reentered -> value.handle
            else -> return value
        }
        if (handle.leasePolicy is LeasePolicy.Fixed) return value
        return when (val reconciled = distributed.reconcile(handle.ownerId, handle.requestId)) {
            is LockReconcileResult.Owned ->
                when (value) {
                    is LockAcquireResult.Acquired -> LockAcquireResult.Acquired(handle)
                    is LockAcquireResult.Reentered -> LockAcquireResult.Reentered(handle, value.holdCount)
                }
            LockReconcileResult.Closed -> LockAcquireResult.Closed
            is LockReconcileResult.BackendFailure -> LockAcquireResult.BackendFailure(reconciled.failure)
            is LockReconcileResult.IntegrityFailure -> LockAcquireResult.IntegrityFailure(reconciled.failure)
            else -> LockAcquireResult.Ambiguous(
                handle.ownerId,
                handle.requestId,
                LockRecoveryAction.RECONCILE_REQUEST,
            )
        }
    }

    private fun materializeAttemptAsync(attempt: FairAttempt): CompletableFuture<LockAcquireResult<LockHandle>> {
        val value = (attempt as? FairAttempt.Result)?.value
            ?: return CompletableFuture.completedFuture((attempt as FairAttempt.Queued).toPublic(AtomicReference()))
        val handle = when (value) {
            is LockAcquireResult.Acquired -> value.handle
            is LockAcquireResult.Reentered -> value.handle
            else -> return CompletableFuture.completedFuture(value)
        }
        if (handle.leasePolicy is LeasePolicy.Fixed) return CompletableFuture.completedFuture(value)
        return distributed.reconcileAsync(handle.ownerId, handle.requestId).thenApply { reconciled ->
            when (reconciled) {
                is LockReconcileResult.Owned -> value
                LockReconcileResult.Closed -> LockAcquireResult.Closed
                is LockReconcileResult.BackendFailure -> LockAcquireResult.BackendFailure(reconciled.failure)
                is LockReconcileResult.IntegrityFailure -> LockAcquireResult.IntegrityFailure(reconciled.failure)
                else -> LockAcquireResult.Ambiguous(
                    handle.ownerId,
                    handle.requestId,
                    LockRecoveryAction.RECONCILE_REQUEST,
                )
            }
        }
    }

    private fun materializeReconcile(
        result: LockReconcileResult<LockHandle>,
    ): LockReconcileResult<LockHandle> {
        val owned = result as? LockReconcileResult.Owned ?: return result
        return mapReconcile(distributed.reconcile(owned.handle.ownerId, owned.handle.requestId))
    }

    private fun materializeReconcileAsync(
        result: LockReconcileResult<LockHandle>,
    ): CompletableFuture<LockReconcileResult<LockHandle>> {
        val owned = result as? LockReconcileResult.Owned
            ?: return CompletableFuture.completedFuture(result)
        return distributed.reconcileAsync(owned.handle.ownerId, owned.handle.requestId).thenApply(::mapReconcile)
    }

    private fun cleanupTimedOut(
        result: LockAcquireResult<LockHandle>,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: FairWaiterIdentity?,
    ): LockAcquireResult<LockHandle> =
        if (result == LockAcquireResult.TimedOut && identity != null) {
            cleanupResult(removeWaiter(ownerId, requestId, identity), ownerId, requestId)
        } else {
            result
        }

    private fun cleanupResult(
        result: LockReconcileResult<LockHandle>,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockAcquireResult<LockHandle> =
        when (result) {
            LockReconcileResult.Removed,
            LockReconcileResult.NotFound,
            -> LockAcquireResult.TimedOut
            LockReconcileResult.Closed -> LockAcquireResult.Closed
            is LockReconcileResult.BackendFailure ->
                LockAcquireResult.Ambiguous(
                    ownerId,
                    requestId,
                    LockRecoveryAction.RECONCILE_REQUEST,
                )
            is LockReconcileResult.IntegrityFailure -> LockAcquireResult.IntegrityFailure(result.failure)
            LockReconcileResult.StaleGeneration,
            is LockReconcileResult.Queued,
            is LockReconcileResult.Owned,
            LockReconcileResult.Released,
            is LockReconcileResult.Ambiguous,
            -> LockAcquireResult.CleanupPending
        }

    internal fun removeWaiter(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: FairWaiterIdentity,
    ): LockReconcileResult<LockHandle> {
        val args = removeArgs(ownerId, requestId, identity)
        return fairClassified(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeRemove(executor.run(FairLockOperation.REMOVE, keys, args))
        }
    }

    private fun removeWaiterAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: FairWaiterIdentity,
    ): CompletableFuture<LockReconcileResult<LockHandle>> =
        executor.runAsync(FairLockOperation.REMOVE, keys, removeArgs(ownerId, requestId, identity))
            .fairMap(
                decode = ::decodeRemove,
                backend = { LockReconcileResult.BackendFailure(it) },
                integrity = { LockReconcileResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RECONCILE_REQUEST,
            )

    private suspend fun removeWaiterSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: FairWaiterIdentity,
    ): LockReconcileResult<LockHandle> =
        fairClassifiedSuspending(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeRemove(
                executor.runSuspending(FairLockOperation.REMOVE, keys, removeArgs(ownerId, requestId, identity)),
            )
        }

    private fun acquireArgs(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
    ): List<String> {
        val lease = encodeLeasePolicy(leasePolicy)
        val waitMillis = if (waitTime.isZero) 0L else waitTime.toRedisMillisCeil()
        return listOf(
            ownerId.value,
            requestId.value,
            fairWaiterMember(ownerId, requestId),
            lease.wireValue,
            lease.ttlMillis.toString(),
            config.lock.maxReentrantHolds.toString(),
            waitMillis.toString(),
            config.cleanupBatchSize.toString(),
            config.maxQueueSize.toString(),
        )
    }

    private fun removeArgs(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: FairWaiterIdentity,
    ): List<String> {
        require(identity.sequence > 0L) { "Fair waiter sequence must be positive." }
        require(identity.generation >= 0L) { "Fair waiter generation must not be negative." }
        return listOf(
            ownerId.value,
            requestId.value,
            fairWaiterMember(ownerId, requestId),
            identity.sequence.toString(),
            identity.generation.toString(),
        )
    }

    private fun validateWait(waitTime: Duration) {
        waitTime.toRedisMillisCeil()
        require(waitTime <= MAX_FAIR_LOCK_WAIT) {
            "Lock wait time must not exceed $MAX_FAIR_LOCK_WAIT."
        }
    }

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FairLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): FairLockClient {
            val keys = deriveFairLockKeys(name, config, connection.codec)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return FairLockClient(
                keys,
                config,
                DefaultFairLockCommandExecutor(connection.sync(), connection.async()),
                runtime.registerObject(keys.fingerprint),
                DistributedLockClient.create(
                    connection,
                    name,
                    config.lock,
                    scheduler,
                    observationSink.asFairObservationSink(),
                ),
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FairLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): FairLockClient {
            val keys = deriveFairLockKeys(name, config, connection.codec)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return FairLockClient(
                keys,
                config,
                DefaultFairLockCommandExecutor(connection.sync(), connection.async()),
                runtime.registerObject(keys.fingerprint),
                DistributedLockClient.create(
                    connection,
                    name,
                    config.lock,
                    scheduler,
                    observationSink.asFairObservationSink(),
                ),
            )
        }
    }
}

private fun decodeAttempt(
    raw: Any?,
    keys: FairLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): FairAttempt {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "ACQUIRED" to 5,
            "REPLAY" to 5,
            "REENTERED" to 5,
            "QUEUED" to 5,
            "CONTENDED" to 2,
            "CLEANUP_PENDING" to 1,
            "CAPACITY" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "ACQUIRED", "REPLAY", "REENTERED" -> {
            val generation = LockGeneration(frame.fairPositiveLong(0))
            val holdCount = frame.fairPositiveInt(1)
            frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            val handle = LockHandle(keys.fingerprint, ownerId, generation, requestId, policy, LockKind.FAIR)
            val result =
                if (frame.tag == "REENTERED" || frame.tag == "REPLAY" && holdCount > 1) {
                    LockAcquireResult.Reentered(handle, holdCount)
                } else {
                    LockAcquireResult.Acquired(handle)
                }
            FairAttempt.Result(result)
        }
        "QUEUED" -> FairAttempt.Queued(
            FairWaiterState(
                FairWaiterStatus.QUEUED,
                frame.fairPositiveLong(0),
                frame.nonNegativeLong(2),
            ),
            FairWaiterIdentity(frame.fairPositiveLong(0), frame.nonNegativeLong(1)),
            frame.nonNegativeLong(3),
        )
        "CONTENDED" -> FairAttempt.Result(LockAcquireResult.Contended(frame.nonNegativeLong(0)))
        "CLEANUP_PENDING" -> FairAttempt.Result(LockAcquireResult.CleanupPending)
        "CAPACITY" -> FairAttempt.Result(LockAcquireResult.CapacityExceeded)
        "INTEGRITY" -> FairAttempt.Result(LockAcquireResult.IntegrityFailure(FAIR_INVALID_STATE))
        else -> fairMalformedReply()
    }
}

private fun decodeFairReconcile(
    raw: Any?,
    keys: FairLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): LockReconcileResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "OWNED" to 5,
            "QUEUED" to 4,
            "REMOVED" to 1,
            "RELEASED" to 1,
            "NOT_FOUND" to 1,
            "STALE" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "OWNED" -> {
            val generation = LockGeneration(frame.fairPositiveLong(0))
            val holdCount = frame.fairPositiveInt(1)
            val ttl = frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            LockReconcileResult.Owned(
                LockHandle(keys.fingerprint, ownerId, generation, requestId, policy, LockKind.FAIR),
                holdCount,
                ttl,
            )
        }
        "QUEUED" -> LockReconcileResult.Queued(
            FairWaiterState(FairWaiterStatus.QUEUED, frame.fairPositiveLong(0), frame.nonNegativeLong(2)),
        )
        "REMOVED" -> LockReconcileResult.Removed
        "RELEASED" -> LockReconcileResult.Released
        "NOT_FOUND" -> LockReconcileResult.NotFound
        "STALE" -> LockReconcileResult.StaleGeneration
        "INTEGRITY" -> LockReconcileResult.IntegrityFailure(FAIR_INVALID_STATE)
        else -> fairMalformedReply()
    }
}

private fun decodeRemove(raw: Any?): LockReconcileResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf("REMOVED" to 1, "NOT_FOUND" to 1, "STALE" to 1, "INTEGRITY" to 1),
    )
    return when (frame.tag) {
        "REMOVED" -> LockReconcileResult.Removed
        "NOT_FOUND" -> LockReconcileResult.NotFound
        "STALE" -> LockReconcileResult.StaleGeneration
        "INTEGRITY" -> LockReconcileResult.IntegrityFailure(FAIR_INVALID_STATE)
        else -> fairMalformedReply()
    }
}

internal fun fairWaiterMember(ownerId: LockOwnerId, requestId: LockRequestId): String {
    val owner = ownerId.value
    val request = requestId.value
    return "${owner.toByteArray(StandardCharsets.UTF_8).size}:$owner" +
        "${request.toByteArray(StandardCharsets.UTF_8).size}:$request"
}

private fun LockHandle.toDistributed(): LockHandle {
    require(kind == LockKind.FAIR) { "Handle kind must be FAIR." }
    return copy(kind = LockKind.DISTRIBUTED)
}

private fun LockHandle.toFair(): LockHandle = copy(kind = LockKind.FAIR)

private fun mapInspect(result: LockInspectResult<LockHandle>): LockInspectResult<LockHandle> =
    when (result) {
        is LockInspectResult.Owned -> result.copy(handle = result.handle.toFair())
        else -> result
    }

private fun mapReconcile(result: LockReconcileResult<LockHandle>): LockReconcileResult<LockHandle> =
    when (result) {
        is LockReconcileResult.Owned -> result.copy(handle = result.handle.toFair())
        else -> result
    }

private fun mapMutation(result: LockMutationResult<LockHandle>): LockMutationResult<LockHandle> =
    when (result) {
        is LockMutationResult.Renewed -> result.copy(handle = result.handle.toFair())
        else -> result
    }

private fun LockObservationSink.asFairObservationSink(): LockObservationSink =
    LockObservationSink { observation ->
        val fair = when (observation) {
            is LockObservation.Counter ->
                observation.copy(dimensions = observation.dimensions.copy(objectKind = LockKind.FAIR))
            is LockObservation.Gauge ->
                observation.copy(dimensions = observation.dimensions.copy(objectKind = LockKind.FAIR))
            is LockObservation.Histogram ->
                observation.copy(dimensions = observation.dimensions.copy(objectKind = LockKind.FAIR))
            is LockObservation.Event ->
                observation.copy(event = observation.event.copy(objectKind = LockKind.FAIR))
        }
        try {
            record(fair)
        } catch (_: Exception) {
            // Observations never alter fair-lock behavior.
        }
    }

private inline fun <R> fairClassified(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
    block: () -> R,
): R =
    try {
        block()
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND -> backend(fairBackendFailure(error, action))
            CoordinationFailureClassification.INTEGRITY -> integrity(fairMalformedIntegrity())
        }
    } catch (_: IllegalArgumentException) {
        integrity(fairMalformedIntegrity())
    } catch (error: Exception) {
        backend(fairBackendFailure(error, action))
    }

private suspend inline fun <R> fairClassifiedSuspending(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
    crossinline block: suspend () -> R,
): R =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND -> backend(fairBackendFailure(error, action))
            CoordinationFailureClassification.INTEGRITY -> integrity(fairMalformedIntegrity())
        }
    } catch (_: IllegalArgumentException) {
        integrity(fairMalformedIntegrity())
    } catch (error: Exception) {
        backend(fairBackendFailure(error, action))
    }

private fun <R> CompletableFuture<List<String>>.fairMap(
    decode: (List<String>) -> R,
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
): CompletableFuture<R> {
    val result = CompletableFuture<R>()
    whenComplete { value, error ->
        if (result.isDone) return@whenComplete
        try {
            result.complete(
                if (error == null) {
                    try {
                        decode(value)
                    } catch (_: CoordinationProtocolException) {
                        integrity(fairMalformedIntegrity())
                    } catch (_: IllegalArgumentException) {
                        integrity(fairMalformedIntegrity())
                    }
                } else {
                    backend(fairBackendFailure(error, action))
                },
            )
        } catch (failure: Throwable) {
            result.completeExceptionally(failure)
        }
    }
    result.whenComplete { _, _ ->
        if (result.isCancelled && !isDone) cancel(false)
    }
    return result
}

private fun fairBackendFailure(error: Throwable, action: LockRecoveryAction): LockBackendFailure {
    val cause = error.fairUnwrap()
    val kind = when (cause) {
        is RedisConnectionException -> LockBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException, is TimeoutException -> LockBackendFailureKind.TIMEOUT
        is RedisException -> LockBackendFailureKind.COMMAND
        else -> throw cause
    }
    return LockBackendFailure(kind, action)
}

private fun Throwable.fairUnwrap(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(FAIR_COMPLETION_DEPTH) {
        if (current is CancellationException) throw current
        if (!seen.add(current)) return current
        val next = when (current) {
            is CompletionException, is ExecutionException -> current.cause
            else -> null
        } ?: return current
        current = next
    }
    return current
}

private fun fairMalformedIntegrity(): LockIntegrityFailure =
    LockIntegrityFailure(LockIntegrityFailureKind.MALFORMED_REPLY)

private fun fairMalformedReply(): Nothing =
    throw CoordinationProtocolException(
        CoordinationFailureClassification.INTEGRITY,
        "fair lock response is malformed",
    )

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.fairPositiveLong(index: Int): Long =
    nonNegativeLong(index).also { require(it > 0L) }

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.fairPositiveInt(index: Int): Int =
    fairPositiveLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun ByteBuffer.toByteArray(): ByteArray =
    duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

private val FAIR_INVALID_STATE = LockIntegrityFailure(LockIntegrityFailureKind.INVALID_STATE)
private val MAX_FAIR_LOCK_WAIT: Duration = Duration.ofHours(24)
private const val FAIR_FINGERPRINT_BYTES = 8
private const val FAIR_COMPLETION_DEPTH = 8
