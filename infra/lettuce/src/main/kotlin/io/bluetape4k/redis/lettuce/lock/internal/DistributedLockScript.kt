package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.script.RedisScript

internal val DISTRIBUTED_LOCK_SCRIPT = RedisScript(
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
            and (string.len(value) < 16 or value <= '9007199254740991')
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

    local function valid_terminal()
        local terminal_type = key_type(KEYS[4])
        if terminal_type == 'none' then
            return true
        end
        if terminal_type ~= 'hash' or redis.call('PTTL', KEYS[4]) <= 0 then
            return false
        end
        local owner = redis.call('HGET', KEYS[4], 'owner')
        local generation = redis.call('HGET', KEYS[4], 'generation')
        local request = redis.call('HGET', KEYS[4], 'request')
        return owner ~= false and string.len(owner) >= 1 and string.len(owner) <= 256
            and positive_decimal(generation)
            and request ~= false and string.len(request) >= 1 and string.len(request) <= 256
            and redis.call('HLEN', KEYS[4]) == 3
    end

    local function validate()
        local generation_type = key_type(KEYS[2])
        if generation_type ~= 'none' and generation_type ~= 'string' then
            return false
        end
        if generation_type == 'string' then
            local counter = redis.call('GET', KEYS[2])
            if not positive_decimal(counter) or redis.call('PTTL', KEYS[2]) ~= -1 then
                return false
            end
        end
        if not valid_terminal() then
            return false
        end

        local state_type = key_type(KEYS[1])
        local holds_type = key_type(KEYS[3])
        if state_type == 'none' then
            return holds_type == 'none', false
        end
        if state_type ~= 'hash' or holds_type ~= 'hash' then
            return false
        end
        if redis.call('PTTL', KEYS[1]) <= 0 or redis.call('PTTL', KEYS[3]) <= 0 then
            return false
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
            return false
        end
        return true, true
    end

    local function missing_result(owner, request, generation)
        if key_type(KEYS[4]) == 'hash'
            and redis.call('HGET', KEYS[4], 'owner') == owner
            and redis.call('HGET', KEYS[4], 'request') == request
            and redis.call('HGET', KEYS[4], 'generation') == generation then
            return {'ALREADY_RELEASED'}
        end
        local counter = redis.call('GET', KEYS[2])
        if counter ~= false and compare_decimal(counter, generation) > 0 then
            return {'STALE'}
        end
        return {'EXPIRED'}
    end

    local valid, active = validate()
    if not valid then
        return {'INTEGRITY'}
    end

    if operation == 'ACQUIRE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        local policy = ARGV[4]
        local ttl = tonumber(ARGV[5])
        local maximum = tonumber(ARGV[6])
        if not active then
            local previous_generation = redis.call('GET', KEYS[2])
            if previous_generation ~= false and compare_decimal(previous_generation, '9007199254740990') > 0 then
                return {'CAPACITY'}
            end
            redis.call('INCR', KEYS[2])
            local generation = redis.call('GET', KEYS[2])
            redis.call('DEL', KEYS[4])
            redis.call('HSET', KEYS[1], 'owner', owner, 'generation', generation, 'holdCount', 1)
            redis.call('HSET', KEYS[3], request, tostring(generation) .. '|A|' .. policy)
            redis.call('PEXPIRE', KEYS[1], ttl)
            redis.call('PEXPIRE', KEYS[3], ttl)
            return {'ACQUIRED', tostring(generation), '1', tostring(ttl), policy}
        end

        local current_owner = redis.call('HGET', KEYS[1], 'owner')
        local remaining = redis.call('PTTL', KEYS[1])
        if current_owner ~= owner then
            return {'CONTENDED', tostring(remaining)}
        end
        local generation = redis.call('HGET', KEYS[1], 'generation')
        local hold_count = tonumber(redis.call('HGET', KEYS[1], 'holdCount'))
        local existing = redis.call('HGET', KEYS[3], request)
        if existing ~= false then
            local existing_generation, existing_acquisition, existing_policy = parse_hold(existing)
            if existing_generation ~= generation then
                return {'INTEGRITY'}
            end
            local replay_tag = existing_acquisition == 'A' and 'ACQUIRED' or 'REENTERED'
            return {replay_tag, generation, tostring(hold_count), tostring(remaining), existing_policy}
        end
        if hold_count >= maximum then
            return {'CAPACITY'}
        end
        hold_count = hold_count + 1
        redis.call('HSET', KEYS[1], 'holdCount', hold_count)
        redis.call('HSET', KEYS[3], request, generation .. '|R|' .. policy)
        redis.call('PEXPIRE', KEYS[1], ttl)
        redis.call('PEXPIRE', KEYS[3], ttl)
        return {'REENTERED', generation, tostring(hold_count), tostring(ttl), policy}
    end

    if operation == 'RECONCILE' then
        local owner = ARGV[2]
        local request = ARGV[3]
        if not active then
            if key_type(KEYS[4]) == 'hash'
                and redis.call('HGET', KEYS[4], 'owner') == owner
                and redis.call('HGET', KEYS[4], 'request') == request then
                return {'RELEASED'}
            end
            return {'NOT_FOUND'}
        end
        if redis.call('HGET', KEYS[1], 'owner') ~= owner then
            return {'NOT_FOUND'}
        end
        local hold = redis.call('HGET', KEYS[3], request)
        if hold == false then
            return {'NOT_FOUND'}
        end
        local generation, acquisition, policy = parse_hold(hold)
        if generation == nil or generation ~= redis.call('HGET', KEYS[1], 'generation') then
            return {'INTEGRITY'}
        end
        return {
            'OWNED',
            generation,
            redis.call('HGET', KEYS[1], 'holdCount'),
            tostring(redis.call('PTTL', KEYS[1])),
            policy
        }
    end

    local owner = ARGV[2]
    local request = ARGV[3]
    local generation = ARGV[4]
    if not active then
        local missing = missing_result(owner, request, generation)
        if operation == 'INSPECT' and missing[1] == 'ALREADY_RELEASED' then
            return {'RELEASED'}
        end
        return missing
    end

    local current_generation = redis.call('HGET', KEYS[1], 'generation')
    if compare_decimal(current_generation, generation) > 0 then
        return {'STALE'}
    end
    if current_generation ~= generation then
        return {'INTEGRITY'}
    end
    if redis.call('HGET', KEYS[1], 'owner') ~= owner then
        return {'LOST'}
    end

    local hold = redis.call('HGET', KEYS[3], request)
    if hold == false then
        if operation == 'INSPECT' then
            return {'RELEASED'}
        end
        return {'ALREADY_RELEASED'}
    end
    local hold_generation, acquisition, policy = parse_hold(hold)
    if hold_generation == nil or hold_generation ~= generation then
        return {'INTEGRITY'}
    end

    if operation == 'INSPECT' then
        return {
            'OWNED',
            generation,
            redis.call('HGET', KEYS[1], 'holdCount'),
            tostring(redis.call('PTTL', KEYS[1])),
            policy
        }
    end

    if operation == 'RENEW' then
        local ttl = tonumber(ARGV[5])
        redis.call('PEXPIRE', KEYS[1], ttl)
        redis.call('PEXPIRE', KEYS[3], ttl)
        return {'RENEWED', tostring(ttl)}
    end

    if operation == 'RELEASE' then
        local terminal_ttl = tonumber(ARGV[5])
        redis.call('HDEL', KEYS[3], request)
        local hold_count = tonumber(redis.call('HGET', KEYS[1], 'holdCount')) - 1
        if hold_count > 0 then
            redis.call('HSET', KEYS[1], 'holdCount', hold_count)
            return {'RELEASED', tostring(hold_count)}
        end
        redis.call('DEL', KEYS[4])
        redis.call('HSET', KEYS[4], 'owner', owner, 'generation', generation, 'request', request)
        redis.call('PEXPIRE', KEYS[4], terminal_ttl)
        redis.call('DEL', KEYS[1], KEYS[3])
        return {'RELEASED', '0'}
    end

    return {'INTEGRITY'}
    """.trimIndent(),
)
