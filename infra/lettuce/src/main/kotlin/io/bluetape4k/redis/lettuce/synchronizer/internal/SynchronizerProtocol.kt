package io.bluetape4k.redis.lettuce.synchronizer.internal

import io.bluetape4k.redis.lettuce.synchronizer.LatchConfig
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreConfig
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.security.MessageDigest

internal data class SemaphoreKeys(
    val available: String,
    val generation: String,
    val capacity: String,
    val holds: String,
    val requests: String,
    val allocations: String,
    val allocationLeases: String,
    val leases: String,
    val deadlines: String,
    val fingerprint: String,
) {
    val regular: Array<String> = arrayOf(available, generation, capacity, holds, requests)
    val expirable: Array<String> =
        arrayOf(available, generation, capacity, allocations, allocationLeases, leases, deadlines, requests)
    val all: List<String> = (regular.asList() + expirable.asList()).distinct()
}

internal data class LatchKeys(
    val count: String,
    val generation: String,
    val waiters: String,
    val requests: String,
    val fingerprint: String,
) {
    val all: List<String> = listOf(count, generation, waiters, requests)
    val array: Array<String> = all.toTypedArray()
}

internal fun deriveSemaphoreKeys(
    name: String,
    config: SemaphoreConfig,
    codec: RedisCodec<String, *>,
): SemaphoreKeys {
    val prefix = prefix(config.namespace, config.hashTag ?: name, "semaphore", name)
    val keys = SemaphoreKeys(
        "$prefix:available",
        "$prefix:generation",
        "$prefix:capacity",
        "$prefix:holds",
        "$prefix:requests",
        "$prefix:allocations",
        "$prefix:allocation-leases",
        "$prefix:leases",
        "$prefix:deadlines",
        fingerprint(codec, "$prefix:available"),
    )
    requireSameWireSlot(codec, keys.all, keys.fingerprint)
    return keys
}

internal fun deriveLatchKeys(
    name: String,
    config: LatchConfig,
    codec: RedisCodec<String, *>,
): LatchKeys {
    val prefix = prefix(config.namespace, config.hashTag ?: name, "latch", name)
    val keys = LatchKeys(
        "$prefix:count",
        "$prefix:generation",
        "$prefix:waiters",
        "$prefix:requests",
        fingerprint(codec, "$prefix:count"),
    )
    requireSameWireSlot(codec, keys.all, keys.fingerprint)
    return keys
}

private fun prefix(namespace: String, hashTag: String, kind: String, name: String): String {
    require(name.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))) { "name has an invalid format" }
    require(hashTag.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))) { "hashTag has an invalid format" }
    return "$namespace:{$hashTag}:$kind:$name"
}

private fun requireSameWireSlot(codec: RedisCodec<String, *>, keys: Collection<String>, fingerprint: String) {
    val slots = keys.map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet()
    require(slots.size == 1) { "coordination keys must share one Redis slot (fingerprint=$fingerprint)" }
}

private fun fingerprint(codec: RedisCodec<String, *>, key: String): String {
    val bytes = codec.encodeKey(key).toByteArray()
    return MessageDigest.getInstance("SHA-256").digest(bytes)
        .take(8)
        .joinToString("") { "%02x".format(it) }
}

private fun ByteBuffer.toByteArray(): ByteArray =
    duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

internal fun decodeReply(raw: List<String>): List<String>? {
    if (raw.isEmpty() || raw.size > 16) return null
    if (raw.sumOf { it.toByteArray().size } > 2_048) return null
    return raw
}
