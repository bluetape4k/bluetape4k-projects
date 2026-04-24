package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.json.JsonSerializer

/**
 * 다양한 [JsonSerializer] 구현체를 사용하는 [LettuceJsonCodec] 팩토리 모음.
 *
 * Jackson 3 기반 또는 Fastjson2 기반의 [LettuceJsonCodec]을 생성하는 편의 함수를 제공합니다.
 *
 * ```kotlin
 * // reified 타입 버전 (가장 간편)
 * val codec = LettuceJsonCodecs.jackson3<CustomData>()
 * val connection = redisClient.connect(codec)
 * connection.sync().set("key", customData)
 *
 * // Class<V> 명시 버전
 * val codec = LettuceJsonCodecs.fastjson2(CustomData::class.java)
 *
 * // 커스텀 JsonSerializer
 * val codec = LettuceJsonCodecs.codec<CustomData>(mySerializer, CustomData::class.java)
 * ```
 */
object LettuceJsonCodecs {

    /**
     * 지정된 [JsonSerializer]와 [valueType]을 사용하는 [LettuceJsonCodec]을 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceJsonCodecs.codec<MyData>(mySerializer, MyData::class.java)
     * ```
     */
    fun <V: Any> codec(serializer: JsonSerializer, valueType: Class<V>): LettuceJsonCodec<V> =
        LettuceJsonCodec(serializer, valueType)

    /**
     * Jackson 3 [JacksonSerializer]와 명시적 [valueType]을 사용하는 [LettuceJsonCodec]을 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceJsonCodecs.jackson3(CustomData::class.java)
     * ```
     */
    fun <V: Any> jackson3(valueType: Class<V>): LettuceJsonCodec<V> =
        codec(io.bluetape4k.jackson3.JacksonSerializer(), valueType)

    /**
     * Jackson 3 [JacksonSerializer]를 사용하는 [LettuceJsonCodec]을 생성합니다. (reified 버전)
     *
     * ```kotlin
     * val codec = LettuceJsonCodecs.jackson3<CustomData>()
     * ```
     */
    inline fun <reified V: Any> jackson3(): LettuceJsonCodec<V> =
        jackson3(V::class.java)

    /**
     * Fastjson2 [FastjsonSerializer]와 명시적 [valueType]을 사용하는 [LettuceJsonCodec]을 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceJsonCodecs.fastjson2(CustomData::class.java)
     * ```
     */
    fun <V: Any> fastjson2(valueType: Class<V>): LettuceJsonCodec<V> =
        codec(io.bluetape4k.fastjson2.FastjsonSerializer(), valueType)

    /**
     * Fastjson2 [FastjsonSerializer]를 사용하는 [LettuceJsonCodec]을 생성합니다. (reified 버전)
     *
     * ```kotlin
     * val codec = LettuceJsonCodecs.fastjson2<CustomData>()
     * ```
     */
    inline fun <reified V: Any> fastjson2(): LettuceJsonCodec<V> =
        fastjson2(V::class.java)
}
