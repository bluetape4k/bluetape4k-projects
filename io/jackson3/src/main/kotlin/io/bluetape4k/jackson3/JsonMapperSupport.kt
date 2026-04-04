package io.bluetape4k.jackson3

import io.bluetape4k.logging.KotlinLogging
import tools.jackson.core.JsonParser
import tools.jackson.core.TreeNode
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringWriter
import java.nio.file.Path
import kotlin.use

private val log by lazy { KotlinLogging.logger { } }

/**
 * [JsonMapper.Builder] DSL로 Jackson 3 매퍼를 생성합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 새 [JsonMapper]를 생성합니다.
 * - [builder] 블록 설정만 반영되며 기존 매퍼를 mutate하지 않습니다.
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
inline fun jsonMapper(builder: JsonMapper.Builder.() -> Unit): JsonMapper {
    return JsonMapper.builder().apply(builder).build()
}

/**
 * reified 타입 [T]의 Jackson 3 [TypeReference]를 생성합니다.
 *
 * ```kotlin
 * val typeRef = jacksonTypeRef<List<String>>()
 * // typeRef.type == List<String> TypeReference
 * ```
 */
inline fun <reified T> jacksonTypeRef(): TypeReference<T> = object: TypeReference<T>() {}

/**
 * JSON 문자열을 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val value: Map<String, Int>? = mapper.readValueOrNull("{\"id\":1}")
 * // value?.get("id") == 1
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(content: String): T? =
    runCatching { readValue(content, jacksonTypeRef<T>()) }.getOrNull()

/**
 * [Reader]에서 JSON 데이터를 읽어 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val value: Map<String, Int>? = mapper.readValueOrNull(StringReader("{\"id\":1}"))
 * // value?.get("id") == 1
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(input: Reader): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/**
 * [InputStream]에서 JSON 데이터를 읽어 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val value: Map<String, Int>? = mapper.readValueOrNull("{\"id\":1}".byteInputStream())
 * // value?.get("id") == 1
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(input: InputStream): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/**
 * JSON [ByteArray]를 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val value: Map<String, Int>? = mapper.readValueOrNull("{\"id\":1}".toByteArray())
 * // value?.get("id") == 1
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(input: ByteArray, offset: Int = 0, length: Int = input.size): T? =
    runCatching { readValue(input, offset, length, jacksonTypeRef<T>()) }.getOrNull()

/**
 * [File]에서 JSON 데이터를 읽어 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val value: Map<String, Int>? = mapper.readValueOrNull(File("data.json"))
 * // value != null (파일이 올바른 JSON인 경우)
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(input: File): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/**
 * [Path]에서 JSON 데이터를 읽어 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val value: Map<String, Int>? = mapper.readValueOrNull(Path.of("data.json"))
 * // value != null (경로가 유효하고 올바른 JSON인 경우)
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(input: Path): T? =
    runCatching { readValue(input, jacksonTypeRef<T>()) }.getOrNull()

/**
 * [JsonParser]에서 토큰을 읽어 reified 타입 [T]의 객체로 역직렬화합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val parser = mapper.createParser("{\"id\":1}")
 * val value: Map<String, Int>? = mapper.readValueOrNull(parser)
 * // value?.get("id") == 1
 * ```
 */
inline fun <reified T> ObjectMapper.readValueOrNull(parser: JsonParser): T? =
    runCatching { readValue(parser, jacksonTypeRef<T>()) }.getOrNull()

/**
 * 임의 객체를 reified 타입 [T]로 변환합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val map = mapOf("id" to 1, "name" to "debop")
 * data class User(val id: Int, val name: String)
 * val user: User? = mapper.convertValueOrNull(map)
 * // user?.name == "debop"
 * ```
 */
inline fun <reified T> ObjectMapper.convertValueOrNull(from: Any): T? =
    runCatching { convertValue(from, jacksonTypeRef<T>()) }.getOrNull()

/**
 * [TreeNode]를 reified 타입 [T]의 객체로 변환합니다. 실패 시 null 반환
 */
@Deprecated(
    "TreeNode 대신 JsonNode를 받는 오버로드를 사용하세요. " +
        "val jsonNode = treeNode as? JsonNode 로 변환 후 사용하거나, " +
        "ObjectMapper.treeToValue(treeNode, T::class.java) 를 직접 호출하세요.",
    replaceWith = ReplaceWith("treeToValueOrNull<T>(treeNode as tools.jackson.databind.JsonNode)")
)
inline fun <reified T> ObjectMapper.treeToValueOrNull(treeNode: TreeNode): T? =
    runCatching { treeToValue(treeNode as JsonNode, T::class.java) }.getOrNull()

/**
 * [JsonNode]를 reified 타입 [T]의 객체로 변환합니다. 실패 시 null 반환
 *
 * ```kotlin
 * val node = mapper.readTree("{\"id\":1}")
 * data class User(val id: Int)
 * val user: User? = mapper.treeToValueOrNull(node)
 * // user?.id == 1
 * ```
 */
inline fun <reified T> ObjectMapper.treeToValueOrNull(jsonNode: JsonNode): T? =
    runCatching { treeToValue(jsonNode, T::class.java) }.getOrNull()

/**
 * 객체를 JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - [graph]가 null이면 null을 반환합니다.
 * - null이 아니면 [ObjectMapper.writeValueAsString] 결과를 반환합니다.
 */
fun <T: Any> ObjectMapper.writeAsString(graph: T?): String? =
    graph?.run { writeValueAsString(graph) }

/**
 * [JsonNode]를 JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - 내부 [tools.jackson.core.JsonGenerator]를 사용해 노드를 문자열로 씁니다.
 * - 입력 노드를 변경하지 않습니다.
 *
 * ```kotlin
 * val node = mapper.readTree("{\"id\":1}")
 * val text = mapper.writeAsString(node)
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
 * 객체를 JSON 형식의 [ByteArray]로 직렬화합니다.
 *
 * ## 동작/계약
 * - [graph]가 null이면 null을 반환합니다.
 * - null이 아니면 JSON 바이트 배열을 반환합니다.
 *
 * ```kotlin
 * val bytes = mapper.writeAsBytes(mapOf("id" to 1))
 * // bytes?.isNotEmpty() == true
 * ```
 */
fun <T: Any> ObjectMapper.writeAsBytes(graph: T?): ByteArray? =
    graph?.run { writeValueAsBytes(graph) }

/**
 * 객체를 pretty-print JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - [graph]가 null이면 null을 반환합니다.
 * - null이 아니면 들여쓰기가 적용된 JSON 문자열을 반환합니다.
 *
 * ```kotlin
 * val json = mapper.prettyWriteAsString(mapOf("id" to 1))
 * // json?.contains("\n") == true
 * ```
 */
fun <T: Any> ObjectMapper.prettyWriteAsString(graph: T?): String? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsString(graph) }

/**
 * 객체를 pretty-print JSON 바이트 배열로 직렬화합니다.
 *
 * ## 동작/계약
 * - [graph]가 null이면 null을 반환합니다.
 * - null이 아니면 들여쓰기가 적용된 JSON 바이트 배열을 반환합니다.
 *
 * ```kotlin
 * val bytes = mapper.prettyWriteAsBytes(mapOf("id" to 1))
 * // bytes?.isNotEmpty() == true
 * ```
 */
fun <T: Any> ObjectMapper.prettyWriteAsBytes(graph: T?): ByteArray? =
    graph?.run { writerWithDefaultPrettyPrinter().writeValueAsBytes(graph) }

/**
 * [JsonNode]를 JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - [writeAsString]과 동일한 결과를 반환합니다.
 * - 기존 Jackson 2 계열 API와의 호환을 위해 유지합니다.
 *
 * @deprecated [writeAsString]으로 대체되었습니다.
 */
@Deprecated(
    "writeAsString(JsonNode)으로 대체되었습니다.",
    replaceWith = ReplaceWith("writeAsString(jsonNode)"),
)
fun ObjectMapper.writeTree(jsonNode: JsonNode): String = writeAsString(jsonNode)

/**
 * 등록된 Jackson 모듈의 이름 목록을 반환합니다.
 *
 * ```kotlin
 * val names = mapper.registeredModuleNames()
 * // names.contains("KotlinModule") == true
 * ```
 */
fun ObjectMapper.registeredModuleNames(): List<String> =
    registeredModules().map { it.moduleName }

/**
 * 등록된 Jackson 모듈의 ID 목록을 반환합니다.
 *
 * ```kotlin
 * val ids = mapper.registeredModuleIds()
 * // ids.isNotEmpty() == true
 * ```
 */
fun ObjectMapper.registeredModuleIds(): List<Any> =
    registeredModules().map { it.registrationId }
