package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class MultiKeyLeaseResultTest {

    @Test
    fun `multi-key lease config uses a bounded positive key limit`() {
        LettuceMultiKeyLeaseConfig().maxKeys shouldBeEqualTo 32

        assertFailsWith<IllegalArgumentException> {
            LettuceMultiKeyLeaseConfig(maxKeys = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceMultiKeyLeaseConfig(maxKeys = -1)
        }
    }

    @Test
    fun `all public result variants expose the documented values`() {
        val counts = sampleCounts

        MultiKeyAcquireResult.Acquired shouldBeEqualTo MultiKeyAcquireResult.Acquired
        MultiKeyAcquireResult.AlreadyOwned(900).minimumPttlMillis shouldBeEqualTo 900L
        MultiKeyAcquireResult.PartialOwnership(counts).counts shouldBeEqualTo counts
        MultiKeyAcquireResult.Conflicted(counts).counts shouldBeEqualTo counts

        MultiKeyInspectResult.Owned(800).minimumPttlMillis shouldBeEqualTo 800L
        MultiKeyInspectResult.Lost shouldBeEqualTo MultiKeyInspectResult.Lost
        MultiKeyInspectResult.PartialOwnership(counts).counts shouldBeEqualTo counts
        MultiKeyInspectResult.Conflicted(counts).counts shouldBeEqualTo counts

        MultiKeyRenewResult.Renewed shouldBeEqualTo MultiKeyRenewResult.Renewed
        MultiKeyRenewResult.PartialLoss(counts).counts shouldBeEqualTo counts
        MultiKeyRenewResult.Lost shouldBeEqualTo MultiKeyRenewResult.Lost
        MultiKeyRenewResult.OwnershipMismatch(counts).counts shouldBeEqualTo counts

        MultiKeyReleaseResult.Released shouldBeEqualTo MultiKeyReleaseResult.Released
        MultiKeyReleaseResult.PartialRelease(counts).counts shouldBeEqualTo counts
        MultiKeyReleaseResult.Lost shouldBeEqualTo MultiKeyReleaseResult.Lost
        MultiKeyReleaseResult.OwnershipMismatch(counts).counts shouldBeEqualTo counts
    }

    @Test
    fun `all value results have stable Java serialization contracts`() {
        serializableSamples.forEach { original ->
            val restored = javaRoundTrip(original)

            restored shouldBeEqualTo original
            restored.javaClass shouldBeEqualTo original.javaClass
            ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        }
    }

    @Test
    fun `result properties never expose keys or owner tokens`() {
        val forbiddenProperties = resultTypes
            .flatMap { type -> type.memberProperties.map { property -> property.name } }
            .filter { propertyName ->
                propertyName.contains("key", ignoreCase = true) ||
                    propertyName.contains("token", ignoreCase = true)
            }

        forbiddenProperties.shouldBeEmpty()
    }

    @Test
    fun `sealed result subclasses never declare key or token properties`() {
        val forbiddenProperties = resultContracts
            .flatMap { contract -> contract.sealedSubclasses }
            .flatMap { subtype -> subtype.memberProperties.map { property -> property.name } }
            .filter { propertyName ->
                propertyName.contains("key", ignoreCase = true) ||
                    propertyName.contains("token", ignoreCase = true)
            }

        forbiddenProperties.shouldBeEmpty()
    }

    @Test
    fun `cross-slot exception exposes only the distinct slot count`() {
        val exception = MultiKeyLeaseCrossSlotException(distinctSlotCount = 2)

        exception.distinctSlotCount shouldBeEqualTo 2
        exception.message shouldBeEqualTo
            "Multi-key lease requires one Redis Cluster slot; distinctSlotCount=2."
        assertSecretsAbsent(exception, secretKey, secretToken)
    }

    @Test
    fun `cross-slot exception has a stable redacted Java serialization contract`() {
        val original = MultiKeyLeaseCrossSlotException(distinctSlotCount = 2)

        val restored = javaRoundTrip(original) as MultiKeyLeaseCrossSlotException

        restored.javaClass shouldBeEqualTo original.javaClass
        restored.message shouldBeEqualTo original.message
        restored.distinctSlotCount shouldBeEqualTo original.distinctSlotCount
        ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        assertSecretsAbsent(restored, secretKey, secretToken)
    }

    @Test
    fun `integrity exception exposes only operation and counts`() {
        val exception = MultiKeyLeaseIntegrityException(
            operation = MultiKeyLeaseOperation.INSPECT,
            requestedKeyCount = 2,
            invalidLeaseKeyCount = 1,
        )

        exception.operation shouldBeEqualTo MultiKeyLeaseOperation.INSPECT
        exception.requestedKeyCount shouldBeEqualTo 2
        exception.invalidLeaseKeyCount shouldBeEqualTo 1
        exception.message shouldBeEqualTo
            "Multi-key lease integrity failure: operation=INSPECT, requestedKeyCount=2, invalidLeaseKeyCount=1."
        assertSecretsAbsent(exception, secretKey, secretToken)
    }

    @Test
    fun `integrity exception has a stable redacted Java serialization contract`() {
        val original = MultiKeyLeaseIntegrityException(
            operation = MultiKeyLeaseOperation.INSPECT,
            requestedKeyCount = 2,
            invalidLeaseKeyCount = 1,
        )

        val restored = javaRoundTrip(original) as MultiKeyLeaseIntegrityException

        restored.javaClass shouldBeEqualTo original.javaClass
        restored.message shouldBeEqualTo original.message
        restored.operation shouldBeEqualTo original.operation
        restored.requestedKeyCount shouldBeEqualTo original.requestedKeyCount
        restored.invalidLeaseKeyCount shouldBeEqualTo original.invalidLeaseKeyCount
        ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        assertSecretsAbsent(restored, secretKey, secretToken)
    }

    private fun javaRoundTrip(original: Serializable): Any =
        ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { output -> output.writeObject(original) }
            ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { input -> input.readObject() }
        }

    private fun assertSecretsAbsent(
        exception: Throwable,
        vararg secrets: String,
    ) {
        val causeChain = generateSequence(exception) { current -> current.cause }.toList()
        val publicPropertyValues = causeChain.flatMap { current ->
            current::class.memberProperties
                .filter { property -> property.visibility == KVisibility.PUBLIC }
                .map { property -> property.getter.call(current) }
        }
        val observableText = buildList {
            causeChain.forEach { current ->
                add(current.message)
                add(current.toString())
            }
            addAll(publicPropertyValues)
        }.joinToString(separator = "\n")

        causeChain.first() shouldBeEqualTo exception
        secrets.forEach { secret ->
            observableText.contains(secret).shouldBeEqualTo(false)
        }
    }

    private companion object {
        const val secretKey = "lease-key-super-secret"
        const val secretToken = "owner-token-super-secret"

        val sampleCounts = MultiKeyLeaseCounts(
            requestedKeys = 4,
            ownedKeys = 2,
            missingKeys = 1,
            mismatchedKeys = 1,
        )

        val serializableSamples: List<Serializable> = listOf(
            LettuceMultiKeyLeaseConfig(),
            sampleCounts,
            MultiKeyAcquireResult.Acquired,
            MultiKeyAcquireResult.AlreadyOwned(minimumPttlMillis = 900),
            MultiKeyAcquireResult.PartialOwnership(sampleCounts),
            MultiKeyAcquireResult.Conflicted(sampleCounts),
            MultiKeyInspectResult.Owned(minimumPttlMillis = 800),
            MultiKeyInspectResult.Lost,
            MultiKeyInspectResult.PartialOwnership(sampleCounts),
            MultiKeyInspectResult.Conflicted(sampleCounts),
            MultiKeyRenewResult.Renewed,
            MultiKeyRenewResult.PartialLoss(sampleCounts),
            MultiKeyRenewResult.Lost,
            MultiKeyRenewResult.OwnershipMismatch(sampleCounts),
            MultiKeyReleaseResult.Released,
            MultiKeyReleaseResult.PartialRelease(sampleCounts),
            MultiKeyReleaseResult.Lost,
            MultiKeyReleaseResult.OwnershipMismatch(sampleCounts),
        )

        val resultContracts: List<KClass<*>> = listOf(
            MultiKeyAcquireResult::class,
            MultiKeyInspectResult::class,
            MultiKeyRenewResult::class,
            MultiKeyReleaseResult::class,
        )

        val resultTypes: List<KClass<out Serializable>> = serializableSamples
            .drop(2)
            .map { sample -> sample::class }
    }
}
