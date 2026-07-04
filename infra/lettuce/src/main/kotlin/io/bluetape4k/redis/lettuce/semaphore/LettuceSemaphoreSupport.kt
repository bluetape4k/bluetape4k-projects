package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.redis.lettuce.script.RedisScript
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object LettuceSemaphoreScripts {

    val AVAILABLE_SCRIPT = RedisScript(
        """
local function cleanup(now, total)
  local expired = redis.call('zrangebyscore', KEYS[3], '-inf', now)
  for _, token in ipairs(expired) do
    local held = tonumber(redis.call('hget', KEYS[2], token) or '0')
    if held > 0 then
      redis.call('incrby', KEYS[1], held)
    end
    redis.call('hdel', KEYS[2], token)
    redis.call('zrem', KEYS[3], token)
  end
  local current = tonumber(redis.call('get', KEYS[1]) or '0')
  if current > total then
    redis.call('set', KEYS[1], total)
    current = total
  end
  return current
end
return cleanup(tonumber(ARGV[1]), tonumber(ARGV[2]))
"""
    )

    val ACQUIRE_SCRIPT = RedisScript(
        """
local function cleanup(now, total)
  local expired = redis.call('zrangebyscore', KEYS[3], '-inf', now)
  for _, token in ipairs(expired) do
    local held = tonumber(redis.call('hget', KEYS[2], token) or '0')
    if held > 0 then
      redis.call('incrby', KEYS[1], held)
    end
    redis.call('hdel', KEYS[2], token)
    redis.call('zrem', KEYS[3], token)
  end
  local current = tonumber(redis.call('get', KEYS[1]) or '0')
  if current > total then
    redis.call('set', KEYS[1], total)
    current = total
  end
  return current
end
local now = tonumber(ARGV[1])
local total = tonumber(ARGV[2])
local permits = tonumber(ARGV[3])
local token = ARGV[4]
local expiresAt = tonumber(ARGV[5])
local current = cleanup(now, total)
if current >= permits then
  redis.call('decrby', KEYS[1], permits)
  redis.call('hset', KEYS[2], token, permits)
  redis.call('zadd', KEYS[3], expiresAt, token)
  return current - permits
end
return -1
"""
    )

    val RELEASE_SCRIPT = RedisScript(
        """
local function cleanup(now, total)
  local expired = redis.call('zrangebyscore', KEYS[3], '-inf', now)
  for _, token in ipairs(expired) do
    local held = tonumber(redis.call('hget', KEYS[2], token) or '0')
    if held > 0 then
      redis.call('incrby', KEYS[1], held)
    end
    redis.call('hdel', KEYS[2], token)
    redis.call('zrem', KEYS[3], token)
  end
  local current = tonumber(redis.call('get', KEYS[1]) or '0')
  if current > total then
    redis.call('set', KEYS[1], total)
    current = total
  end
  return current
end
local now = tonumber(ARGV[1])
local total = tonumber(ARGV[2])
local permits = tonumber(ARGV[3])
local token = ARGV[4]
cleanup(now, total)
local held = tonumber(redis.call('hget', KEYS[2], token) or '-1')
if held < 0 then
  return -1
end
if held < permits then
  return -2
end
local current = tonumber(redis.call('incrby', KEYS[1], permits))
if current > total then
  redis.call('set', KEYS[1], total)
  current = total
end
if held == permits then
  redis.call('hdel', KEYS[2], token)
  redis.call('zrem', KEYS[3], token)
else
  redis.call('hincrby', KEYS[2], token, -permits)
end
return current
"""
    )
}

internal class LocalSemaphorePermits {
    private val leases = mutableListOf<Lease>()
    private val lock = ReentrantLock()

    fun record(token: String, permits: Int): Unit = lock.withLock {
        leases += Lease(token, permits)
    }

    fun select(permits: Int): List<PermitRelease> = lock.withLock {
        var remaining = permits
        val selected = mutableListOf<PermitRelease>()
        for (lease in leases) {
            if (remaining == 0) break
            val releasePermits = minOf(lease.permits, remaining)
            selected += PermitRelease(lease.token, releasePermits)
            remaining -= releasePermits
        }
        check(remaining == 0) { "Cannot release permits that are not owned by this semaphore instance: permits=$permits" }
        selected
    }

    fun markReleased(release: PermitRelease): Unit = lock.withLock {
        val lease = leases.firstOrNull { it.token == release.token } ?: return
        if (lease.permits <= release.permits) {
            leases.remove(lease)
        } else {
            lease.permits -= release.permits
        }
    }

    fun markLost(release: PermitRelease): Unit = lock.withLock {
        leases.removeAll { it.token == release.token }
    }

    fun clear(): Unit = lock.withLock {
        leases.clear()
    }

    private class Lease(
        val token: String,
        var permits: Int,
    )
}

internal class PermitRelease(
    val token: String,
    val permits: Int,
)

internal fun Duration.requirePositiveMillis(parameterName: String): Duration {
    require(!isNegative && !isZero && toMillis() > 0L) {
        "$parameterName must be positive and at least 1 millisecond."
    }
    return this
}
