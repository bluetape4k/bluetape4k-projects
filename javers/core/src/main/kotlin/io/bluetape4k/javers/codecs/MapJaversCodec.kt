package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

class MapJaversCodec: JaversCodec<Map<String, Any?>> {

    override fun encode(jsonElement: JsonObject): Map<String, Any?> {
        return JaversGsonElementConverter.fromJsonObject(jsonElement)
    }

    override fun decode(encodedData: Map<String, Any?>): JsonObject {
        return JaversGsonElementConverter.toJsonObject(encodedData)
    }
}
