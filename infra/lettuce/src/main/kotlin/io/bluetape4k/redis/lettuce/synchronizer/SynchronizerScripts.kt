package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.redis.lettuce.script.RedisScript

internal object SynchronizerScripts {
    val TRY_SET_PERMITS_SCRIPT = RedisScript(
        """
local permits = tonumber(ARGV[1])
if permits == nil or permits <= 0 then return {'INVALID_CAPACITY'} end
if redis.call('exists', KEYS[3]) == 1 then
  return {'ALREADY_INITIALIZED', redis.call('get', KEYS[2]) or '0'}
end
local generation = redis.call('incr', KEYS[2])
redis.call('set', KEYS[1], permits)
redis.call('set', KEYS[3], permits)
redis.call('del', KEYS[4], KEYS[5])
return {'INITIALIZED', tostring(generation)}
"""
    )

    val ACQUIRE_SCRIPT = RedisScript(
        """
local permits = tonumber(ARGV[1])
if permits == nil or permits <= 0 then return {'INVALID'} end
local capacity = tonumber(redis.call('get', KEYS[3]) or '-1')
if capacity < 0 then return {'NOT_INITIALIZED'} end
if permits > capacity then return {'CAPACITY_EXCEEDED'} end
local requestKey = ARGV[2] .. '|' .. ARGV[3]
local replay = redis.call('hget', KEYS[5], requestKey)
if replay ~= false then
  if replay == '!released' then return {'REQUEST_COMPLETED'} end
  local value = redis.call('hget', KEYS[4], replay)
  if value ~= false then
    local generation, held = string.match(value, '^[^|]+|[^|]+|(%d+)|(%d+)$')
    return {'ACQUIRED', replay, generation, held, redis.call('get', KEYS[1]) or '0'}
  end
end
local available = tonumber(redis.call('get', KEYS[1]) or '0')
if available < permits then return {'UNAVAILABLE'} end
local generation = redis.call('get', KEYS[2]) or '0'
redis.call('decrby', KEYS[1], permits)
redis.call('hset', KEYS[4], ARGV[4], ARGV[2] .. '|' .. ARGV[3] .. '|' .. generation .. '|' .. permits)
redis.call('hset', KEYS[5], requestKey, ARGV[4])
return {'ACQUIRED', ARGV[4], generation, tostring(permits), tostring(available - permits)}
"""
    )

    val RELEASE_SCRIPT = RedisScript(
        """
local value = redis.call('hget', KEYS[4], ARGV[1])
if value == false then return {'ALREADY_RELEASED'} end
local owner, request, generation, permits = string.match(value, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
if generation == nil or tonumber(generation) ~= tonumber(ARGV[2]) then return {'STALE_GENERATION'} end
if owner ~= ARGV[3] or request ~= ARGV[4] then return {'INTEGRITY'} end
redis.call('hdel', KEYS[4], ARGV[1])
redis.call('hset', KEYS[5], owner .. '|' .. request, '!released')
local remaining = redis.call('incrby', KEYS[1], tonumber(permits))
return {'RELEASED', tostring(remaining)}
"""
    )

    val INSPECT_SCRIPT = RedisScript(
        """
local value = redis.call('hget', KEYS[4], ARGV[1])
if value == false then return {'RELEASED'} end
local owner, request, generation, permits = string.match(value, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
if generation == nil then return {'INTEGRITY'} end
if tonumber(generation) ~= tonumber(ARGV[2]) then return {'STALE_GENERATION'} end
if owner ~= ARGV[3] or request ~= ARGV[4] then return {'INTEGRITY'} end
return {'OWNED', redis.call('get', KEYS[1]) or '0', permits}
"""
    )

    val RECONCILE_SCRIPT = RedisScript(
        """
local token = redis.call('hget', KEYS[5], ARGV[1] .. '|' .. ARGV[2])
if token == false then return {'NOT_FOUND'} end
if token == '!released' then return {'RELEASED'} end
local value = redis.call('hget', KEYS[4], token)
if value == false then return {'RELEASED'} end
local owner, request, generation, permits = string.match(value, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
if generation == nil then return {'INTEGRITY'} end
return {'OWNED', token, generation, permits, redis.call('get', KEYS[1]) or '0'}
"""
    )

    val AVAILABLE_SCRIPT = RedisScript("return {redis.call('get', KEYS[1]) or '-1'}")

    private val EXPIRABLE_CLEANUP_FUNCTION = """
local function redisNowMillis()
  local redisTime = redis.call('TIME')
  return tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
end
local function cleanupExpired(now, limit)
local expired = redis.call('zrangebyscore', KEYS[7], '-inf', now, 'LIMIT', 0, limit)
for _, permitId in ipairs(expired) do
  local allocationId = redis.call('hget', KEYS[6], permitId)
  local allocation = allocationId and redis.call('hget', KEYS[4], allocationId)
  if allocation ~= false and allocation ~= nil then
    local owner, request, generation, allocationPermits = string.match(allocation, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
    local leaseIds = redis.call('hget', KEYS[5], allocationId) or ''
    for leaseId in string.gmatch(leaseIds, '([^,]+)') do
      redis.call('hdel', KEYS[6], leaseId)
      redis.call('zrem', KEYS[7], leaseId)
    end
    redis.call('hdel', KEYS[4], allocationId)
    redis.call('hdel', KEYS[5], allocationId)
    redis.call('hset', KEYS[8], owner .. '|' .. request, '!expired')
    redis.call('incrby', KEYS[1], tonumber(allocationPermits))
  else
    redis.call('hdel', KEYS[6], permitId)
    redis.call('zrem', KEYS[7], permitId)
  end
end
end
""".trimIndent()

    val EXPIRABLE_CLEANUP_SCRIPT = RedisScript(
        EXPIRABLE_CLEANUP_FUNCTION + """
cleanupExpired(redisNowMillis(), tonumber(ARGV[1]))
return {redis.call('get', KEYS[1]) or '-1'}
""",
    )

    val EXPIRABLE_ACQUIRE_SCRIPT = RedisScript(
        EXPIRABLE_CLEANUP_FUNCTION + """
local now = redisNowMillis()
cleanupExpired(now, tonumber(ARGV[1]))
local permits = tonumber(ARGV[2])
local capacity = tonumber(redis.call('get', KEYS[3]) or '-1')
if capacity < 0 then return {'NOT_INITIALIZED'} end
if permits > capacity then return {'CAPACITY_EXCEEDED'} end
local requestKey = ARGV[3] .. '|' .. ARGV[4]
local replay = redis.call('hget', KEYS[8], requestKey)
if replay ~= false then
  if replay == '!released' or replay == '!expired' then return {'REQUEST_COMPLETED'} end
  local value = redis.call('hget', KEYS[4], replay)
  if value ~= false then
    local replayLeases = redis.call('hget', KEYS[5], replay) or ''
    local firstLease = string.match(replayLeases, '^([^,]+)')
    local replayDeadline = firstLease and redis.call('zscore', KEYS[7], firstLease) or '0'
    return {'REPLAY', replay, value, replayLeases, tostring(replayDeadline)}
  end
end
local available = tonumber(redis.call('get', KEYS[1]) or '0')
if available < permits then return {'UNAVAILABLE'} end
local generation = redis.call('get', KEYS[2]) or '0'
local allocationId = ARGV[5]
local leaseIds = ARGV[6]
local deadline = now + tonumber(ARGV[7])
redis.call('decrby', KEYS[1], permits)
redis.call('hset', KEYS[4], allocationId, ARGV[3] .. '|' .. ARGV[4] .. '|' .. generation .. '|' .. permits)
redis.call('hset', KEYS[5], allocationId, leaseIds)
for permitId in string.gmatch(leaseIds, '([^,]+)') do
  redis.call('hset', KEYS[6], permitId, allocationId)
  redis.call('zadd', KEYS[7], deadline, permitId)
end
redis.call('hset', KEYS[8], requestKey, allocationId)
return {'ACQUIRED', allocationId, generation, tostring(permits), tostring(deadline), leaseIds, tostring(available - permits)}
""",
    )

    val EXPIRABLE_RELEASE_SCRIPT = RedisScript(
        EXPIRABLE_CLEANUP_FUNCTION + """
cleanupExpired(redisNowMillis(), tonumber(ARGV[1]))
local allocationId = ARGV[2]
local allocation = redis.call('hget', KEYS[4], allocationId)
if allocation == false then
  local marker = redis.call('hget', KEYS[8], ARGV[4] .. '|' .. ARGV[5])
  if marker == '!expired' then return {'EXPIRED'} end
  if marker == '!released' then return {'ALREADY_RELEASED'} end
  if marker ~= false then return {'OWNERSHIP_LOST'} end
  return {'ALREADY_RELEASED'}
end
local owner, request, generation, allocationPermits = string.match(allocation, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
if generation == nil then return {'INTEGRITY'} end
if tonumber(generation) ~= tonumber(ARGV[3]) then return {'STALE_GENERATION'} end
if owner ~= ARGV[4] or request ~= ARGV[5] then return {'INTEGRITY'} end
local leaseIds = redis.call('hget', KEYS[5], allocationId) or ''
for leaseId in string.gmatch(leaseIds, '([^,]+)') do
  redis.call('hdel', KEYS[6], leaseId)
  redis.call('zrem', KEYS[7], leaseId)
end
redis.call('hdel', KEYS[4], allocationId)
redis.call('hdel', KEYS[5], allocationId)
redis.call('hset', KEYS[8], owner .. '|' .. request, '!released')
local remaining = redis.call('incrby', KEYS[1], tonumber(allocationPermits))
return {'RELEASED', tostring(remaining)}
""",
    )

    val EXPIRABLE_INSPECT_SCRIPT = RedisScript(
        EXPIRABLE_CLEANUP_FUNCTION + """
cleanupExpired(redisNowMillis(), tonumber(ARGV[1]))
local allocation = redis.call('hget', KEYS[4], ARGV[2])
if allocation == false then
  local marker = redis.call('hget', KEYS[8], ARGV[4] .. '|' .. ARGV[5])
  if marker == '!expired' then return {'EXPIRED'} end
  if marker == '!released' then return {'RELEASED'} end
  if marker ~= false then return {'OWNERSHIP_LOST'} end
  return {'RELEASED'}
end
local owner, request, generation, allocationPermits = string.match(allocation, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
if generation == nil then return {'INTEGRITY'} end
if tonumber(generation) ~= tonumber(ARGV[3]) then return {'STALE_GENERATION'} end
if owner ~= ARGV[4] or request ~= ARGV[5] then return {'INTEGRITY'} end
return {'OWNED', redis.call('get', KEYS[1]) or '0'}
""",
    )

    val EXPIRABLE_RENEW_SCRIPT = RedisScript(
        EXPIRABLE_CLEANUP_FUNCTION + """
local now = redisNowMillis()
cleanupExpired(now, tonumber(ARGV[1]))
local allocation = redis.call('hget', KEYS[4], ARGV[2])
if allocation == false then
  local marker = redis.call('hget', KEYS[8], ARGV[4] .. '|' .. ARGV[5])
  if marker == '!expired' then return {'EXPIRED'} end
  if marker == '!released' then return {'RELEASED'} end
  if marker ~= false then return {'OWNERSHIP_LOST'} end
  return {'EXPIRED'}
end
local owner, request, generation = string.match(allocation, '^([^|]+)|([^|]+)|(%d+)|')
if generation == nil then return {'INTEGRITY'} end
if tonumber(generation) ~= tonumber(ARGV[3]) then return {'STALE_GENERATION'} end
if owner ~= ARGV[4] or request ~= ARGV[5] then return {'INTEGRITY'} end
local deadline = now + tonumber(ARGV[6])
local leaseIds = redis.call('hget', KEYS[5], ARGV[2]) or ''
for leaseId in string.gmatch(leaseIds, '([^,]+)') do
  if redis.call('hget', KEYS[6], leaseId) ~= ARGV[2] then return {'INTEGRITY'} end
end
for leaseId in string.gmatch(leaseIds, '([^,]+)') do
  redis.call('zadd', KEYS[7], deadline, leaseId)
end
return {'RENEWED', tostring(deadline), leaseIds}
""",
    )

    val EXPIRABLE_RECONCILE_SCRIPT = RedisScript(
        EXPIRABLE_CLEANUP_FUNCTION + """
cleanupExpired(redisNowMillis(), tonumber(ARGV[1]))
local allocationId = redis.call('hget', KEYS[8], ARGV[2] .. '|' .. ARGV[3])
if allocationId == false then return {'NOT_FOUND'} end
if allocationId == '!released' or allocationId == '!expired' then return {'RELEASED'} end
local allocation = redis.call('hget', KEYS[4], allocationId)
if allocation == false then return {'RELEASED'} end
local owner, request, generation, permits = string.match(allocation, '^([^|]+)|([^|]+)|(%d+)|(%d+)$')
if generation == nil then return {'INTEGRITY'} end
local leaseIds = redis.call('hget', KEYS[5], allocationId) or ''
local firstLease = string.match(leaseIds, '^([^,]+)')
local deadline = firstLease and redis.call('zscore', KEYS[7], firstLease) or '0'
return {'OWNED', allocationId, generation, permits, leaseIds, tostring(deadline), redis.call('get', KEYS[1]) or '0'}
""",
    )
}

internal object LatchScripts {
    val TRY_SET_COUNT_SCRIPT = RedisScript(
        """
local count = tonumber(ARGV[1])
if count == nil or count < 0 then return {'INVALID_COUNT'} end
local replay = redis.call('hget', KEYS[4], 'set|' .. ARGV[2])
if replay ~= false and redis.call('exists', KEYS[1]) == 1 then
  return {'CREATED', replay}
end
if redis.call('exists', KEYS[1]) == 1 then
  return {'ACTIVE_GENERATION', redis.call('get', KEYS[2]) or '0', redis.call('get', KEYS[1]) or '0'}
end
local generation = redis.call('incr', KEYS[2])
redis.call('set', KEYS[1], count)
redis.call('del', KEYS[3], KEYS[4])
redis.call('hset', KEYS[4], 'set|' .. ARGV[2], tostring(generation))
return {'CREATED', tostring(generation)}
"""
    )

    val GET_COUNT_SCRIPT = RedisScript(
        """
if redis.call('exists', KEYS[1]) == 0 then return {'DELETED'} end
local generation = tonumber(redis.call('get', KEYS[2]) or '0')
if tonumber(ARGV[1]) ~= generation then return {'STALE_GENERATION'} end
local redisTime = redis.call('TIME')
local now = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
redis.call('zremrangebyscore', KEYS[3], '-inf', now)
local count = tonumber(redis.call('get', KEYS[1]) or '0')
if count == 0 then return {'COMPLETED', tostring(generation)} end
return {'ACTIVE', tostring(generation), tostring(count), tostring(redis.call('zcard', KEYS[3]))}
"""
    )

    val COUNT_DOWN_SCRIPT = RedisScript(
        """
if redis.call('exists', KEYS[1]) == 0 then return {'DELETED'} end
local generation = tonumber(redis.call('get', KEYS[2]) or '0')
if tonumber(ARGV[1]) ~= generation then return {'STALE_GENERATION'} end
local requestKey = tostring(generation) .. '|' .. ARGV[2]
local replay = redis.call('hget', KEYS[4], requestKey)
if replay ~= false then
  local tag, value = string.match(replay, '^([^|]+)|?(.*)$')
  return {tag, value}
end
local count = tonumber(redis.call('get', KEYS[1]) or '0')
if count == 0 then
  redis.call('hset', KEYS[4], requestKey, 'ALREADY_COMPLETED|0')
  return {'ALREADY_COMPLETED', '0'}
end
local remaining = redis.call('decrby', KEYS[1], 1)
local tag = remaining == 0 and 'COMPLETED' or 'DECREMENTED'
redis.call('hset', KEYS[4], requestKey, tag .. '|' .. remaining)
return {tag, tostring(remaining)}
"""
    )

    val DELETE_SCRIPT = RedisScript(
        """
local requestKey = 'delete|' .. ARGV[1] .. '|' .. ARGV[2]
if redis.call('exists', KEYS[1]) == 0 then
  if redis.call('hget', KEYS[4], requestKey) ~= false then return {'DELETED'} end
  return {'NOT_FOUND'}
end
if tonumber(ARGV[1]) ~= tonumber(redis.call('get', KEYS[2]) or '0') then return {'STALE_GENERATION'} end
local redisTime = redis.call('TIME')
local now = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
redis.call('zremrangebyscore', KEYS[3], '-inf', now)
local waiters = redis.call('zcard', KEYS[3])
if waiters > 0 then return {'ACTIVE_WAITERS', tostring(waiters)} end
redis.call('del', KEYS[1], KEYS[3], KEYS[4])
redis.call('hset', KEYS[4], requestKey, 'DELETED')
return {'DELETED'}
"""
    )

    val REGISTER_WAITER_SCRIPT = RedisScript(
        """
if redis.call('exists', KEYS[1]) == 0 then return {'DELETED'} end
if tonumber(ARGV[1]) ~= tonumber(redis.call('get', KEYS[2]) or '0') then return {'STALE_GENERATION'} end
if tonumber(redis.call('get', KEYS[1]) or '0') == 0 then return {'COMPLETED'} end
local redisTime = redis.call('TIME')
local now = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
redis.call('zremrangebyscore', KEYS[3], '-inf', now)
local waiterId = ARGV[1] .. '|' .. ARGV[2]
if redis.call('zscore', KEYS[3], waiterId) == false and redis.call('zcard', KEYS[3]) >= tonumber(ARGV[4]) then
  return {'CAPACITY_EXCEEDED'}
end
local deadline = now + tonumber(ARGV[3]) + tonumber(ARGV[5])
redis.call('zadd', KEYS[3], deadline, waiterId)
return {'REGISTERED'}
"""
    )

    val UNREGISTER_WAITER_SCRIPT = RedisScript(
        """
redis.call('zrem', KEYS[3], ARGV[1] .. '|' .. ARGV[2])
return {'REMOVED'}
"""
    )
}
