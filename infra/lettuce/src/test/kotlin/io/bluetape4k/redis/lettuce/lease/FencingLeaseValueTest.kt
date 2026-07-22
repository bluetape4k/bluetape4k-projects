package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable

class FencingLeaseValueTest {

    @Test
    fun `config accepts safe ordering domain values`() {
        val config = LettuceFencingLeaseConfig(
            namespace = "n".repeat(128),
            resourceName = "resource-1._safe",
            epoch = 1,
        )

        config.namespace shouldBeEqualTo "n".repeat(128)
        config.resourceName shouldBeEqualTo "resource-1._safe"
        config.epoch shouldBeEqualTo 1L
    }

    @Test
    fun `config rejects unsafe ordering domain values`() {
        listOf("", "n".repeat(129), "white space", "hash{tag}", "colon:value", "line\nbreak").forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                LettuceFencingLeaseConfig(value, "resource", 1)
            }
            assertFailsWith<IllegalArgumentException> {
                LettuceFencingLeaseConfig("namespace", value, 1)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceFencingLeaseConfig("namespace", "resource", 0)
        }
    }

    @Test
    fun `owner id validates UTF-8 byte length and redacts text`() {
        val ascii = "a".repeat(256)
        val utf8 = "한".repeat(85)

        FencingOwnerId.from(ascii).toString() shouldBeEqualTo "FencingOwnerId(<redacted>)"
        FencingOwnerId.from(utf8).toString() shouldBeEqualTo "FencingOwnerId(<redacted>)"
        FencingOwnerId.from("secret-owner").toString() shouldNotContain "secret-owner"

        listOf("", "   ", "a".repeat(257), "한".repeat(86)).forEach { value ->
            assertFailsWith<IllegalArgumentException> { FencingOwnerId.from(value) }
        }
    }

    @Test
    fun `owner id equality uses the raw opaque value`() {
        val first = FencingOwnerId.from("attempt-1")
        val same = FencingOwnerId.from("attempt-1")
        val other = FencingOwnerId.from("attempt-2")

        first shouldBeEqualTo same
        first.hashCode() shouldBeEqualTo same.hashCode()
        first shouldNotBeEqualTo other
    }

    @Test
    fun `random owner id uses a fresh 22 character value`() {
        val first = FencingOwnerId.random()
        val second = FencingOwnerId.random()

        first.value.length shouldBeEqualTo 22
        second.value.length shouldBeEqualTo 22
        first.value.all { character -> character in BASE58_ALPHABET }.shouldBeTrue()
        second.value.all { character -> character in BASE58_ALPHABET }.shouldBeTrue()
        first shouldNotBeEqualTo second
    }

    @Test
    fun `token validates positive components and orders epoch before sequence`() {
        val epochOneLast = FencingToken(1, Long.MAX_VALUE)
        val epochTwoFirst = FencingToken(2, 1)

        FencingToken(1, 1).compareTo(FencingToken(1, 2)) shouldBeLessThan 0
        FencingToken(1, 2).compareTo(FencingToken(1, 2)) shouldBeEqualTo 0
        epochOneLast.compareTo(epochTwoFirst) shouldBeLessThan 0
        epochTwoFirst.toString() shouldBeEqualTo "FencingToken(<redacted>)"
        epochTwoFirst.toString() shouldNotContain "2"

        assertFailsWith<IllegalArgumentException> { FencingToken(0, 1) }
        assertFailsWith<IllegalArgumentException> { FencingToken(1, 0) }
    }

    @Test
    fun `value objects have stable Java serialization contracts`() {
        val samples = listOf<Serializable>(
            LettuceFencingLeaseConfig("orders", "rebuild", 7),
            FencingOwnerId.from("attempt-1"),
            FencingToken(7, 42),
        )

        samples.forEach { original ->
            javaRoundTrip(original) shouldBeEqualTo original
            ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        }
    }

    @Test
    fun `deserialization revalidates config owner and token without leaking raw values`() {
        val invalidConfigs = listOf(
            LettuceFencingLeaseConfig("orders", "rebuild", 7).withField("namespace", "secret namespace"),
            LettuceFencingLeaseConfig("orders", "rebuild", 7).withField("namespace", null),
            LettuceFencingLeaseConfig("orders", "rebuild", 7).withField("resourceName", null),
        )
        val invalidOwner = FencingOwnerId.from("attempt-1")
            .withField("value", null)
        val invalidToken = FencingToken(7, 42)
            .withField("sequence", 0L)

        (invalidConfigs + listOf(invalidOwner, invalidToken)).forEach { invalid ->
            val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }
            error.cause shouldBeEqualTo null
            error.message shouldBeEqualTo "Invalid serialized ${invalid.javaClass.simpleName}."
            error.message shouldNotContain "secret namespace"
            error.message shouldNotContain "attempt-1"
        }
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
        const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    }
}
