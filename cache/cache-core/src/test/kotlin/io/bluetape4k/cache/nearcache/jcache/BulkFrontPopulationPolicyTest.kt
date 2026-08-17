package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

class BulkFrontPopulationPolicyTest {

    @Test
    fun `PopulateIfAtMost는 양수 상한만 허용한다`() {
        listOf(0, -1).forEach { maximumEntryCount ->
            assertFailsWith<IllegalArgumentException> {
                BulkFrontPopulationPolicy.PopulateIfAtMost(maximumEntryCount)
            }
        }
    }

    @Test
    fun `각 policy는 Java serialization round trip을 유지한다`() {
        listOf(
            BulkFrontPopulationPolicy.BypassFront,
            BulkFrontPopulationPolicy.PopulateIfAtMost(2),
        ).forEach { policy ->
            ObjectStreamClass.lookup(policy.javaClass).serialVersionUID shouldBeEqualTo 1L
            deserialize<BulkFrontPopulationPolicy>(serialize(policy)) shouldBeEqualTo policy
        }
    }

    @Test
    fun `역직렬화는 reflection으로 변조한 상한을 거부한다`() {
        listOf(0, -1).forEach { invalidCount ->
            val policy = BulkFrontPopulationPolicy.PopulateIfAtMost(2)
            policy.javaClass.getDeclaredField("maximumEntryCount").apply {
                isAccessible = true
                setInt(policy, invalidCount)
            }

            assertFailsWith<InvalidObjectException> {
                deserialize<BulkFrontPopulationPolicy>(serialize(policy))
            }
        }
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        ObjectOutputStream(bytes).use { output -> output.writeObject(value) }
        bytes.toByteArray()
    }

    private inline fun <reified T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { input -> input.readObject() as T }
}
