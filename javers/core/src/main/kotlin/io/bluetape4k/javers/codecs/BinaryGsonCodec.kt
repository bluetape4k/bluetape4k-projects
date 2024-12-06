package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging

class BinaryGsonCodec(
    private val serializer: BinarySerializer,
): GsonCodec<ByteArray> {

    companion object: KLogging()

    private val mapCodec: MapGsonCodec = MapGsonCodec()

    override fun encode(jsonElement: JsonObject): ByteArray {
        val map: Map<String, Any?> = mapCodec.encode(jsonElement)
        return serializer.serialize(map)
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        val map: Map<String, Any?>? = serializer.deserialize(encodedData)
        return map?.let { mapCodec.decode(it) }
    }
}
