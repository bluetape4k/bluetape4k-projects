package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging

/**
 * [JsonObject]를 [BinarySerializer]로 바이너리 직렬화하는 코덱.
 *
 * ## 동작/계약
 * - 내부적으로 [MapJaversCodec]을 사용하여 JsonObject → Map 변환 후 직렬화한다
 * - [decode] 시 역직렬화 실패하면 null을 반환한다
 *
 * ```kotlin
 * val codec = BinaryJaversCodec(BinarySerializers.Kryo)
 * val bytes = codec.encode(jsonObject)
 * val decoded = codec.decode(bytes)
 * // decoded != null
 * ```
 *
 * @property serializer 바이너리 직렬화에 사용할 [BinarySerializer]
 */
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
