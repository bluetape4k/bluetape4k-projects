package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import java.time.Duration

class LockConfigTest {

    @Test
    fun `default namespace and bounded namespace segments are accepted`() {
        LockConfig().namespace shouldBeEqualTo "bt4k:coord:v1"
        LockConfig(namespace = "a:b.c:d_e:e-f").namespace shouldBeEqualTo "a:b.c:d_e:e-f"

        listOf(
            "",
            " ",
            "a::b",
            "a:b:",
            ":a",
            "a{b}",
            "a b",
            "a\nb",
            "a".repeat(33),
            (1..9).joinToString(":") { "s$it" },
            List(5) { "x".repeat(32) }.joinToString(":"),
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { LockConfig(namespace = invalid) }
        }
    }

    @Test
    fun `hash tag and resource components fail closed before key derivation`() {
        LockConfig(hashTag = "orders-1").validateResourceName("checkout_1") shouldBeEqualTo "checkout_1"

        listOf("", "a".repeat(129), "hash{tag}", "a:b", "a b", "a\nb").forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { LockConfig(hashTag = invalid) }
            assertFailsWith<IllegalArgumentException> { LockConfig().validateResourceName(invalid) }
        }
        assertFailsWith<IllegalArgumentException> {
            LockConfig(namespace = "n".repeat(32)).validateDerivedKey("k".repeat(513))
        }
    }

    @Test
    fun `positive nanoseconds round up and duration overflow fails`() {
        Duration.ofNanos(1).toRedisMillisCeil() shouldBeEqualTo 1L
        Duration.ofMillis(1).plusNanos(1).toRedisMillisCeil() shouldBeEqualTo 2L
        Duration.ofHours(24).toRedisMillisCeil() shouldBeEqualTo Duration.ofHours(24).toMillis()

        assertFailsWith<IllegalArgumentException> { Duration.ZERO.toRedisMillisCeil() }
        assertFailsWith<IllegalArgumentException> { Duration.ofNanos(-1).toRedisMillisCeil() }
        assertFailsWith<IllegalArgumentException> { Duration.ofSeconds(Long.MAX_VALUE).toRedisMillisCeil() }
    }

    @Test
    fun `lease and lock configuration hard caps fail closed`() {
        LeasePolicy.Fixed(Duration.ofMillis(100))
        LeasePolicy.Fixed(Duration.ofHours(24))
        LeasePolicy.Watchdog()

        assertFailsWith<IllegalArgumentException> { LeasePolicy.Fixed(Duration.ofMillis(99)) }
        assertFailsWith<IllegalArgumentException> { LeasePolicy.Fixed(Duration.ofHours(24).plusNanos(1)) }
        assertFailsWith<IllegalArgumentException> { LeasePolicy.Watchdog(ttl = Duration.ofSeconds(2)) }
        assertFailsWith<IllegalArgumentException> {
            LeasePolicy.Watchdog(ttl = Duration.ofSeconds(3), renewalInterval = Duration.ofSeconds(2))
        }
        assertFailsWith<IllegalArgumentException> {
            LeasePolicy.Watchdog(maxLifetime = Duration.ofDays(7).plusNanos(1))
        }
        assertFailsWith<IllegalArgumentException> { LockConfig(maxReentrantHolds = 0) }
        assertFailsWith<IllegalArgumentException> { LockConfig(maxReentrantHolds = 10_001) }
    }

    @Test
    fun `specialized config bounds and Lua exact epoch are enforced`() {
        FairLockConfig(cleanupBatchSize = 1, maxQueueSize = 1)
        ReadWriteLockConfig(cleanupBatchSize = 256, maxQueueSize = 10_000)
        FencedLockConfig(epoch = MAX_LUA_EXACT_INTEGER)
        SpinLockConfig()
        MultiLockConfig(maxKeys = 32).validateInputKeyCount(32)

        assertFailsWith<IllegalArgumentException> { FairLockConfig(cleanupBatchSize = 0) }
        assertFailsWith<IllegalArgumentException> { FairLockConfig(maxQueueSize = 10_001) }
        assertFailsWith<IllegalArgumentException> { FencedLockConfig(epoch = 0) }
        assertFailsWith<IllegalArgumentException> { FencedLockConfig(epoch = MAX_LUA_EXACT_INTEGER + 1) }
        assertFailsWith<IllegalArgumentException> { SpinLockConfig(multiplier = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { SpinLockConfig(jitterRatio = 0.251) }
        assertFailsWith<IllegalArgumentException> { SpinLockConfig(maxAttemptsPerSecond = 101) }
        assertFailsWith<IllegalArgumentException> { SpinLockConfig(initialDelay = Duration.ofSeconds(2)) }
        assertFailsWith<IllegalArgumentException> { MultiLockConfig(maxKeys = 0) }
        assertFailsWith<IllegalArgumentException> { MultiLockConfig(maxKeys = 33) }
        assertFailsWith<IllegalArgumentException> { MultiLockConfig(maxKeys = 2).validateInputKeyCount(3) }
    }

    @Test
    fun `configs have stable serialization and deserialize through validation`() {
        val samples = listOf<Serializable>(
            LockConfig(),
            FairLockConfig(),
            FencedLockConfig(epoch = 1),
            ReadWriteLockConfig(),
            SpinLockConfig(),
            MultiLockConfig(),
        )
        samples.forEach { original ->
            javaRoundTrip(original) shouldBeEqualTo original
            ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        }

        val invalid = FencedLockConfig(epoch = 1).withField("epoch", MAX_LUA_EXACT_INTEGER + 1)
        val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }
        error.message shouldBeEqualTo "Invalid serialized FencedLockConfig."
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

    private companion object {
        const val MAX_LUA_EXACT_INTEGER = 9_007_199_254_740_991L
    }
}
