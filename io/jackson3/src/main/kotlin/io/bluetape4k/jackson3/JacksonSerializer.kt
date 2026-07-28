package io.bluetape4k.jackson3

import io.bluetape4k.io.ByteBufferInputStream
import io.bluetape4k.io.ByteBufferOutputStream
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.io.OutputStream
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

private const val JACKSON3_OUTPUT_LIMIT_MESSAGE = "Serialized output exceeds Int.MAX_VALUE bytes."

private class Jackson3CallerOwnedCountingOutputStream(
    private val target: OutputStream,
): OutputStream() {

    var written: Int = 0
        private set

    override fun write(value: Int) {
        val next = checkedCount(1)
        target.write(value)
        written = next
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        val next = checkedCount(length)
        target.write(bytes, offset, length)
        written = next
    }

    override fun flush() = Unit

    override fun close() = Unit

    private fun checkedCount(length: Int): Int =
        try {
            Math.addExact(written, length)
        } catch (failure: ArithmeticException) {
            throw IllegalStateException(JACKSON3_OUTPUT_LIMIT_MESSAGE, failure)
        }
}

/**
 * Jackson 3 기반 [JsonSerializer] 구현체입니다.
 *
 * ## 동작/계약
 * - [serialize]는 입력이 null이면 빈 바이트 배열을 반환합니다.
 * - 역직렬화 계열은 입력이 null이면 null을 반환하고 실패 시 [JsonSerializationException]을 던집니다.
 * - 기본 매퍼는 [Jackson.defaultJsonMapper]입니다.
 * - [ByteBuffer] 출력은 mapper의 stream API로 caller-owned storage에 직접 쓰고, 입력은 duplicate view로 읽어
 *   caller의 position, limit, mark, byte order를 보존합니다.
 * - concrete reified [deserialize]는 parameterized type을 보존하지만 [JsonSerializer] receiver는 기존 raw class
 *   token 동작을 유지합니다.
 *
 * ```kotlin
 * val serializer = JacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * val value: Map<*, *>? = serializer.deserialize(bytes, Map::class.java)
 * // value?.get("id") == 1
 * ```
 *
 * The [issue #1039 evidence](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)
 * accepted lower allocation for measured ByteBuffer output; input was inconclusive.
 *
 * @param mapper JSON 처리에 사용할 ObjectMapper
 */
open class JacksonSerializer(
    val mapper: ObjectMapper = Jackson.defaultJsonMapper,
): JsonSerializer {

    companion object: KLogging()

    /**
     * 객체를 JSON [ByteArray]로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 null이면 빈 배열을 반환합니다.
     * - 직렬화 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val bytes = JacksonSerializer().serialize(mapOf("name" to "debop"))
     * // bytes.isNotEmpty() == true
     * ```
     * @param graph 직렬화할 객체
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null) {
            return emptyByteArray
        }
        return try {
            requireNotNull(mapper.writeAsBytes(graph)) { "mapper.writeAsBytes returned null." }
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to serialize by Jackson3. graphType=${graph.javaClass.name}", e)
        }
    }

    /**
     * 호출자 소유권을 보존하면서 이 serializer에 설정된 mapper로 [graph]를 직접 직렬화합니다
     * [target]. Encoder close drains buffered wire bytes without flushing or closing the caller stream.
     */
    @Throws(IOException::class)
    override fun serializeJsonToStream(graph: Any?, target: OutputStream): Int {
        val source = graph ?: return 0
        val output = Jackson3CallerOwnedCountingOutputStream(target)

        return try {
            val writer = mapper.writer()
            writer.createGenerator(output).use { generator ->
                writer.writeValue(generator, source)
            }
            output.written
        } catch (failure: Throwable) {
            throw jackson3WriteFailure(failure, source)
        }
    }

    /**
     * 호환성 [serialize] 메서드 대신 설정된 mapper를 통해 [target]으로 직렬화합니다.
     * target 위치는 성공할 때만 커밋됩니다. 읽기 전용과 overflow 실패는 원래 buffer 예외로 유지합니다.
     */
    override fun serializeTo(graph: Any?, target: ByteBuffer): Int {
        if (target.isReadOnly) throw ReadOnlyBufferException()
        if (graph == null) return 0

        val start = target.position()
        val view = target.duplicate()
        return try {
            ByteBufferOutputStream.fixed(view).use { output ->
                val writer = mapper.writer()
                writer.createGenerator(output).use { generator ->
                    writer.writeValue(generator, graph)
                }
            }
            val written = view.position() - start
            target.position(start + written)
            written
        } catch (failure: Throwable) {
            target.position(start)
            throw jackson3WriteFailure(failure, graph)
        }
    }

    /**
     * JSON [ByteArray]를 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 null이면 null을 반환합니다.
     * - 파싱/타입 매핑 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val value = JacksonSerializer().deserialize("{\"id\":1}".toByteArray(), Map::class.java)
     * // value?.get("id") == 1
     * ```
     * @param bytes JSON 바이트 배열
     * @param clazz 대상 타입 클래스
     */
    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        if (bytes == null) {
            return null
        }
        return try {
            mapper.readValue(bytes, clazz)
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${clazz.name}", e)
        }
    }

    /**
     * 남은 [source] 범위를 duplicate 기반 stream으로 역직렬화하고 호출자 상태를 보존합니다.
     */
    override fun <T: Any> deserializeFrom(source: ByteBuffer, clazz: Class<T>): T? =
        try {
            ByteBufferInputStream(source.duplicate()).use { input ->
                mapper.readValue(input, clazz)
            }
        } catch (e: Error) {
            throw e
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${clazz.name}", e)
        }

    /**
     * JSON [ByteArray]를 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 null이면 null을 반환합니다.
     * - 파싱/타입 변환 실패 시 [io.bluetape4k.json.JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val value: Map<String, Int>? = JacksonSerializer().deserialize("{\"id\":1}".toByteArray())
     * // value?.get("id") == 1
     * ```
     * @param bytes JSON 바이트 배열
     * @return 역직렬화된 객체. null이거나 실패 시 null 반환
     */
    inline fun <reified T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.let {
            try {
                mapper.readValue(it, jacksonTypeRef<T>())
            } catch (e: Throwable) {
                throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${T::class.java.name}", e)
            }
        }

    /**
     * JSON 문자열을 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [jsonText]가 null이면 null을 반환합니다.
     * - 파싱/타입 변환 실패 시 [io.bluetape4k.json.JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val value: Map<String, Int>? = JacksonSerializer().deserializeFromString("{\"id\":1}")
     * // value?.get("id") == 1
     * ```
     * @param jsonText JSON 문자열
     * @return 역직렬화된 객체. null이거나 실패 시 null 반환
     */
    inline fun <reified T: Any> deserializeFromString(jsonText: String?): T? =
        jsonText?.let {
            try {
                mapper.readValue(it, jacksonTypeRef<T>())
            } catch (e: Throwable) {
                throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${T::class.java.name}", e)
            }
        }
}

/**
 * 남은 [source] 범위를 Jackson type reference로 역직렬화하여 generic type argument를 보존합니다.
 *
 * This stays an extension so adding it does not introduce a final JVM method on the open [JacksonSerializer] class.
 */
inline fun <reified T: Any> JacksonSerializer.deserialize(source: ByteBuffer): T? =
    try {
        ByteBufferInputStream(source.duplicate()).use { input ->
            mapper.readValue(input, jacksonTypeRef<T>())
        }
    } catch (e: Error) {
        throw e
    } catch (e: Throwable) {
        throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${T::class.java.name}", e)
    }

private fun jackson3WriteFailure(failure: Throwable, graph: Any): Throwable {
    findCauseFailure(failure)?.let { return it }
    return JsonSerializationException("Fail to serialize by Jackson3. graphType=${graph.javaClass.name}", failure)
}

private fun findCauseFailure(root: Throwable): Throwable? {
    var current: Throwable? = root
    var depth = 0
    while (current != null && depth < 64) {
        when (current) {
            is Error -> return current
            is BufferOverflowException ->
                return if (current === root) current else BufferOverflowException().apply { initCause(root) }
        }
        current = current.cause
        depth++
    }
    return null
}
