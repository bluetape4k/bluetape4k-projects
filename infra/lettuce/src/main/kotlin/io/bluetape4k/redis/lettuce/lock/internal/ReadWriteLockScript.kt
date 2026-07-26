package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationDeadline
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationCapacityException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocol
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRenewalOutcome
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.DowngradeResult
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockBackendFailure
import io.bluetape4k.redis.lettuce.lock.LockBackendFailureKind
import io.bluetape4k.redis.lettuce.lock.LockCounterName
import io.bluetape4k.redis.lettuce.lock.LockDimensions
import io.bluetape4k.redis.lettuce.lock.LockEvent
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailureKind
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
import io.bluetape4k.redis.lettuce.lock.ReadLockHandle
import io.bluetape4k.redis.lettuce.lock.ReadWriteLockConfig
import io.bluetape4k.redis.lettuce.lock.WriteLockHandle
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
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport
import kotlin.time.toKotlinDuration

internal val READ_WRITE_LOCK_SCRIPT = RedisScript(
    """
    local operation = ARGV[1]

    local function key_type(key)
        local result = redis.call('TYPE', key)
        if type(result) == 'table' then return result.ok end
        return result
    end

    local function positive(value)
        return type(value) == 'string'
            and string.len(value) <= 16
            and string.match(value, '^[1-9][0-9]*$') ~= nil
            and (string.len(value) < 16 or value <= '9007199254740991')
    end

    local function non_negative(value)
        return value == '0' or positive(value)
    end

    local function now_millis()
        local value = redis.call('TIME')
        return tonumber(value[1]) * 1000 + math.floor(tonumber(value[2]) / 1000)
    end

    local function split(value, expected)
        if type(value) ~= 'string' or string.len(value) > 1024 then return nil end
        local result = {}
        for field in string.gmatch(value .. '|', '(.-)|') do
            result[#result + 1] = field
        end
        if #result ~= expected then return nil end
        return result
    end

    local function valid_policy(value)
        if type(value) ~= 'string' or string.len(value) > 128 then return false end
        local fixed = string.match(value, '^F:([1-9][0-9]*)$')
        if fixed ~= nil then return positive(fixed) end
        local ttl, interval, lifetime = string.match(value, '^W:([1-9][0-9]*):([1-9][0-9]*):([1-9][0-9]*)$')
        return positive(ttl) and positive(interval) and positive(lifetime)
    end

    local function parse_reader(value)
        local fields = split(value, 3)
        if fields == nil or not positive(fields[1]) or not positive(fields[2]) or not positive(fields[3]) then
            return nil
        end
        return fields
    end

    local function parse_hold(value)
        local fields = split(value, 6)
        if fields == nil
            or (fields[1] ~= 'R' and fields[1] ~= 'W')
            or string.len(fields[2]) < 1 or string.len(fields[2]) > 256
            or string.len(fields[3]) < 1 or string.len(fields[3]) > 256
            or not positive(fields[4])
            or (fields[5] ~= 'A' and fields[5] ~= 'R' and fields[5] ~= 'D')
            or not valid_policy(fields[6]) then
            return nil
        end
        return fields
    end

    local function parse_waiter(value)
        local fields = split(value, 8)
        if fields == nil
            or not positive(fields[1])
            or not non_negative(fields[2])
            or not positive(fields[3])
            or (fields[4] ~= 'R' and fields[4] ~= 'W')
            or string.len(fields[5]) < 1 or string.len(fields[5]) > 256
            or string.len(fields[6]) < 1 or string.len(fields[6]) > 256
            or not valid_policy(fields[7])
            or not positive(fields[8]) then
            return nil
        end
        return fields
    end

    local function parse_terminal(value)
        local fields = split(value, 7)
        if fields == nil
            or (fields[1] ~= 'R' and fields[1] ~= 'W')
            or string.len(fields[2]) < 1 or string.len(fields[2]) > 256
            or string.len(fields[3]) < 1 or string.len(fields[3]) > 256
            or not positive(fields[4])
            or (fields[5] ~= 'X' and fields[5] ~= 'D')
            or not valid_policy(fields[6])
            or not non_negative(fields[7]) then
            return nil
        end
        return fields
    end

    local function validate_types()
        local expected = {'hash', 'string', 'hash', 'hash', 'hash', 'zset', 'hash', 'string'}
        for index = 1, 8 do
            local actual = key_type(KEYS[index])
            if actual ~= 'none' and actual ~= expected[index] then return false end
        end
        local generation = redis.call('GET', KEYS[2])
        if generation ~= false and not positive(generation) then return false end
        local sequence = redis.call('GET', KEYS[8])
        if sequence ~= false and not positive(sequence) then return false end
        if redis.call('ZCARD', KEYS[6]) ~= redis.call('HLEN', KEYS[7]) then return false end
        local mode = redis.call('HGET', KEYS[1], 'mode')
        if mode ~= false and mode ~= 'R' and mode ~= 'W' then return false end
        return true
    end

    local function delete_holds(mode, owner, generation)
        local values = redis.call('HGETALL', KEYS[4])
        for index = 1, #values, 2 do
            local fields = parse_hold(values[index + 1])
            if fields == nil then return false end
            if fields[1] == mode and fields[2] == owner and fields[4] == generation then
                redis.call('HDEL', KEYS[4], values[index])
            end
        end
        return true
    end

    local function cleanup_holders(now)
        local mode = redis.call('HGET', KEYS[1], 'mode')
        if mode == false then
            if redis.call('HLEN', KEYS[3]) ~= 0 or redis.call('HLEN', KEYS[4]) ~= 0 then return false end
            return true
        end
        if mode == 'W' then
            local owner = redis.call('HGET', KEYS[1], 'owner')
            local generation = redis.call('HGET', KEYS[1], 'generation')
            local count = redis.call('HGET', KEYS[1], 'holdCount')
            local deadline = redis.call('HGET', KEYS[1], 'deadline')
            if owner == false or string.len(owner) < 1 or string.len(owner) > 256
                or not positive(generation) or not positive(count) or not positive(deadline)
                or redis.call('HLEN', KEYS[1]) ~= 5 then return false end
            if tonumber(deadline) <= now then
                if not delete_holds('W', owner, generation) then return false end
                if redis.call('HLEN', KEYS[3]) == 0 then
                    redis.call('DEL', KEYS[1])
                else
                    redis.call('DEL', KEYS[1])
                    redis.call('HSET', KEYS[1], 'mode', 'R')
                end
            end
            local pending_readers = redis.call('HGETALL', KEYS[3])
            for index = 1, #pending_readers, 2 do
                local reader_owner = pending_readers[index]
                local reader = parse_reader(pending_readers[index + 1])
                if reader == nil then return false end
                if tonumber(reader[3]) <= now then
                    if not delete_holds('R', reader_owner, reader[1]) then return false end
                    redis.call('HDEL', KEYS[3], reader_owner)
                end
            end
            return true
        end

        if redis.call('HLEN', KEYS[1]) ~= 1 then return false end
        local readers = redis.call('HGETALL', KEYS[3])
        for index = 1, #readers, 2 do
            local owner = readers[index]
            local fields = parse_reader(readers[index + 1])
            if fields == nil then return false end
            if tonumber(fields[3]) <= now then
                if not delete_holds('R', owner, fields[1]) then return false end
                redis.call('HDEL', KEYS[3], owner)
            end
        end
        if redis.call('HLEN', KEYS[3]) == 0 then
            redis.call('DEL', KEYS[1])
        end
        return true
    end

    local function cleanup_waiters(batch, now)
        local size = redis.call('ZCARD', KEYS[6])
        local values = redis.call('ZRANGE', KEYS[6], 0, batch - 1, 'WITHSCORES')
        local stale = {}
        for index = 1, #values, 2 do
            local member = values[index]
            local score = values[index + 1]
            local fields = parse_waiter(redis.call('HGET', KEYS[7], member))
            if fields == nil or fields[1] ~= score then return nil end
            if tonumber(fields[3]) <= now then
                stale[#stale + 1] = member
            else
                break
            end
        end
        for index = 1, #stale do
            redis.call('ZREM', KEYS[6], stale[index])
            redis.call('HDEL', KEYS[7], stale[index])
        end
        return #stale == batch and size > batch
    end

    local function remaining_ttl(mode, owner, now)
        if mode == 'W' then
            local deadline = redis.call('HGET', KEYS[1], 'deadline')
            return deadline == false and 0 or math.max(0, tonumber(deadline) - now)
        end
        local fields = parse_reader(redis.call('HGET', KEYS[3], owner))
        return fields == nil and 0 or math.max(0, tonumber(fields[3]) - now)
    end

    local function owner_hold_count(mode, owner)
        if mode == 'W' then return tonumber(redis.call('HGET', KEYS[1], 'holdCount')) end
        local fields = parse_reader(redis.call('HGET', KEYS[3], owner))
        return fields == nil and 0 or tonumber(fields[2])
    end

    local function remove_waiter(member)
        redis.call('ZREM', KEYS[6], member)
        redis.call('HDEL', KEYS[7], member)
    end

    local function has_queued_writer()
        local queued = redis.call('ZRANGE', KEYS[6], 0, -1)
        for index = 1, #queued do
            local fields = parse_waiter(redis.call('HGET', KEYS[7], queued[index]))
            if fields == nil then return nil end
            if fields[4] == 'W' then return true end
        end
        return false
    end

    local function next_generation()
        local current = redis.call('GET', KEYS[2])
        if current ~= false and tonumber(current) >= 9007199254740991 then return nil end
        redis.call('INCR', KEYS[2])
        return redis.call('GET', KEYS[2])
    end

    local function add_read(owner, request, member, policy, ttl, acquisition, now)
        local generation = next_generation()
        if generation == nil then return {'CAPACITY'} end
        redis.call('HSET', KEYS[1], 'mode', 'R')
        redis.call('HSET', KEYS[3], owner, generation .. '|1|' .. tostring(now + ttl))
        redis.call('HSET', KEYS[4], member, 'R|' .. owner .. '|' .. request .. '|' .. generation .. '|' .. acquisition .. '|' .. policy)
        remove_waiter(member)
        return {'ACQUIRED', generation, '1', tostring(ttl), policy}
    end

    local function add_write(owner, request, member, policy, ttl, acquisition, now)
        local generation = next_generation()
        if generation == nil then return {'CAPACITY'} end
        redis.call('DEL', KEYS[1])
        redis.call('HSET', KEYS[1],
            'mode', 'W',
            'owner', owner,
            'generation', generation,
            'holdCount', '1',
            'deadline', tostring(now + ttl))
        redis.call('HSET', KEYS[4], member, 'W|' .. owner .. '|' .. request .. '|' .. generation .. '|' .. acquisition .. '|' .. policy)
        remove_waiter(member)
        return {'ACQUIRED', generation, '1', tostring(ttl), policy}
    end

    if not validate_types() then return {'INTEGRITY'} end
    local now = now_millis()
    if not cleanup_holders(now) then return {'INTEGRITY'} end

    if operation == 'ACQUIRE' then
        local requested_mode = ARGV[2]
        local owner = ARGV[3]
        local request = ARGV[4]
        local member = ARGV[5]
        local policy = ARGV[6]
        local ttl = tonumber(ARGV[7])
        local maximum = tonumber(ARGV[8])
        local wait_millis = tonumber(ARGV[9])
        local cleanup_batch = tonumber(ARGV[10])
        local maximum_queue = tonumber(ARGV[11])

        local cleanup_pending = cleanup_waiters(cleanup_batch, now)
        if cleanup_pending == nil then return {'INTEGRITY'} end
        if cleanup_pending then return {'CLEANUP_PENDING'} end

        local active_mode = redis.call('HGET', KEYS[1], 'mode')
        local hold = redis.call('HGET', KEYS[4], member)
        if hold ~= false then
            local fields = parse_hold(hold)
            if fields == nil or fields[1] ~= requested_mode or fields[2] ~= owner or fields[3] ~= request then
                return {'INTEGRITY'}
            end
            local current_generation = requested_mode == 'W'
                and redis.call('HGET', KEYS[1], 'generation')
                or (parse_reader(redis.call('HGET', KEYS[3], owner)) or {})[1]
            if current_generation == fields[4] then
                remove_waiter(member)
                local count = owner_hold_count(requested_mode, owner)
                local tag = fields[5] == 'A' and 'ACQUIRED' or 'REENTERED'
                return {tag, fields[4], tostring(count), tostring(remaining_ttl(requested_mode, owner, now)), fields[6]}
            end
            return {'INTEGRITY'}
        end

        if requested_mode == 'R' and active_mode == 'R' then
            local reader = parse_reader(redis.call('HGET', KEYS[3], owner))
            local writer_queued = has_queued_writer()
            if writer_queued == nil then return {'INTEGRITY'} end
            if reader ~= nil and not writer_queued then
                local count = tonumber(reader[2])
                if count >= maximum then return {'CAPACITY'} end
                count = count + 1
                redis.call('HSET', KEYS[3], owner, reader[1] .. '|' .. tostring(count) .. '|' .. tostring(now + ttl))
                redis.call('HSET', KEYS[4], member, 'R|' .. owner .. '|' .. request .. '|' .. reader[1] .. '|R|' .. policy)
                remove_waiter(member)
                return {'REENTERED', reader[1], tostring(count), tostring(ttl), policy}
            end
        elseif requested_mode == 'W' and active_mode == 'W' and redis.call('HGET', KEYS[1], 'owner') == owner then
            local count = tonumber(redis.call('HGET', KEYS[1], 'holdCount'))
            if count >= maximum then return {'CAPACITY'} end
            count = count + 1
            local generation = redis.call('HGET', KEYS[1], 'generation')
            redis.call('HSET', KEYS[1], 'holdCount', tostring(count), 'deadline', tostring(now + ttl))
            redis.call('HSET', KEYS[4], member, 'W|' .. owner .. '|' .. request .. '|' .. generation .. '|R|' .. policy)
            remove_waiter(member)
            return {'REENTERED', generation, tostring(count), tostring(ttl), policy}
        end

        local waiter = redis.call('HGET', KEYS[7], member)
        local waiter_fields = waiter ~= false and parse_waiter(waiter) or nil
        if waiter ~= false and waiter_fields == nil then return {'INTEGRITY'} end
        if waiter_fields ~= nil and (waiter_fields[4] ~= requested_mode or waiter_fields[5] ~= owner or waiter_fields[6] ~= request) then
            return {'INTEGRITY'}
        end

        local queue_empty = redis.call('ZCARD', KEYS[6]) == 0
        if active_mode == 'R' and requested_mode == 'R' and queue_empty then
            return add_read(owner, request, member, policy, ttl, 'A', now)
        end
        if active_mode == false and queue_empty then
            if requested_mode == 'R' then return add_read(owner, request, member, policy, ttl, 'A', now) end
            return add_write(owner, request, member, policy, ttl, 'A', now)
        end

        if waiter_fields == nil and wait_millis > 0 then
            if redis.call('ZCARD', KEYS[6]) >= maximum_queue then return {'CAPACITY'} end
            local current = redis.call('GET', KEYS[8])
            if current ~= false and tonumber(current) >= 9007199254740991 then return {'CAPACITY'} end
            redis.call('INCR', KEYS[8])
            local sequence = redis.call('GET', KEYS[8])
            local observed_generation = redis.call('GET', KEYS[2]) or '0'
            local deadline = tostring(now + wait_millis)
            redis.call('ZADD', KEYS[6], sequence, member)
            redis.call('HSET', KEYS[7], member,
                sequence .. '|' .. observed_generation .. '|' .. deadline .. '|' ..
                requested_mode .. '|' .. owner .. '|' .. request .. '|' .. policy .. '|' .. tostring(ttl))
            waiter_fields = {sequence, observed_generation, deadline, requested_mode, owner, request, policy, tostring(ttl)}
        end

        if waiter_fields ~= nil then
            local rank = redis.call('ZRANK', KEYS[6], member)
            if rank == false then return {'INTEGRITY'} end
            if active_mode == false then
                local before = redis.call('ZRANGE', KEYS[6], 0, rank)
                local blocked = false
                for index = 1, #before do
                    local fields = parse_waiter(redis.call('HGET', KEYS[7], before[index]))
                    if fields == nil then return {'INTEGRITY'} end
                    if before[index] ~= member and fields[4] == 'W' then blocked = true end
                end
                if not blocked then
                    if requested_mode == 'R' then
                        return add_read(owner, request, member, policy, ttl, 'A', now)
                    elseif rank == 0 then
                        return add_write(owner, request, member, policy, ttl, 'A', now)
                    end
                end
            elseif active_mode == 'R' and requested_mode == 'R' then
                local before = redis.call('ZRANGE', KEYS[6], 0, rank)
                local writer_before = false
                for index = 1, #before do
                    local fields = parse_waiter(redis.call('HGET', KEYS[7], before[index]))
                    if fields == nil then return {'INTEGRITY'} end
                    if before[index] ~= member and fields[4] == 'W' then writer_before = true end
                end
                if not writer_before then
                    return add_read(owner, request, member, policy, ttl, 'A', now)
                end
            end
            return {'QUEUED', waiter_fields[1], waiter_fields[2],
                tostring(math.max(0, tonumber(waiter_fields[3]) - now)),
                tostring(remaining_ttl(active_mode or requested_mode, owner, now))}
        end
        return {'CONTENDED', tostring(active_mode == false and 0 or remaining_ttl(active_mode, owner, now))}
    end

    if operation == 'RECONCILE' then
        local requested_mode = ARGV[2]
        local owner = ARGV[3]
        local request = ARGV[4]
        local member = ARGV[5]
        local hold = redis.call('HGET', KEYS[4], member)
        if hold ~= false then
            local fields = parse_hold(hold)
            if fields == nil or fields[1] ~= requested_mode or fields[2] ~= owner or fields[3] ~= request then
                return {'INTEGRITY'}
            end
            local count = owner_hold_count(requested_mode, owner)
            if count > 0 then
                return {'OWNED', fields[4], tostring(count), tostring(remaining_ttl(requested_mode, owner, now)), fields[6]}
            end
        end
        local waiter = redis.call('HGET', KEYS[7], member)
        if waiter ~= false then
            local fields = parse_waiter(waiter)
            if fields == nil or fields[4] ~= requested_mode or fields[5] ~= owner or fields[6] ~= request then
                return {'INTEGRITY'}
            end
            local remaining = tonumber(fields[3]) - now
            if remaining <= 0 then
                remove_waiter(member)
                return {'REMOVED'}
            end
            return {'QUEUED', fields[1], fields[2], tostring(remaining)}
        end
        local terminal = redis.call('HGET', KEYS[5], member)
        if terminal ~= false then
            local fields = parse_terminal(terminal)
            if fields == nil or fields[1] ~= requested_mode or fields[2] ~= owner or fields[3] ~= request then
                return {'INTEGRITY'}
            end
            return {'RELEASED'}
        end
        return {'NOT_FOUND'}
    end

    if operation == 'REMOVE' then
        local member = ARGV[5]
        local expected_sequence = ARGV[6]
        local expected_generation = ARGV[7]
        local waiter = redis.call('HGET', KEYS[7], member)
        if waiter == false then return {'NOT_FOUND'} end
        local fields = parse_waiter(waiter)
        if fields == nil then return {'INTEGRITY'} end
        if fields[1] ~= expected_sequence or fields[2] ~= expected_generation then return {'STALE'} end
        remove_waiter(member)
        return {'REMOVED'}
    end

    if operation == 'INSPECT' or operation == 'RENEW' or operation == 'RELEASE' then
        local requested_mode = ARGV[2]
        local owner = ARGV[3]
        local request = ARGV[4]
        local member = ARGV[5]
        local expected_generation = ARGV[6]
        local hold = redis.call('HGET', KEYS[4], member)
        if hold == false then
            local terminal = redis.call('HGET', KEYS[5], member)
            if terminal ~= false then
                local fields = parse_terminal(terminal)
                if fields == nil then return {'INTEGRITY'} end
                if fields[4] ~= expected_generation then return {'STALE'} end
                return operation == 'INSPECT' and {'RELEASED'} or {'ALREADY_RELEASED'}
            end
            local current = redis.call('GET', KEYS[2])
            if current ~= false and tonumber(current) > tonumber(expected_generation) then return {'STALE'} end
            return {'EXPIRED'}
        end
        local fields = parse_hold(hold)
        if fields == nil or fields[1] ~= requested_mode or fields[2] ~= owner or fields[3] ~= request then
            return {'INTEGRITY'}
        end
        if fields[4] ~= expected_generation then return {'STALE'} end
        local count = owner_hold_count(requested_mode, owner)
        if count == 0 then return {'LOST'} end
        local ttl = remaining_ttl(requested_mode, owner, now)
        if operation == 'INSPECT' then
            return {'OWNED', fields[4], tostring(count), tostring(ttl), fields[6]}
        end
        if operation == 'RENEW' then
            local extension = tonumber(ARGV[7])
            local deadline = now + extension
            if requested_mode == 'W' then
                redis.call('HSET', KEYS[1], 'deadline', tostring(deadline))
            else
                redis.call('HSET', KEYS[3], owner, fields[4] .. '|' .. tostring(count) .. '|' .. tostring(deadline))
            end
            return {'RENEWED', fields[4], tostring(extension), fields[6]}
        end

        redis.call('HDEL', KEYS[4], member)
        count = count - 1
        if requested_mode == 'W' then
            if count == 0 then
                if redis.call('HLEN', KEYS[3]) == 0 then
                    redis.call('DEL', KEYS[1])
                else
                    redis.call('DEL', KEYS[1])
                    redis.call('HSET', KEYS[1], 'mode', 'R')
                end
            else
                redis.call('HSET', KEYS[1], 'holdCount', tostring(count))
            end
        else
            if count == 0 then
                redis.call('HDEL', KEYS[3], owner)
                if redis.call('HLEN', KEYS[3]) == 0 and redis.call('HGET', KEYS[1], 'mode') == 'R' then
                    redis.call('DEL', KEYS[1])
                end
            else
                local reader = parse_reader(redis.call('HGET', KEYS[3], owner))
                redis.call('HSET', KEYS[3], owner, fields[4] .. '|' .. tostring(count) .. '|' .. reader[3])
            end
        end
        redis.call('HSET', KEYS[5], member,
            requested_mode .. '|' .. owner .. '|' .. request .. '|' .. expected_generation .. '|X|' .. fields[6] .. '|0')
        redis.call('PEXPIRE', KEYS[5], tonumber(ARGV[7]))
        return {'RELEASED', tostring(count)}
    end

    if operation == 'DOWNGRADE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        local write_member = ARGV[4]
        local read_member = ARGV[5]
        local expected_generation = ARGV[6]
        local terminal_ttl = tonumber(ARGV[7])
        local terminal = redis.call('HGET', KEYS[5], write_member)
        if terminal ~= false then
            local fields = parse_terminal(terminal)
            if fields == nil then return {'INTEGRITY'} end
            if fields[4] ~= expected_generation then return {'STALE'} end
            if fields[5] == 'D' and tonumber(fields[7]) > 0 then
                return {'DOWNGRADED', fields[7], fields[6]}
            end
            local current = redis.call('GET', KEYS[2])
            if current ~= false and tonumber(current) > tonumber(expected_generation) then return {'STALE'} end
            return {'ALREADY_RELEASED'}
        end

        local hold = redis.call('HGET', KEYS[4], write_member)
        if hold == false then
            local current = redis.call('GET', KEYS[2])
            if current ~= false and tonumber(current) > tonumber(expected_generation) then return {'STALE'} end
            return {'EXPIRED'}
        end
        local fields = parse_hold(hold)
        if fields == nil or fields[1] ~= 'W' or fields[2] ~= owner or fields[3] ~= request then
            return {'INTEGRITY'}
        end
        if fields[4] ~= expected_generation then return {'STALE'} end
        if redis.call('HGET', KEYS[1], 'mode') ~= 'W'
            or redis.call('HGET', KEYS[1], 'owner') ~= owner
            or redis.call('HGET', KEYS[1], 'generation') ~= expected_generation then
            return {'LOST'}
        end

        local writer_count = tonumber(redis.call('HGET', KEYS[1], 'holdCount'))
        local deadline = redis.call('HGET', KEYS[1], 'deadline')
        local read_generation = next_generation()
        if read_generation == nil then return {'CAPACITY'} end
        redis.call('HDEL', KEYS[4], write_member)
        writer_count = writer_count - 1
        redis.call('HSET', KEYS[3], owner, read_generation .. '|1|' .. deadline)
        redis.call('HSET', KEYS[4], read_member,
            'R|' .. owner .. '|' .. request .. '|' .. read_generation .. '|D|' .. fields[6])
        if writer_count == 0 then
            redis.call('DEL', KEYS[1])
            redis.call('HSET', KEYS[1], 'mode', 'R')
        else
            redis.call('HSET', KEYS[1], 'holdCount', tostring(writer_count))
        end
        redis.call('HSET', KEYS[5], write_member,
            'W|' .. owner .. '|' .. request .. '|' .. expected_generation .. '|D|' .. fields[6] .. '|' .. read_generation)
        redis.call('PEXPIRE', KEYS[5], terminal_ttl)
        return {'DOWNGRADED', read_generation, fields[6]}
    end

    return {'INTEGRITY'}
    """.trimIndent(),
)

internal enum class ReadWriteLockOperation(val wireValue: String) {
    ACQUIRE("ACQUIRE"),
    RECONCILE("RECONCILE"),
    REMOVE("REMOVE"),
    INSPECT("INSPECT"),
    RENEW("RENEW"),
    RELEASE("RELEASE"),
    DOWNGRADE("DOWNGRADE"),
}

private fun ReadWriteLockOperation.toLockOperation(): LockOperation =
    when (this) {
        ReadWriteLockOperation.ACQUIRE -> LockOperation.ACQUIRE
        ReadWriteLockOperation.RECONCILE -> LockOperation.RECONCILE
        ReadWriteLockOperation.REMOVE -> LockOperation.CLEANUP
        ReadWriteLockOperation.INSPECT -> LockOperation.INSPECT
        ReadWriteLockOperation.RENEW -> LockOperation.RENEW
        ReadWriteLockOperation.RELEASE -> LockOperation.RELEASE
        ReadWriteLockOperation.DOWNGRADE -> LockOperation.DOWNGRADE
    }

internal data class ReadWriteLockKeys(
    val state: String,
    val generation: String,
    val readers: String,
    val holds: String,
    val terminal: String,
    val queue: String,
    val waiters: String,
    val sequence: String,
    val fingerprint: String,
) {
    val all: Array<String> = arrayOf(state, generation, readers, holds, terminal, queue, waiters, sequence)
}

internal fun deriveReadWriteLockKeys(
    name: String,
    config: ReadWriteLockConfig,
    codec: RedisCodec<String, String>,
): ReadWriteLockKeys {
    val lock = config.lock
    val resource = lock.validateResourceName(name)
    val hashTag = lock.hashTag ?: resource
    val prefix = "${lock.namespace}:{$hashTag}:read-write-lock:$resource"
    val keys = listOf(
        lock.validateDerivedKey("$prefix:state"),
        lock.validateDerivedKey("$prefix:generation"),
        lock.validateDerivedKey("$prefix:readers"),
        lock.validateDerivedKey("$prefix:holds"),
        lock.validateDerivedKey("$prefix:terminal"),
        lock.validateDerivedKey("$prefix:queue"),
        lock.validateDerivedKey("$prefix:waiters"),
        lock.validateDerivedKey("$prefix:queue-sequence"),
    )
    require(keys.map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet().size == 1) {
        "Derived read/write lock keys must share one Redis Cluster slot."
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(codec.encodeKey(keys[0]).rwBytes())
    val fingerprint = digest.take(RW_FINGERPRINT_BYTES).joinToString("") { byte -> "%02x".format(byte) }
    return ReadWriteLockKeys(
        keys[0], keys[1], keys[2], keys[3], keys[4], keys[5], keys[6], keys[7], fingerprint,
    )
}

private interface ReadWriteLockCommandExecutor {
    fun run(operation: ReadWriteLockOperation, keys: ReadWriteLockKeys, args: List<String>): List<String>
    fun runAsync(
        operation: ReadWriteLockOperation,
        keys: ReadWriteLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>>
    suspend fun runSuspending(
        operation: ReadWriteLockOperation,
        keys: ReadWriteLockKeys,
        args: List<String>,
    ): List<String>
}

private class DefaultReadWriteLockCommandExecutor(
    private val sync: RedisScriptingCommands<String, String>,
    private val async: RedisScriptingAsyncCommands<String, String>,
    private val readObservationRecorder: LockObservationRecorder,
    private val writeObservationRecorder: LockObservationRecorder,
): ReadWriteLockCommandExecutor {
    override fun run(
        operation: ReadWriteLockOperation,
        keys: ReadWriteLockKeys,
        args: List<String>,
    ): List<String> =
        recorder(operation, args).runScript(
            sync, READ_WRITE_LOCK_SCRIPT, ScriptOutputType.MULTI, keys.all,
            operation.toLockOperation(),
            args = arrayOf(operation.wireValue, *args.toTypedArray()),
        )

    override fun runAsync(
        operation: ReadWriteLockOperation,
        keys: ReadWriteLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>> =
        recorder(operation, args).runScriptAsync(
            async, READ_WRITE_LOCK_SCRIPT, ScriptOutputType.MULTI, keys.all,
            operation.toLockOperation(),
            args = arrayOf(operation.wireValue, *args.toTypedArray()),
        )

    override suspend fun runSuspending(
        operation: ReadWriteLockOperation,
        keys: ReadWriteLockKeys,
        args: List<String>,
    ): List<String> =
        recorder(operation, args).runScriptSuspending(
            async, READ_WRITE_LOCK_SCRIPT, ScriptOutputType.MULTI, keys.all,
            operation.toLockOperation(),
            args = arrayOf(operation.wireValue, *args.toTypedArray()),
        )

    private fun recorder(
        operation: ReadWriteLockOperation,
        args: List<String>,
    ): LockObservationRecorder =
        if (operation == ReadWriteLockOperation.DOWNGRADE || args.firstOrNull() == LockKind.WRITE.rwMode) {
            writeObservationRecorder
        } else {
            readObservationRecorder
        }
}

private data class ReadWriteWaiterIdentity(
    val sequence: Long,
    val generation: Long,
)

private sealed interface ReadWriteAttempt {
    data class Result(val value: LockAcquireResult<LockHandle>): ReadWriteAttempt
    data class Queued(
        val identity: ReadWriteWaiterIdentity,
        val lockTtlMillis: Long,
    ): ReadWriteAttempt
}

internal class ReadWriteLockClient private constructor(
    private val keys: ReadWriteLockKeys,
    private val config: ReadWriteLockConfig,
    private val executor: ReadWriteLockCommandExecutor,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val observationSink: LockObservationSink,
) {
    private val closed = AtomicBoolean()
    private val watchdogs = ConcurrentHashMap<LockHandle, CoordinationRuntime.CoordinationTaskRegistration>()
    private val pendingAsync = ConcurrentHashMap.newKeySet<CompletableFuture<LockAcquireResult<LockHandle>>>()
    private val pendingSignals = ConcurrentHashMap.newKeySet<CompletableFuture<Unit>>()
    private val readWaitObservation = LockWaitObservation(LockObservationRecorder(LockKind.READ, observationSink))
    private val writeWaitObservation = LockWaitObservation(LockObservationRecorder(LockKind.WRITE, observationSink))

    fun tryAcquireRead(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<ReadLockHandle> =
        mapReadAcquire(registerWatchdog(tryAcquire(LockKind.READ, ownerId, requestId, leasePolicy)))

    fun tryAcquireReadAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<ReadLockHandle>> =
        mapHandleResultAsync(
            tryAcquireAsync(LockKind.READ, ownerId, requestId, leasePolicy),
            LockAcquireResult<LockHandle>::acquiredHandleOrNull,
        ) { mapReadAcquire(registerWatchdog(it)) }

    suspend fun tryAcquireReadSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<ReadLockHandle> =
        mapReadAcquire(registerWatchdog(tryAcquireSuspending(LockKind.READ, ownerId, requestId, leasePolicy)))

    fun acquireRead(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<ReadLockHandle> =
        mapReadAcquire(registerWatchdog(acquire(LockKind.READ, ownerId, requestId, waitTime, leasePolicy)))

    fun acquireReadAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<ReadLockHandle>> =
        mapHandleResultAsync(
            acquireAsync(LockKind.READ, ownerId, requestId, waitTime, leasePolicy),
            LockAcquireResult<LockHandle>::acquiredHandleOrNull,
        ) { mapReadAcquire(registerWatchdog(it)) }

    suspend fun acquireReadSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<ReadLockHandle> =
        mapReadAcquire(registerWatchdog(acquireSuspending(LockKind.READ, ownerId, requestId, waitTime, leasePolicy)))

    fun inspectRead(handle: ReadLockHandle): LockInspectResult<ReadLockHandle> =
        mapReadInspect(inspect(handle.lock, LockKind.READ))

    fun inspectReadAsync(handle: ReadLockHandle): CompletableFuture<LockInspectResult<ReadLockHandle>> =
        inspectAsync(handle.lock, LockKind.READ).thenApply(::mapReadInspect)

    suspend fun inspectReadSuspending(handle: ReadLockHandle): LockInspectResult<ReadLockHandle> =
        mapReadInspect(inspectSuspending(handle.lock, LockKind.READ))

    fun reconcileRead(ownerId: LockOwnerId, requestId: LockRequestId): LockReconcileResult<ReadLockHandle> =
        mapReadReconcile(registerWatchdog(reconcile(LockKind.READ, ownerId, requestId)))

    fun reconcileReadAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<ReadLockHandle>> =
        mapHandleResultAsync(
            reconcileAsync(LockKind.READ, ownerId, requestId),
            LockReconcileResult<LockHandle>::ownedHandleOrNull,
        ) { mapReadReconcile(registerWatchdog(it)) }

    suspend fun reconcileReadSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<ReadLockHandle> =
        mapReadReconcile(registerWatchdog(reconcileSuspending(LockKind.READ, ownerId, requestId)))

    fun renewRead(handle: ReadLockHandle, extension: Duration): LockMutationResult<ReadLockHandle> =
        mapReadMutation(recordRenew(handle.lock, renew(handle.lock, LockKind.READ, extension)))

    fun renewReadAsync(
        handle: ReadLockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<ReadLockHandle>> =
        renewAsync(handle.lock, LockKind.READ, extension)
            .thenApply { recordRenew(handle.lock, it) }
            .thenApply(::mapReadMutation)

    suspend fun renewReadSuspending(
        handle: ReadLockHandle,
        extension: Duration,
    ): LockMutationResult<ReadLockHandle> =
        mapReadMutation(recordRenew(handle.lock, renewSuspending(handle.lock, LockKind.READ, extension)))

    fun releaseRead(handle: ReadLockHandle): LockMutationResult<ReadLockHandle> =
        mapReadMutation(recordRelease(handle.lock, release(handle.lock, LockKind.READ)))

    fun releaseReadAsync(handle: ReadLockHandle): CompletableFuture<LockMutationResult<ReadLockHandle>> =
        releaseAsync(handle.lock, LockKind.READ)
            .thenApply { recordRelease(handle.lock, it) }
            .thenApply(::mapReadMutation)

    suspend fun releaseReadSuspending(handle: ReadLockHandle): LockMutationResult<ReadLockHandle> =
        mapReadMutation(recordRelease(handle.lock, releaseSuspending(handle.lock, LockKind.READ)))

    fun tryAcquireWrite(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<WriteLockHandle> =
        mapWriteAcquire(registerWatchdog(tryAcquire(LockKind.WRITE, ownerId, requestId, leasePolicy)))

    fun tryAcquireWriteAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<WriteLockHandle>> =
        mapHandleResultAsync(
            tryAcquireAsync(LockKind.WRITE, ownerId, requestId, leasePolicy),
            LockAcquireResult<LockHandle>::acquiredHandleOrNull,
        ) { mapWriteAcquire(registerWatchdog(it)) }

    suspend fun tryAcquireWriteSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<WriteLockHandle> =
        mapWriteAcquire(registerWatchdog(tryAcquireSuspending(LockKind.WRITE, ownerId, requestId, leasePolicy)))

    fun acquireWrite(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<WriteLockHandle> =
        mapWriteAcquire(registerWatchdog(acquire(LockKind.WRITE, ownerId, requestId, waitTime, leasePolicy)))

    fun acquireWriteAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<WriteLockHandle>> =
        mapHandleResultAsync(
            acquireAsync(LockKind.WRITE, ownerId, requestId, waitTime, leasePolicy),
            LockAcquireResult<LockHandle>::acquiredHandleOrNull,
        ) { mapWriteAcquire(registerWatchdog(it)) }

    suspend fun acquireWriteSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<WriteLockHandle> =
        mapWriteAcquire(registerWatchdog(acquireSuspending(LockKind.WRITE, ownerId, requestId, waitTime, leasePolicy)))

    fun inspectWrite(handle: WriteLockHandle): LockInspectResult<WriteLockHandle> =
        mapWriteInspect(inspect(handle.lock, LockKind.WRITE))

    fun inspectWriteAsync(handle: WriteLockHandle): CompletableFuture<LockInspectResult<WriteLockHandle>> =
        inspectAsync(handle.lock, LockKind.WRITE).thenApply(::mapWriteInspect)

    suspend fun inspectWriteSuspending(handle: WriteLockHandle): LockInspectResult<WriteLockHandle> =
        mapWriteInspect(inspectSuspending(handle.lock, LockKind.WRITE))

    fun reconcileWrite(ownerId: LockOwnerId, requestId: LockRequestId): LockReconcileResult<WriteLockHandle> =
        mapWriteReconcile(registerWatchdog(reconcile(LockKind.WRITE, ownerId, requestId)))

    fun reconcileWriteAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<WriteLockHandle>> =
        mapHandleResultAsync(
            reconcileAsync(LockKind.WRITE, ownerId, requestId),
            LockReconcileResult<LockHandle>::ownedHandleOrNull,
        ) { mapWriteReconcile(registerWatchdog(it)) }

    suspend fun reconcileWriteSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<WriteLockHandle> =
        mapWriteReconcile(registerWatchdog(reconcileSuspending(LockKind.WRITE, ownerId, requestId)))

    fun renewWrite(handle: WriteLockHandle, extension: Duration): LockMutationResult<WriteLockHandle> =
        mapWriteMutation(recordRenew(handle.lock, renew(handle.lock, LockKind.WRITE, extension)))

    fun renewWriteAsync(
        handle: WriteLockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<WriteLockHandle>> =
        renewAsync(handle.lock, LockKind.WRITE, extension)
            .thenApply { recordRenew(handle.lock, it) }
            .thenApply(::mapWriteMutation)

    suspend fun renewWriteSuspending(
        handle: WriteLockHandle,
        extension: Duration,
    ): LockMutationResult<WriteLockHandle> =
        mapWriteMutation(recordRenew(handle.lock, renewSuspending(handle.lock, LockKind.WRITE, extension)))

    fun releaseWrite(handle: WriteLockHandle): LockMutationResult<WriteLockHandle> =
        mapWriteMutation(recordRelease(handle.lock, release(handle.lock, LockKind.WRITE)))

    fun releaseWriteAsync(handle: WriteLockHandle): CompletableFuture<LockMutationResult<WriteLockHandle>> =
        releaseAsync(handle.lock, LockKind.WRITE)
            .thenApply { recordRelease(handle.lock, it) }
            .thenApply(::mapWriteMutation)

    suspend fun releaseWriteSuspending(handle: WriteLockHandle): LockMutationResult<WriteLockHandle> =
        mapWriteMutation(recordRelease(handle.lock, releaseSuspending(handle.lock, LockKind.WRITE)))

    fun downgrade(handle: WriteLockHandle): DowngradeResult {
        validateHandle(handle.lock, LockKind.WRITE)
        if (closed.get()) return DowngradeResult.Closed
        return rwClassified(
            backend = { DowngradeResult.BackendFailure(it) },
            integrity = { DowngradeResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordDowngrade(
                handle,
                decodeDowngrade(executor.run(ReadWriteLockOperation.DOWNGRADE, keys, downgradeArgs(handle)), keys, handle),
            )
        }
    }

    fun downgradeAsync(handle: WriteLockHandle): CompletableFuture<DowngradeResult> {
        validateHandle(handle.lock, LockKind.WRITE)
        if (closed.get()) return CompletableFuture.completedFuture(DowngradeResult.Closed)
        return executor.runAsync(ReadWriteLockOperation.DOWNGRADE, keys, downgradeArgs(handle))
            .rwMap(
                decode = { decodeDowngrade(it, keys, handle) },
                backend = { DowngradeResult.BackendFailure(it) },
                integrity = { DowngradeResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RETRY_SAME_HANDLE,
            )
            .thenApply { recordDowngrade(handle, it) }
    }

    suspend fun downgradeSuspending(handle: WriteLockHandle): DowngradeResult {
        validateHandle(handle.lock, LockKind.WRITE)
        if (closed.get()) return DowngradeResult.Closed
        return rwClassifiedSuspending(
            backend = { DowngradeResult.BackendFailure(it) },
            integrity = { DowngradeResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordDowngrade(
                handle,
                decodeDowngrade(
                    executor.runSuspending(ReadWriteLockOperation.DOWNGRADE, keys, downgradeArgs(handle)),
                    keys,
                    handle,
                ),
            )
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            pendingAsync.forEach { it.complete(LockAcquireResult.Closed) }
            pendingSignals.forEach { it.complete(Unit) }
            watchdogs.values.forEach(CoordinationRuntime.CoordinationTaskRegistration::close)
            watchdogs.clear()
            registration.close()
        }
    }

    private fun <S, R> mapHandleResultAsync(
        source: CompletableFuture<S>,
        acquiredHandle: (S) -> LockHandle?,
        transform: (S) -> R,
    ): CompletableFuture<R> =
        mapHandleResultAsync(source, acquiredHandle, transform, ::releaseAbandoned)

    private fun waitObservation(kind: LockKind): LockWaitObservation =
        if (kind == LockKind.WRITE) writeWaitObservation else readWaitObservation

    private fun tryAcquire(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(kind, ownerId, requestId, leasePolicy, Duration.ZERO)
        if (closed.get()) return LockAcquireResult.Closed
        return rwClassified(
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAttempt(executor.run(ReadWriteLockOperation.ACQUIRE, keys, args), keys, kind, ownerId, requestId)
                .toPublic(AtomicReference())
        }
    }

    private fun tryAcquireAsync(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        val args = acquireArgs(kind, ownerId, requestId, leasePolicy, Duration.ZERO)
        if (closed.get()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        return executor.runAsync(ReadWriteLockOperation.ACQUIRE, keys, args)
            .rwMap(
                decode = { decodeAttempt(it, keys, kind, ownerId, requestId).toPublic(AtomicReference()) },
                backend = { acquireBackendResult(ownerId, requestId, it) },
                integrity = { LockAcquireResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RECONCILE_REQUEST,
                onLateSuccess = { raw ->
                    val late = decodeAttempt(raw, keys, kind, ownerId, requestId).toPublic(AtomicReference())
                    late.acquiredHandleOrNull()?.let(::releaseAbandoned)
                },
            )
    }

    private suspend fun tryAcquireSuspending(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(kind, ownerId, requestId, leasePolicy, Duration.ZERO)
        if (closed.get()) return LockAcquireResult.Closed
        return rwClassifiedSuspending(
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAttempt(
                executor.runSuspending(ReadWriteLockOperation.ACQUIRE, keys, args),
                keys,
                kind,
                ownerId,
                requestId,
            ).toPublic(AtomicReference())
        }
    }

    private fun acquire(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        validateWait(waitTime)
        val observation = waitObservation(kind).begin()
        val identity = AtomicReference<ReadWriteWaiterIdentity?>()
        val deadline = CoordinationDeadline.after(waitTime.toKotlinDuration())
        var outcome = LockOutcome.CANCELLED
        try {
            while (true) {
                val result = acquireAttempt(kind, ownerId, requestId, leasePolicy, waitTime, identity)
                if (result !is LockAcquireResult.Contended) {
                    outcome = result.observationOutcome()
                    return result
                }
                observation.onContended()
                val remaining = deadline.remainingNanos()
                if (remaining == 0L) {
                    val timedOut = cleanupTimedOut(kind, ownerId, requestId, identity.get())
                    outcome = timedOut.observationOutcome()
                    return timedOut
                }
                LockSupport.parkNanos(minOf(remaining, RW_RETRY_DELAY.toNanos()))
            }
        } finally {
            observation.complete(outcome)
        }
    }

    private fun acquireAsync(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        validateWait(waitTime)
        if (closed.get()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        val observation = waitObservation(kind).begin()
        val identity = AtomicReference<ReadWriteWaiterIdentity?>()
        val deadline = CoordinationDeadline.after(waitTime.toKotlinDuration())
        val result = CompletableFuture<LockAcquireResult<LockHandle>>()
        val scheduled = AtomicReference<CoordinationRuntime.CoordinationTaskRegistration?>()
        val inFlight = AtomicReference<CompletableFuture<LockAcquireResult<LockHandle>>?>()
        pendingAsync += result

        lateinit var retry: () -> Unit
        fun schedule() {
            if (result.isDone) return
            val remaining = deadline.remainingNanos()
            if (remaining == 0L) {
                completeTimedOutAsync(result, kind, ownerId, requestId, identity.get())
                return
            }
            try {
                scheduled.getAndSet(
                    registration.registerTask(
                        Duration.ofNanos(minOf(remaining, RW_RETRY_DELAY.toNanos())).toKotlinDuration(),
                        retry,
                    ),
                )?.close()
            } catch (_: Exception) {
                result.complete(if (closed.get()) LockAcquireResult.Closed else LockAcquireResult.CapacityExceeded)
            }
        }
        retry = {
            scheduled.getAndSet(null)?.close()
            if (closed.get()) {
                result.complete(LockAcquireResult.Closed)
            } else if (!result.isDone) {
                val pending = acquireAttemptAsync(kind, ownerId, requestId, leasePolicy, waitTime, identity)
                inFlight.set(pending)
                pending.whenComplete { value, error ->
                    inFlight.compareAndSet(pending, null)
                    if (result.isDone) {
                        value.acquiredHandleOrNull()?.let(::releaseAbandoned)
                        return@whenComplete
                    }
                    if (error != null) result.completeExceptionally(error)
                    else if (value is LockAcquireResult.Contended) {
                        observation.onContended()
                        schedule()
                    }
                    else if (!result.complete(value)) value.acquiredHandleOrNull()?.let(::releaseAbandoned)
                }
            }
        }
        result.whenComplete { value, error ->
            pendingAsync -= result
            scheduled.getAndSet(null)?.close()
            val pending = inFlight.getAndSet(null)
            if (result.isCancelled || value == LockAcquireResult.Closed) {
                val knownIdentity = identity.get()
                if (knownIdentity != null) {
                    removeWaiterAsync(kind, ownerId, requestId, knownIdentity)
                } else {
                    pending?.whenComplete { _, _ ->
                        identity.get()?.let { removeWaiterAsync(kind, ownerId, requestId, it) }
                    }
                }
            } else {
                pending?.cancel(false)
            }
            observation.complete(
                when {
                    result.isCancelled -> LockOutcome.CANCELLED
                    error != null -> LockOutcome.BACKEND_FAILED
                    else -> value.observationOutcome()
                },
            )
        }
        retry()
        return result
    }

    private suspend fun acquireSuspending(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        validateWait(waitTime)
        val observation = waitObservation(kind).begin()
        val identity = AtomicReference<ReadWriteWaiterIdentity?>()
        val deadline = CoordinationDeadline.after(waitTime.toKotlinDuration())
        var outcome = LockOutcome.CANCELLED
        return try {
            while (true) {
                val result = acquireAttemptSuspending(kind, ownerId, requestId, leasePolicy, waitTime, identity)
                if (result !is LockAcquireResult.Contended) {
                    outcome = result.observationOutcome()
                    return result
                }
                observation.onContended()
                val remaining = deadline.remainingNanos()
                if (remaining == 0L) {
                    val timedOut = cleanupTimedOutSuspending(kind, ownerId, requestId, identity.get())
                    outcome = timedOut.observationOutcome()
                    return timedOut
                }
                val delay = Duration.ofNanos(minOf(remaining, RW_RETRY_DELAY.toNanos()))
                awaitRetry(delay)
            }
            @Suppress("UNREACHABLE_CODE")
            LockAcquireResult.TimedOut
        } catch (cancelled: CancellationException) {
            identity.get()?.let {
                withContext(NonCancellable) {
                    removeWaiterSuspending(kind, ownerId, requestId, it)
                }
            }
            throw cancelled
        } finally {
            observation.complete(outcome)
        }
    }

    private fun acquireAttempt(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
        identity: AtomicReference<ReadWriteWaiterIdentity?>,
    ): LockAcquireResult<LockHandle> {
        if (closed.get()) return LockAcquireResult.Closed
        return rwClassified(
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAttempt(
                executor.run(
                    ReadWriteLockOperation.ACQUIRE,
                    keys,
                    acquireArgs(kind, ownerId, requestId, leasePolicy, waitTime),
                ),
                keys,
                kind,
                ownerId,
                requestId,
            ).toPublic(identity)
        }
    }

    private fun acquireAttemptAsync(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
        identity: AtomicReference<ReadWriteWaiterIdentity?>,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        if (closed.get()) {
            CompletableFuture.completedFuture(LockAcquireResult.Closed)
        } else {
            executor.runAsync(
                ReadWriteLockOperation.ACQUIRE,
                keys,
                acquireArgs(kind, ownerId, requestId, leasePolicy, waitTime),
            ).rwMap(
                decode = { decodeAttempt(it, keys, kind, ownerId, requestId).toPublic(identity) },
                backend = { acquireBackendResult(ownerId, requestId, it) },
                integrity = { LockAcquireResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RECONCILE_REQUEST,
                onLateSuccess = { raw ->
                    val late = decodeAttempt(raw, keys, kind, ownerId, requestId).toPublic(identity)
                    late.acquiredHandleOrNull()?.let(::releaseAbandoned)
                    identity.get()?.let { removeWaiterAsync(kind, ownerId, requestId, it) }
                },
            )
        }

    private suspend fun acquireAttemptSuspending(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
        identity: AtomicReference<ReadWriteWaiterIdentity?>,
    ): LockAcquireResult<LockHandle> {
        if (closed.get()) return LockAcquireResult.Closed
        return rwClassifiedSuspending(
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAttempt(
                executor.runSuspending(
                    ReadWriteLockOperation.ACQUIRE,
                    keys,
                    acquireArgs(kind, ownerId, requestId, leasePolicy, waitTime),
                ),
                keys,
                kind,
                ownerId,
                requestId,
            ).toPublic(identity)
        }
    }

    private fun inspect(handle: LockHandle, kind: LockKind): LockInspectResult<LockHandle> {
        validateHandle(handle, kind)
        if (closed.get()) return LockInspectResult.Closed
        return rwClassified(
            backend = { LockInspectResult.BackendFailure(it) },
            integrity = { LockInspectResult.IntegrityFailure(it) },
            action = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeInspect(executor.run(ReadWriteLockOperation.INSPECT, keys, handleArgs(handle, kind)), keys, handle)
        }
    }

    private fun inspectAsync(
        handle: LockHandle,
        kind: LockKind,
    ): CompletableFuture<LockInspectResult<LockHandle>> {
        validateHandle(handle, kind)
        if (closed.get()) return CompletableFuture.completedFuture(LockInspectResult.Closed)
        return executor.runAsync(ReadWriteLockOperation.INSPECT, keys, handleArgs(handle, kind))
            .rwMap(
                decode = { decodeInspect(it, keys, handle) },
                backend = { LockInspectResult.BackendFailure(it) },
                integrity = { LockInspectResult.IntegrityFailure(it) },
                action = LockRecoveryAction.INSPECT_HANDLE,
            )
    }

    private suspend fun inspectSuspending(handle: LockHandle, kind: LockKind): LockInspectResult<LockHandle> {
        validateHandle(handle, kind)
        if (closed.get()) return LockInspectResult.Closed
        return rwClassifiedSuspending(
            backend = { LockInspectResult.BackendFailure(it) },
            integrity = { LockInspectResult.IntegrityFailure(it) },
            action = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeInspect(
                executor.runSuspending(ReadWriteLockOperation.INSPECT, keys, handleArgs(handle, kind)),
                keys,
                handle,
            )
        }
    }

    private fun reconcile(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        if (closed.get()) return LockReconcileResult.Closed
        return rwClassified(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeReconcile(
                executor.run(ReadWriteLockOperation.RECONCILE, keys, reconcileArgs(kind, ownerId, requestId)),
                keys,
                kind,
                ownerId,
                requestId,
            )
        }
    }

    private fun reconcileAsync(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> {
        if (closed.get()) return CompletableFuture.completedFuture(LockReconcileResult.Closed)
        return executor.runAsync(ReadWriteLockOperation.RECONCILE, keys, reconcileArgs(kind, ownerId, requestId))
            .rwMap(
                decode = { decodeReconcile(it, keys, kind, ownerId, requestId) },
                backend = { LockReconcileResult.BackendFailure(it) },
                integrity = { LockReconcileResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RECONCILE_REQUEST,
            )
    }

    private suspend fun reconcileSuspending(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        if (closed.get()) return LockReconcileResult.Closed
        return rwClassifiedSuspending(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeReconcile(
                executor.runSuspending(
                    ReadWriteLockOperation.RECONCILE,
                    keys,
                    reconcileArgs(kind, ownerId, requestId),
                ),
                keys,
                kind,
                ownerId,
                requestId,
            )
        }
    }

    private fun renew(handle: LockHandle, kind: LockKind, extension: Duration): LockMutationResult<LockHandle> {
        validateHandle(handle, kind)
        val args = handleArgs(handle, kind) + extension.toRedisMillisCeil().toString()
        if (closed.get()) return LockMutationResult.Closed
        return rwClassified(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            decodeRenew(executor.run(ReadWriteLockOperation.RENEW, keys, args), keys, handle)
        }
    }

    private fun renewAsync(
        handle: LockHandle,
        kind: LockKind,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> {
        validateHandle(handle, kind)
        val args = handleArgs(handle, kind) + extension.toRedisMillisCeil().toString()
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(ReadWriteLockOperation.RENEW, keys, args)
            .rwMap(
                decode = { decodeRenew(it, keys, handle) },
                backend = { LockMutationResult.BackendFailure(it) },
                integrity = { LockMutationResult.IntegrityFailure(it) },
                action = LockRecoveryAction.RETRY_SAME_HANDLE,
            )
    }

    private suspend fun renewSuspending(
        handle: LockHandle,
        kind: LockKind,
        extension: Duration,
    ): LockMutationResult<LockHandle> {
        validateHandle(handle, kind)
        val args = handleArgs(handle, kind) + extension.toRedisMillisCeil().toString()
        if (closed.get()) return LockMutationResult.Closed
        return rwClassifiedSuspending(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            decodeRenew(executor.runSuspending(ReadWriteLockOperation.RENEW, keys, args), keys, handle)
        }
    }

    private fun release(handle: LockHandle, kind: LockKind): LockMutationResult<LockHandle> {
        validateHandle(handle, kind)
        if (closed.get()) return LockMutationResult.Closed
        return rwClassified(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            rwDecodeRelease(
                executor.run(
                    ReadWriteLockOperation.RELEASE,
                    keys,
                    handleArgs(handle, kind) + RW_TERMINAL_TTL_MILLIS.toString(),
                ),
            )
        }
    }

    private fun releaseAsync(
        handle: LockHandle,
        kind: LockKind,
    ): CompletableFuture<LockMutationResult<LockHandle>> {
        validateHandle(handle, kind)
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(
            ReadWriteLockOperation.RELEASE,
            keys,
            handleArgs(handle, kind) + RW_TERMINAL_TTL_MILLIS.toString(),
        ).rwMap(
            decode = ::rwDecodeRelease,
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        )
    }

    private fun releaseAbandoned(handle: LockHandle) {
        validateHandle(handle, handle.kind)
        removeWatchdog(handle)
        executor.runAsync(
            ReadWriteLockOperation.RELEASE,
            keys,
            handleArgs(handle, handle.kind) + RW_TERMINAL_TTL_MILLIS.toString(),
        ).exceptionally { null }
    }

    private suspend fun releaseSuspending(
        handle: LockHandle,
        kind: LockKind,
    ): LockMutationResult<LockHandle> {
        validateHandle(handle, kind)
        if (closed.get()) return LockMutationResult.Closed
        return rwClassifiedSuspending(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            rwDecodeRelease(
                executor.runSuspending(
                    ReadWriteLockOperation.RELEASE,
                    keys,
                    handleArgs(handle, kind) + RW_TERMINAL_TTL_MILLIS.toString(),
                ),
            )
        }
    }

    private fun cleanupTimedOut(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity?,
    ): LockAcquireResult<LockHandle> {
        if (identity == null) return LockAcquireResult.TimedOut
        return cleanupResult(removeWaiter(kind, ownerId, requestId, identity), ownerId, requestId)
    }

    private suspend fun awaitRetry(delay: Duration) {
        val signal = CompletableFuture<Unit>()
        pendingSignals += signal
        val task = registration.registerTask(delay.toKotlinDuration()) {
            signal.complete(Unit)
        }
        try {
            signal.await()
        } finally {
            pendingSignals -= signal
            task.close()
        }
    }

    private suspend fun cleanupTimedOutSuspending(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity?,
    ): LockAcquireResult<LockHandle> {
        if (identity == null) return LockAcquireResult.TimedOut
        return cleanupResult(removeWaiterSuspending(kind, ownerId, requestId, identity), ownerId, requestId)
    }

    private fun completeTimedOutAsync(
        target: CompletableFuture<LockAcquireResult<LockHandle>>,
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity?,
    ) {
        if (identity == null) {
            target.complete(LockAcquireResult.TimedOut)
            return
        }
        removeWaiterAsync(kind, ownerId, requestId, identity).whenComplete { removed, error ->
            if (error != null) target.completeExceptionally(error)
            else target.complete(cleanupResult(removed, ownerId, requestId))
        }
    }

    private fun removeWaiter(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity,
    ): LockReconcileResult<LockHandle> =
        rwClassified(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeRemove(
                executor.run(
                    ReadWriteLockOperation.REMOVE,
                    keys,
                    removeArgs(kind, ownerId, requestId, identity),
                ),
            )
        }

    private fun removeWaiterAsync(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity,
    ): CompletableFuture<LockReconcileResult<LockHandle>> =
        executor.runAsync(
            ReadWriteLockOperation.REMOVE,
            keys,
            removeArgs(kind, ownerId, requestId, identity),
        ).rwMap(
            decode = ::decodeRemove,
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        )

    private suspend fun removeWaiterSuspending(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity,
    ): LockReconcileResult<LockHandle> =
        rwClassifiedSuspending(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            action = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeRemove(
                executor.runSuspending(
                    ReadWriteLockOperation.REMOVE,
                    keys,
                    removeArgs(kind, ownerId, requestId, identity),
                ),
            )
        }

    private fun ReadWriteAttempt.toPublic(
        identity: AtomicReference<ReadWriteWaiterIdentity?>,
    ): LockAcquireResult<LockHandle> =
        when (this) {
            is ReadWriteAttempt.Result -> value
            is ReadWriteAttempt.Queued -> {
                identity.set(this.identity)
                LockAcquireResult.Contended(lockTtlMillis)
            }
        }

    private fun acquireArgs(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
        waitTime: Duration,
    ): List<String> {
        val encoded = encodeLeasePolicy(leasePolicy)
        val waitMillis = if (waitTime.isZero) 0L else waitTime.toRedisMillisCeil()
        return listOf(
            kind.rwMode,
            ownerId.value,
            requestId.value,
            readWriteMember(kind, ownerId, requestId),
            encoded.wireValue,
            encoded.ttlMillis.toString(),
            config.lock.maxReentrantHolds.toString(),
            waitMillis.toString(),
            config.cleanupBatchSize.toString(),
            config.maxQueueSize.toString(),
        )
    }

    private fun reconcileArgs(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): List<String> =
        listOf(kind.rwMode, ownerId.value, requestId.value, readWriteMember(kind, ownerId, requestId))

    private fun removeArgs(
        kind: LockKind,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        identity: ReadWriteWaiterIdentity,
    ): List<String> =
        reconcileArgs(kind, ownerId, requestId) +
            listOf(identity.sequence.toString(), identity.generation.toString())

    private fun handleArgs(handle: LockHandle, kind: LockKind): List<String> =
        listOf(
            kind.rwMode,
            handle.ownerId.value,
            handle.requestId.value,
            readWriteMember(kind, handle.ownerId, handle.requestId),
            handle.generation.value.toString(),
        )

    private fun downgradeArgs(handle: WriteLockHandle): List<String> =
        listOf(
            handle.lock.ownerId.value,
            handle.lock.requestId.value,
            readWriteMember(LockKind.WRITE, handle.lock.ownerId, handle.lock.requestId),
            readWriteMember(LockKind.READ, handle.lock.ownerId, handle.lock.requestId),
            handle.lock.generation.value.toString(),
            RW_TERMINAL_TTL_MILLIS.toString(),
        )

    private fun validateHandle(handle: LockHandle, kind: LockKind) {
        require(handle.kind == kind) { "Read/write handle kind does not match the selected view." }
        require(handle.objectFingerprint == keys.fingerprint) { "Lock handle belongs to a different lock object." }
    }

    private fun validateWait(waitTime: Duration) {
        waitTime.toRedisMillisCeil()
        require(waitTime <= RW_MAX_WAIT) { "Lock wait time must not exceed $RW_MAX_WAIT." }
    }

    private fun registerWatchdog(result: LockAcquireResult<LockHandle>): LockAcquireResult<LockHandle> {
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

    private fun registerWatchdog(result: LockReconcileResult<LockHandle>): LockReconcileResult<LockHandle> {
        val handle = (result as? LockReconcileResult.Owned)?.handle ?: return result
        return if (ensureWatchdog(handle)) {
            result
        } else if (closed.get() || registration.isClosed) {
            LockReconcileResult.Closed
        } else {
            LockReconcileResult.Ambiguous(LockRecoveryAction.RECONCILE_REQUEST)
        }
    }

    private fun ensureWatchdog(handle: LockHandle): Boolean {
        val policy = handle.leasePolicy as? LeasePolicy.Watchdog ?: return true
        watchdogs.entries.removeIf { it.value.isClosed }
        var registered = true
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
                            watchdogs.remove(handle)?.close()
                            recordOwnershipLoss(handle.kind, policy)
                        },
                    ) {
                        renewForWatchdog(handle, policy)
                    }
                } catch (_: CoordinationCapacityException) {
                    registered = false
                    recordCapacityRejection(handle.kind, policy)
                    null
                } catch (_: IllegalStateException) {
                    registered = false
                    null
                }
            }
        }
        return registered
    }

    private fun renewForWatchdog(
        handle: LockHandle,
        policy: LeasePolicy.Watchdog,
    ): CompletableFuture<CoordinationRenewalOutcome> {
        if (closed.get()) {
            return CompletableFuture.completedFuture(CoordinationRenewalOutcome.OWNERSHIP_LOST)
        }
        return renewAsync(handle, handle.kind, policy.ttl).thenApply { result ->
            if (result is LockMutationResult.Renewed) {
                CoordinationRenewalOutcome.RENEWED
            } else {
                CoordinationRenewalOutcome.OWNERSHIP_LOST
            }
        }
    }

    private fun recordRenew(
        handle: LockHandle,
        result: LockMutationResult<LockHandle>,
    ): LockMutationResult<LockHandle> {
        when (result) {
            LockMutationResult.AlreadyReleased,
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.kind, handle.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun recordRelease(
        handle: LockHandle,
        result: LockMutationResult<LockHandle>,
    ): LockMutationResult<LockHandle> {
        when (result) {
            is LockMutationResult.Released,
            LockMutationResult.AlreadyReleased,
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.kind, handle.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun recordDowngrade(
        handle: WriteLockHandle,
        result: DowngradeResult,
    ): DowngradeResult {
        when (result) {
            is DowngradeResult.Downgraded -> {
                removeWatchdog(handle.lock)
                if (!ensureWatchdog(result.handle.lock)) {
                    return if (closed.get() || registration.isClosed) {
                        DowngradeResult.Closed
                    } else {
                        DowngradeResult.Ambiguous(LockRecoveryAction.INSPECT_HANDLE)
                    }
                }
            }
            DowngradeResult.Expired,
            DowngradeResult.OwnershipLost,
            DowngradeResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.lock.kind, handle.lock.leasePolicy)
                removeWatchdog(handle.lock)
            }
            else -> Unit
        }
        return result
    }

    private fun removeWatchdog(handle: LockHandle) {
        watchdogs.remove(handle)?.close()
    }

    private fun recordCapacityRejection(kind: LockKind, policy: LeasePolicy) {
        recordObservation(
            counter = LockCounterName.CAPACITY_REJECTION_TOTAL,
            kind = kind,
            operation = LockOperation.ACQUIRE,
            outcome = LockOutcome.CAPACITY_REJECTED,
            policy = policy,
        )
    }

    private fun recordOwnershipLoss(kind: LockKind, policy: LeasePolicy) {
        recordObservation(
            counter = LockCounterName.OWNERSHIP_LOSS_TOTAL,
            kind = kind,
            operation = LockOperation.RENEW,
            outcome = LockOutcome.OWNERSHIP_LOST,
            policy = policy,
        )
    }

    private fun recordObservation(
        counter: LockCounterName,
        kind: LockKind,
        operation: LockOperation,
        outcome: LockOutcome,
        policy: LeasePolicy,
    ) {
        val leasePolicy = when (policy) {
            is LeasePolicy.Fixed -> LockLeasePolicyKind.FIXED
            is LeasePolicy.Watchdog -> LockLeasePolicyKind.WATCHDOG
        }
        val dimensions = LockDimensions(kind, operation, outcome, null, leasePolicy)
        observationSink.recordSafely(LockObservation.Counter(counter, 1L, dimensions))
        observationSink.recordSafely(
            LockObservation.Event(LockEvent(kind, operation, outcome, null, leasePolicy)),
        )
    }

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): ReadWriteLockClient {
            val keys = deriveReadWriteLockKeys(name, config, connection.codec)
            val readObservationRecorder = LockObservationRecorder(LockKind.READ, observationSink)
            val writeObservationRecorder = LockObservationRecorder(LockKind.WRITE, observationSink)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return ReadWriteLockClient(
                keys,
                config,
                DefaultReadWriteLockCommandExecutor(
                    connection.sync(),
                    connection.async(),
                    readObservationRecorder,
                    writeObservationRecorder,
                ),
                runtime.registerObject(
                    keys.fingerprint,
                    coordinationObserver(readObservationRecorder, writeObservationRecorder),
                ),
                observationSink,
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): ReadWriteLockClient {
            val keys = deriveReadWriteLockKeys(name, config, connection.codec)
            val readObservationRecorder = LockObservationRecorder(LockKind.READ, observationSink)
            val writeObservationRecorder = LockObservationRecorder(LockKind.WRITE, observationSink)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return ReadWriteLockClient(
                keys,
                config,
                DefaultReadWriteLockCommandExecutor(
                    connection.sync(),
                    connection.async(),
                    readObservationRecorder,
                    writeObservationRecorder,
                ),
                runtime.registerObject(
                    keys.fingerprint,
                    coordinationObserver(readObservationRecorder, writeObservationRecorder),
                ),
                observationSink,
            )
        }
    }
}

private fun decodeAttempt(
    raw: Any?,
    keys: ReadWriteLockKeys,
    kind: LockKind,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): ReadWriteAttempt {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "ACQUIRED" to 5,
            "REENTERED" to 5,
            "CONTENDED" to 2,
            "QUEUED" to 5,
            "CLEANUP_PENDING" to 1,
            "CAPACITY" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "ACQUIRED", "REENTERED" -> {
            val generation = LockGeneration(frame.rwPositiveLong(0))
            val holdCount = frame.rwPositiveInt(1)
            val policy = decodeLeasePolicy(frame.field(3))
            val handle = LockHandle(keys.fingerprint, ownerId, generation, requestId, policy, kind)
            val result =
                if (frame.tag == "REENTERED") LockAcquireResult.Reentered(handle, holdCount)
                else LockAcquireResult.Acquired(handle)
            ReadWriteAttempt.Result(result)
        }
        "CONTENDED" ->
            ReadWriteAttempt.Result(LockAcquireResult.Contended(frame.rwNonNegativeLong(0)))
        "QUEUED" ->
            ReadWriteAttempt.Queued(
                ReadWriteWaiterIdentity(frame.rwPositiveLong(0), frame.rwNonNegativeLong(1)),
                frame.rwNonNegativeLong(3),
            )
        "CLEANUP_PENDING" -> ReadWriteAttempt.Result(LockAcquireResult.CleanupPending)
        "CAPACITY" -> ReadWriteAttempt.Result(LockAcquireResult.CapacityExceeded)
        "INTEGRITY" -> ReadWriteAttempt.Result(LockAcquireResult.IntegrityFailure(RW_INVALID_STATE))
        else -> rwMalformedReply()
    }
}

private fun decodeInspect(
    raw: Any?,
    keys: ReadWriteLockKeys,
    handle: LockHandle,
): LockInspectResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "OWNED" to 5,
            "RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "OWNED" -> {
            val generation = LockGeneration(frame.rwPositiveLong(0))
            val policy = decodeLeasePolicy(frame.field(3))
            val current = handle.copy(
                objectFingerprint = keys.fingerprint,
                generation = generation,
                leasePolicy = policy,
            )
            LockInspectResult.Owned(current, frame.rwPositiveInt(1), frame.rwNonNegativeLong(2))
        }
        "RELEASED" -> LockInspectResult.Released
        "EXPIRED" -> LockInspectResult.Expired
        "STALE" -> LockInspectResult.StaleGeneration
        "LOST" -> LockInspectResult.OwnershipLost
        "INTEGRITY" -> LockInspectResult.IntegrityFailure(RW_INVALID_STATE)
        else -> rwMalformedReply()
    }
}

private fun decodeReconcile(
    raw: Any?,
    keys: ReadWriteLockKeys,
    kind: LockKind,
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
            val handle = LockHandle(
                keys.fingerprint,
                ownerId,
                LockGeneration(frame.rwPositiveLong(0)),
                requestId,
                decodeLeasePolicy(frame.field(3)),
                kind,
            )
            LockReconcileResult.Owned(handle, frame.rwPositiveInt(1), frame.rwNonNegativeLong(2))
        }
        "QUEUED" ->
            LockReconcileResult.Queued(
                io.bluetape4k.redis.lettuce.lock.FairWaiterState(
                    io.bluetape4k.redis.lettuce.lock.FairWaiterStatus.QUEUED,
                    frame.rwPositiveLong(0),
                    frame.rwNonNegativeLong(2),
                ),
            )
        "REMOVED" -> LockReconcileResult.Removed
        "RELEASED" -> LockReconcileResult.Released
        "NOT_FOUND" -> LockReconcileResult.NotFound
        "STALE" -> LockReconcileResult.StaleGeneration
        "INTEGRITY" -> LockReconcileResult.IntegrityFailure(RW_INVALID_STATE)
        else -> rwMalformedReply()
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
        "INTEGRITY" -> LockReconcileResult.IntegrityFailure(RW_INVALID_STATE)
        else -> rwMalformedReply()
    }
}

private fun decodeRenew(
    raw: Any?,
    keys: ReadWriteLockKeys,
    handle: LockHandle,
): LockMutationResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "RENEWED" to 4,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "RENEWED" -> {
            val renewed = handle.copy(
                objectFingerprint = keys.fingerprint,
                generation = LockGeneration(frame.rwPositiveLong(0)),
                leasePolicy = decodeLeasePolicy(frame.field(2)),
            )
            LockMutationResult.Renewed(renewed, frame.rwNonNegativeLong(1))
        }
        "ALREADY_RELEASED" -> LockMutationResult.AlreadyReleased
        "EXPIRED" -> LockMutationResult.Expired
        "STALE" -> LockMutationResult.StaleGeneration
        "LOST" -> LockMutationResult.OwnershipLost
        "INTEGRITY" -> LockMutationResult.IntegrityFailure(RW_INVALID_STATE)
        else -> rwMalformedReply()
    }
}

private fun rwDecodeRelease(raw: Any?): LockMutationResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "RELEASED" to 2,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "RELEASED" -> LockMutationResult.Released(frame.rwNonNegativeInt(0))
        "ALREADY_RELEASED" -> LockMutationResult.AlreadyReleased
        "EXPIRED" -> LockMutationResult.Expired
        "STALE" -> LockMutationResult.StaleGeneration
        "LOST" -> LockMutationResult.OwnershipLost
        "INTEGRITY" -> LockMutationResult.IntegrityFailure(RW_INVALID_STATE)
        else -> rwMalformedReply()
    }
}

private fun decodeDowngrade(
    raw: Any?,
    keys: ReadWriteLockKeys,
    handle: WriteLockHandle,
): DowngradeResult {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "DOWNGRADED" to 3,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "CAPACITY" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "DOWNGRADED" -> {
            val read = handle.lock.copy(
                objectFingerprint = keys.fingerprint,
                generation = LockGeneration(frame.rwPositiveLong(0)),
                leasePolicy = decodeLeasePolicy(frame.field(1)),
                kind = LockKind.READ,
            )
            DowngradeResult.Downgraded(ReadLockHandle(read))
        }
        "EXPIRED", "ALREADY_RELEASED" -> DowngradeResult.Expired
        "STALE" -> DowngradeResult.StaleGeneration
        "LOST" -> DowngradeResult.OwnershipLost
        "CAPACITY", "INTEGRITY" -> DowngradeResult.IntegrityFailure(RW_INVALID_STATE)
        else -> rwMalformedReply()
    }
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
        is LockReconcileResult.IntegrityFailure -> LockAcquireResult.IntegrityFailure(result.failure)
        is LockReconcileResult.BackendFailure ->
            LockAcquireResult.Ambiguous(ownerId, requestId, LockRecoveryAction.RECONCILE_REQUEST)
        else -> LockAcquireResult.CleanupPending
    }

private fun mapReadAcquire(result: LockAcquireResult<LockHandle>): LockAcquireResult<ReadLockHandle> =
    when (result) {
        is LockAcquireResult.Acquired -> LockAcquireResult.Acquired(ReadLockHandle(result.handle))
        is LockAcquireResult.Reentered -> LockAcquireResult.Reentered(ReadLockHandle(result.handle), result.holdCount)
        is LockAcquireResult.Contended -> result
        LockAcquireResult.TimedOut -> LockAcquireResult.TimedOut
        LockAcquireResult.CleanupPending -> LockAcquireResult.CleanupPending
        LockAcquireResult.CapacityExceeded -> LockAcquireResult.CapacityExceeded
        LockAcquireResult.Closed -> LockAcquireResult.Closed
        is LockAcquireResult.BackendFailure -> result
        is LockAcquireResult.IntegrityFailure -> result
        is LockAcquireResult.Ambiguous -> result
    }

private fun mapWriteAcquire(result: LockAcquireResult<LockHandle>): LockAcquireResult<WriteLockHandle> =
    when (result) {
        is LockAcquireResult.Acquired -> LockAcquireResult.Acquired(WriteLockHandle(result.handle))
        is LockAcquireResult.Reentered -> LockAcquireResult.Reentered(WriteLockHandle(result.handle), result.holdCount)
        is LockAcquireResult.Contended -> result
        LockAcquireResult.TimedOut -> LockAcquireResult.TimedOut
        LockAcquireResult.CleanupPending -> LockAcquireResult.CleanupPending
        LockAcquireResult.CapacityExceeded -> LockAcquireResult.CapacityExceeded
        LockAcquireResult.Closed -> LockAcquireResult.Closed
        is LockAcquireResult.BackendFailure -> result
        is LockAcquireResult.IntegrityFailure -> result
        is LockAcquireResult.Ambiguous -> result
    }

private fun mapReadInspect(result: LockInspectResult<LockHandle>): LockInspectResult<ReadLockHandle> =
    when (result) {
        is LockInspectResult.Owned -> LockInspectResult.Owned(
            ReadLockHandle(result.handle),
            result.holdCount,
            result.remainingTtlMillis,
        )
        LockInspectResult.Released -> LockInspectResult.Released
        LockInspectResult.Expired -> LockInspectResult.Expired
        LockInspectResult.StaleGeneration -> LockInspectResult.StaleGeneration
        LockInspectResult.OwnershipLost -> LockInspectResult.OwnershipLost
        LockInspectResult.Closed -> LockInspectResult.Closed
        is LockInspectResult.BackendFailure -> result
        is LockInspectResult.IntegrityFailure -> result
    }

private fun mapWriteInspect(result: LockInspectResult<LockHandle>): LockInspectResult<WriteLockHandle> =
    when (result) {
        is LockInspectResult.Owned -> LockInspectResult.Owned(
            WriteLockHandle(result.handle),
            result.holdCount,
            result.remainingTtlMillis,
        )
        LockInspectResult.Released -> LockInspectResult.Released
        LockInspectResult.Expired -> LockInspectResult.Expired
        LockInspectResult.StaleGeneration -> LockInspectResult.StaleGeneration
        LockInspectResult.OwnershipLost -> LockInspectResult.OwnershipLost
        LockInspectResult.Closed -> LockInspectResult.Closed
        is LockInspectResult.BackendFailure -> result
        is LockInspectResult.IntegrityFailure -> result
    }

private fun mapReadReconcile(result: LockReconcileResult<LockHandle>): LockReconcileResult<ReadLockHandle> =
    when (result) {
        is LockReconcileResult.Owned -> LockReconcileResult.Owned(
            ReadLockHandle(result.handle),
            result.holdCount,
            result.remainingTtlMillis,
        )
        is LockReconcileResult.Queued -> result
        LockReconcileResult.Removed -> LockReconcileResult.Removed
        LockReconcileResult.Released -> LockReconcileResult.Released
        LockReconcileResult.NotFound -> LockReconcileResult.NotFound
        LockReconcileResult.StaleGeneration -> LockReconcileResult.StaleGeneration
        LockReconcileResult.Closed -> LockReconcileResult.Closed
        is LockReconcileResult.BackendFailure -> result
        is LockReconcileResult.IntegrityFailure -> result
        is LockReconcileResult.Ambiguous -> result
    }

private fun mapWriteReconcile(result: LockReconcileResult<LockHandle>): LockReconcileResult<WriteLockHandle> =
    when (result) {
        is LockReconcileResult.Owned -> LockReconcileResult.Owned(
            WriteLockHandle(result.handle),
            result.holdCount,
            result.remainingTtlMillis,
        )
        is LockReconcileResult.Queued -> result
        LockReconcileResult.Removed -> LockReconcileResult.Removed
        LockReconcileResult.Released -> LockReconcileResult.Released
        LockReconcileResult.NotFound -> LockReconcileResult.NotFound
        LockReconcileResult.StaleGeneration -> LockReconcileResult.StaleGeneration
        LockReconcileResult.Closed -> LockReconcileResult.Closed
        is LockReconcileResult.BackendFailure -> result
        is LockReconcileResult.IntegrityFailure -> result
        is LockReconcileResult.Ambiguous -> result
    }

private fun mapReadMutation(result: LockMutationResult<LockHandle>): LockMutationResult<ReadLockHandle> =
    when (result) {
        is LockMutationResult.Renewed -> LockMutationResult.Renewed(
            ReadLockHandle(result.handle),
            result.remainingTtlMillis,
        )
        is LockMutationResult.Released -> result
        LockMutationResult.AlreadyReleased -> LockMutationResult.AlreadyReleased
        LockMutationResult.Expired -> LockMutationResult.Expired
        LockMutationResult.StaleGeneration -> LockMutationResult.StaleGeneration
        LockMutationResult.OwnershipLost -> LockMutationResult.OwnershipLost
        LockMutationResult.Closed -> LockMutationResult.Closed
        is LockMutationResult.BackendFailure -> result
        is LockMutationResult.IntegrityFailure -> result
        is LockMutationResult.Ambiguous -> result
    }

private fun mapWriteMutation(result: LockMutationResult<LockHandle>): LockMutationResult<WriteLockHandle> =
    when (result) {
        is LockMutationResult.Renewed -> LockMutationResult.Renewed(
            WriteLockHandle(result.handle),
            result.remainingTtlMillis,
        )
        is LockMutationResult.Released -> result
        LockMutationResult.AlreadyReleased -> LockMutationResult.AlreadyReleased
        LockMutationResult.Expired -> LockMutationResult.Expired
        LockMutationResult.StaleGeneration -> LockMutationResult.StaleGeneration
        LockMutationResult.OwnershipLost -> LockMutationResult.OwnershipLost
        LockMutationResult.Closed -> LockMutationResult.Closed
        is LockMutationResult.BackendFailure -> result
        is LockMutationResult.IntegrityFailure -> result
        is LockMutationResult.Ambiguous -> result
    }

private inline fun <R> rwClassified(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
    block: () -> R,
): R =
    try {
        block()
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND -> backend(rwBackendFailure(error, action))
            CoordinationFailureClassification.INTEGRITY -> integrity(RW_MALFORMED_REPLY)
        }
    } catch (_: IllegalArgumentException) {
        integrity(RW_MALFORMED_REPLY)
    } catch (error: Exception) {
        backend(rwBackendFailure(error, action))
    }

private suspend inline fun <R> rwClassifiedSuspending(
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
            CoordinationFailureClassification.BACKEND -> backend(rwBackendFailure(error, action))
            CoordinationFailureClassification.INTEGRITY -> integrity(RW_MALFORMED_REPLY)
        }
    } catch (_: IllegalArgumentException) {
        integrity(RW_MALFORMED_REPLY)
    } catch (error: Exception) {
        backend(rwBackendFailure(error, action))
    }

private fun <R> CompletableFuture<List<String>>.rwMap(
    decode: (List<String>) -> R,
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
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
            mapped.complete(
                if (error == null) {
                    try {
                        decode(value)
                    } catch (_: CoordinationProtocolException) {
                        integrity(RW_MALFORMED_REPLY)
                    } catch (_: IllegalArgumentException) {
                        integrity(RW_MALFORMED_REPLY)
                    }
                } else {
                    backend(rwBackendFailure(error, action))
                },
            )
        } catch (failure: Throwable) {
            mapped.completeExceptionally(failure)
        }
    }
    mapped.whenComplete { _, _ ->
        if (mapped.isCancelled && !isDone) cancel(false)
    }
    return mapped
}

internal fun <S, R> mapHandleResultAsync(
    source: CompletableFuture<S>,
    acquiredHandle: (S) -> LockHandle?,
    transform: (S) -> R,
    releaseAbandoned: (LockHandle) -> Unit,
): CompletableFuture<R> {
    val mapped = CompletableFuture<R>()
    source.whenComplete { value, error ->
        if (mapped.isDone) {
            if (error == null) acquiredHandle(value)?.let(releaseAbandoned)
            return@whenComplete
        }
        if (error != null) {
            mapped.completeExceptionally(error)
        } else {
            try {
                val transformed = transform(value)
                if (!mapped.complete(transformed)) {
                    acquiredHandle(value)?.let(releaseAbandoned)
                }
            } catch (failure: Throwable) {
                mapped.completeExceptionally(failure)
            }
        }
    }
    mapped.whenComplete { _, _ ->
        if (mapped.isCancelled && !source.isDone) source.cancel(false)
    }
    return mapped
}

private fun rwBackendFailure(error: Throwable, action: LockRecoveryAction): LockBackendFailure {
    val cause = error.rwUnwrap()
    val kind = when (cause) {
        is RedisConnectionException -> LockBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException, is TimeoutException -> LockBackendFailureKind.TIMEOUT
        is RedisException -> LockBackendFailureKind.COMMAND
        else -> throw cause
    }
    return LockBackendFailure(kind, action)
}

private fun Throwable.rwUnwrap(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(RW_MAX_COMPLETION_DEPTH) {
        if (current is java.util.concurrent.CancellationException) throw current
        if (!seen.add(current)) return current
        val next = when (current) {
            is CompletionException, is ExecutionException -> current.cause
            else -> null
        } ?: return current
        if (next === current) return current
        current = next
    }
    return current
}

private fun readWriteMember(kind: LockKind, ownerId: LockOwnerId, requestId: LockRequestId): String {
    val input = "${kind.rwMode}\u0000${ownerId.value}\u0000${requestId.value}".toByteArray()
    return MessageDigest.getInstance("SHA-256").digest(input).joinToString("") { "%02x".format(it) }
}

private val LockKind.rwMode: String
    get() = when (this) {
        LockKind.READ -> "R"
        LockKind.WRITE -> "W"
        else -> error("Read/write lock supports only READ and WRITE modes.")
    }

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.rwPositiveLong(index: Int): Long =
    field(index).rwCanonicalLong(positive = true)

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.rwNonNegativeLong(index: Int): Long =
    field(index).rwCanonicalLong(positive = false)

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.rwPositiveInt(index: Int): Int =
    rwPositiveLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.rwNonNegativeInt(index: Int): Int =
    rwNonNegativeLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun String.rwCanonicalLong(positive: Boolean): Long {
    require(matches(if (positive) RW_POSITIVE_DECIMAL else RW_NON_NEGATIVE_DECIMAL))
    require(length <= 16)
    val value = toLong()
    require(value <= RW_MAX_LUA_EXACT_INTEGER)
    if (positive) require(value > 0L) else require(value >= 0L)
    return value
}

private fun ByteBuffer.rwBytes(): ByteArray =
    duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

private fun LockAcquireResult<LockHandle>?.acquiredHandleOrNull(): LockHandle? =
    when (this) {
        is LockAcquireResult.Acquired -> handle
        is LockAcquireResult.Reentered -> handle
        else -> null
    }

private fun LockReconcileResult<LockHandle>.ownedHandleOrNull(): LockHandle? =
    (this as? LockReconcileResult.Owned)?.handle

private fun rwMalformedReply(): Nothing =
    throw CoordinationProtocolException(
        CoordinationFailureClassification.INTEGRITY,
        "Malformed read/write lock reply.",
    )

private val RW_INVALID_STATE = LockIntegrityFailure(LockIntegrityFailureKind.INVALID_STATE)
private val RW_MALFORMED_REPLY = LockIntegrityFailure(LockIntegrityFailureKind.MALFORMED_REPLY)
private val RW_RETRY_DELAY: Duration = Duration.ofMillis(10)
private val RW_MAX_WAIT: Duration = Duration.ofHours(24)
private const val RW_TERMINAL_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L
private const val RW_MAX_LUA_EXACT_INTEGER = 9_007_199_254_740_991L
private const val RW_FINGERPRINT_BYTES = 16
private const val RW_MAX_COMPLETION_DEPTH = 8
private val RW_POSITIVE_DECIMAL = Regex("[1-9][0-9]*")
private val RW_NON_NEGATIVE_DECIMAL = Regex("0|[1-9][0-9]*")
