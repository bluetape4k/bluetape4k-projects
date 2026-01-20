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
        return serializer.serialize(mapCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return serializer.deserialize<Map<String, Any?>>(encodedData)?.let {
            mapCodec.decode(it)
        }
    }
}
