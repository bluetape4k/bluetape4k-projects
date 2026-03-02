package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

/**
 * [JsonObject]를 `Map<String, Any?>`로 변환하는 코덱.
 *
 * ## 동작/계약
 * - [JaversGsonElementConverter]를 사용하여 양방향 변환을 수행한다
 * - [decode]는 항상 non-null [JsonObject]를 반환한다
 *
 * ```kotlin
 * val codec = MapJaversCodec()
 * val map = codec.encode(jsonObject)
 * val restored = codec.decode(map)
 * // restored.toString() == jsonObject.toString()
 * ```
 */
class MapJaversCodec: JaversCodec<Map<String, Any?>> {

    override fun encode(jsonElement: JsonObject): Map<String, Any?> {
        return JaversGsonElementConverter.fromJsonObject(jsonElement)
    }

    override fun decode(encodedData: Map<String, Any?>): JsonObject {
        return JaversGsonElementConverter.toJsonObject(encodedData)
    }
}
