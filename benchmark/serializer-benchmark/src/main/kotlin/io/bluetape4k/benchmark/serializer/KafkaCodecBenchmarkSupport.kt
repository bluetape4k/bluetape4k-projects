package io.bluetape4k.benchmark.serializer

import io.bluetape4k.kafka.codec.BufferAwareKafkaCodec
import io.bluetape4k.kafka.codec.KryoKafkaCodec
import java.nio.ByteBuffer

/**
 * Precomputes and validates equivalent Kafka codec paths outside timed benchmark cells.
 *
 * The read-only source is reused for deserialization, while callers reuse targets allocated by [newTarget].
 *
 * @param codec codec under measurement
 * @param payload deterministic payload shared by every codec path
 */
class KafkaCodecBenchmarkFixture(
    private val codec: BufferAwareKafkaCodec<Any?> = KryoKafkaCodec(),
    /** Payload shared by the validated codec paths. */
    val payload: SerializerBenchmarkPayload = SerializerBenchmarkPayload.sample(),
) {
    companion object {
        /** Topic name used for codec-only allocation benchmarks. */
        const val TOPIC = "allocation-benchmark"
    }

    private val wire = requireNotNull(codec.serialize(TOPIC, null, payload))
    private val source = ByteBuffer.allocateDirect(wire.size).apply {
        put(wire)
        flip()
    }.asReadOnlyBuffer()

    /** Size of the precomputed wire representation in bytes. */
    val wireSize: Int = wire.size

    /** Allocates a reusable output target outside timed benchmark cells. */
    fun newTarget(): ByteBuffer = ByteBuffer.allocate(maxOf(wireSize * 2, 4096))

    /** Serializes through Kafka's standard ByteArray codec path. */
    fun serializeByteArray(): ByteArray = requireNotNull(codec.serialize(TOPIC, null, payload))

    /** Serializes into a caller-owned target through the optimized codec path. */
    fun serializeOptimized(target: ByteBuffer): Int = codec.serializeTo(TOPIC, payload, target)

    /** Deserializes the precomputed wire bytes through Kafka's standard path. */
    fun deserializeByteArray(): SerializerBenchmarkPayload? =
        codec.deserialize(TOPIC, null, wire) as? SerializerBenchmarkPayload

    /** Deserializes the reused read-only source through the optimized codec path. */
    fun deserializeOptimized(): SerializerBenchmarkPayload? =
        codec.deserializeFrom(TOPIC, source) as? SerializerBenchmarkPayload

    /** Validates path equivalence and reusable buffer contracts before measurement. */
    fun validate() {
        check(wire.isNotEmpty()) { "Kafka Kryo codec produced an empty payload." }
        check(payload.semanticallyEquals(deserializeByteArray())) { "Kafka ByteArray path changed the payload." }

        val sourcePosition = source.position()
        val sourceLimit = source.limit()
        check(payload.semanticallyEquals(deserializeOptimized())) { "Kafka ByteBuffer path changed the payload." }
        check(source.position() == sourcePosition) { "Kafka ByteBuffer input changed source position." }
        check(source.limit() == sourceLimit) { "Kafka ByteBuffer input changed source limit." }

        val target = newTarget().apply { position(3) }
        val start = target.position()
        val written = serializeOptimized(target)
        check(written > 0) { "Kafka ByteBuffer output wrote no bytes." }
        check(target.position() == start + written) { "Kafka ByteBuffer output reported an inconsistent count." }
        val bytes = target.duplicate().apply {
            position(start)
            limit(start + written)
        }.let { view -> ByteArray(view.remaining()).also(view::get) }
        check(payload.semanticallyEquals(codec.deserialize(TOPIC, null, bytes) as? SerializerBenchmarkPayload)) {
            "Kafka ByteBuffer output changed the payload."
        }
    }
}
