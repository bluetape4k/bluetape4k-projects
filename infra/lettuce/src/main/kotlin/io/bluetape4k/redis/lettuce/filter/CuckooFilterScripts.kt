package io.bluetape4k.redis.lettuce.filter

/**
 * Cuckoo Filter 구현이 공유하는 Lua 스크립트 모음입니다.
 *
 * 삽입 스크립트는 kick-out 재배치 중 실패하면 undo-log를 이용해 기존 버킷 상태를 복구합니다.
 */
internal object CuckooFilterScripts {

    const val INSERT = """
local fp = ARGV[1]
local i1 = tonumber(ARGV[2])
local i2 = tonumber(ARGV[3])
local bucketSize = tonumber(ARGV[4])
local maxIter = tonumber(ARGV[5])
local numBuckets = tonumber(ARGV[6])

local undo_log = {}

local function getSlots(idx)
    local val = redis.call('hget', KEYS[1], tostring(idx))
    if not val or val == '' then return {} end
    local t = {}
    for s in string.gmatch(val, '[^,]+') do t[#t+1] = s end
    return t
end

local function setSlots(idx, t)
    local orig = redis.call('hget', KEYS[1], tostring(idx)) or ''
    undo_log[#undo_log+1] = {idx=idx, orig=orig}
    if #t == 0 then redis.call('hdel', KEYS[1], tostring(idx))
    else redis.call('hset', KEYS[1], tostring(idx), table.concat(t, ',')) end
end

local function restoreAll()
    for i = #undo_log, 1, -1 do
        local e = undo_log[i]
        if e.orig == '' then redis.call('hdel', KEYS[1], tostring(e.idx))
        else redis.call('hset', KEYS[1], tostring(e.idx), e.orig) end
    end
end

local function tryAdd(idx, fp_val)
    local t = getSlots(idx)
    if #t < bucketSize then t[#t+1] = fp_val; setSlots(idx, t); return true end
    return false
end

if tryAdd(i1, fp) then redis.call('hincrby', KEYS[2], 'count', 1); return 1 end
if tryAdd(i2, fp) then redis.call('hincrby', KEYS[2], 'count', 1); return 1 end

local cur = i1
local cur_fp = fp
for iter = 1, maxIter do
    local t = getSlots(cur)
    if #t == 0 then break end
    local kick_pos = (iter % #t) + 1
    local kicked = t[kick_pos]
    t[kick_pos] = cur_fp
    setSlots(cur, t)

    local fp_num = tonumber(kicked) or 0
    local fp_hash = math.abs(fp_num * 2654435761) % numBuckets
    local alt = bit.bxor(cur - 1, fp_hash) % numBuckets + 1

    if tryAdd(alt, kicked) then
        redis.call('hincrby', KEYS[2], 'count', 1)
        return 1
    end
    cur = alt
    cur_fp = kicked
end

restoreAll()
return 0"""

    const val CONTAINS = """
local fp = ARGV[1]
local i1 = tostring(ARGV[2])
local i2 = tostring(ARGV[3])

local function bucketContains(idx, fp_val)
    local val = redis.call('hget', KEYS[1], idx)
    if not val then return false end
    for s in string.gmatch(val, '[^,]+') do
        if s == fp_val then return true end
    end
    return false
end

if bucketContains(i1, fp) or bucketContains(i2, fp) then return 1 end
return 0"""

    const val DELETE = """
local fp = ARGV[1]
local i1 = tostring(ARGV[2])
local i2 = tostring(ARGV[3])

local function bucketRemove(idx, fp_val)
    local val = redis.call('hget', KEYS[1], idx)
    if not val then return false end
    local t = {}
    local found = false
    for s in string.gmatch(val, '[^,]+') do
        if s == fp_val and not found then found = true
        else t[#t+1] = s end
    end
    if found then
        if #t == 0 then redis.call('hdel', KEYS[1], idx)
        else redis.call('hset', KEYS[1], idx, table.concat(t, ',')) end
    end
    return found
end

if bucketRemove(i1, fp) then redis.call('hincrby', KEYS[2], 'count', -1); return 1 end
if bucketRemove(i2, fp) then redis.call('hincrby', KEYS[2], 'count', -1); return 1 end
return 0"""
}
