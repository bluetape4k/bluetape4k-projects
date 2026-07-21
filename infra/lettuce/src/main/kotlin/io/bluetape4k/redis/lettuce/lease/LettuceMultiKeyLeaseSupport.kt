package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.time.Duration
import java.util.concurrent.CompletableFuture

internal data class ValidatedLeaseInput(
    val keys: List<String>,
    val ttlMillis: Long?,
)

internal fun validateLeaseInput(
    keys: Collection<String>,
    ownerToken: String,
    ttl: Duration?,
    config: LettuceMultiKeyLeaseConfig,
    codec: RedisCodec<String, String>,
): ValidatedLeaseInput {
    require(ownerToken.isNotBlank()) { "ownerToken must not be blank." }
    val snapshot = snapshotKeys(keys, config.maxKeys)
    requireSameSlot(snapshot, codec)
    return ValidatedLeaseInput(snapshot, ttl?.requirePositiveMillis())
}

internal fun runAcquire(
    commands: RedisScriptingCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyAcquireResult = decodeAcquire(
    runLeaseScript(commands, ACQUIRE_SCRIPT, input, ownerToken, requireTtl = true),
)

internal fun runAcquireAsync(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): CompletableFuture<MultiKeyAcquireResult> =
    runLeaseScriptAsync(commands, ACQUIRE_SCRIPT, input, ownerToken, requireTtl = true)
        .thenApply(::decodeAcquire)

internal suspend fun runAcquireSuspending(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyAcquireResult = decodeAcquire(
    runLeaseScriptSuspending(commands, ACQUIRE_SCRIPT, input, ownerToken, requireTtl = true),
)

internal fun runInspect(
    commands: RedisScriptingCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyInspectResult = decodeInspect(runLeaseScript(commands, INSPECT_SCRIPT, input, ownerToken))

internal fun runInspectAsync(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): CompletableFuture<MultiKeyInspectResult> =
    runLeaseScriptAsync(commands, INSPECT_SCRIPT, input, ownerToken).thenApply(::decodeInspect)

internal suspend fun runInspectSuspending(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyInspectResult = decodeInspect(runLeaseScriptSuspending(commands, INSPECT_SCRIPT, input, ownerToken))

internal fun runRenew(
    commands: RedisScriptingCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyRenewResult = decodeRenew(
    runLeaseScript(commands, RENEW_SCRIPT, input, ownerToken, requireTtl = true),
)

internal fun runRenewAsync(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): CompletableFuture<MultiKeyRenewResult> =
    runLeaseScriptAsync(commands, RENEW_SCRIPT, input, ownerToken, requireTtl = true).thenApply(::decodeRenew)

internal suspend fun runRenewSuspending(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyRenewResult = decodeRenew(
    runLeaseScriptSuspending(commands, RENEW_SCRIPT, input, ownerToken, requireTtl = true),
)

internal fun runRelease(
    commands: RedisScriptingCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyReleaseResult = decodeRelease(runLeaseScript(commands, RELEASE_SCRIPT, input, ownerToken))

internal fun runReleaseAsync(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): CompletableFuture<MultiKeyReleaseResult> =
    runLeaseScriptAsync(commands, RELEASE_SCRIPT, input, ownerToken).thenApply(::decodeRelease)

internal suspend fun runReleaseSuspending(
    commands: RedisScriptingAsyncCommands<String, String>,
    input: ValidatedLeaseInput,
    ownerToken: String,
): MultiKeyReleaseResult = decodeRelease(runLeaseScriptSuspending(commands, RELEASE_SCRIPT, input, ownerToken))

private fun runLeaseScript(
    commands: RedisScriptingCommands<String, String>,
    script: RedisScript,
    input: ValidatedLeaseInput,
    ownerToken: String,
    requireTtl: Boolean = false,
): List<Long> = RedisScriptRunner.run(
    commands,
    script,
    ScriptOutputType.MULTI,
    input.keys.toTypedArray(),
    *scriptArguments(input, ownerToken, requireTtl),
)

private fun runLeaseScriptAsync(
    commands: RedisScriptingAsyncCommands<String, String>,
    script: RedisScript,
    input: ValidatedLeaseInput,
    ownerToken: String,
    requireTtl: Boolean = false,
): CompletableFuture<List<Long>> = RedisScriptRunner.runAsync(
    commands,
    script,
    ScriptOutputType.MULTI,
    input.keys.toTypedArray(),
    *scriptArguments(input, ownerToken, requireTtl),
)

private suspend fun runLeaseScriptSuspending(
    commands: RedisScriptingAsyncCommands<String, String>,
    script: RedisScript,
    input: ValidatedLeaseInput,
    ownerToken: String,
    requireTtl: Boolean = false,
): List<Long> = RedisScriptRunner.runSuspending(
    commands,
    script,
    ScriptOutputType.MULTI,
    input.keys.toTypedArray(),
    *scriptArguments(input, ownerToken, requireTtl),
)

private fun scriptArguments(
    input: ValidatedLeaseInput,
    ownerToken: String,
    requireTtl: Boolean,
): Array<String> = if (requireTtl) {
    arrayOf(ownerToken, checkNotNull(input.ttlMillis) { "Validated lease input requires a TTL." }.toString())
} else {
    arrayOf(ownerToken)
}

internal fun decodeAcquire(vector: List<Long>): MultiKeyAcquireResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.ACQUIRE, ttlStatus = 11)
    return when (decoded.status) {
        10 -> decoded.requireShape(decoded.allMissing) { MultiKeyAcquireResult.Acquired }
        11 -> decoded.requireShape(decoded.allOwned) { MultiKeyAcquireResult.AlreadyOwned(decoded.minimumPttlMillis) }
        12 -> decoded.requireShape(decoded.partialWithoutMismatch) {
            MultiKeyAcquireResult.PartialOwnership(decoded.counts)
        }
        13 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyAcquireResult.Conflicted(decoded.counts) }
        90 -> decoded.throwIntegrity()
        else -> invalidVector()
    }
}

internal fun decodeInspect(vector: List<Long>): MultiKeyInspectResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.INSPECT, ttlStatus = 20)
    return when (decoded.status) {
        20 -> decoded.requireShape(decoded.allOwned) { MultiKeyInspectResult.Owned(decoded.minimumPttlMillis) }
        21 -> decoded.requireShape(decoded.allMissing) { MultiKeyInspectResult.Lost }
        22 -> decoded.requireShape(decoded.partialWithoutMismatch) {
            MultiKeyInspectResult.PartialOwnership(decoded.counts)
        }
        23 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyInspectResult.Conflicted(decoded.counts) }
        90 -> decoded.throwIntegrity()
        else -> invalidVector()
    }
}

internal fun decodeRenew(vector: List<Long>): MultiKeyRenewResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.RENEW)
    return when (decoded.status) {
        40 -> decoded.requireShape(decoded.allOwned) { MultiKeyRenewResult.Renewed }
        41 -> decoded.requireShape(decoded.partialWithoutMismatch) { MultiKeyRenewResult.PartialLoss(decoded.counts) }
        42 -> decoded.requireShape(decoded.allMissing) { MultiKeyRenewResult.Lost }
        43 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyRenewResult.OwnershipMismatch(decoded.counts) }
        90 -> decoded.throwIntegrity()
        else -> invalidVector()
    }
}

internal fun decodeRelease(vector: List<Long>): MultiKeyReleaseResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.RELEASE)
    return when (decoded.status) {
        50 -> decoded.requireShape(decoded.allOwned) { MultiKeyReleaseResult.Released }
        51 -> decoded.requireShape(decoded.partialWithoutMismatch) { MultiKeyReleaseResult.PartialRelease(decoded.counts) }
        52 -> decoded.requireShape(decoded.allMissing) { MultiKeyReleaseResult.Lost }
        53 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyReleaseResult.OwnershipMismatch(decoded.counts) }
        else -> invalidVector()
    }
}

private fun snapshotKeys(keys: Collection<String>, maxKeys: Int): List<String> {
    val snapshot = ArrayList<String>(minOf(maxKeys, 32))
    val distinctKeys = HashSet<String>(minOf(maxKeys, 32))
    val iterator = keys.iterator()
    require(iterator.hasNext()) { "keys must not be empty." }
    while (iterator.hasNext()) {
        require(snapshot.size < maxKeys) { "keys exceed the configured limit." }
        val key = iterator.next()
        require(key.isNotBlank()) { "keys must not contain blank values." }
        require(distinctKeys.add(key)) { "keys must not contain duplicates." }
        snapshot += key
    }
    return snapshot
}

private fun requireSameSlot(keys: List<String>, codec: RedisCodec<String, String>) {
    val slots = HashSet<Int>()
    keys.forEach { key -> slots += SlotHash.getSlot(codec.encodeKey(key)) }
    if (slots.size != 1) {
        throw MultiKeyLeaseCrossSlotException(slots.size)
    }
}

private fun Duration.requirePositiveMillis(): Long {
    val millis = toMillis()
    require(millis > 0) { "ttl must be at least one millisecond." }
    return millis
}

private data class DecodedVector(
    val status: Int,
    val operation: MultiKeyLeaseOperation,
    val counts: MultiKeyLeaseCounts,
    val invalidTtl: Int,
    val minimumPttlMillis: Long,
) {
    val allOwned: Boolean get() = counts.ownedKeys == counts.requestedKeys
    val allMissing: Boolean get() = counts.missingKeys == counts.requestedKeys
    val partialWithoutMismatch: Boolean
        get() = counts.ownedKeys > 0 && counts.missingKeys > 0 && counts.mismatchedKeys == 0
    val hasMismatch: Boolean get() = counts.mismatchedKeys > 0

    inline fun <T> requireShape(condition: Boolean, result: () -> T): T {
        if (!condition || invalidTtl != 0) invalidVector()
        return result()
    }

    fun throwIntegrity(): Nothing {
        if (invalidTtl <= 0) invalidVector()
        throw MultiKeyLeaseIntegrityException(operation, counts.requestedKeys, invalidTtl)
    }
}

private fun decodeVector(
    vector: List<Long>,
    operation: MultiKeyLeaseOperation,
    ttlStatus: Int? = null,
): DecodedVector {
    if (vector.size != VECTOR_SIZE) invalidVector()
    val status = vector[0].toIntExact()
    val requested = vector[1].toCount()
    val owned = vector[2].toCount()
    val missing = vector[3].toCount()
    val mismatched = vector[4].toCount()
    val invalidTtl = vector[5].toCount()
    val minimumPttl = vector[6]

    if (requested <= 0) invalidVector()
    val observed = owned.toLong() + missing.toLong() + mismatched.toLong()
    if (observed != requested.toLong() || invalidTtl > owned) invalidVector()
    if (status == ttlStatus) {
        if (minimumPttl < 1) invalidVector()
    } else if (minimumPttl != NO_PTTL) {
        invalidVector()
    }

    return DecodedVector(
        status,
        operation,
        MultiKeyLeaseCounts(requested, owned, missing, mismatched),
        invalidTtl,
        minimumPttl,
    )
}

private fun Long.toCount(): Int {
    if (this < 0 || this > Int.MAX_VALUE) invalidVector()
    return toInt()
}

private fun Long.toIntExact(): Int {
    if (this < Int.MIN_VALUE || this > Int.MAX_VALUE) invalidVector()
    return toInt()
}

private fun invalidVector(): Nothing = throw IllegalStateException("Malformed multi-key lease response.")

private val ACQUIRE_SCRIPT = RedisScript(
    """
    local token = ARGV[1]
    local ttl = tonumber(ARGV[2])
    if not ttl or ttl <= 0 then error('invalid lease ttl') end

    local requested = #KEYS
    local owned, missing, mismatched, invalidTtl, minimumPttl = 0, 0, 0, 0, -1
    for _, key in ipairs(KEYS) do
      local value = redis.call('GET', key)
      if not value then
        missing = missing + 1
      elseif value == token then
        owned = owned + 1
        local pttl = redis.call('PTTL', key)
        if pttl < 0 then
          invalidTtl = invalidTtl + 1
        elseif minimumPttl < 0 or pttl < minimumPttl then
          minimumPttl = pttl
        end
      else
        mismatched = mismatched + 1
      end
    end

    if invalidTtl > 0 then return {90, requested, owned, missing, mismatched, invalidTtl, -1} end
    if mismatched > 0 then return {13, requested, owned, missing, mismatched, 0, -1} end
    if owned == requested then return {11, requested, owned, missing, mismatched, 0, minimumPttl} end
    if owned > 0 then return {12, requested, owned, missing, mismatched, 0, -1} end

    for _, key in ipairs(KEYS) do
      redis.call('SET', key, token, 'PX', ttl)
    end
    return {10, requested, owned, missing, mismatched, 0, -1}
    """.trimIndent(),
)

private val INSPECT_SCRIPT = RedisScript(
    """
    local token = ARGV[1]
    local requested = #KEYS
    local owned, missing, mismatched, invalidTtl, minimumPttl = 0, 0, 0, 0, -1
    for _, key in ipairs(KEYS) do
      local value = redis.call('GET', key)
      if not value then
        missing = missing + 1
      elseif value == token then
        owned = owned + 1
        local pttl = redis.call('PTTL', key)
        if pttl < 0 then
          invalidTtl = invalidTtl + 1
        elseif minimumPttl < 0 or pttl < minimumPttl then
          minimumPttl = pttl
        end
      else
        mismatched = mismatched + 1
      end
    end

    if invalidTtl > 0 then return {90, requested, owned, missing, mismatched, invalidTtl, -1} end
    if mismatched > 0 then return {23, requested, owned, missing, mismatched, 0, -1} end
    if owned == requested then return {20, requested, owned, missing, mismatched, 0, minimumPttl} end
    if owned > 0 then return {22, requested, owned, missing, mismatched, 0, -1} end
    return {21, requested, owned, missing, mismatched, 0, -1}
    """.trimIndent(),
)

private val RENEW_SCRIPT = RedisScript(
    """
    local token = ARGV[1]
    local ttl = tonumber(ARGV[2])
    if not ttl or ttl <= 0 then error('invalid lease ttl') end

    local requested = #KEYS
    local owned, missing, mismatched, invalidTtl = 0, 0, 0, 0
    local ownedKeys = {}
    for _, key in ipairs(KEYS) do
      local value = redis.call('GET', key)
      if not value then
        missing = missing + 1
      elseif value == token then
        owned = owned + 1
        ownedKeys[#ownedKeys + 1] = key
        if redis.call('PTTL', key) < 0 then invalidTtl = invalidTtl + 1 end
      else
        mismatched = mismatched + 1
      end
    end

    if invalidTtl > 0 then return {90, requested, owned, missing, mismatched, invalidTtl, -1} end
    for _, key in ipairs(ownedKeys) do redis.call('PEXPIRE', key, ttl) end

    if mismatched > 0 then return {43, requested, owned, missing, mismatched, 0, -1} end
    if owned == requested then return {40, requested, owned, missing, mismatched, 0, -1} end
    if owned > 0 then return {41, requested, owned, missing, mismatched, 0, -1} end
    return {42, requested, owned, missing, mismatched, 0, -1}
    """.trimIndent(),
)

private val RELEASE_SCRIPT = RedisScript(
    """
    local token = ARGV[1]
    local requested = #KEYS
    local owned, missing, mismatched = 0, 0, 0
    local ownedKeys = {}
    for _, key in ipairs(KEYS) do
      local value = redis.call('GET', key)
      if not value then
        missing = missing + 1
      elseif value == token then
        owned = owned + 1
        ownedKeys[#ownedKeys + 1] = key
      else
        mismatched = mismatched + 1
      end
    end

    for _, key in ipairs(ownedKeys) do redis.call('DEL', key) end

    if mismatched > 0 then return {53, requested, owned, missing, mismatched, 0, -1} end
    if owned == requested then return {50, requested, owned, missing, mismatched, 0, -1} end
    if owned > 0 then return {51, requested, owned, missing, mismatched, 0, -1} end
    return {52, requested, owned, missing, mismatched, 0, -1}
    """.trimIndent(),
)

private const val VECTOR_SIZE = 7
private const val NO_PTTL = -1L
