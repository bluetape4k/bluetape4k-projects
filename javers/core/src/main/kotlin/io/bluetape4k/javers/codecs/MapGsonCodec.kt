package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

class MapGsonCodec: GsonCodec<Map<String, Any?>> {

    override fun encode(jsonElement: JsonObject): Map<String, Any?> {
        return GsonElementConverter.fromJsonObject(jsonElement)
    }

    override fun decode(encodedData: Map<String, Any?>): JsonObject {
        return GsonElementConverter.toJsonObject(encodedData)
    }
}
