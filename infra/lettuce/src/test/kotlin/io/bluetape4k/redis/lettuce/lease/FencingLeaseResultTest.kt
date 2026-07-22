package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class FencingLeaseResultTest {

    @Test
    fun `all public results expose the documented values`() {
        FencingBootstrapResult.Initialized shouldBeSameInstanceAs FencingBootstrapResult.Initialized
        FencingBootstrapResult.AlreadyInitialized shouldBeSameInstanceAs FencingBootstrapResult.AlreadyInitialized

        FencingAcquireResult.Acquired(token).token shouldBeEqualTo token
        FencingAcquireResult.AlreadyOwned(token, 10).remainingTtlMillis shouldBeEqualTo 10L
        FencingAcquireResult.Contended(9).remainingTtlMillis shouldBeEqualTo 9L
        FencingAcquireResult.CounterUnavailable shouldBeSameInstanceAs FencingAcquireResult.CounterUnavailable
        FencingAcquireResult.SequenceExhausted shouldBeSameInstanceAs FencingAcquireResult.SequenceExhausted

        FencingInspectResult.Owned(token, 8).token shouldBeEqualTo token
        FencingInspectResult.Lost shouldBeSameInstanceAs FencingInspectResult.Lost
        FencingInspectResult.Contended(7).remainingTtlMillis shouldBeEqualTo 7L

        FencingRenewResult.Renewed shouldBeSameInstanceAs FencingRenewResult.Renewed
        FencingRenewResult.Lost shouldBeSameInstanceAs FencingRenewResult.Lost
        FencingRenewResult.OwnershipMismatch shouldBeSameInstanceAs FencingRenewResult.OwnershipMismatch

        FencingReleaseResult.Released shouldBeSameInstanceAs FencingReleaseResult.Released
        FencingReleaseResult.Lost shouldBeSameInstanceAs FencingReleaseResult.Lost
        FencingReleaseResult.OwnershipMismatch shouldBeSameInstanceAs FencingReleaseResult.OwnershipMismatch
    }

    @Test
    fun `ttl results reject negative values`() {
        assertFailsWith<IllegalArgumentException> { FencingAcquireResult.AlreadyOwned(token, -1) }
        assertFailsWith<IllegalArgumentException> { FencingAcquireResult.Contended(-1) }
        assertFailsWith<IllegalArgumentException> { FencingInspectResult.Owned(token, -1) }
        assertFailsWith<IllegalArgumentException> { FencingInspectResult.Contended(-1) }
    }

    @Test
    fun `all non-enum values and result variants have stable serialization contracts`() {
        serializableSamples.forEach { original ->
            val restored = javaRoundTrip(original)

            restored shouldBeEqualTo original
            restored.javaClass shouldBeSameInstanceAs original.javaClass
            ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        }
    }

    @Test
    fun `all sealed result contracts declare serial version uid`() {
        resultContracts.forEach { contract ->
            val companionClass = contract.java.declaredClasses.single { nested -> nested.simpleName == "Companion" }
            val serialVersionUid = companionClass.getDeclaredField("serialVersionUID").also { field ->
                field.isAccessible = true
            }

            serialVersionUid.getLong(null) shouldBeEqualTo 1L
        }
    }

    @Test
    fun `all result singleton variants preserve identity after serialization`() {
        singletonSamples.forEach { original ->
            javaRoundTrip(original) shouldBeSameInstanceAs original
        }
    }

    @Test
    fun `failure enums preserve identity after serialization`() {
        enumSamples.forEach { original ->
            javaRoundTrip(original) shouldBeSameInstanceAs original
        }
    }

    @Test
    fun `deserialization revalidates ttl result variants`() {
        val invalidSamples = listOf(
            FencingAcquireResult.AlreadyOwned(token, 1).withField("remainingTtlMillis", -1L) to
                "Invalid serialized FencingAcquireResult.AlreadyOwned.",
            FencingAcquireResult.Contended(1).withField("remainingTtlMillis", -1L) to
                "Invalid serialized FencingAcquireResult.Contended.",
            FencingInspectResult.Owned(token, 1).withField("remainingTtlMillis", -1L) to
                "Invalid serialized FencingInspectResult.Owned.",
            FencingInspectResult.Contended(1).withField("remainingTtlMillis", -1L) to
                "Invalid serialized FencingInspectResult.Contended.",
        )

        invalidSamples.forEach { (invalid, expectedMessage) ->
            val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }
            error.cause shouldBeEqualTo null
            error.message shouldBeEqualTo expectedMessage
        }
    }

    @Test
    fun `deserialization rejects null nested failure without leaking a sentinel`() {
        val invalidSamples = listOf(
            FencingBootstrapResult.IntegrityFailure(integrityFailure).withField("failure", null) to
                "Invalid serialized FencingBootstrapResult.IntegrityFailure.",
            FencingAcquireResult.BackendFailure(backendFailure).withField("failure", null) to
                "Invalid serialized FencingAcquireResult.BackendFailure.",
            FencingInspectResult.IntegrityFailure(integrityFailure).withField("failure", null) to
                "Invalid serialized FencingInspectResult.IntegrityFailure.",
            FencingRenewResult.BackendFailure(backendFailure).withField("failure", null) to
                "Invalid serialized FencingRenewResult.BackendFailure.",
            FencingReleaseResult.IntegrityFailure(integrityFailure).withField("failure", null) to
                "Invalid serialized FencingReleaseResult.IntegrityFailure.",
        )

        invalidSamples.forEach { (invalid, expectedMessage) ->
            val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }
            error.cause shouldBeEqualTo null
            error.message shouldBeEqualTo expectedMessage
        }
    }

    @Test
    fun `deserialization rejects null failure kind with a stable cause-free message`() {
        val invalidSamples = listOf(
            FencingLeaseBackendFailure(FencingBackendFailureKind.COMMAND).withField("kind", null) to
                "Invalid serialized FencingLeaseBackendFailure.",
            FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.MALFORMED_LEASE).withField("kind", null) to
                "Invalid serialized FencingLeaseIntegrityFailure.",
        )

        invalidSamples.forEach { (invalid, expectedMessage) ->
            val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }
            error.cause shouldBeEqualTo null
            error.message shouldBeEqualTo expectedMessage
        }
    }

    @Test
    fun `failure values expose no raw backend details`() {
        val forbiddenProperties = failureTypes
            .flatMap { type -> type.memberProperties }
            .filter { property ->
                property.visibility == KVisibility.PUBLIC && (
                    property.name.contains("key", ignoreCase = true) ||
                        property.name.contains("owner", ignoreCase = true) ||
                        property.name.contains("token", ignoreCase = true) ||
                        property.name.contains("raw", ignoreCase = true) ||
                        property.name.contains("message", ignoreCase = true) ||
                        Throwable::class.java.isAssignableFrom(property.returnType.classifier.let { it as? KClass<*> }?.java)
                    )
            }

        forbiddenProperties.shouldBeEmpty()
    }

    @Test
    fun `sealed result variants never expose key owner raw reply throwable or message properties`() {
        val forbiddenProperties = resultContracts
            .flatMap { contract -> contract.sealedSubclasses }
            .flatMap { subtype -> subtype.memberProperties }
            .filter { property ->
                property.visibility == KVisibility.PUBLIC && (
                    property.name.contains("key", ignoreCase = true) ||
                        property.name.contains("owner", ignoreCase = true) ||
                        property.name.contains("raw", ignoreCase = true) ||
                        property.name.contains("reply", ignoreCase = true) ||
                        property.name.contains("message", ignoreCase = true) ||
                        Throwable::class.java.isAssignableFrom(property.returnType.classifier.let { it as? KClass<*> }?.java)
                    )
            }

        forbiddenProperties.shouldBeEmpty()
    }

    private fun <T: Serializable> T.withField(name: String, value: Any?): T = apply {
        javaClass.getDeclaredField(name).also { field ->
            field.isAccessible = true
            field.set(this, value)
        }
    }

    private fun javaRoundTrip(original: Serializable): Any =
        ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { output -> output.writeObject(original) }
            ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { input -> input.readObject() }
        }

    private companion object {
        val token = FencingToken(7, 42)
        val backendFailure = FencingLeaseBackendFailure(FencingBackendFailureKind.TIMEOUT)
        val integrityFailure = FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.INVALID_COUNTER)

        val singletonSamples: List<Serializable> = listOf(
            FencingBootstrapResult.Initialized,
            FencingBootstrapResult.AlreadyInitialized,
            FencingAcquireResult.CounterUnavailable,
            FencingAcquireResult.SequenceExhausted,
            FencingInspectResult.Lost,
            FencingRenewResult.Renewed,
            FencingRenewResult.Lost,
            FencingRenewResult.OwnershipMismatch,
            FencingReleaseResult.Released,
            FencingReleaseResult.Lost,
            FencingReleaseResult.OwnershipMismatch,
        )

        val serializableSamples: List<Serializable> = listOf(
            backendFailure,
            integrityFailure,
            *singletonSamples.toTypedArray(),
            FencingBootstrapResult.IntegrityFailure(integrityFailure),
            FencingBootstrapResult.BackendFailure(backendFailure),
            FencingAcquireResult.Acquired(token),
            FencingAcquireResult.AlreadyOwned(token, 10),
            FencingAcquireResult.Contended(9),
            FencingAcquireResult.IntegrityFailure(integrityFailure),
            FencingAcquireResult.BackendFailure(backendFailure),
            FencingInspectResult.Owned(token, 8),
            FencingInspectResult.Contended(7),
            FencingInspectResult.IntegrityFailure(integrityFailure),
            FencingInspectResult.BackendFailure(backendFailure),
            FencingRenewResult.IntegrityFailure(integrityFailure),
            FencingRenewResult.BackendFailure(backendFailure),
            FencingReleaseResult.IntegrityFailure(integrityFailure),
            FencingReleaseResult.BackendFailure(backendFailure),
        )

        val enumSamples: List<Serializable> = listOf(
            FencingBackendFailureKind.CONNECTION,
            FencingBackendFailureKind.TIMEOUT,
            FencingBackendFailureKind.COMMAND,
            FencingIntegrityFailureKind.MALFORMED_LEASE,
            FencingIntegrityFailureKind.INVALID_COUNTER,
            FencingIntegrityFailureKind.COUNTER_BEHIND_LEASE,
        )

        val failureTypes = listOf(
            FencingLeaseBackendFailure::class,
            FencingLeaseIntegrityFailure::class,
        )

        val resultContracts = listOf(
            FencingBootstrapResult::class,
            FencingAcquireResult::class,
            FencingInspectResult::class,
            FencingRenewResult::class,
            FencingReleaseResult::class,
        )
    }
}
