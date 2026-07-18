package io.bluetape4k.benchmark.serializer

import java.io.Serializable

/** Deterministic payload shared by every serializer allocation benchmark cell. */
data class SerializerBenchmarkPayload @JvmOverloads constructor(
    var id: Int = 42,
    var name: String = "bluetape4k-serializer-allocation",
    var timestamp: Long = 1_700_000_000_000L,
    var tags: List<String> = listOf("byte-array", "byte-buffer", "allocation"),
    var payload: ByteArray = ByteArray(1024) { index -> (index * 31).toByte() },
): Serializable {

    fun semanticallyEquals(other: SerializerBenchmarkPayload?): Boolean =
        other != null &&
            id == other.id &&
            name == other.name &&
            timestamp == other.timestamp &&
            tags == other.tags &&
            payload.contentEquals(other.payload)

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun sample(): SerializerBenchmarkPayload = SerializerBenchmarkPayload()
    }
}
