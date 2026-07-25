package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.codec.Base58
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import java.time.Duration

class LockIdentityTest {

    @Test
    fun `owner and request ids validate UTF-8 bytes and redact diagnostics`() {
        val owner = LockOwnerId.from("owner-secret")
        val request = LockRequestId.from("request-secret")

        LockOwnerId.from("한".repeat(85)).toString() shouldBeEqualTo "LockOwnerId(<redacted>)"
        LockRequestId.from("한".repeat(85)).toString() shouldBeEqualTo "LockRequestId(<redacted>)"
        owner.toString() shouldNotContain "owner-secret"
        request.toString() shouldNotContain "request-secret"

        listOf("", " ".repeat(3), "a".repeat(257), "한".repeat(86)).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { LockOwnerId.from(invalid) }
            assertFailsWith<IllegalArgumentException> { LockRequestId.from(invalid) }
        }
    }

    @Test
    fun `generated ids contain at least 128 decoded CSPRNG bits and do not collide in a bounded sample`() {
        val owners = List(256) { LockOwnerId.random() }
        val requests = List(256) { LockRequestId.random() }

        owners.map { Base58.decode(it.value).size }.min() shouldBeGreaterOrEqualTo 16
        requests.map { Base58.decode(it.value).size }.min() shouldBeGreaterOrEqualTo 16
        owners.map { it.value }.distinct().size shouldBeEqualTo owners.size
        requests.map { it.value }.distinct().size shouldBeEqualTo requests.size
    }

    @Test
    fun `identity equality uses opaque values while generations remain ordered and redacted`() {
        LockOwnerId.from("same") shouldBeEqualTo LockOwnerId.from("same")
        LockOwnerId.from("same") shouldNotBeEqualTo LockOwnerId.from("other")
        LockRequestId.from("same") shouldBeEqualTo LockRequestId.from("same")

        LockGeneration(1).compareTo(LockGeneration(2)) shouldBeLessThan 0
        LockGeneration(2).toString() shouldBeEqualTo "LockGeneration(<redacted>)"
        assertFailsWith<IllegalArgumentException> { LockGeneration(0) }
    }

    @Test
    fun `lease policies and specialized handles validate and redact sensitive values`() {
        val lock = LockHandle(
            objectFingerprint = "object-secret",
            ownerId = LockOwnerId.from("owner-secret"),
            generation = LockGeneration(7),
            requestId = LockRequestId.from("request-secret"),
            leasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(1)),
            kind = LockKind.DISTRIBUTED,
        )
        val fenced = FencedLockHandle(lock.copy(kind = LockKind.FENCED), epoch = 1, fencingToken = 9)
        val waiter = FairWaiterState(FairWaiterStatus.QUEUED, enqueueSequence = 3, remainingWaitMillis = 10)

        lock.toString() shouldNotContain "object-secret"
        lock.toString() shouldNotContain "owner-secret"
        lock.toString() shouldNotContain "request-secret"
        lock.toString() shouldNotContain "7"
        fenced.toString() shouldNotContain "9"
        waiter.toString() shouldNotContain "3"
        waiter.toString() shouldNotContain "10"

        assertFailsWith<IllegalArgumentException> { FencedLockHandle(lock.copy(kind = LockKind.FENCED), 0, 1) }
        assertFailsWith<IllegalArgumentException> { FencedLockHandle(lock.copy(kind = LockKind.FENCED), 1, 0) }
        assertFailsWith<IllegalArgumentException> { MultiLockHandle(lock.copy(kind = LockKind.MULTI), 0) }
        assertFailsWith<IllegalArgumentException> {
            FairWaiterState(FairWaiterStatus.QUEUED, 0, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            FairWaiterState(FairWaiterStatus.QUEUED, 1, -1)
        }
    }

    @Test
    fun `identity and handle values have stable validated serialization`() {
        val lock = LockHandle(
            objectFingerprint = "fp",
            ownerId = LockOwnerId.from("owner"),
            generation = LockGeneration(2),
            requestId = LockRequestId.from("request"),
            leasePolicy = LeasePolicy.Watchdog(),
            kind = LockKind.FENCED,
        )
        val samples = listOf<Serializable>(
            LockOwnerId.from("owner"),
            LockRequestId.from("request"),
            LockGeneration(2),
            LeasePolicy.Fixed(Duration.ofMillis(100)),
            LeasePolicy.Watchdog(),
            lock,
            FencedLockHandle(lock, 1, 2),
            ReadLockHandle(lock.copy(kind = LockKind.READ)),
            WriteLockHandle(lock.copy(kind = LockKind.WRITE)),
            MultiLockHandle(lock.copy(kind = LockKind.MULTI), 2),
            FairWaiterState(FairWaiterStatus.QUEUED, 1, 0),
        )

        samples.forEach { original ->
            javaRoundTrip(original) shouldBeEqualTo original
            ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        }

        val invalid = lock.withField("objectFingerprint", "")
        val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }
        error.message shouldBeEqualTo "Invalid serialized LockHandle."
        error.message shouldNotContain "owner"
    }

    private fun <T: Serializable> T.withField(name: String, value: Any?): T = apply {
        javaClass.getDeclaredField(name).also { field ->
            field.isAccessible = true
            field.set(this, value)
        }
    }

    private fun javaRoundTrip(original: Serializable): Any =
        ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(original) }
            ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        }
}
