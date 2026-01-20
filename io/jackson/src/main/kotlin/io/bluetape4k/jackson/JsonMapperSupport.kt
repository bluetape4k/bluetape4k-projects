package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.bluetape4k.logging.KotlinLogging
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringWriter
import java.net.URL
import kotlin.use

private val log by lazy { KotlinLogging.logger { } }

/**
 * Jackson Json Library 가 제공하는 [JsonMapper] 를 빌드합니다.
 *
 * ```
 * val mapper: JsonMapper = jsonMapper {
 *    findAndAddModules()
 *    enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
 *    enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
 *    enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
 * }
 * ```
 *
 * @param builder JsonMapper 빌더 초기화 람다
 */
inline fun jsonMapper(@BuilderInference builder: JsonMapper.Builder.() -> Unit): JsonMapper {
    return JsonMapper.builder().apply(builder).build()
}

inline fun <reified T> jacksonTypeRef(): TypeReference<T> = object: TypeReference<T>() {}

inline fun <reified T> JsonMapper.readValueOrNull(content: String): T? =
    runCatching { readValue(content, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.readValueOrNull(input: Reader): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.readValueOrNull(input: InputStream): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.readValueOrNull(input: ByteArray, offset: Int = 0, length: Int = input.size): T? =
    runCatching { readValue(input, offset, length, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.readValueOrNull(input: File): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.readValueOrNull(input: URL): T? =
    runCatching {
        input.openStream().use { stream -> readValueOrNull<T>(stream) }
    }.getOrNull()

inline fun <reified T> JsonMapper.readValueOrNull(parser: JsonParser): T? =
    runCatching { readValue(parser, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.convertValueOrNull(from: Any): T? =
    runCatching { convertValue(from, jacksonTypeRef<T>()) }.getOrNull()

inline fun <reified T> JsonMapper.treeToValueOrNull(node: TreeNode): T? =
    runCatching { treeToValue(node, T::class.java) }.getOrNull()

/**
 * 객체를 JSON 형식의 문자열로 변환합니다.
 */
fun <T: Any> JsonMapper.writeAsString(graph: T?): String? =
    graph?.run { writeValueAsString(graph) }

/**
 * JsonNode 를 문자열로 변환합니다.
 */
fun JsonMapper.writeAsString(jsonNode: JsonNode): String {
    return StringWriter().use { writer ->
        createGenerator(writer).use { generator ->
            writeTree(generator, jsonNode)
        }
        writer.toString()
    }
}

/**
 * 객체를 JSON 형식의 [ByteArray]로 변환합니다.
 */
fun <T: Any> JsonMapper.writeAsBytes(graph: T?): ByteArray? =
    graph?.run { writeValueAsBytes(graph) }

/**
 * 객체를 JSON 형식의 읽기편하게 포맷된 문자열로 변환합니다.
 */
fun <T: Any> JsonMapper.prettyWriteAsString(graph: T?): String? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsString(graph) }

/**
 * 객체를 JSON 형식의 읽기편하게 포맷된 [ByteArray]로 변환합니다.
 */
fun <T: Any> JsonMapper.prettyWriteAsBytes(graph: T?): ByteArray? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsBytes(graph) }


/**
 * JsonNode 를 문자열로 변환합니다.
 */
fun JsonMapper.writeTree(jsonNode: JsonNode): String {
    return StringWriter().use { writer ->
        createGenerator(writer).use { generator ->
            writeTree(generator, jsonNode)
        }
        writer.toString()
    }
}
