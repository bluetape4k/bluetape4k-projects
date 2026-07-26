package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationCapacityException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRenewalOutcome
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocol
import io.bluetape4k.redis.lettuce.lock.FencedBootstrapResult
import io.bluetape4k.redis.lettuce.lock.FencedLockConfig
import io.bluetape4k.redis.lettuce.lock.FencedLockHandle
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockBackendFailure
import io.bluetape4k.redis.lettuce.lock.LockBackendFailureKind
import io.bluetape4k.redis.lettuce.lock.LockCounterName
import io.bluetape4k.redis.lettuce.lock.LockDimensions
import io.bluetape4k.redis.lettuce.lock.LockEvent
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailureKind
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockLeasePolicyKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockObservation
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOperation
import io.bluetape4k.redis.lettuce.lock.LockOutcome
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRecoveryAction
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.lock.recordSafely
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.toKotlinDuration

internal enum class FencedLockOperation(
    val wireValue: String,
) {
    BOOTSTRAP("BOOTSTRAP"),
    ACQUIRE("ACQUIRE"),
    INSPECT("INSPECT"),
    RECONCILE("RECONCILE"),
    RENEW("RENEW"),
    RELEASE("RELEASE"),
}

internal data class FencedLockKeys(
    val state: String,
    val generation: String,
    val holds: String,
    val terminal: String,
    val counter: String,
    val fingerprint: String,
) {
    val all: Array<String> = arrayOf(state, generation, holds, terminal, counter)
}

internal fun deriveFencedLockKeys(
    name: String,
    config: FencedLockConfig,
    codec: RedisCodec<String, String>,
): FencedLockKeys {
    val lockConfig = config.lock
    val resource = lockConfig.validateResourceName(name)
    val hashTag = lockConfig.hashTag ?: resource
    val prefix = "${lockConfig.namespace}:{$hashTag}:lock:$resource"
    val epoch = config.epoch.toString()
    val state = lockConfig.validateDerivedKey("$prefix:state")
    val generation = lockConfig.validateDerivedKey("$prefix:generation")
    val holds = lockConfig.validateDerivedKey("$prefix:holds")
    val terminal = lockConfig.validateDerivedKey("$prefix:terminal")
    val counter = lockConfig.validateDerivedKey("$prefix:fence:$epoch:counter")
    val keys = listOf(state, generation, holds, terminal, counter)
    require(keys.map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet().size == 1) {
        "Derived fenced lock keys must share one Redis Cluster slot."
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(codec.encodeKey("$state|$epoch").toByteArray())
    val fingerprint = digest.take(FENCED_FINGERPRINT_BYTES).joinToString("") { byte -> "%02x".format(byte) }
    return FencedLockKeys(state, generation, holds, terminal, counter, fingerprint)
}

internal val FENCED_LOCK_SCRIPT = RedisScript(
    """
    local operation = ARGV[1]
    local MAXIMUM = '9007199254740991'

    local function key_type(key)
        local result = redis.call('TYPE', key)
        if type(result) == 'table' then return result.ok end
        return result
    end

    local function non_negative_decimal(value)
        return type(value) == 'string'
            and (value == '0' or string.match(value, '^[1-9][0-9]*$') ~= nil)
            and (string.len(value) < 16 or string.len(value) == 16 and value <= MAXIMUM)
    end

    local function positive_decimal(value)
        return non_negative_decimal(value) and value ~= '0'
    end

    local function compare_decimal(left, right)
        if string.len(left) ~= string.len(right) then
            return string.len(left) < string.len(right) and -1 or 1
        end
        if left == right then return 0 end
        return left < right and -1 or 1
    end

    local function parse_hold(value)
        if type(value) ~= 'string' or string.len(value) > 128 then return nil, nil, nil end
        local generation, acquisition, policy = string.match(value, '^([1-9][0-9]*)|([AR])|(.+)$')
        if not positive_decimal(generation) or acquisition == nil or policy == nil then
            return nil, nil, nil
        end
        return generation, acquisition, policy
    end

    local function valid_terminal()
        local terminal_type = key_type(KEYS[4])
        if terminal_type == 'none' then return true end
        if terminal_type ~= 'hash' or redis.call('PTTL', KEYS[4]) <= 0 then return false end
        local owner = redis.call('HGET', KEYS[4], 'owner')
        local generation = redis.call('HGET', KEYS[4], 'generation')
        local request = redis.call('HGET', KEYS[4], 'request')
        local epoch = redis.call('HGET', KEYS[4], 'epoch')
        local token = redis.call('HGET', KEYS[4], 'token')
        return owner ~= false and string.len(owner) >= 1 and string.len(owner) <= 256
            and positive_decimal(generation)
            and request ~= false and string.len(request) >= 1 and string.len(request) <= 256
            and positive_decimal(epoch) and positive_decimal(token)
            and redis.call('HLEN', KEYS[4]) == 5
    end

    local function validate_state()
        local generation_type = key_type(KEYS[2])
        if generation_type ~= 'none' and generation_type ~= 'string' then return false, false end
        if not valid_terminal() then return false, false end
        if generation_type == 'string' then
            local generation_counter = redis.call('GET', KEYS[2])
            if not positive_decimal(generation_counter) or redis.call('PTTL', KEYS[2]) ~= -1 then
                return false, false
            end
            if key_type(KEYS[4]) == 'hash'
                and compare_decimal(generation_counter, redis.call('HGET', KEYS[4], 'generation')) < 0 then
                return false, false
            end
        end

        local state_type = key_type(KEYS[1])
        local holds_type = key_type(KEYS[3])
        if state_type == 'none' then return holds_type == 'none', false end
        if state_type ~= 'hash' or holds_type ~= 'hash' then return false, false end
        if redis.call('PTTL', KEYS[1]) <= 0 or redis.call('PTTL', KEYS[3]) <= 0 then return false, false end

        local owner = redis.call('HGET', KEYS[1], 'owner')
        local generation = redis.call('HGET', KEYS[1], 'generation')
        local hold_count = redis.call('HGET', KEYS[1], 'holdCount')
        local epoch = redis.call('HGET', KEYS[1], 'epoch')
        local token = redis.call('HGET', KEYS[1], 'token')
        local generation_counter = redis.call('GET', KEYS[2])
        if owner == false or string.len(owner) < 1 or string.len(owner) > 256
            or not positive_decimal(generation)
            or not positive_decimal(hold_count)
            or not positive_decimal(epoch)
            or not positive_decimal(token)
            or not positive_decimal(generation_counter)
            or compare_decimal(generation, generation_counter) > 0
            or tonumber(hold_count) ~= redis.call('HLEN', KEYS[3])
            or redis.call('HLEN', KEYS[1]) ~= 5 then
            return false, false
        end
        return true, true
    end

    local function valid_counter()
        if key_type(KEYS[5]) ~= 'string' or redis.call('PTTL', KEYS[5]) ~= -1 then return false, nil end
        local counter = redis.call('GET', KEYS[5])
        if not non_negative_decimal(counter) then return false, nil end
        return true, counter
    end

    if operation == 'BOOTSTRAP' then
        local epoch = ARGV[2]
        if not positive_decimal(epoch) then return {'INTEGRITY'} end
        local state_valid, active = validate_state()
        if not state_valid then return {'INTEGRITY'} end
        local counter_type = key_type(KEYS[5])
        if counter_type == 'none' then
            if active and redis.call('HGET', KEYS[1], 'epoch') == epoch
                or key_type(KEYS[4]) == 'hash' and redis.call('HGET', KEYS[4], 'epoch') == epoch then
                return {'COUNTER_REGRESSION'}
            end
            redis.call('SET', KEYS[5], '0')
            return {'INITIALIZED'}
        end
        local counter_valid, counter = valid_counter()
        if not counter_valid then return {'COUNTER_REGRESSION'} end
        if active
            and redis.call('HGET', KEYS[1], 'epoch') == epoch
            and compare_decimal(counter, redis.call('HGET', KEYS[1], 'token')) < 0 then
            return {'COUNTER_REGRESSION'}
        end
        if key_type(KEYS[4]) == 'hash'
            and redis.call('HGET', KEYS[4], 'epoch') == epoch
            and compare_decimal(counter, redis.call('HGET', KEYS[4], 'token')) < 0 then
            return {'COUNTER_REGRESSION'}
        end
        return {'ALREADY_INITIALIZED'}
    end

    local valid, active = validate_state()
    if not valid then return {'INTEGRITY'} end

    if operation == 'ACQUIRE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        local policy = ARGV[4]
        local ttl = tonumber(ARGV[5])
        local maximum = tonumber(ARGV[6])
        local epoch = ARGV[8]
        if not active then
            local counter_valid, counter = valid_counter()
            if not counter_valid then return {'COUNTER_REGRESSION'} end
            if key_type(KEYS[4]) == 'hash'
                and redis.call('HGET', KEYS[4], 'epoch') == epoch
                and compare_decimal(counter, redis.call('HGET', KEYS[4], 'token')) < 0 then
                return {'COUNTER_REGRESSION'}
            end
            local previous_generation = redis.call('GET', KEYS[2])
            if counter == MAXIMUM
                or previous_generation ~= false and compare_decimal(previous_generation, '9007199254740990') > 0 then
                return {'CAPACITY'}
            end
            redis.call('INCR', KEYS[2])
            redis.call('INCR', KEYS[5])
            local generation = redis.call('GET', KEYS[2])
            local token = redis.call('GET', KEYS[5])
            redis.call('DEL', KEYS[4])
            redis.call('HSET', KEYS[1],
                'owner', owner, 'generation', generation, 'holdCount', 1, 'epoch', epoch, 'token', token)
            redis.call('HSET', KEYS[3], request, generation .. '|A|' .. policy)
            redis.call('PEXPIRE', KEYS[1], ttl)
            redis.call('PEXPIRE', KEYS[3], ttl)
            return {'ACQUIRED', generation, '1', tostring(ttl), policy, epoch, token}
        end

        local current_owner = redis.call('HGET', KEYS[1], 'owner')
        local current_epoch = redis.call('HGET', KEYS[1], 'epoch')
        local remaining = redis.call('PTTL', KEYS[1])
        if current_owner ~= owner or current_epoch ~= epoch then
            return {'CONTENDED', tostring(remaining)}
        end
        local counter_valid, counter = valid_counter()
        local token = redis.call('HGET', KEYS[1], 'token')
        if not counter_valid or compare_decimal(counter, token) < 0 then return {'COUNTER_REGRESSION'} end
        local generation = redis.call('HGET', KEYS[1], 'generation')
        local hold_count = tonumber(redis.call('HGET', KEYS[1], 'holdCount'))
        local existing = redis.call('HGET', KEYS[3], request)
        if existing ~= false then
            local existing_generation, existing_acquisition, existing_policy = parse_hold(existing)
            if existing_generation ~= generation then return {'INTEGRITY'} end
            local replay_tag = existing_acquisition == 'A' and 'REPLAY' or 'REENTERED'
            return {replay_tag, generation, tostring(hold_count), tostring(remaining), existing_policy, epoch, token}
        end
        if hold_count >= maximum then return {'CAPACITY'} end
        hold_count = hold_count + 1
        redis.call('HSET', KEYS[1], 'holdCount', hold_count)
        redis.call('HSET', KEYS[3], request, generation .. '|R|' .. policy)
        redis.call('PEXPIRE', KEYS[1], ttl)
        redis.call('PEXPIRE', KEYS[3], ttl)
        return {'REENTERED', generation, tostring(hold_count), tostring(ttl), policy, epoch, token}
    end

    if operation == 'RECONCILE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        local epoch = ARGV[4]
        if not active then
            if key_type(KEYS[4]) == 'hash'
                and redis.call('HGET', KEYS[4], 'owner') == owner
                and redis.call('HGET', KEYS[4], 'request') == request
                and redis.call('HGET', KEYS[4], 'epoch') == epoch then
                return {'RELEASED'}
            end
            return {'NOT_FOUND'}
        end
        if redis.call('HGET', KEYS[1], 'owner') ~= owner
            or redis.call('HGET', KEYS[1], 'epoch') ~= epoch then
            return {'NOT_FOUND'}
        end
        local counter_valid, counter = valid_counter()
        local token = redis.call('HGET', KEYS[1], 'token')
        if not counter_valid or compare_decimal(counter, token) < 0 then return {'COUNTER_REGRESSION'} end
        local hold = redis.call('HGET', KEYS[3], request)
        if hold == false then return {'NOT_FOUND'} end
        local generation, acquisition, policy = parse_hold(hold)
        if generation == nil or generation ~= redis.call('HGET', KEYS[1], 'generation') then
            return {'INTEGRITY'}
        end
        return {'OWNED', generation, redis.call('HGET', KEYS[1], 'holdCount'),
            tostring(redis.call('PTTL', KEYS[1])), policy, epoch, token}
    end

    local owner = ARGV[2]
    local request = ARGV[3]
    local generation = ARGV[4]
    local epoch = ARGV[5]
    local supplied_token = ARGV[6]
    if not active then
        if key_type(KEYS[4]) == 'hash'
            and redis.call('HGET', KEYS[4], 'owner') == owner
            and redis.call('HGET', KEYS[4], 'request') == request
            and redis.call('HGET', KEYS[4], 'generation') == generation
            and redis.call('HGET', KEYS[4], 'epoch') == epoch
            and redis.call('HGET', KEYS[4], 'token') == supplied_token then
            if operation == 'INSPECT' then return {'RELEASED'} end
            return {'ALREADY_RELEASED'}
        end
        local generation_counter = redis.call('GET', KEYS[2])
        if generation_counter ~= false and compare_decimal(generation_counter, generation) > 0 then
            return {'STALE'}
        end
        return {'EXPIRED'}
    end

    local current_generation = redis.call('HGET', KEYS[1], 'generation')
    if compare_decimal(current_generation, generation) > 0 then return {'STALE'} end
    if current_generation ~= generation then return {'INTEGRITY'} end
    if redis.call('HGET', KEYS[1], 'owner') ~= owner
        or redis.call('HGET', KEYS[1], 'epoch') ~= epoch
        or redis.call('HGET', KEYS[1], 'token') ~= supplied_token then
        return {'LOST'}
    end
    local counter_valid, counter = valid_counter()
    if not counter_valid or compare_decimal(counter, supplied_token) < 0 then return {'COUNTER_REGRESSION'} end
    local hold = redis.call('HGET', KEYS[3], request)
    if hold == false then
        if operation == 'INSPECT' then return {'RELEASED'} end
        return {'ALREADY_RELEASED'}
    end
    local hold_generation, acquisition, policy = parse_hold(hold)
    if hold_generation == nil or hold_generation ~= generation then return {'INTEGRITY'} end

    if operation == 'INSPECT' then
        return {'OWNED', generation, redis.call('HGET', KEYS[1], 'holdCount'),
            tostring(redis.call('PTTL', KEYS[1])), policy, epoch, supplied_token}
    end
    if operation == 'RENEW' then
        local ttl = tonumber(ARGV[7])
        redis.call('PEXPIRE', KEYS[1], ttl)
        redis.call('PEXPIRE', KEYS[3], ttl)
        return {'RENEWED', tostring(ttl)}
    end
    if operation == 'RELEASE' then
        local terminal_ttl = tonumber(ARGV[7])
        redis.call('HDEL', KEYS[3], request)
        local hold_count = tonumber(redis.call('HGET', KEYS[1], 'holdCount')) - 1
        if hold_count > 0 then
            redis.call('HSET', KEYS[1], 'holdCount', hold_count)
            return {'RELEASED', tostring(hold_count)}
        end
        redis.call('DEL', KEYS[4])
        redis.call('HSET', KEYS[4],
            'owner', owner, 'generation', generation, 'request', request, 'epoch', epoch, 'token', supplied_token)
        redis.call('PEXPIRE', KEYS[4], terminal_ttl)
        redis.call('DEL', KEYS[1], KEYS[3])
        return {'RELEASED', '0'}
    end
    return {'INTEGRITY'}
    """.trimIndent(),
)

private fun fencedAcquireArgs(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    leasePolicy: LeasePolicy,
    maxReentrantHolds: Int,
    epoch: Long,
): List<String> {
    val encoded = encodeLeasePolicy(leasePolicy)
    return listOf(
        ownerId.value,
        requestId.value,
        encoded.wireValue,
        encoded.ttlMillis.toString(),
        maxReentrantHolds.toString(),
        FENCED_TERMINAL_TTL_MILLIS.toString(),
        epoch.toString(),
    )
}

private fun fencedHandleArgs(handle: FencedLockHandle): List<String> =
    listOf(
        handle.ownerId.value,
        handle.requestId.value,
        handle.generation.value.toString(),
        handle.epoch.toString(),
        handle.fencingToken.toString(),
    )

private fun fencedRenewArgs(handle: FencedLockHandle, extension: Duration): List<String> =
    fencedHandleArgs(handle) + extension.toRedisMillisCeil().toString()

private fun fencedReconcileArgs(ownerId: LockOwnerId, requestId: LockRequestId, epoch: Long): List<String> =
    listOf(ownerId.value, requestId.value, epoch.toString())

private fun decodeFencedBootstrap(raw: Any?): FencedBootstrapResult {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "INITIALIZED" to 1,
            "ALREADY_INITIALIZED" to 1,
            "COUNTER_REGRESSION" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "INITIALIZED" -> FencedBootstrapResult.Initialized
        "ALREADY_INITIALIZED" -> FencedBootstrapResult.AlreadyInitialized
        "COUNTER_REGRESSION" -> FencedBootstrapResult.IntegrityFailure(FENCED_COUNTER_REGRESSION)
        "INTEGRITY" -> FencedBootstrapResult.IntegrityFailure(FENCED_INVALID_STATE)
        else -> fencedMalformedReply()
    }
}

private fun decodeFencedAcquire(
    raw: Any?,
    keys: FencedLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): LockAcquireResult<FencedLockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "ACQUIRED" to 7,
            "REPLAY" to 7,
            "REENTERED" to 7,
            "CONTENDED" to 2,
            "CAPACITY" to 1,
            "COUNTER_REGRESSION" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "ACQUIRED", "REPLAY", "REENTERED" -> {
            val generation = LockGeneration(frame.fencedPositiveLong(0))
            val holdCount = frame.fencedPositiveInt(1)
            frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            val epoch = frame.fencedPositiveLong(4)
            val token = frame.fencedPositiveLong(5)
            val handle = fencedHandle(keys, ownerId, requestId, generation, policy, epoch, token)
            if (frame.tag == "REENTERED" || frame.tag == "REPLAY" && holdCount > 1) {
                LockAcquireResult.Reentered(handle, holdCount)
            } else {
                LockAcquireResult.Acquired(handle)
            }
        }
        "CONTENDED" -> LockAcquireResult.Contended(frame.nonNegativeLong(0))
        "CAPACITY" -> LockAcquireResult.CapacityExceeded
        "COUNTER_REGRESSION" -> LockAcquireResult.IntegrityFailure(FENCED_COUNTER_REGRESSION)
        "INTEGRITY" -> LockAcquireResult.IntegrityFailure(FENCED_INVALID_STATE)
        else -> fencedMalformedReply()
    }
}

private fun decodeFencedInspect(
    raw: Any?,
    keys: FencedLockKeys,
    handle: FencedLockHandle,
): LockInspectResult<FencedLockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "OWNED" to 7,
            "RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "COUNTER_REGRESSION" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "OWNED" -> {
            val generation = LockGeneration(frame.fencedPositiveLong(0))
            val holdCount = frame.fencedPositiveInt(1)
            val ttl = frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            val epoch = frame.fencedPositiveLong(4)
            val token = frame.fencedPositiveLong(5)
            LockInspectResult.Owned(
                fencedHandle(keys, handle.ownerId, handle.requestId, generation, policy, epoch, token),
                holdCount,
                ttl,
            )
        }
        "RELEASED" -> LockInspectResult.Released
        "EXPIRED" -> LockInspectResult.Expired
        "STALE" -> LockInspectResult.StaleGeneration
        "LOST" -> LockInspectResult.OwnershipLost
        "COUNTER_REGRESSION" -> LockInspectResult.IntegrityFailure(FENCED_COUNTER_REGRESSION)
        "INTEGRITY" -> LockInspectResult.IntegrityFailure(FENCED_INVALID_STATE)
        else -> fencedMalformedReply()
    }
}

private fun decodeFencedReconcile(
    raw: Any?,
    keys: FencedLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): LockReconcileResult<FencedLockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "OWNED" to 7,
            "RELEASED" to 1,
            "NOT_FOUND" to 1,
            "COUNTER_REGRESSION" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "OWNED" -> {
            val generation = LockGeneration(frame.fencedPositiveLong(0))
            val holdCount = frame.fencedPositiveInt(1)
            val ttl = frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            val epoch = frame.fencedPositiveLong(4)
            val token = frame.fencedPositiveLong(5)
            LockReconcileResult.Owned(
                fencedHandle(keys, ownerId, requestId, generation, policy, epoch, token),
                holdCount,
                ttl,
            )
        }
        "RELEASED" -> LockReconcileResult.Released
        "NOT_FOUND" -> LockReconcileResult.NotFound
        "COUNTER_REGRESSION" -> LockReconcileResult.IntegrityFailure(FENCED_COUNTER_REGRESSION)
        "INTEGRITY" -> LockReconcileResult.IntegrityFailure(FENCED_INVALID_STATE)
        else -> fencedMalformedReply()
    }
}

private fun decodeFencedRenew(
    raw: Any?,
    handle: FencedLockHandle,
): LockMutationResult<FencedLockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "RENEWED" to 2,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "COUNTER_REGRESSION" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "RENEWED" -> LockMutationResult.Renewed(handle, frame.nonNegativeLong(0))
        "ALREADY_RELEASED" -> LockMutationResult.AlreadyReleased
        "EXPIRED" -> LockMutationResult.Expired
        "STALE" -> LockMutationResult.StaleGeneration
        "LOST" -> LockMutationResult.OwnershipLost
        "COUNTER_REGRESSION" -> LockMutationResult.IntegrityFailure(FENCED_COUNTER_REGRESSION)
        "INTEGRITY" -> LockMutationResult.IntegrityFailure(FENCED_INVALID_STATE)
        else -> fencedMalformedReply()
    }
}

private fun decodeFencedRelease(raw: Any?): LockMutationResult<FencedLockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "RELEASED" to 2,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "COUNTER_REGRESSION" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "RELEASED" -> LockMutationResult.Released(frame.fencedNonNegativeInt(0))
        "ALREADY_RELEASED" -> LockMutationResult.AlreadyReleased
        "EXPIRED" -> LockMutationResult.Expired
        "STALE" -> LockMutationResult.StaleGeneration
        "LOST" -> LockMutationResult.OwnershipLost
        "COUNTER_REGRESSION" -> LockMutationResult.IntegrityFailure(FENCED_COUNTER_REGRESSION)
        "INTEGRITY" -> LockMutationResult.IntegrityFailure(FENCED_INVALID_STATE)
        else -> fencedMalformedReply()
    }
}

private fun fencedHandle(
    keys: FencedLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    generation: LockGeneration,
    leasePolicy: LeasePolicy,
    epoch: Long,
    token: Long,
): FencedLockHandle =
    FencedLockHandle(
        LockHandle(
            objectFingerprint = keys.fingerprint,
            ownerId = ownerId,
            generation = generation,
            requestId = requestId,
            leasePolicy = leasePolicy,
            kind = LockKind.FENCED,
        ),
        epoch,
        token,
    )

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.fencedPositiveLong(index: Int): Long =
    nonNegativeLong(index).also {
        require(it in 1..FENCED_MAX_EXACT_INTEGER) { "response number must be a positive exact Redis integer" }
    }

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.fencedPositiveInt(index: Int): Int =
    fencedPositiveLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.fencedNonNegativeInt(index: Int): Int =
    nonNegativeLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun fencedMalformedReply(): Nothing =
    throw CoordinationProtocolException(
        CoordinationFailureClassification.INTEGRITY,
        "fenced lock response is malformed",
    )

private val FENCED_INVALID_STATE = LockIntegrityFailure(LockIntegrityFailureKind.INVALID_STATE)
private val FENCED_COUNTER_REGRESSION = LockIntegrityFailure(LockIntegrityFailureKind.COUNTER_REGRESSION)
private const val FENCED_FINGERPRINT_BYTES = 8
private const val FENCED_MAX_EXACT_INTEGER = 9_007_199_254_740_991L
private const val FENCED_TERMINAL_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L

private fun ByteBuffer.toByteArray(): ByteArray =
    duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

private val FencedLockHandle.ownerId: LockOwnerId get() = lock.ownerId
private val FencedLockHandle.requestId: LockRequestId get() = lock.requestId
private val FencedLockHandle.generation: LockGeneration get() = lock.generation
private val FencedLockHandle.leasePolicy: LeasePolicy get() = lock.leasePolicy
private val FencedLockHandle.kind: LockKind get() = lock.kind
private val FencedLockHandle.objectFingerprint: String get() = lock.objectFingerprint

private fun LockAcquireResult<FencedLockHandle>.toBaseAcquire(
    acquired: AtomicReference<FencedLockHandle?>,
): LockAcquireResult<LockHandle> =
    when (this) {
        is LockAcquireResult.Acquired -> {
            acquired.set(handle)
            LockAcquireResult.Acquired(handle.lock)
        }
        is LockAcquireResult.Reentered -> {
            acquired.set(handle)
            LockAcquireResult.Reentered(handle.lock, holdCount)
        }
        else -> @Suppress("UNCHECKED_CAST") (this as LockAcquireResult<LockHandle>)
    }

private fun LockAcquireResult<LockHandle>.toFencedAcquire(
    acquired: FencedLockHandle?,
): LockAcquireResult<FencedLockHandle> =
    when (this) {
        is LockAcquireResult.Acquired ->
            LockAcquireResult.Acquired(requireNotNull(acquired) { "Missing fenced acquisition handle." })
        is LockAcquireResult.Reentered ->
            LockAcquireResult.Reentered(requireNotNull(acquired) { "Missing fenced reentry handle." }, holdCount)
        else -> @Suppress("UNCHECKED_CAST") (this as LockAcquireResult<FencedLockHandle>)
    }

private fun LockAcquireResult<FencedLockHandle>?.acquiredHandleOrNull(): FencedLockHandle? =
    when (this) {
        is LockAcquireResult.Acquired -> handle
        is LockAcquireResult.Reentered -> handle
        else -> null
    }

internal interface FencedLockCommandExecutor {
    fun run(
        operation: FencedLockOperation,
        keys: FencedLockKeys,
        args: List<String>,
    ): List<String>

    fun runAsync(
        operation: FencedLockOperation,
        keys: FencedLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>>

    suspend fun runSuspending(
        operation: FencedLockOperation,
        keys: FencedLockKeys,
        args: List<String>,
    ): List<String>
}

internal class DefaultFencedLockCommandExecutor(
    private val syncCommands: RedisScriptingCommands<String, String>,
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    private val observationRecorder: LockObservationRecorder,
): FencedLockCommandExecutor {
    override fun run(
        operation: FencedLockOperation,
        keys: FencedLockKeys,
        args: List<String>,
    ): List<String> =
        observationRecorder.runScript(
            syncCommands,
            FENCED_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.toLockOperation(),
            args = arrayOf(operation.wireValue, *args.toTypedArray()),
        )

    override fun runAsync(
        operation: FencedLockOperation,
        keys: FencedLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>> =
        observationRecorder.runScriptAsync(
            asyncCommands,
            FENCED_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.toLockOperation(),
            args = arrayOf(operation.wireValue, *args.toTypedArray()),
        )

    override suspend fun runSuspending(
        operation: FencedLockOperation,
        keys: FencedLockKeys,
        args: List<String>,
    ): List<String> =
        observationRecorder.runScriptSuspending(
            asyncCommands,
            FENCED_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.toLockOperation(),
            args = arrayOf(operation.wireValue, *args.toTypedArray()),
        )
}

internal class FencedLockClient(
    private val keys: FencedLockKeys,
    private val config: FencedLockConfig,
    private val executor: FencedLockCommandExecutor,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val observationSink: LockObservationSink,
) {
    private val closed = AtomicBoolean()
    private val waitSupport = LockWaitSupport(
        registration,
        closed::get,
        waitObservation = LockWaitObservation(LockObservationRecorder(LockKind.FENCED, observationSink)),
    )
    private val watchdogs = ConcurrentHashMap<FencedLockHandle, CoordinationRuntime.CoordinationTaskRegistration>()

    fun bootstrapFencing(): FencedBootstrapResult {
        if (closed.get()) return FencedBootstrapResult.Closed
        return classified(
            backend = { FencedBootstrapResult.BackendFailure(it) },
            integrity = { FencedBootstrapResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeFencedBootstrap(
                executor.run(FencedLockOperation.BOOTSTRAP, keys, listOf(config.epoch.toString())),
            )
        }
    }

    fun bootstrapFencingAsync(): CompletableFuture<FencedBootstrapResult> {
        if (closed.get()) return CompletableFuture.completedFuture(FencedBootstrapResult.Closed)
        return executor.runAsync(FencedLockOperation.BOOTSTRAP, keys, listOf(config.epoch.toString()))
            .mapResult(
                decode = ::decodeFencedBootstrap,
                backend = { FencedBootstrapResult.BackendFailure(it) },
                integrity = { FencedBootstrapResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
            )
    }

    suspend fun bootstrapFencingSuspending(): FencedBootstrapResult {
        if (closed.get()) return FencedBootstrapResult.Closed
        return classifiedSuspending(
            backend = { FencedBootstrapResult.BackendFailure(it) },
            integrity = { FencedBootstrapResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeFencedBootstrap(
                executor.runSuspending(FencedLockOperation.BOOTSTRAP, keys, listOf(config.epoch.toString())),
            )
        }
    }

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> {
        val args = fencedAcquireArgs(ownerId, requestId, leasePolicy, config.lock.maxReentrantHolds, config.epoch)
        if (closed.get()) return LockAcquireResult.Closed
        return registerWatchdog(classifiedAcquire(ownerId, requestId) {
            executor.run(FencedLockOperation.ACQUIRE, keys, args)
        })
    }

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<FencedLockHandle>> {
        val args = fencedAcquireArgs(ownerId, requestId, leasePolicy, config.lock.maxReentrantHolds, config.epoch)
        if (closed.get()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        return executor.runAsync(FencedLockOperation.ACQUIRE, keys, args)
            .mapResult(
                decode = { registerWatchdog(decodeFencedAcquire(it, keys, ownerId, requestId)) },
                backend = { acquireBackendResult(ownerId, requestId, it) },
                integrity = { LockAcquireResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
                onLateSuccess = { raw ->
                    decodeFencedAcquire(raw, keys, ownerId, requestId)
                        .acquiredHandleOrNull()
                        ?.let(::releaseAbandoned)
                },
            )
    }

    suspend fun tryAcquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> {
        val args = fencedAcquireArgs(ownerId, requestId, leasePolicy, config.lock.maxReentrantHolds, config.epoch)
        if (closed.get()) return LockAcquireResult.Closed
        return classifiedSuspending(
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(decodeFencedAcquire(
                executor.runSuspending(FencedLockOperation.ACQUIRE, keys, args),
                keys,
                ownerId,
                requestId,
            ))
        }
    }

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> {
        fencedAcquireArgs(ownerId, requestId, leasePolicy, config.lock.maxReentrantHolds, config.epoch)
        val acquired = AtomicReference<FencedLockHandle?>()
        return waitSupport.acquire(waitTime) {
            tryAcquire(ownerId, requestId, leasePolicy).toBaseAcquire(acquired)
        }.toFencedAcquire(acquired.get())
    }

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<FencedLockHandle>> {
        fencedAcquireArgs(ownerId, requestId, leasePolicy, config.lock.maxReentrantHolds, config.epoch)
        val acquired = AtomicReference<FencedLockHandle?>()
        val pending = waitSupport.acquireAsync(
            waitTime,
            abandonedRelease = {
                acquired.getAndSet(null)?.let(::releaseAbandoned)
            },
        ) {
            toBaseAcquireAsync(tryAcquireAsync(ownerId, requestId, leasePolicy), acquired)
        }
        val mapped = CompletableFuture<LockAcquireResult<FencedLockHandle>>()
        pending.whenComplete { result, error ->
            if (mapped.isDone) {
                acquired.get()?.let(::releaseAbandoned)
                return@whenComplete
            }
            if (error == null) {
                val fenced = result.toFencedAcquire(acquired.get())
                if (!mapped.complete(fenced)) {
                    fenced.acquiredHandleOrNull()?.let(::releaseAbandoned)
                }
            } else {
                mapped.completeExceptionally(error)
            }
        }
        mapped.whenComplete { _, _ ->
            if (mapped.isCancelled && !pending.isDone) {
                pending.cancel(false)
            }
        }
        return mapped
    }

    private fun toBaseAcquireAsync(
        source: CompletableFuture<LockAcquireResult<FencedLockHandle>>,
        acquired: AtomicReference<FencedLockHandle?>,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        val mapped = CompletableFuture<LockAcquireResult<LockHandle>>()
        source.whenComplete { value, error ->
            if (mapped.isDone) {
                value.acquiredHandleOrNull()?.let(::releaseAbandoned)
                return@whenComplete
            }
            if (error != null) {
                mapped.completeExceptionally(error)
            } else {
                val base = value.toBaseAcquire(acquired)
                if (!mapped.complete(base)) {
                    value.acquiredHandleOrNull()?.let(::releaseAbandoned)
                }
            }
        }
        mapped.whenComplete { _, _ ->
            if (mapped.isCancelled && !source.isDone) source.cancel(false)
        }
        return mapped
    }

    suspend fun acquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> {
        fencedAcquireArgs(ownerId, requestId, leasePolicy, config.lock.maxReentrantHolds, config.epoch)
        val acquired = AtomicReference<FencedLockHandle?>()
        return waitSupport.acquireSuspending(waitTime) {
            tryAcquireSuspending(ownerId, requestId, leasePolicy).toBaseAcquire(acquired)
        }.toFencedAcquire(acquired.get())
    }

    fun inspect(handle: FencedLockHandle): LockInspectResult<FencedLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockInspectResult.Closed
        return classified(
            backend = { LockInspectResult.BackendFailure(it) },
            integrity = { LockInspectResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeFencedInspect(
                executor.run(FencedLockOperation.INSPECT, keys, fencedHandleArgs(handle)),
                keys,
                handle,
            )
        }
    }

    fun inspectAsync(handle: FencedLockHandle): CompletableFuture<LockInspectResult<FencedLockHandle>> {
        validateHandle(handle)
        val args = fencedHandleArgs(handle)
        if (closed.get()) return CompletableFuture.completedFuture(LockInspectResult.Closed)
        return executor.runAsync(FencedLockOperation.INSPECT, keys, args)
            .mapResult(
                decode = { decodeFencedInspect(it, keys, handle) },
                backend = { LockInspectResult.BackendFailure(it) },
                integrity = { LockInspectResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
            )
    }

    suspend fun inspectSuspending(handle: FencedLockHandle): LockInspectResult<FencedLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockInspectResult.Closed
        return classifiedSuspending(
            backend = { LockInspectResult.BackendFailure(it) },
            integrity = { LockInspectResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeFencedInspect(
                executor.runSuspending(FencedLockOperation.INSPECT, keys, fencedHandleArgs(handle)),
                keys,
                handle,
            )
        }
    }

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<FencedLockHandle> {
        val args = fencedReconcileArgs(ownerId, requestId, config.epoch)
        if (closed.get()) return LockReconcileResult.Closed
        return classified(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(decodeFencedReconcile(
                executor.run(FencedLockOperation.RECONCILE, keys, args),
                keys,
                ownerId,
                requestId,
            ))
        }
    }

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<FencedLockHandle>> {
        val args = fencedReconcileArgs(ownerId, requestId, config.epoch)
        if (closed.get()) return CompletableFuture.completedFuture(LockReconcileResult.Closed)
        return executor.runAsync(FencedLockOperation.RECONCILE, keys, args)
            .mapResult(
                decode = { registerWatchdog(decodeFencedReconcile(it, keys, ownerId, requestId)) },
                backend = { LockReconcileResult.BackendFailure(it) },
                integrity = { LockReconcileResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
            )
    }

    suspend fun reconcileSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<FencedLockHandle> {
        val args = fencedReconcileArgs(ownerId, requestId, config.epoch)
        if (closed.get()) return LockReconcileResult.Closed
        return classifiedSuspending(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(decodeFencedReconcile(
                executor.runSuspending(FencedLockOperation.RECONCILE, keys, args),
                keys,
                ownerId,
                requestId,
            ))
        }
    }

    fun renew(
        handle: FencedLockHandle,
        extension: Duration,
    ): LockMutationResult<FencedLockHandle> {
        validateHandle(handle)
        val args = fencedRenewArgs(handle, extension)
        if (closed.get()) return LockMutationResult.Closed
        return classified(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordRenewOutcome(handle, decodeFencedRenew(executor.run(FencedLockOperation.RENEW, keys, args), handle))
        }
    }

    fun renewAsync(
        handle: FencedLockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<FencedLockHandle>> {
        validateHandle(handle)
        val args = fencedRenewArgs(handle, extension)
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(FencedLockOperation.RENEW, keys, args)
            .mapResult(
                decode = { recordRenewOutcome(handle, decodeFencedRenew(it, handle)) },
                backend = { LockMutationResult.BackendFailure(it) },
                integrity = { LockMutationResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
            )
    }

    suspend fun renewSuspending(
        handle: FencedLockHandle,
        extension: Duration,
    ): LockMutationResult<FencedLockHandle> {
        validateHandle(handle)
        val args = fencedRenewArgs(handle, extension)
        if (closed.get()) return LockMutationResult.Closed
        return classifiedSuspending(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordRenewOutcome(
                handle,
                decodeFencedRenew(executor.runSuspending(FencedLockOperation.RENEW, keys, args), handle),
            )
        }
    }

    fun release(handle: FencedLockHandle): LockMutationResult<FencedLockHandle> {
        validateHandle(handle)
        val args = fencedHandleArgs(handle) + FENCED_TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return LockMutationResult.Closed
        return classified(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordReleaseOutcome(handle, decodeFencedRelease(executor.run(FencedLockOperation.RELEASE, keys, args)))
        }
    }

    fun releaseAsync(handle: FencedLockHandle): CompletableFuture<LockMutationResult<FencedLockHandle>> {
        validateHandle(handle)
        val args = fencedHandleArgs(handle) + FENCED_TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(FencedLockOperation.RELEASE, keys, args)
            .mapResult(
                decode = { recordReleaseOutcome(handle, decodeFencedRelease(it)) },
                backend = { LockMutationResult.BackendFailure(it) },
                integrity = { LockMutationResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
            )
    }

    suspend fun releaseSuspending(handle: FencedLockHandle): LockMutationResult<FencedLockHandle> {
        validateHandle(handle)
        val args = fencedHandleArgs(handle) + FENCED_TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return LockMutationResult.Closed
        return classifiedSuspending(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordReleaseOutcome(
                handle,
                decodeFencedRelease(executor.runSuspending(FencedLockOperation.RELEASE, keys, args)),
            )
        }
    }

    private fun releaseAbandoned(handle: FencedLockHandle) {
        validateHandle(handle)
        removeWatchdog(handle)
        executor.runAsync(
            FencedLockOperation.RELEASE,
            keys,
            fencedHandleArgs(handle) + FENCED_TERMINAL_TTL_MILLIS.toString(),
        ).exceptionally { null }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            waitSupport.close()
            watchdogs.values.forEach(CoordinationRuntime.CoordinationTaskRegistration::close)
            watchdogs.clear()
            registration.close()
        }
    }

    private fun registerWatchdog(
        result: LockAcquireResult<FencedLockHandle>,
    ): LockAcquireResult<FencedLockHandle> {
        val handle = when (result) {
            is LockAcquireResult.Acquired -> result.handle
            is LockAcquireResult.Reentered -> result.handle
            else -> return result
        }
        return if (ensureWatchdog(handle)) {
            result
        } else if (closed.get() || registration.isClosed) {
            LockAcquireResult.Closed
        } else {
            LockAcquireResult.Ambiguous(
                handle.ownerId,
                handle.requestId,
                LockRecoveryAction.RECONCILE_REQUEST,
            )
        }
    }

    private fun registerWatchdog(
        result: LockReconcileResult<FencedLockHandle>,
    ): LockReconcileResult<FencedLockHandle> {
        val handle = (result as? LockReconcileResult.Owned)?.handle ?: return result
        return if (ensureWatchdog(handle)) {
            result
        } else if (closed.get() || registration.isClosed) {
            LockReconcileResult.Closed
        } else {
            LockReconcileResult.Ambiguous(LockRecoveryAction.RECONCILE_REQUEST)
        }
    }

    private fun ensureWatchdog(handle: FencedLockHandle): Boolean {
        val policy = handle.leasePolicy as? LeasePolicy.Watchdog ?: return true
        discardClosedWatchdogs()
        var registered = true
        var capacityRejected = false
        watchdogs.compute(handle) { _, current ->
            if (current != null && !current.isClosed) {
                current
            } else {
                try {
                    registration.registerWatchdog(
                        ttl = policy.ttl.toKotlinDuration(),
                        renewalInterval = policy.renewalInterval.toKotlinDuration(),
                        generation = handle.generation.value,
                        maxLifetime = policy.maxLifetime.toKotlinDuration(),
                        onOwnershipLost = {
                            watchdogs.computeIfPresent(handle) { _, current ->
                                current.takeUnless { it.isClosed }
                            }
                            recordOwnershipLoss(policy)
                        },
                    ) {
                        renewForWatchdog(handle, policy)
                    }
                } catch (_: CoordinationCapacityException) {
                    capacityRejected = true
                    registered = false
                    null
                } catch (_: IllegalStateException) {
                    registered = false
                    null
                }
            }
        }
        if (capacityRejected) {
            recordCapacityRejection(policy)
        }
        return registered
    }

    private fun renewForWatchdog(
        handle: FencedLockHandle,
        policy: LeasePolicy.Watchdog,
    ): CompletableFuture<CoordinationRenewalOutcome> {
        if (closed.get()) {
            return CompletableFuture.completedFuture(CoordinationRenewalOutcome.OWNERSHIP_LOST)
        }
        return executor.runAsync(
            FencedLockOperation.RENEW,
            keys,
            fencedRenewArgs(handle, policy.ttl),
        ).mapResult(
            decode = { decodeFencedRenew(it, handle) },
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ).thenApply { result ->
            when (result) {
                is LockMutationResult.Renewed -> CoordinationRenewalOutcome.RENEWED
                else -> CoordinationRenewalOutcome.OWNERSHIP_LOST
            }
        }
    }

    private fun recordRenewOutcome(
        handle: FencedLockHandle,
        result: LockMutationResult<FencedLockHandle>,
    ): LockMutationResult<FencedLockHandle> {
        when (result) {
            LockMutationResult.AlreadyReleased,
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun recordReleaseOutcome(
        handle: FencedLockHandle,
        result: LockMutationResult<FencedLockHandle>,
    ): LockMutationResult<FencedLockHandle> {
        when (result) {
            is LockMutationResult.Released,
            LockMutationResult.AlreadyReleased,
            -> removeWatchdog(handle)
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun removeWatchdog(handle: FencedLockHandle) {
        watchdogs.remove(handle)?.close()
    }

    private fun discardClosedWatchdogs() {
        watchdogs.entries.removeIf { it.value.isClosed }
    }

    private fun recordCapacityRejection(policy: LeasePolicy) {
        recordObservation(
            counter = LockCounterName.CAPACITY_REJECTION_TOTAL,
            operation = LockOperation.ACQUIRE,
            outcome = LockOutcome.CAPACITY_REJECTED,
            policy = policy,
        )
    }

    private fun recordOwnershipLoss(policy: LeasePolicy) {
        recordObservation(
            counter = LockCounterName.OWNERSHIP_LOSS_TOTAL,
            operation = LockOperation.RENEW,
            outcome = LockOutcome.OWNERSHIP_LOST,
            policy = policy,
        )
    }

    private fun recordObservation(
        counter: LockCounterName,
        operation: LockOperation,
        outcome: LockOutcome,
        policy: LeasePolicy,
    ) {
        val leasePolicy = when (policy) {
            is LeasePolicy.Fixed -> LockLeasePolicyKind.FIXED
            is LeasePolicy.Watchdog -> LockLeasePolicyKind.WATCHDOG
        }
        val dimensions = LockDimensions(
            objectKind = LockKind.FENCED,
            operation = operation,
            outcome = outcome,
            failureKind = null,
            leasePolicy = leasePolicy,
        )
        observationSink.recordSafely(
            LockObservation.Counter(
                name = counter,
                delta = 1L,
                dimensions = dimensions,
            ),
        )
        observationSink.recordSafely(
            LockObservation.Event(
                LockEvent(
                    objectKind = dimensions.objectKind,
                    operation = dimensions.operation,
                    outcome = dimensions.outcome,
                    failureKind = dimensions.failureKind,
                    leasePolicy = dimensions.leasePolicy,
                ),
            ),
        )
    }

    private fun validateHandle(handle: FencedLockHandle) {
        require(handle.kind == LockKind.FENCED) { "Handle kind must be FENCED." }
        require(handle.epoch == config.epoch) { "Handle epoch must match the fenced lock configuration." }
        require(handle.objectFingerprint == keys.fingerprint) {
            "Handle belongs to a different fenced lock object."
        }
    }

    private fun classifiedAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        command: () -> List<String>,
    ): LockAcquireResult<FencedLockHandle> =
        classified(
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeFencedAcquire(command(), keys, ownerId, requestId)
        }

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FencedLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): FencedLockClient {
            val keys = deriveFencedLockKeys(name, config, connection.codec)
            val observationRecorder = LockObservationRecorder(LockKind.FENCED, observationSink)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return FencedLockClient(
                keys,
                config,
                DefaultFencedLockCommandExecutor(connection.sync(), connection.async(), observationRecorder),
                runtime.registerObject(keys.fingerprint, observationRecorder.asCoordinationObserver()),
                observationSink,
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FencedLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): FencedLockClient {
            val keys = deriveFencedLockKeys(name, config, connection.codec)
            val observationRecorder = LockObservationRecorder(LockKind.FENCED, observationSink)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return FencedLockClient(
                keys,
                config,
                DefaultFencedLockCommandExecutor(connection.sync(), connection.async(), observationRecorder),
                runtime.registerObject(keys.fingerprint, observationRecorder.asCoordinationObserver()),
                observationSink,
            )
        }
    }
}

private fun FencedLockOperation.toLockOperation(): LockOperation =
    when (this) {
        FencedLockOperation.BOOTSTRAP,
        FencedLockOperation.INSPECT,
        -> LockOperation.INSPECT
        FencedLockOperation.ACQUIRE -> LockOperation.ACQUIRE
        FencedLockOperation.RECONCILE -> LockOperation.RECONCILE
        FencedLockOperation.RENEW -> LockOperation.RENEW
        FencedLockOperation.RELEASE -> LockOperation.RELEASE
    }

private inline fun <R> classified(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    recoveryAction: LockRecoveryAction,
    block: () -> R,
): R =
    try {
        block()
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND ->
                backend(classifyLockBackendFailure(error, recoveryAction))
            CoordinationFailureClassification.INTEGRITY ->
                integrity(malformedIntegrityFailure())
        }
    } catch (error: IllegalArgumentException) {
        integrity(malformedIntegrityFailure())
    } catch (error: Exception) {
        backend(classifyLockBackendFailure(error, recoveryAction))
    }

private suspend inline fun <R> classifiedSuspending(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    recoveryAction: LockRecoveryAction,
    crossinline block: suspend () -> R,
): R =
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND ->
                backend(classifyLockBackendFailure(error, recoveryAction))
            CoordinationFailureClassification.INTEGRITY ->
                integrity(malformedIntegrityFailure())
        }
    } catch (error: IllegalArgumentException) {
        integrity(malformedIntegrityFailure())
    } catch (error: Exception) {
        backend(classifyLockBackendFailure(error, recoveryAction))
    }

private fun <R> CompletableFuture<List<String>>.mapResult(
    decode: (List<String>) -> R,
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    recoveryAction: LockRecoveryAction,
    onLateSuccess: (List<String>) -> Unit = {},
): CompletableFuture<R> {
    val mapped = CompletableFuture<R>()
    whenComplete { value, error ->
        if (mapped.isDone) {
            if (error == null) {
                runCatching { onLateSuccess(value) }
            }
            return@whenComplete
        }
        try {
            val result = if (error == null) {
                try {
                    decode(value)
                } catch (_: CoordinationProtocolException) {
                    integrity(malformedIntegrityFailure())
                } catch (_: IllegalArgumentException) {
                    integrity(malformedIntegrityFailure())
                }
            } else {
                backend(classifyLockBackendFailure(error, recoveryAction))
            }
            mapped.complete(result)
        } catch (failure: Throwable) {
            mapped.completeExceptionally(failure)
        }
    }
    mapped.whenComplete { _, _ ->
        if (mapped.isCancelled && !isDone) {
            cancel(false)
        }
    }
    return mapped
}

private fun classifyLockBackendFailure(
    error: Throwable,
    recoveryAction: LockRecoveryAction,
): LockBackendFailure {
    val cause = error.unwrapCompletionCause()
    val kind = when (cause) {
        is RedisConnectionException -> LockBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException,
        is TimeoutException,
        -> LockBackendFailureKind.TIMEOUT
        is RedisException -> LockBackendFailureKind.COMMAND
        else -> throw cause
    }
    return LockBackendFailure(kind, recoveryAction)
}

private fun Throwable.unwrapCompletionCause(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(MAX_COMPLETION_DEPTH) {
        if (current is CancellationException) throw current
        if (!seen.add(current)) return current
        val next = when (current) {
            is CompletionException,
            is ExecutionException,
            -> current.cause
            else -> null
        } ?: return current
        if (next === current) return current
        current = next
    }
    if (current is CancellationException) throw current
    return current
}

private const val MAX_COMPLETION_DEPTH = 8
