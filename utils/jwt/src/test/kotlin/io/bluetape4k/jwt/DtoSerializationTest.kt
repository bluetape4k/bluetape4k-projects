package io.bluetape4k.jwt

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.jwt.keychain.KeyChainDto
import io.bluetape4k.jwt.reader.JwtReaderDto
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

class DtoSerializationTest {

    @Test
    fun `JwtReaderDto는 기존 계산 UID와 Java serialization round trip을 유지한다`() {
        val dto = JwtReaderDto(
            headers = mapOf("alg" to "RS256"),
            claims = mapOf("subject" to "alice"),
            digest = byteArrayOf(1, 2, 3),
            tokenString = "token",
        )

        deserialize<JwtReaderDto>(serialize(dto)) shouldBeEqualTo dto
        JwtReaderDto::class.java.getDeclaredField("serialVersionUID").apply { isAccessible = true }
            .getLong(null) shouldBeEqualTo 6285801536022841892L
        ObjectStreamClass.lookup(JwtReaderDto::class.java).serialVersionUID shouldBeEqualTo 6285801536022841892L
    }

    @Test
    fun `KeyChainDto는 기존 계산 UID와 Java serialization round trip을 유지한다`() {
        val dto = KeyChainDto(
            id = "key-1",
            algorithmName = "RS256",
            createdAt = 1_000L,
            expiredTtl = 60_000L,
        ).apply {
            publicKey = byteArrayOf(1, 2)
            privateKey = byteArrayOf(3, 4)
        }

        deserialize<KeyChainDto>(serialize(dto)).apply {
            id shouldBeEqualTo dto.id
            algorithmName shouldBeEqualTo dto.algorithmName
            createdAt shouldBeEqualTo dto.createdAt
            expiredTtl shouldBeEqualTo dto.expiredTtl
            publicKey?.contentEquals(dto.publicKey).shouldBeEqualTo(true)
            privateKey?.contentEquals(dto.privateKey).shouldBeEqualTo(true)
        }
        KeyChainDto::class.java.getDeclaredField("serialVersionUID").apply { isAccessible = true }
            .getLong(null) shouldBeEqualTo -1267149397241058308L
        ObjectStreamClass.lookup(KeyChainDto::class.java).serialVersionUID shouldBeEqualTo -1267149397241058308L
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        bytes.toByteArray()
    }

    private inline fun <reified T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
}
