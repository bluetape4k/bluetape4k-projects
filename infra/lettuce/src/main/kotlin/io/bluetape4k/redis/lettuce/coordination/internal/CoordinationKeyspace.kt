package io.bluetape4k.redis.lettuce.coordination.internal

import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.security.MessageDigest

internal class CoordinationKeyspace(
    objectKind: String,
    name: String,
    private val codec: RedisCodec<String, *>,
    version: Int = 1,
) {
    private val validObjectKind = objectKind.requirePattern(OBJECT_KIND_PATTERN, "objectKind")
    private val validName = name.requirePattern(NAME_PATTERN, "name")
    private val validVersion = version.also {
        require(it > 0) { "version must be positive" }
    }

    private val prefix = "bt4k:coord:v$validVersion:{$validName}:$validObjectKind:$validName"

    val stateKey: String = "$prefix:state"

    val slot: Int = encodedSlot(stateKey)

    val fingerprint: String = encodedFingerprint(stateKey)

    fun key(suffix: String): String =
        "$prefix:${suffix.requirePattern(SUFFIX_PATTERN, "suffix")}"

    fun requireSameSlot(keys: Collection<String>): Int {
        require(keys.isNotEmpty()) { "keys must not be empty" }
        val slots = keys.map(::encodedSlot).toSet()
        require(slots.size == 1) {
            "coordination keys must share one Redis slot (fingerprint=$fingerprint)"
        }
        return slots.single()
    }

    private fun encodedSlot(key: String): Int =
        SlotHash.getSlot(codec.encodeKey(key))

    private fun encodedFingerprint(key: String): String {
        val encoded = codec.encodeKey(key).toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
        return digest.take(FINGERPRINT_BYTES).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun String.requirePattern(pattern: Regex, parameterName: String): String {
        require(matches(pattern)) { "$parameterName has an invalid format" }
        return this
    }

    private fun ByteBuffer.toByteArray(): ByteArray =
        duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

    private companion object {
        const val FINGERPRINT_BYTES = 8
        val OBJECT_KIND_PATTERN = Regex("[a-z][a-z0-9-]{0,31}")
        val NAME_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
        val SUFFIX_PATTERN = Regex("[a-z][a-z0-9-]{0,63}")
    }
}
