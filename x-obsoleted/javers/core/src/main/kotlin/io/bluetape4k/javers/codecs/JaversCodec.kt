package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

/**
 * JaVers [JsonObject]를 지정한 형식 [T]로 인코딩/디코딩하는 코덱 인터페이스.
 *
 * ## 동작/계약
 * - [encode]는 [JsonObject]를 대상 형식으로 변환한다
 * - [decode]는 인코딩된 데이터를 [JsonObject]로 복원하거나, 실패 시 null을 반환한다
 *
 * ```kotlin
 * val codec: JaversCodec<String> = JaversCodecs.String
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
interface JaversCodec<T: Any> {

    /**
     * [JsonObject]를 대상 형식 [T]로 인코딩한다.
     */
    fun encode(jsonElement: JsonObject): T

    /**
     * 인코딩된 데이터를 [JsonObject]로 디코딩하거나, 실패 시 null을 반환한다.
     */
    fun decode(encodedData: T): JsonObject?

}
