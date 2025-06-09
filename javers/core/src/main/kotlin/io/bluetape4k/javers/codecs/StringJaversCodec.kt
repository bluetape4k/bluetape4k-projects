package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import com.google.gson.JsonParser

open class StringJaversCodec: JaversCodec<String> {

    override fun encode(jsonElement: JsonObject): String {
        return jsonElement.toString()
    }

    override fun decode(encodedData: String): JsonObject? {
        return runCatching { JsonParser.parseString(encodedData) as JsonObject }.getOrNull()
    }
}
