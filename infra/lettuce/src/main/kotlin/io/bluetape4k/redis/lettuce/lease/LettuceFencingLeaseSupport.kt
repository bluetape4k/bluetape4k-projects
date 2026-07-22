package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.support.requirePositiveNumber
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.HexFormat
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

internal data class FencingLeaseKeys(
    val lease: String,
    val counter: String,
)

internal enum class FencingLeaseOperation {
    BOOTSTRAP,
    ACQUIRE,
    INSPECT,
    RENEW,
    RELEASE,
}

internal class FencingLeaseProtocolException: IllegalStateException(
    "Malformed fencing lease response.",
) {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal object FencingLeaseSupportLogger: KLogging() {
    fun backendFailure(
        operation: FencingLeaseOperation,
        failure: FencingLeaseBackendFailure,
        exceptionClassName: String,
        domainFingerprint: String?,
    ) {
        val domain = domainFingerprint ?: "unavailable"
        log.warn {
            "Fencing lease backend failure operation=$operation kind=${failure.kind} " +
                "exception=$exceptionClassName domain=$domain"
        }
    }
}

internal fun deriveFencingLeaseKeys(
    config: LettuceFencingLeaseConfig,
    codec: RedisCodec<String, String>,
): FencingLeaseKeys {
    val tag = "${config.namespace}:${config.resourceName}"
    val prefix = "fence:{$tag}:${config.epoch}"
    val keys = FencingLeaseKeys("$prefix:lease", "$prefix:counter")
    require(SlotHash.getSlot(codec.encodeKey(keys.lease)) == SlotHash.getSlot(codec.encodeKey(keys.counter))) {
        "Derived fencing lease keys must share one Redis Cluster slot."
    }
    return keys
}

internal fun requireCanonicalFencingDecimal(value: String): String {
    val canonical = value == "0" ||
        value.firstOrNull() in '1'..'9' && value.all { character -> character in '0'..'9' }
    require(canonical) { "Invalid decimal value." }
    require(value.length <= MAX_LONG_DECIMAL.length) { "Decimal value is out of range." }
    require(compareCanonicalFencingDecimalsWithoutValidation(value, MAX_LONG_DECIMAL) <= 0) {
        "Decimal value is out of range."
    }
    return value
}

internal fun compareCanonicalFencingDecimals(left: String, right: String): Int {
    requireCanonicalFencingDecimal(left)
    requireCanonicalFencingDecimal(right)
    return compareCanonicalFencingDecimalsWithoutValidation(left, right)
}

internal fun parseCanonicalFencingLong(value: String): Long = requireCanonicalFencingDecimal(value).toLong()

internal fun Duration.requireFencingLeaseMillis(): Long {
    val millis = try {
        toMillis()
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException(INVALID_LEASE_TIME_MESSAGE)
    }
    require(millis > 0 && this == Duration.ofMillis(millis)) { INVALID_LEASE_TIME_MESSAGE }
    return millis
}

internal fun Long.requireRedisFencingLeaseTimeMillis(): Long {
    requirePositiveNumber("leaseTimeMillis")
    require(this <= MAX_EXACT_REDIS_LEASE_TIME_MILLIS) {
        "leaseTimeMillis exceeds the exact Redis Lua integer range."
    }
    return this
}

internal fun requireFencingTokenEpoch(
    config: LettuceFencingLeaseConfig,
    token: FencingToken,
): FencingToken {
    require(token.epoch == config.epoch) {
        "Fencing token epoch must match the configured ordering domain."
    }
    return token
}

internal fun Throwable.unwrapFencingCompletionCause(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(MAX_COMPLETION_DEPTH) {
        if (current is CancellationException) {
            throw current
        }
        if (!seen.add(current)) {
            return current
        }
        val next = when (current) {
            is CompletionException,
            is ExecutionException,
            -> current.cause
            else -> null
        }
        if (next == null || next === current) {
            return current
        }
        current = next
    }
    if (current is CancellationException) {
        throw current
    }
    return current
}

internal fun classifyFencingBackendFailure(
    operation: FencingLeaseOperation,
    error: Throwable,
    domainFingerprint: String? = null,
): FencingLeaseBackendFailure {
    val cause = error.unwrapFencingCompletionCause()
    val kind = when (cause) {
        is RedisConnectionException -> FencingBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException,
        is TimeoutException,
        -> FencingBackendFailureKind.TIMEOUT
        is RedisException -> FencingBackendFailureKind.COMMAND
        else -> throw cause
    }
    return FencingLeaseBackendFailure(kind).also { failure ->
        FencingLeaseSupportLogger.backendFailure(
            operation,
            failure,
            cause.javaClass.name,
            domainFingerprint,
        )
    }
}

internal fun LettuceFencingLeaseConfig.domainFingerprint(): String {
    val source = "$namespace\u0000$resourceName\u0000$epoch".toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(source)
    return HexFormat.of().formatHex(digest, 0, DOMAIN_FINGERPRINT_BYTES)
}

internal fun decodeFencingBootstrap(frame: List<String>): FencingBootstrapResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingBootstrapResult.IntegrityFailure(failure)
    }
    decoded.requireUnusedValues()
    return when (decoded.status) {
        "INITIALIZED" -> FencingBootstrapResult.Initialized
        "ALREADY_INITIALIZED" -> FencingBootstrapResult.AlreadyInitialized
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingAcquire(frame: List<String>): FencingAcquireResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingAcquireResult.IntegrityFailure(failure)
    }
    return when (decoded.status) {
        "ACQUIRED" -> {
            decoded.requireTtlSentinel()
            FencingAcquireResult.Acquired(decoded.token())
        }
        "ALREADY_OWNED" -> FencingAcquireResult.AlreadyOwned(decoded.token(), decoded.ttl())
        "CONTENDED" -> {
            decoded.requireUnusedToken()
            FencingAcquireResult.Contended(decoded.ttl())
        }
        "COUNTER_UNAVAILABLE" -> decoded.withUnusedValues { FencingAcquireResult.CounterUnavailable }
        "SEQUENCE_EXHAUSTED" -> decoded.withUnusedValues { FencingAcquireResult.SequenceExhausted }
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingInspect(frame: List<String>): FencingInspectResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingInspectResult.IntegrityFailure(failure)
    }
    return when (decoded.status) {
        "OWNED" -> FencingInspectResult.Owned(decoded.token(), decoded.ttl())
        "LOST" -> decoded.withUnusedValues { FencingInspectResult.Lost }
        "CONTENDED" -> {
            decoded.requireUnusedToken()
            FencingInspectResult.Contended(decoded.ttl())
        }
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingRenew(frame: List<String>): FencingRenewResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingRenewResult.IntegrityFailure(failure)
    }
    decoded.requireUnusedValues()
    return when (decoded.status) {
        "RENEWED" -> FencingRenewResult.Renewed
        "LOST" -> FencingRenewResult.Lost
        "OWNERSHIP_MISMATCH" -> FencingRenewResult.OwnershipMismatch
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingRelease(frame: List<String>): FencingReleaseResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingReleaseResult.IntegrityFailure(failure)
    }
    decoded.requireUnusedValues()
    return when (decoded.status) {
        "RELEASED" -> FencingReleaseResult.Released
        "LOST" -> FencingReleaseResult.Lost
        "OWNERSHIP_MISMATCH" -> FencingReleaseResult.OwnershipMismatch
        else -> malformedFencingReply()
    }
}

internal fun runFencingBootstrap(
    commands: RedisScriptingCommands<String, String>,
    keys: FencingLeaseKeys,
    config: LettuceFencingLeaseConfig,
): FencingBootstrapResult = decodeFencingBootstrap(
    runFencingScript(commands, FencingLeaseScripts.BOOTSTRAP, keys, config.epoch.toString()),
)

internal fun runFencingAcquire(
    commands: RedisScriptingCommands<String, String>,
    keys: FencingLeaseKeys,
    config: LettuceFencingLeaseConfig,
    ownerId: FencingOwnerId,
    leaseTimeMillis: Long,
): FencingAcquireResult {
    leaseTimeMillis.requireRedisFencingLeaseTimeMillis()
    return decodeFencingAcquire(
        runFencingScript(
            commands,
            FencingLeaseScripts.ACQUIRE,
            keys,
            ownerId.value,
            config.epoch.toString(),
            leaseTimeMillis.toString(),
        ),
    )
}

internal fun runFencingInspect(
    commands: RedisScriptingCommands<String, String>,
    keys: FencingLeaseKeys,
    config: LettuceFencingLeaseConfig,
    ownerId: FencingOwnerId,
): FencingInspectResult = decodeFencingInspect(
    runFencingScript(
        commands,
        FencingLeaseScripts.INSPECT,
        keys,
        ownerId.value,
        config.epoch.toString(),
    ),
)

internal fun runFencingRenew(
    commands: RedisScriptingCommands<String, String>,
    keys: FencingLeaseKeys,
    config: LettuceFencingLeaseConfig,
    ownerId: FencingOwnerId,
    token: FencingToken,
    leaseTimeMillis: Long,
): FencingRenewResult {
    requireFencingTokenEpoch(config, token)
    leaseTimeMillis.requireRedisFencingLeaseTimeMillis()
    return decodeFencingRenew(
        runFencingScript(
            commands,
            FencingLeaseScripts.RENEW,
            keys,
            ownerId.value,
            token.epoch.toString(),
            token.sequence.toString(),
            leaseTimeMillis.toString(),
        ),
    )
}

internal fun runFencingRelease(
    commands: RedisScriptingCommands<String, String>,
    keys: FencingLeaseKeys,
    config: LettuceFencingLeaseConfig,
    ownerId: FencingOwnerId,
    token: FencingToken,
): FencingReleaseResult {
    requireFencingTokenEpoch(config, token)
    return decodeFencingRelease(
        runFencingScript(
            commands,
            FencingLeaseScripts.RELEASE,
            keys,
            ownerId.value,
            token.epoch.toString(),
            token.sequence.toString(),
        ),
    )
}

private fun runFencingScript(
    commands: RedisScriptingCommands<String, String>,
    script: RedisScript,
    keys: FencingLeaseKeys,
    vararg arguments: String,
): List<String> = RedisScriptRunner.run(
    commands,
    script,
    ScriptOutputType.MULTI,
    arrayOf(keys.lease, keys.counter),
    *arguments,
)

private data class DecodedFencingFrame(
    val status: String,
    val value1: String,
    val value2: String,
    val ttl: String,
) {
    fun token(): FencingToken = decodeFencingField {
        FencingToken(
            parseCanonicalFencingLong(value1),
            parseCanonicalFencingLong(value2),
        )
    }

    fun ttl(): Long = decodeFencingField { parseCanonicalFencingLong(ttl) }

    fun integrityFailureOrNull(): FencingLeaseIntegrityFailure? {
        if (status != "INTEGRITY_FAILURE") {
            return null
        }
        if (value2 != UNUSED_VALUE || ttl != UNUSED_TTL) {
            malformedFencingReply()
        }
        val kind = when (value1) {
            "MALFORMED_LEASE" -> FencingIntegrityFailureKind.MALFORMED_LEASE
            "INVALID_COUNTER" -> FencingIntegrityFailureKind.INVALID_COUNTER
            "COUNTER_BEHIND_LEASE" -> FencingIntegrityFailureKind.COUNTER_BEHIND_LEASE
            else -> malformedFencingReply()
        }
        return FencingLeaseIntegrityFailure(kind)
    }

    fun requireUnusedToken() {
        if (value1 != UNUSED_VALUE || value2 != UNUSED_VALUE) {
            malformedFencingReply()
        }
    }

    fun requireTtlSentinel() {
        if (ttl != UNUSED_TTL) {
            malformedFencingReply()
        }
    }

    fun requireUnusedValues() {
        requireUnusedToken()
        requireTtlSentinel()
    }

    inline fun <T> withUnusedValues(result: () -> T): T {
        requireUnusedValues()
        return result()
    }
}

private fun decodeFencingFrame(frame: List<String>): DecodedFencingFrame {
    if (frame.size != FENCING_FRAME_SIZE) {
        malformedFencingReply()
    }
    return DecodedFencingFrame(frame[0], frame[1], frame[2], frame[3])
}

private inline fun <T> decodeFencingField(block: () -> T): T = try {
    block()
} catch (_: IllegalArgumentException) {
    malformedFencingReply()
}

private fun compareCanonicalFencingDecimalsWithoutValidation(left: String, right: String): Int {
    val lengthComparison = left.length.compareTo(right.length)
    return if (lengthComparison != 0) lengthComparison else left.compareTo(right)
}

private fun malformedFencingReply(): Nothing = throw FencingLeaseProtocolException()

private const val FENCING_FRAME_SIZE = 4
private const val UNUSED_VALUE = "0"
private const val UNUSED_TTL = "-1"
private const val MAX_COMPLETION_DEPTH = 8
private const val DOMAIN_FINGERPRINT_BYTES = 12
private const val INVALID_LEASE_TIME_MESSAGE = "leaseTime must fit positive whole milliseconds."
internal const val MAX_EXACT_REDIS_LEASE_TIME_MILLIS = 9_007_199_254_740_991L
private val MAX_LONG_DECIMAL = Long.MAX_VALUE.toString()

internal object FencingLeaseScripts {
    val BOOTSTRAP = RedisScript(
        COMMON_FENCING_PREFLIGHT +
            """

            local lease = readLease(ARGV[1])
            if lease.failure then
              return integrityFailure(lease.failure)
            end
            if lease.present then
              return {'ALREADY_INITIALIZED', '0', '0', '-1'}
            end

            local counter = readCounter()
            if counter.failure then
              return integrityFailure(counter.failure)
            end
            if counter.present then
              return {'ALREADY_INITIALIZED', '0', '0', '-1'}
            end

            redis.call('SET', counterKey, '0')
            return {'INITIALIZED', '0', '0', '-1'}
            """.trimIndent(),
    )

    val ACQUIRE = RedisScript(
        COMMON_FENCING_PREFLIGHT +
            """

            if not isValidLeaseTime(ARGV[3]) then
              return {'INVALID_ARGUMENT', '0', '0', '-1'}
            end
            local lease = readLease(ARGV[2])
            if lease.failure then
              return integrityFailure(lease.failure)
            end
            if lease.present then
              if lease.owner == ARGV[1] then
                return {'ALREADY_OWNED', lease.epoch, lease.sequence, formatInteger(lease.ttl)}
              end
              return {'CONTENDED', '0', '0', formatInteger(lease.ttl)}
            end

            local counter = readCounter()
            if counter.failure then
              return integrityFailure(counter.failure)
            end
            if not counter.present then
              return {'COUNTER_UNAVAILABLE', '0', '0', '-1'}
            end
            if counter.text == MAX_LONG_DECIMAL then
              return {'SEQUENCE_EXHAUSTED', '0', '0', '-1'}
            end

            local nextSequence = redis.call('INCR', counterKey)
            local nextSequenceText = redis.call('GET', counterKey)
            redis.call('HSET', leaseKey,
              'owner', ARGV[1],
              'epoch', ARGV[2],
              'sequence', nextSequenceText)
            redis.call('PEXPIRE', leaseKey, ARGV[3])
            return {'ACQUIRED', ARGV[2], nextSequenceText, '-1'}
            """.trimIndent(),
    )

    val INSPECT = RedisScript(
        COMMON_FENCING_PREFLIGHT +
            """

            local lease = readLease(ARGV[2])
            if lease.failure then
              return integrityFailure(lease.failure)
            end
            if not lease.present then
              return {'LOST', '0', '0', '-1'}
            end
            if lease.owner == ARGV[1] then
              return {'OWNED', lease.epoch, lease.sequence, formatInteger(lease.ttl)}
            end
            return {'CONTENDED', '0', '0', formatInteger(lease.ttl)}
            """.trimIndent(),
    )

    val RENEW = RedisScript(
        COMMON_FENCING_PREFLIGHT +
            """

            if not isValidLeaseTime(ARGV[4]) then
              return {'INVALID_ARGUMENT', '0', '0', '-1'}
            end
            local lease = readLease(ARGV[2])
            if lease.failure then
              return integrityFailure(lease.failure)
            end
            if not lease.present then
              return {'LOST', '0', '0', '-1'}
            end
            if lease.owner ~= ARGV[1] or lease.epoch ~= ARGV[2] or lease.sequence ~= ARGV[3] then
              return {'OWNERSHIP_MISMATCH', '0', '0', '-1'}
            end

            redis.call('PEXPIRE', leaseKey, ARGV[4])
            return {'RENEWED', '0', '0', '-1'}
            """.trimIndent(),
    )

    val RELEASE = RedisScript(
        COMMON_FENCING_PREFLIGHT +
            """

            local lease = readLease(ARGV[2])
            if lease.failure then
              return integrityFailure(lease.failure)
            end
            if not lease.present then
              return {'LOST', '0', '0', '-1'}
            end
            if lease.owner ~= ARGV[1] or lease.epoch ~= ARGV[2] or lease.sequence ~= ARGV[3] then
              return {'OWNERSHIP_MISMATCH', '0', '0', '-1'}
            end

            redis.call('DEL', leaseKey)
            return {'RELEASED', '0', '0', '-1'}
            """.trimIndent(),
    )
}

private const val COMMON_FENCING_PREFLIGHT = """
local leaseKey = KEYS[1]
local counterKey = KEYS[2]
local MAX_LONG_DECIMAL = '9223372036854775807'
local MAX_EXACT_LEASE_TIME = '9007199254740991'

local function integrityFailure(kind)
  return {'INTEGRITY_FAILURE', kind, '0', '-1'}
end

local function isCanonicalDecimal(value)
  if value == '0' then
    return true
  end
  if #value == 0 or #value > #MAX_LONG_DECIMAL then
    return false
  end
  if string.match(value, '^[1-9][0-9]*$') == nil then
    return false
  end
  return #value < #MAX_LONG_DECIMAL or value <= MAX_LONG_DECIMAL
end

local function compareCanonicalDecimals(left, right)
  if #left ~= #right then
    return #left < #right and -1 or 1
  end
  if left == right then
    return 0
  end
  return left < right and -1 or 1
end

local function isValidLeaseTime(value)
  return isCanonicalDecimal(value) and value ~= '0' and
    compareCanonicalDecimals(value, MAX_EXACT_LEASE_TIME) <= 0
end

local function formatInteger(value)
  return string.format('%.0f', value)
end

local function readCounter()
  local counterType = redis.call('TYPE', counterKey).ok
  if counterType == 'none' then
    return {present = false}
  end
  if counterType ~= 'string' then
    return {failure = 'INVALID_COUNTER'}
  end

  local counterTtl = redis.call('PTTL', counterKey)
  if counterTtl ~= -1 then
    return {failure = 'INVALID_COUNTER'}
  end
  if redis.call('STRLEN', counterKey) > #MAX_LONG_DECIMAL then
    return {failure = 'INVALID_COUNTER'}
  end

  local counterText = redis.call('GET', counterKey)
  if not isCanonicalDecimal(counterText) then
    return {failure = 'INVALID_COUNTER'}
  end
  return {present = true, text = counterText}
end

local function readLease(expectedEpoch)
  local leaseType = redis.call('TYPE', leaseKey).ok
  if leaseType == 'none' then
    return {present = false}
  end
  if leaseType ~= 'hash' then
    return {failure = 'MALFORMED_LEASE'}
  end

  local leaseTtl = redis.call('PTTL', leaseKey)
  if leaseTtl == -2 then
    return {present = false}
  end
  if leaseTtl < 0 then
    return {failure = 'MALFORMED_LEASE'}
  end
  if redis.call('HLEN', leaseKey) ~= 3 then
    return {failure = 'MALFORMED_LEASE'}
  end

  local ownerLength = redis.call('HSTRLEN', leaseKey, 'owner')
  local epochLength = redis.call('HSTRLEN', leaseKey, 'epoch')
  local sequenceLength = redis.call('HSTRLEN', leaseKey, 'sequence')
  if ownerLength < 1 or ownerLength > 256 or
     epochLength < 1 or epochLength > #MAX_LONG_DECIMAL or
     sequenceLength < 1 or sequenceLength > #MAX_LONG_DECIMAL then
    return {failure = 'MALFORMED_LEASE'}
  end

  local fields = redis.call('HMGET', leaseKey, 'owner', 'epoch', 'sequence')
  local owner = fields[1]
  local epoch = fields[2]
  local sequence = fields[3]
  if not isCanonicalDecimal(epoch) or not isCanonicalDecimal(sequence) or
     epoch == '0' or sequence == '0' or epoch ~= expectedEpoch then
    return {failure = 'MALFORMED_LEASE'}
  end

  local counter = readCounter()
  if counter.failure then
    return {failure = counter.failure}
  end
  if not counter.present then
    return {failure = 'INVALID_COUNTER'}
  end
  if compareCanonicalDecimals(counter.text, sequence) < 0 then
    return {failure = 'COUNTER_BEHIND_LEASE'}
  end

  return {
    present = true,
    owner = owner,
    epoch = epoch,
    sequence = sequence,
    ttl = leaseTtl
  }
end
"""
