package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class LettuceMultiKeyLeaseScriptTest : AbstractLettuceTest() {

    private lateinit var commands: RedisCommands<String, String>
    private lateinit var keys: List<String>
    private lateinit var token: String
    private val touchedKeys = mutableSetOf<String>()

    @BeforeEach
    fun setUp() {
        commands = connection.sync()
        val tag = randomName()
        keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
        token = "owner-${randomName()}"
        touchedKeys += keys
    }

    @AfterEach
    fun tearDown() {
        commands.del(*touchedKeys.toTypedArray())
        touchedKeys.clear()
    }

    @Test
    fun `acquire creates every missing key and replay does not extend ttl`() {
        acquire(keys, token, 10_000) shouldBeEqualTo MultiKeyAcquireResult.Acquired
        keys.forEach { key -> commands.get(key) shouldBeEqualTo token }

        val before = (inspect(keys, token) as MultiKeyInspectResult.Owned).minimumPttlMillis
        val replay = acquire(keys, token, 20_000) as MultiKeyAcquireResult.AlreadyOwned
        val after = (inspect(keys, token) as MultiKeyInspectResult.Owned).minimumPttlMillis

        replay.minimumPttlMillis shouldBeLessOrEqualTo before
        after shouldBeLessOrEqualTo before
        after shouldBeGreaterOrEqualTo before - TTL_TOLERANCE_MILLIS
    }

    @Test
    fun `acquire reports partial ownership and never writes on conflicts`() {
        commands.psetex(keys[0], 5_000, token)
        acquire(keys, token, 5_000) shouldBeEqualTo
            MultiKeyAcquireResult.PartialOwnership(counts(2, 1, 1, 0))
        commands.get(keys[1]).shouldBeNull()

        commands.del(*keys.toTypedArray())
        commands.psetex(keys[0], 5_000, "other-owner")
        acquire(keys, token, 5_000) shouldBeEqualTo
            MultiKeyAcquireResult.Conflicted(counts(2, 0, 1, 1))
        commands.get(keys[1]).shouldBeNull()

        val sameSlotThird = keys[0].substringBeforeLast(':') + ":three"
        touchedKeys += sameSlotThird
        commands.del(*keys.toTypedArray(), sameSlotThird)
        commands.psetex(keys[0], 5_000, token)
        commands.psetex(sameSlotThird, 5_000, "other-owner")
        acquire(listOf(keys[0], keys[1], sameSlotThird), token, 5_000) shouldBeEqualTo
            MultiKeyAcquireResult.Conflicted(counts(3, 1, 1, 1))
        commands.get(keys[1]).shouldBeNull()
        commands.get(sameSlotThird) shouldBeEqualTo "other-owner"
    }

    @Test
    fun `acquire and inspect reject persistent same-token keys without writes`() {
        commands.set(keys[0], token)
        commands.psetex(keys[1], 5_000, token)
        val before = commands.pttl(keys[1])

        val acquireFailure = assertFailsWith<MultiKeyLeaseIntegrityException> {
            acquire(keys, token, 20_000)
        }
        val inspectFailure = assertFailsWith<MultiKeyLeaseIntegrityException> {
            inspect(keys, token)
        }

        acquireFailure.operation shouldBeEqualTo MultiKeyLeaseOperation.ACQUIRE
        inspectFailure.operation shouldBeEqualTo MultiKeyLeaseOperation.INSPECT
        commands.pttl(keys[0]) shouldBeEqualTo -1L
        commands.pttl(keys[1]) shouldBeLessOrEqualTo before
        commands.get(keys[0]) shouldBeEqualTo token
        commands.get(keys[1]) shouldBeEqualTo token
    }

    @Test
    fun `inspect distinguishes owned partial lost and conflicted observations`() {
        inspect(keys, token) shouldBeEqualTo MultiKeyInspectResult.Lost

        commands.psetex(keys[0], 5_000, token)
        inspect(keys, token) shouldBeEqualTo MultiKeyInspectResult.PartialOwnership(counts(2, 1, 1, 0))

        commands.psetex(keys[1], 5_000, token)
        val owned = inspect(keys, token) as MultiKeyInspectResult.Owned
        owned.minimumPttlMillis shouldBeGreaterOrEqualTo 1L

        commands.psetex(keys[1], 5_000, "other-owner")
        inspect(keys, token) shouldBeEqualTo MultiKeyInspectResult.Conflicted(counts(2, 1, 0, 1))
    }

    @Test
    fun `renew covers full partial lost mismatch and zero-owned mismatch states`() {
        keys.forEach { key -> commands.psetex(key, 5_000, token) }
        renew(keys, token, 20_000) shouldBeEqualTo MultiKeyRenewResult.Renewed
        keys.forEach { key -> commands.pttl(key) shouldBeGreaterOrEqualTo 15_000L }

        commands.del(keys[1])
        renew(keys, token, 20_000) shouldBeEqualTo MultiKeyRenewResult.PartialLoss(counts(2, 1, 1, 0))
        commands.del(keys[0])
        renew(keys, token, 20_000) shouldBeEqualTo MultiKeyRenewResult.Lost

        commands.psetex(keys[0], 5_000, token)
        commands.psetex(keys[1], 5_000, "other-owner")
        renew(keys, token, 20_000) shouldBeEqualTo
            MultiKeyRenewResult.OwnershipMismatch(counts(2, 1, 0, 1))
        commands.pttl(keys[0]) shouldBeGreaterOrEqualTo 15_000L
        commands.get(keys[1]) shouldBeEqualTo "other-owner"
        commands.pttl(keys[1]) shouldBeLessOrEqualTo 5_000L

        commands.del(keys[0])
        renew(keys, token, 20_000) shouldBeEqualTo
            MultiKeyRenewResult.OwnershipMismatch(counts(2, 0, 1, 1))
        commands.get(keys[1]) shouldBeEqualTo "other-owner"
    }

    @Test
    fun `renew rejects persistent same-token keys before changing any ttl`() {
        commands.set(keys[0], token)
        commands.psetex(keys[1], 5_000, token)
        val before = commands.pttl(keys[1])

        val failure = assertFailsWith<MultiKeyLeaseIntegrityException> {
            renew(keys, token, 20_000)
        }

        failure.operation shouldBeEqualTo MultiKeyLeaseOperation.RENEW
        commands.pttl(keys[0]) shouldBeEqualTo -1L
        commands.pttl(keys[1]) shouldBeLessOrEqualTo before
    }

    @Test
    fun `release covers full partial lost mismatch and zero-owned mismatch states`() {
        keys.forEach { key -> commands.psetex(key, 5_000, token) }
        release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released
        commands.exists(*keys.toTypedArray()) shouldBeEqualTo 0L

        commands.psetex(keys[0], 5_000, token)
        release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.PartialRelease(counts(2, 1, 1, 0))
        release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Lost

        commands.psetex(keys[0], 5_000, token)
        commands.psetex(keys[1], 5_000, "other-owner")
        release(keys, token) shouldBeEqualTo
            MultiKeyReleaseResult.OwnershipMismatch(counts(2, 1, 0, 1))
        commands.get(keys[0]).shouldBeNull()
        commands.get(keys[1]) shouldBeEqualTo "other-owner"

        release(keys, token) shouldBeEqualTo
            MultiKeyReleaseResult.OwnershipMismatch(counts(2, 0, 1, 1))
        commands.get(keys[1]) shouldBeEqualTo "other-owner"
    }

    @Test
    fun `release cleans persistent same-token keys`() {
        commands.set(keys[0], token)
        commands.psetex(keys[1], 5_000, token)

        release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released
        commands.exists(*keys.toTypedArray()) shouldBeEqualTo 0L
    }

    @Test
    fun `runtime script failure is recoverable without exposing keys or tokens`() {
        val secretKey = "secret:{${randomName()}}:one"
        val secondSecretKey = secretKey.substringBeforeLast(':') + ":two"
        val secretToken = "secret-owner-${randomName()}"
        val secretKeys = listOf(secretKey, secondSecretKey)
        touchedKeys += secretKeys
        commands.del(*secretKeys.toTypedArray())

        val failure = assertFailsWith<RuntimeException> {
            RedisScriptRunner.run<Any>(
                commands,
                injectedFailureScript,
                ScriptOutputType.MULTI,
                secretKeys.toTypedArray(),
                secretToken,
                "5000",
            )
        }
        val observed = inspect(secretKeys, secretToken)
        val released = release(secretKeys, secretToken)

        observed shouldBeEqualTo MultiKeyInspectResult.PartialOwnership(counts(2, 1, 1, 0))
        released shouldBeEqualTo MultiKeyReleaseResult.PartialRelease(counts(2, 1, 1, 0))
        commands.exists(*secretKeys.toTypedArray()) shouldBeEqualTo 0L
        assertSecretsAbsent(listOf(failure, observed, released), secretKey, secondSecretKey, secretToken)
    }

    @Test
    fun `redaction assertion inspects public properties on nested causes`() {
        val secret = "nested-cause-property-secret"
        val failure = RuntimeException("outer failure", SecretPropertyException(secret))

        assertFailsWith<AssertionError> {
            assertSecretsAbsent(listOf(failure), secret)
        }
    }

    @Test
    fun `redaction assertion fails closed when a public getter throws`() {
        val failure = ThrowingPropertyException()

        val error = assertFailsWith<Exception> {
            assertSecretsAbsent(listOf(failure), "not-present")
        }
        generateSequence<Throwable>(error) { current -> current.cause }
            .any { current -> current.message == "getter failed" }.shouldBeTrue()
    }

    private fun acquire(targetKeys: List<String>, ownerToken: String, ttlMillis: Long): MultiKeyAcquireResult =
        runAcquire(commands, ValidatedLeaseInput(targetKeys, ttlMillis), ownerToken)

    private fun inspect(targetKeys: List<String>, ownerToken: String): MultiKeyInspectResult =
        runInspect(commands, ValidatedLeaseInput(targetKeys, null), ownerToken)

    private fun renew(targetKeys: List<String>, ownerToken: String, ttlMillis: Long): MultiKeyRenewResult =
        runRenew(commands, ValidatedLeaseInput(targetKeys, ttlMillis), ownerToken)

    private fun release(targetKeys: List<String>, ownerToken: String): MultiKeyReleaseResult =
        runRelease(commands, ValidatedLeaseInput(targetKeys, null), ownerToken)

    private fun counts(requested: Int, owned: Int, missing: Int, mismatched: Int) =
        MultiKeyLeaseCounts(requested, owned, missing, mismatched)

    private fun assertSecretsAbsent(observables: List<Any>, vararg secrets: String) {
        val causeChain = observables.filterIsInstance<Throwable>()
            .flatMap { failure -> generateSequence(failure) { current -> current.cause }.toList() }
        val fullObservableSurface = buildList {
            addAll(observables)
            causeChain.forEach { cause ->
                if (none { observable -> observable === cause }) add(cause)
            }
        }
        val publicPropertyValues = fullObservableSurface.flatMap { observable ->
            observable::class.memberProperties
                .filter { property -> property.visibility == KVisibility.PUBLIC }
                .map { property -> property.getter.call(observable) }
        }
        val surface = buildList {
            addAll(fullObservableSurface.map(Any::toString))
            addAll(publicPropertyValues.map { value -> value.toString() })
            causeChain.forEach { failure ->
                add(failure.message.orEmpty())
                add(failure.toString())
            }
        }.joinToString("\n")
        secrets.forEach { secret -> surface.contains(secret).shouldBeFalse() }
    }

    private companion object {
        const val TTL_TOLERANCE_MILLIS = 1_000L

        val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }

        val injectedFailureScript = RedisScript(
            """
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            error('injected')
            """.trimIndent(),
        )
    }

    class SecretPropertyException(
        val publicSecret: String,
    ) : RuntimeException()

    class ThrowingPropertyException : RuntimeException() {
        val inaccessibleSurface: String
            get() = error("getter failed")
    }
}
