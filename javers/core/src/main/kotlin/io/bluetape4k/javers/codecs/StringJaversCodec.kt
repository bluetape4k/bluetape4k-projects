package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * [JsonObject]를 JSON 문자열로 인코딩/디코딩하는 기본 코덱.
 *
 * ## 동작/계약
 * - [encode]는 [JsonObject.toString]으로 변환한다
 * - [decode]는 파싱 실패 시 null을 반환한다
 *
 * ```kotlin
 * val codec = StringJaversCodec()
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
open class StringJaversCodec: JaversCodec<String> {

    override fun encode(jsonElement: JsonObject): String {
        return jsonElement.toString()
    }

    override fun decode(encodedData: String): JsonObject? {
        return runCatching { JsonParser.parseString(encodedData) as JsonObject }.getOrNull()
    }
}
