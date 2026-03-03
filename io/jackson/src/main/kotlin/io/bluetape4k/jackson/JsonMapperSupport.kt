package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper

import io.bluetape4k.logging.KotlinLogging
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringWriter
import java.net.URL
import kotlin.use

private val log by lazy { KotlinLogging.logger { } }

/**
 * [JsonMapper.Builder] DSL로 [JsonMapper]를 생성합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 새 [JsonMapper] 인스턴스를 생성합니다.
 * - [builder] 블록에서 설정한 값만 반영합니다.
 *
 * ```kotlin
 * val mapper = jsonMapper {
 *    findAndAddModules()
 *    enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
 *    enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
 *    enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
 * }
 * // mapper != null
 * ```
 * @param builder 매퍼 빌더 설정 블록
 */
inline fun jsonMapper(@BuilderInference builder: JsonMapper.Builder.() -> Unit): JsonMapper {
    return JsonMapper.builder().apply(builder).build()
}

/** reified 타입 [T]의 Jackson [TypeReference]를 생성합니다. */
inline fun <reified T> jacksonTypeRef(): TypeReference<T> = object: TypeReference<T>() {}

/** JSON 문자열을 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(content: String): T? =
    runCatching { readValue(content, jacksonTypeRef<T>()) }.getOrNull()

/** [Reader]의 JSON 데이터를 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(input: Reader): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/** [InputStream]의 JSON 데이터를 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(input: InputStream): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/** JSON [ByteArray]를 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(input: ByteArray, offset: Int = 0, length: Int = input.size): T? =
    runCatching { readValue(input, offset, length, jacksonTypeRef<T>()) }.getOrNull()

/** [File]의 JSON 데이터를 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(input: File): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/** [URL]의 JSON 데이터를 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(input: URL): T? =
    runCatching {
        input.openStream().use { stream -> readValue(stream, jacksonTypeRef<T>()) }
    }.getOrNull()

/** [JsonParser]의 토큰을 [T]로 역직렬화하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.readValueOrNull(parser: JsonParser): T? =
    runCatching { readValue(parser, jacksonTypeRef<T>()) }.getOrNull()

/** 임의 객체를 [T]로 변환하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.convertValueOrNull(from: Any): T? =
    runCatching { convertValue(from, jacksonTypeRef<T>()) }.getOrNull()

/** [TreeNode]를 [T]로 변환하고 실패 시 null을 반환합니다. */
inline fun <reified T> ObjectMapper.treeToValueOrNull(node: TreeNode): T? =
    runCatching { treeToValue(node, T::class.java) }.getOrNull()

/**
 * 객체를 JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - [graph]가 null이면 null을 반환합니다.
 * - null이 아니면 [ObjectMapper.writeValueAsString] 결과를 반환합니다.
 *
 * ```kotlin
 * val json = mapper.writeAsString(mapOf("id" to 1))
 * // json == "{\"id\":1}"
 * ```
 */
fun <T: Any> ObjectMapper.writeAsString(graph: T?): String? =
    graph?.run { writeValueAsString(graph) }

/**
 * [JsonNode]를 JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - 내부 [com.fasterxml.jackson.core.JsonGenerator]를 사용해 노드를 문자열로 씁니다.
 * - 입력 노드를 변경하지 않습니다.
 *
 * ```kotlin
 * val text = mapper.writeAsString(mapper.createObjectNode().put("id", 1))
 * // text == "{\"id\":1}"
 * ```
 */
fun ObjectMapper.writeAsString(jsonNode: JsonNode): String {
    return StringWriter().use { writer ->
        createGenerator(writer).use { generator ->
            writeTree(generator, jsonNode)
        }
        writer.toString()
    }
}

/**
 * 객체를 JSON 바이트 배열로 직렬화합니다.
 */
fun <T: Any> ObjectMapper.writeAsBytes(graph: T?): ByteArray? =
    graph?.run { writeValueAsBytes(graph) }

/**
 * 객체를 pretty-print JSON 문자열로 직렬화합니다.
 */
fun <T: Any> ObjectMapper.prettyWriteAsString(graph: T?): String? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsString(graph) }

/**
 * 객체를 pretty-print JSON 바이트 배열로 직렬화합니다.
 */
fun <T: Any> ObjectMapper.prettyWriteAsBytes(graph: T?): ByteArray? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsBytes(graph) }

/**
 * [JsonNode]를 JSON 문자열로 직렬화합니다.
 */
fun ObjectMapper.writeTree(jsonNode: JsonNode): String {
    return StringWriter().use { writer ->
        createGenerator(writer).use { generator ->
            writeTree(generator, jsonNode)
        }
        writer.toString()
    }
}
