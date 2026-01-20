package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringWriter
import java.net.URL
import kotlin.use

/**
 * Jackson JSON Library 가 제공하는 [ObjectMapper] 를 빌드합니다.
 *
 * ```
 * val mapper: ObjectMapper = objectMapper {
 *    findAndAddModules()
 * }
 *
 * @param initializer JsonMapper 빌더 초기화 람다
 */
inline fun objectMapper(@BuilderInference builder: JsonMapper.Builder.() -> Unit): ObjectMapper {
    return JsonMapper.builder().apply(builder).build()
}

inline fun <reified T> jacksonTypeReference(): TypeReference<T> = object: TypeReference<T>() {}

inline fun <reified T: Any> ObjectMapper.readValueOrNull(content: String): T? =
    runCatching { readValue(content, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.readValueOrNull(src: Reader): T? =
    runCatching { readValue(src, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.readValueOrNull(src: InputStream): T? =
    runCatching { readValue(src, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.readValueOrNull(src: ByteArray, offset: Int = 0, length: Int = src.size): T? =
    runCatching { readValue(src, offset, length, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.readValueOrNull(src: File): T? =
    runCatching { readValue(src, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.readValueOrNull(src: URL): T? =
    runCatching {
        src.openStream().use { input -> readValueOrNull<T>(input) }
    }.getOrNull()

inline fun <reified T: Any> ObjectMapper.readValueOrNull(parser: JsonParser): T? =
    runCatching { readValue(parser, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.convertValue(fromValue: Any): T? =
    runCatching { convertValue(fromValue, jacksonTypeReference<T>()) }.getOrNull()

inline fun <reified T: Any> ObjectMapper.treeToValueOrNull(node: TreeNode): T? =
    runCatching { treeToValue(node, jacksonTypeReference<T>()) }.getOrNull()

/**
 * 객체를 JSON 형식의 문자열로 변환합니다.
 */
fun <T: Any> ObjectMapper.writeAsString(graph: T?): String? =
    graph?.run { writeValueAsString(graph) }

/**
 * JsonNode 를 문자열로 변환합니다.
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
 * 객체를 JSON 형식의 [ByteArray]로 변환합니다. graph가 null이면 null을 반환합니다.
 */
fun <T: Any> ObjectMapper.writeAsBytes(graph: T?): ByteArray? =
    graph?.run { writeValueAsBytes(graph) }

/**
 * 객체를 JSON 형식의 읽기 편한 포맷의 문자열로 변환합니다.
 * graph가 null이면 null을 반환합니다.
 */
fun <T: Any> ObjectMapper.prettyWriteAsString(graph: T?): String? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsString(graph) }

/**
 * 객체를 JSON 형식의 읽기 편한 포맷의 [ByteArray]로 변환합니다. (굳이 필요 없다 ㅎㅎ)
 * graph가 null이면 null을 반환합니다.
 */
fun <T: Any> ObjectMapper.prettyWriteAsBytes(graph: T?): ByteArray? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsBytes(graph) }


/**
 * JsonNode 를 문자열로 변환합니다.
 */
fun ObjectMapper.writeTree(jsonNode: JsonNode): String {
    return StringWriter().use { writer ->
        createGenerator(writer).use { generator ->
            writeTree(generator, jsonNode)
        }
        writer.toString()
    }
}
