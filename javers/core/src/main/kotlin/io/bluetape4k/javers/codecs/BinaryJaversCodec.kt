package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging

class BinaryJaversCodec(
    private val serializer: BinarySerializer,
): JaversCodec<ByteArray> {

    companion object: KLogging()

    private val mapCodec: MapJaversCodec = MapJaversCodec()

    override fun encode(jsonElement: JsonObject): ByteArray {
        val map: Map<String, Any?> = mapCodec.encode(jsonElement)
        return serializer.serialize(map)
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        val map: Map<String, Any?>? = serializer.deserialize(encodedData)
        return map?.let { mapCodec.decode(it) }
    }
}
