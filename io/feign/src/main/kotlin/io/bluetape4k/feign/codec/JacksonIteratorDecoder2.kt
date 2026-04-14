package io.bluetape4k.feign.codec

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import com.fasterxml.jackson.databind.json.JsonMapper
import feign.Response
import feign.Util
import feign.codec.DecodeException
import feign.codec.Decoder
import feign.codec.DefaultDecoder
import io.bluetape4k.feign.bodyAsReader
import io.bluetape4k.feign.isJsonBody
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.actualIteratorTypeArgument
import io.bluetape4k.support.closeSafe
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type

/**
 * JSON 배열 응답을 스트리밍 [Iterator]로 디코딩하는 Jackson Decoder입니다.
 *
 * ## 동작/계약
 * - JSON 응답이면 iterator 기반 스트리밍 디코딩을 수행합니다.
 * - JSON이 아니면 [Decoder.Default]로 위임합니다.
 * - 반환 iterator를 끝까지 소비하지 않으면 `Closeable`로 `close()`를 호출해 리소스를 정리해야 합니다.
 *
 * ```kotlin
 * val decoder = JacksonIteratorDecoder2()
 * // Iterator 응답 타입에서 요소를 순차 소비할 수 있음
 * ```
 */
class JacksonIteratorDecoder2 private constructor(
    private val mapper: JsonMapper,
): Decoder {

    companion object: KLogging() {
        private val fallbackDecoder: Decoder by lazy { DefaultDecoder() }
        val INSTANCE: JacksonIteratorDecoder2 by lazy { invoke() }

        /**
         * [JacksonIteratorDecoder2] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [mapper] 기본값은 [Jackson.defaultJsonMapper]입니다.
         *
         * ```kotlin
         * val decoder = JacksonIteratorDecoder2()
         * // decoder != null
         * ```
         */
        @JvmStatic
        operator fun invoke(mapper: JsonMapper = Jackson.defaultJsonMapper): JacksonIteratorDecoder2 {
            return JacksonIteratorDecoder2(mapper)
        }
    }

    /**
     * Feign 연동에서 `decode` 함수를 제공합니다.
     *
     * ```kotlin
     * val decoder = JacksonIteratorDecoder2()
     * // Iterator<T> 반환 타입에서 JSON 배열을 순차적으로 디코딩
     * // non-JSON 응답 -> 기본 Feign decoder로 위임
     * ```
     */
    override fun decode(response: Response, type: Type): Any? = when {
        response.isJsonBody() -> jsonDecode(response, type)
        else                  -> fallback(response, type)
    }

    private fun jsonDecode(response: Response, type: Type): Any? {
        if (response.status() == 204 || response.status() == 404) {
            return Util.emptyValueOf(type)
        }
        if (response.body() == null) {
            return null
        }

        var reader: Reader = response.bodyAsReader()

        if (!reader.markSupported()) {
            reader = BufferedReader(reader, 1)
        }
        try {
            // 데이터가 있는지 첫번재 byte를 읽어본다
            reader.mark(1)
            if (reader.read() == -1) {
                // "No content to map due to end-of-input" 예외를 막기 위해 먼저 반환해버린다.
                return null
            }
            reader.reset()
            return JacksonIterator<Any?>(type.actualIteratorTypeArgument(), mapper, response, reader)
        } catch (e: RuntimeJsonMappingException) {
            reader.closeSafe()
            if (e.cause is IOException) {
                throw e.cause as IOException
            }
            throw DecodeException(
                response.status(),
                "$type is not a type supported by JacksonIteratorDecoder2",
                response.request()
            )
        } catch (e: Throwable) {
            reader.closeSafe()
            throw e
        }
    }

    private fun fallback(response: Response, type: Type): Any? {
        return fallbackDecoder.decode(response, type)
    }

    /**
     * Feign 연동에서 사용하는 `JacksonIterator` 타입입니다.
     */
    class JacksonIterator<T>(
        type: Type,
        mapper: JsonMapper,
        private val response: Response,
        reader: Reader,
    ): Iterator<T>, Closeable {

        private val parser: JsonParser = mapper.factory.createParser(reader)
        private val objectReader: ObjectReader = mapper.reader().forType(mapper.constructType(type))

        private var nextLoaded = false
        private var nextElement: T? = null

        /**
         * Feign 연동에서 `hasNext` 함수를 제공합니다.
         *
         * ```kotlin
         * val iterator = decoder.decode(response, iteratorType) as Iterator<*>
         * val hasMore = iterator.hasNext()
         * // hasMore == true  (응답에 요소가 있을 때)
         * ```
         */
        override fun hasNext(): Boolean {
            if (!nextLoaded) {
                nextElement = readNext()
                nextLoaded = true
            }
            return nextElement != null
        }

        /**
         * Feign 연동에서 `next` 함수를 제공합니다.
         *
         * ```kotlin
         * val iterator = decoder.decode(response, iteratorType) as Iterator<*>
         * if (iterator.hasNext()) {
         *     val element = iterator.next()
         *     // element is the next decoded item
         * }
         * ```
         */
        override fun next(): T {
            if (!nextLoaded) {
                nextElement = readNext()
                nextLoaded = true
            }
            if (nextElement == null) throw NoSuchElementException()
            @Suppress("UNCHECKED_CAST")
            val result = nextElement as T
            nextElement = null
            nextLoaded = false
            return result
        }

        private fun readNext(): T? {
            try {
                var jsonToken: JsonToken? = parser.nextToken() ?: return null
                if (jsonToken == JsonToken.START_ARRAY) {
                    jsonToken = parser.nextToken()
                }
                if (jsonToken == JsonToken.END_ARRAY) {
                    closeSafe()
                    return null
                }
                return objectReader.readValue(parser)
            } catch (e: IOException) {
                throw DecodeException(response.status(), "Failed to parse stream", response.request(), e)
            }
        }

        /**
         * 파서와 응답 본문 리소스를 정리합니다.
         */
        override fun close() {
            runCatching { parser.close() }
            runCatching { response.body()?.close() }
        }
    }
}
