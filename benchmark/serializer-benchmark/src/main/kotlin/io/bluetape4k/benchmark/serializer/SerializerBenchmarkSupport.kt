package io.bluetape4k.benchmark.serializer

import io.bluetape4k.avro.AvroReflectSerializer
import io.bluetape4k.avro.impl.DefaultAvroReflectSerializer
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.ForyBinarySerializer
import io.bluetape4k.io.serializer.JdkBinarySerializer
import io.bluetape4k.io.serializer.KryoBinarySerializer
import io.bluetape4k.jackson.JacksonSerializer as Jackson2Serializer
import io.bluetape4k.jackson3.JacksonSerializer as Jackson3Serializer
import io.bluetape4k.json.JsonSerializer
import java.nio.ByteBuffer

enum class BinarySerializerKind {
    JDK,
    KRYO,
    FORY,
}

enum class JsonSerializerKind {
    JACKSON2,
    JACKSON3,
    FASTJSON2,
}

/** Executes only [BinarySerializer]'s ByteBuffer compatibility defaults. */
class CompatibilityBinarySerializer(
    private val delegate: BinarySerializer,
): BinarySerializer {
    override fun serialize(graph: Any?): ByteArray = delegate.serialize(graph)

    override fun <T: Any> deserialize(bytes: ByteArray?): T? = delegate.deserialize(bytes)
}

/** Executes only [JsonSerializer]'s ByteBuffer compatibility defaults. */
class CompatibilityJsonSerializer(
    private val delegate: JsonSerializer,
): JsonSerializer {
    override fun serialize(graph: Any?): ByteArray = delegate.serialize(graph)

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
        delegate.deserialize(bytes, clazz)
}

/** Executes only [AvroReflectSerializer]'s ByteBuffer compatibility defaults. */
class CompatibilityAvroReflectSerializer(
    private val delegate: AvroReflectSerializer,
): AvroReflectSerializer {
    override fun <T> serialize(graph: T?): ByteArray? = delegate.serialize(graph)

    override fun <T> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? =
        delegate.deserialize(avroBytes, clazz)
}

interface SerializerBenchmarkFixture {
    val name: String
    val payload: SerializerBenchmarkPayload
    val claimEligibleSerialize: Boolean
    val claimEligibleDeserialize: Boolean
    val wireSize: Int

    fun newTarget(): ByteBuffer
    fun serializeByteArray(): ByteArray
    fun serializeCompatibility(target: ByteBuffer): Int
    fun serializeOptimized(target: ByteBuffer): Int
    fun deserializeByteArray(): SerializerBenchmarkPayload?
    fun deserializeCompatibility(source: ByteBuffer): SerializerBenchmarkPayload?
    fun deserializeOptimized(source: ByteBuffer): SerializerBenchmarkPayload?
    fun precomputedOptimizedSource(): ByteBuffer
    fun precomputedFallbackDirectSource(): ByteBuffer? = null
    fun precomputedFallbackReadOnlySource(): ByteBuffer? = null
    fun validateTarget(target: ByteBuffer): Int = serializeOptimized(target)
    fun validate()
}

private class DefaultSerializerBenchmarkFixture(
    override val name: String,
    override val payload: SerializerBenchmarkPayload,
    override val claimEligibleSerialize: Boolean,
    override val claimEligibleDeserialize: Boolean,
    private val wire: ByteArray,
    private val byteArraySerializer: () -> ByteArray,
    private val compatibilitySerializer: (ByteBuffer) -> Int,
    private val optimizedSerializer: (ByteBuffer) -> Int,
    private val byteArrayDeserializer: () -> SerializerBenchmarkPayload?,
    private val compatibilityDeserializer: (ByteBuffer) -> SerializerBenchmarkPayload?,
    private val optimizedDeserializer: (ByteBuffer) -> SerializerBenchmarkPayload?,
    optimizedSourceKind: SourceKind,
    includeFallbackSources: Boolean = false,
): SerializerBenchmarkFixture {
    override val wireSize: Int = wire.size
    private val optimizedSource = sourceBuffer(wire, optimizedSourceKind)
    private val fallbackDirectSource =
        if (includeFallbackSources) sourceBuffer(wire, SourceKind.DIRECT) else null
    private val fallbackReadOnlySource =
        if (includeFallbackSources) sourceBuffer(wire, SourceKind.READ_ONLY_HEAP) else null

    override fun newTarget(): ByteBuffer = ByteBuffer.allocateDirect(maxOf(wireSize * 2, 4096))

    override fun serializeByteArray(): ByteArray = byteArraySerializer()

    override fun serializeCompatibility(target: ByteBuffer): Int = compatibilitySerializer(target)

    override fun serializeOptimized(target: ByteBuffer): Int = optimizedSerializer(target)

    override fun deserializeByteArray(): SerializerBenchmarkPayload? = byteArrayDeserializer()

    override fun deserializeCompatibility(source: ByteBuffer): SerializerBenchmarkPayload? =
        compatibilityDeserializer(source)

    override fun deserializeOptimized(source: ByteBuffer): SerializerBenchmarkPayload? = optimizedDeserializer(source)

    override fun precomputedOptimizedSource(): ByteBuffer = optimizedSource

    override fun precomputedFallbackDirectSource(): ByteBuffer? = fallbackDirectSource

    override fun precomputedFallbackReadOnlySource(): ByteBuffer? = fallbackReadOnlySource

    override fun validate() {
        check(wire.isNotEmpty()) { "$name produced an empty ByteArray wire payload." }
        check(payload.semanticallyEquals(deserializeByteArray())) { "$name ByteArray round trip changed the payload." }

        validateSource("compatibility", precomputedOptimizedSource(), ::deserializeCompatibility)
        validateSource("optimized", precomputedOptimizedSource(), ::deserializeOptimized)
        validateWrite("compatibility", ::serializeCompatibility)
        validateWrite("optimized", ::serializeOptimized)
        validateOverflow()
    }

    private fun validateSource(
        path: String,
        source: ByteBuffer,
        operation: (ByteBuffer) -> SerializerBenchmarkPayload?,
    ) {
        val position = source.position()
        val limit = source.limit()
        val order = source.order()
        check(payload.semanticallyEquals(operation(source))) { "$name $path input changed the payload." }
        check(source.position() == position) { "$name $path input changed source position." }
        check(source.limit() == limit) { "$name $path input changed source limit." }
        check(source.order() == order) { "$name $path input changed source byte order." }
    }

    private fun validateWrite(path: String, operation: (ByteBuffer) -> Int) {
        val target = newTarget().apply { position(3) }
        val start = target.position()
        val written = operation(target)
        check(written > 0) { "$name $path output wrote no bytes." }
        check(target.position() == start + written) { "$name $path output reported an inconsistent byte count." }
        val bytes = target.duplicate().apply {
            position(start)
            limit(start + written)
        }.let { view -> ByteArray(view.remaining()).also(view::get) }
        check(payload.semanticallyEquals(deserializeBytes(bytes))) { "$name $path output changed the payload." }
    }

    private fun validateOverflow() {
        val target = ByteBuffer.allocate(maxOf(wireSize - 1, 0))
        val start = target.position()
        try {
            validateTarget(target)
            error("$name optimized output did not reject an undersized target.")
        } catch (expected: java.nio.BufferOverflowException) {
            check(target.position() == start) { "$name overflow did not restore target position." }
        }
    }

    private fun deserializeBytes(bytes: ByteArray): SerializerBenchmarkPayload? {
        val source = sourceBuffer(bytes, SourceKind.HEAP)
        return optimizedDeserializer(source)
    }
}

fun binarySerializerBenchmarkFixture(kind: BinarySerializerKind): SerializerBenchmarkFixture {
    val serializer: BinarySerializer = when (kind) {
        BinarySerializerKind.JDK -> JdkBinarySerializer()
        BinarySerializerKind.KRYO -> KryoBinarySerializer()
        BinarySerializerKind.FORY -> ForyBinarySerializer()
    }
    val compatibility = CompatibilityBinarySerializer(serializer)
    val payload = SerializerBenchmarkPayload.sample()
    val wire = serializer.serialize(payload)
    return DefaultSerializerBenchmarkFixture(
        name = kind.name.lowercase(),
        payload = payload,
        claimEligibleSerialize = kind != BinarySerializerKind.FORY,
        claimEligibleDeserialize = true,
        wire = wire,
        byteArraySerializer = { serializer.serialize(payload) },
        compatibilitySerializer = { compatibility.serializeTo(payload, it) },
        optimizedSerializer = { serializer.serializeTo(payload, it) },
        byteArrayDeserializer = { serializer.deserialize(wire) },
        compatibilityDeserializer = { compatibility.deserializeFrom(it) },
        optimizedDeserializer = { serializer.deserializeFrom(it) },
        optimizedSourceKind = SourceKind.DIRECT,
    )
}

fun jsonSerializerBenchmarkFixture(kind: JsonSerializerKind): SerializerBenchmarkFixture {
    val serializer: JsonSerializer = when (kind) {
        JsonSerializerKind.JACKSON2 -> Jackson2Serializer()
        JsonSerializerKind.JACKSON3 -> Jackson3Serializer()
        JsonSerializerKind.FASTJSON2 -> FastjsonSerializer()
    }
    val compatibility = CompatibilityJsonSerializer(serializer)
    val payload = SerializerBenchmarkPayload.sample()
    val wire = serializer.serialize(payload)
    val optimizedSourceKind =
        if (kind == JsonSerializerKind.FASTJSON2) SourceKind.HEAP else SourceKind.DIRECT
    return DefaultSerializerBenchmarkFixture(
        name = when (kind) {
            JsonSerializerKind.JACKSON2 -> "jackson2"
            JsonSerializerKind.JACKSON3 -> "jackson3"
            JsonSerializerKind.FASTJSON2 -> "fastjson2"
        },
        payload = payload,
        claimEligibleSerialize = kind != JsonSerializerKind.FASTJSON2,
        claimEligibleDeserialize = true,
        wire = wire,
        byteArraySerializer = { serializer.serialize(payload) },
        compatibilitySerializer = { compatibility.serializeTo(payload, it) },
        optimizedSerializer = { serializer.serializeTo(payload, it) },
        byteArrayDeserializer = { serializer.deserialize(wire, SerializerBenchmarkPayload::class.java) },
        compatibilityDeserializer = {
            compatibility.deserializeFrom(it, SerializerBenchmarkPayload::class.java)
        },
        optimizedDeserializer = { serializer.deserializeFrom(it, SerializerBenchmarkPayload::class.java) },
        optimizedSourceKind = optimizedSourceKind,
        includeFallbackSources = kind == JsonSerializerKind.FASTJSON2,
    )
}

fun avroSerializerBenchmarkFixture(): SerializerBenchmarkFixture {
    val serializer: AvroReflectSerializer = DefaultAvroReflectSerializer()
    val compatibility = CompatibilityAvroReflectSerializer(serializer)
    val payload = SerializerBenchmarkPayload.sample()
    val wire = requireNotNull(serializer.serialize(payload)) { "Avro reflect could not serialize benchmark payload." }
    return DefaultSerializerBenchmarkFixture(
        name = "avro-reflect",
        payload = payload,
        claimEligibleSerialize = true,
        claimEligibleDeserialize = true,
        wire = wire,
        byteArraySerializer = {
            requireNotNull(serializer.serialize(payload)) { "Avro reflect could not serialize benchmark payload." }
        },
        compatibilitySerializer = { compatibility.serializeTo(payload, it) },
        optimizedSerializer = { serializer.serializeTo(payload, it) },
        byteArrayDeserializer = { serializer.deserialize(wire, SerializerBenchmarkPayload::class.java) },
        compatibilityDeserializer = {
            compatibility.deserializeFrom(it, SerializerBenchmarkPayload::class.java)
        },
        optimizedDeserializer = { serializer.deserializeFrom(it, SerializerBenchmarkPayload::class.java) },
        optimizedSourceKind = SourceKind.DIRECT,
    )
}

fun serializerBenchmarkFixtures(): List<SerializerBenchmarkFixture> =
    BinarySerializerKind.entries.map(::binarySerializerBenchmarkFixture) +
        JsonSerializerKind.entries.map(::jsonSerializerBenchmarkFixture) +
        avroSerializerBenchmarkFixture()

private enum class SourceKind {
    HEAP,
    DIRECT,
    READ_ONLY_HEAP,
}

private fun sourceBuffer(bytes: ByteArray, kind: SourceKind): ByteBuffer {
    val writable = when (kind) {
        SourceKind.HEAP, SourceKind.READ_ONLY_HEAP -> ByteBuffer.allocate(bytes.size)
        SourceKind.DIRECT -> ByteBuffer.allocateDirect(bytes.size)
    }.apply {
        put(bytes)
        flip()
    }
    return if (kind == SourceKind.READ_ONLY_HEAP) writable.asReadOnlyBuffer() else writable
}
