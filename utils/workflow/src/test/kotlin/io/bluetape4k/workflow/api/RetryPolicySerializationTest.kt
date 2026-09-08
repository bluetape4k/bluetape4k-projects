package io.bluetape4k.workflow.api

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import kotlin.time.Duration.Companion.milliseconds

class RetryPolicySerializationTest {

    @Test
    fun `RetryPolicy는 Java serialization round trip과 명시 UID를 유지한다`() {
        val policy = RetryPolicy(
            maxAttempts = 3,
            delay = 100.milliseconds,
            backoffMultiplier = 2.0,
            maxDelay = 1_000.milliseconds,
        )

        deserialize<RetryPolicy>(serialize(policy)) shouldBeEqualTo policy
        RetryPolicy::class.java.getDeclaredField("serialVersionUID").apply { isAccessible = true }
            .getLong(null) shouldBeEqualTo 1L
        ObjectStreamClass.lookup(RetryPolicy::class.java).serialVersionUID shouldBeEqualTo 1L
    }

    @Test
    fun `maxAttempts와 backoffMultiplier는 공용 경계 검증을 사용한다`() {
        assertFailsWith<IllegalArgumentException> { RetryPolicy(maxAttempts = 0) }
        assertFailsWith<IllegalArgumentException> { RetryPolicy(backoffMultiplier = 0.5) }
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        bytes.toByteArray()
    }

    private inline fun <reified T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
}
