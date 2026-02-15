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
import io.bluetape4k.feign.bodyAsReader
import io.bluetape4k.feign.isJsonBody
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.actualIteratorTypeArgument
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.uninitialized
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type

/**
 * JSON 배열 응답을 스트리밍 방식의 [Iterator]로 디코딩하는 Jackson Decoder입니다.
 *
 * 반환되는 Iterator는 배열 끝까지 읽거나 파싱에 실패하면 내부 리소스를 정리합니다.
 * 중간에 순회를 중단할 경우, `Closeable`로 캐스팅해 명시적으로 `close()`를 호출해야 합니다.
 *
 * Example:
 *```
 * Feign.builder()
 *   .decoder(JacksonIteratorDecoder2())
 *   .doNotCloseAfterDecode()   // Required to fetch the iterator after the response is processed, need to be close
 *   .target(GitHub::class.java, "https://api.github.com")
 *
 * interface GitHub {
 *   @RequestLine("GET /repos/{owner}/{repo}/contributors")
 *   fun contributors(@Param("owner") owner String, @Param("repo") repo String): Iterator<Contributor>
 * }
 * ```
 */
class JacksonIteratorDecoder2 private constructor(
    private val mapper: JsonMapper,
): Decoder {

    companion object: KLogging() {
        private val fallbackDecoder: Decoder by lazy { Decoder.Default() }
        val INSTANCE: JacksonIteratorDecoder2 by lazy { invoke() }

        /**
         * Feign 연동용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(mapper: JsonMapper = Jackson.defaultJsonMapper): JacksonIteratorDecoder2 {
            return JacksonIteratorDecoder2(mapper)
        }
    }

    /**
     * Feign 연동에서 `decode` 함수를 제공합니다.
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

        private var current: T = uninitialized()

        /**
         * Feign 연동에서 `hasNext` 함수를 제공합니다.
         */
        override fun hasNext(): Boolean {
            if (current == null) {
                current = readNext()
            }
            return current != null
        }

        /**
         * Feign 연동에서 `next` 함수를 제공합니다.
         */
        override fun next(): T {
            if (current != null) {
                val result = current
                current = uninitialized()
                return result
            }
            return readNext() ?: throw NoSuchElementException()
        }

        private fun readNext(): T {
            try {
                var jsonToken: JsonToken? = parser.nextToken() ?: return uninitialized()
                if (jsonToken == JsonToken.START_ARRAY) {
                    jsonToken = parser.nextToken()
                }
                if (jsonToken == JsonToken.END_ARRAY) {
                    closeSafe()
                    return uninitialized()
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
