package io.bluetape4k.jackson3

import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.exc.JsonNodeException
import tools.jackson.databind.json.JsonMapper
import java.io.Serializable
import java.math.BigDecimal
import java.nio.charset.StandardCharsets.UTF_8
import java.text.Normalizer

private const val CANONICAL_SEPARATOR = ", "

/**
 * canonical JSON 입력과 출력에 적용할 제한입니다.
 *
 * [CanonicalJson]은 raw [ByteArray]를 읽을 때 [maxBodyBytes]를 먼저 적용하고,
 * Jackson parser에도 같은 입력 제한을 전달합니다. [JsonNode] 진입점은 이미 파싱된
 * 트리이므로 body 제한 대신 문자열, 필드명, 숫자, 깊이, container 원소 수,
 * canonical 출력 제한을 적용합니다. 이 방식으로 제한되지 않은 tree 입력이 정렬이나
 * canonical 출력 상한을 우회하지 못합니다.
 *
 * @param maxBodyBytes raw JSON body에 허용하는 최대 UTF-8 byte 수
 * @param maxDepth root node를 0으로 세고 child마다 1씩 증가하는 최대 깊이
 * @param maxStringLength JSON 문자열 value에 허용하는 최대 문자 수
 * @param maxNameLength object field name에 허용하는 최대 문자 수
 * @param maxNumberLength raw JSON 숫자 token에 허용하는 최대 문자 수
 * @param maxObjectEntries object 하나에 허용하는 최대 field 수
 * @param maxArrayElements array 하나에 허용하는 최대 원소 수
 * @param maxOutputBytes canonical JSON 출력에 허용하는 최대 UTF-8 byte 수
 */
data class CanonicalJsonLimits(
    val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES,
    val maxDepth: Int = DEFAULT_MAX_DEPTH,
    val maxStringLength: Int = DEFAULT_MAX_STRING_LENGTH,
    val maxNameLength: Int = DEFAULT_MAX_NAME_LENGTH,
    val maxNumberLength: Int = DEFAULT_MAX_NUMBER_LENGTH,
    val maxObjectEntries: Int = DEFAULT_MAX_OBJECT_ENTRIES,
    val maxArrayElements: Int = DEFAULT_MAX_ARRAY_ELEMENTS,
    val maxOutputBytes: Int = DEFAULT_MAX_OUTPUT_BYTES,
): Serializable {

    init {
        require(maxBodyBytes > 0) { "maxBodyBytes($maxBodyBytes)는 양수여야 합니다" }
        require(maxDepth in 0..<Int.MAX_VALUE) { "maxDepth($maxDepth)는 0 이상 Int.MAX_VALUE 미만이어야 합니다" }
        require(maxStringLength >= 0) { "maxStringLength($maxStringLength)는 0 이상이어야 합니다" }
        require(maxNameLength >= 0) { "maxNameLength($maxNameLength)는 0 이상이어야 합니다" }
        require(maxNumberLength > 0) { "maxNumberLength($maxNumberLength)는 양수여야 합니다" }
        require(maxObjectEntries >= 0) { "maxObjectEntries($maxObjectEntries)는 0 이상이어야 합니다" }
        require(maxArrayElements >= 0) { "maxArrayElements($maxArrayElements)는 0 이상이어야 합니다" }
        require(maxOutputBytes > 0) { "maxOutputBytes($maxOutputBytes)는 양수여야 합니다" }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        /** Workshop의 기존 canonical JSON 입력 계약과 맞춘 기본 body 제한입니다. */
        const val DEFAULT_MAX_BODY_BYTES: Int = 256 * 1024

        /** Workshop의 기존 canonical JSON 깊이 제한입니다. */
        const val DEFAULT_MAX_DEPTH: Int = 12

        /** Workshop의 기존 canonical JSON 문자열 제한입니다. */
        const val DEFAULT_MAX_STRING_LENGTH: Int = 256 * 1024

        /** Workshop의 기존 canonical JSON field name 제한입니다. */
        const val DEFAULT_MAX_NAME_LENGTH: Int = 200

        /** Jackson의 숫자 token 기본 제한과 같은 기본값입니다. */
        const val DEFAULT_MAX_NUMBER_LENGTH: Int = 1_000

        /** object key 정렬의 CPU와 메모리를 제한하는 기본 field 수입니다. */
        const val DEFAULT_MAX_OBJECT_ENTRIES: Int = 10_000

        /** tree array 순회 비용을 제한하는 기본 원소 수입니다. */
        const val DEFAULT_MAX_ARRAY_ELEMENTS: Int = 10_000

        /** 큰 숫자의 평문 확장을 포함해 canonical 출력에 적용할 기본 제한입니다. */
        const val DEFAULT_MAX_OUTPUT_BYTES: Int = 256 * 1024
    }
}

/** canonical JSON 문자열 value에 적용할 정규화 정책입니다. */
enum class CanonicalJsonStringNormalization {
    /** 입력 문자열을 변경하지 않습니다. */
    NONE,

    /** 문자열 value를 Unicode NFC로 정규화합니다. field name은 정규화하지 않습니다. */
    NFC,
}

/**
 * 제한된 입력을 정렬되고 결정적인 canonical JSON UTF-8 표현으로 변환합니다.
 *
 * 이 API는 RFC/JCS 구현이나 digest/HMAC framing을 제공하지 않습니다. object key는
 * UTF-16 순서로 정렬하고 array 원소 순서는 유지하며, 기존 Workshop 구현의 `", "`
 * separator와 숫자 정규화(`stripTrailingZeros().toPlainString()`)를 보존합니다.
 * 문자열 NFC는 [CanonicalJsonStringNormalization.NFC]를 명시한 경우에만 value에
 * 적용하고 field name에는 적용하지 않습니다.
 * envelope schema와 field allowlist, tenant/key scope, domain exception 변환,
 * persistence 및 retry 정책도 호출자가 소유합니다.
 *
 * parser는 [Jackson.defaultJsonMapper]와 별도로 생성되므로 canonical JSON의 strict
 * 설정이 애플리케이션의 공유 mapper를 변경하지 않습니다. raw [ByteArray] 입력은
 * duplicate key와 trailing token을 거부합니다.
 *
 * ```kotlin
 * val canonical = CanonicalJson(
 *     limits = CanonicalJsonLimits(maxOutputBytes = 64 * 1024),
 *     stringNormalization = CanonicalJsonStringNormalization.NFC,
 * )
 * val bytes = canonical.canonicalBytes("""{"b":1.0,"a":"café"}""".toByteArray())
 * // bytes.decodeToString() == """{"a":"café", "b":1}"""
 * ```
 *
 * @param limits raw 입력, tree 순회 및 출력에 적용할 제한
 * @param stringNormalization 문자열 value 정규화 정책
 */
class CanonicalJson(
    val limits: CanonicalJsonLimits = CanonicalJsonLimits(),
    val stringNormalization: CanonicalJsonStringNormalization = CanonicalJsonStringNormalization.NONE,
) {

    private val mapper: JsonMapper = JsonMapper.builder(
        JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(
                StreamReadConstraints.builder()
                    .maxNestingDepth(limits.maxDepth + 1)
                    .maxStringLength(limits.maxStringLength)
                    .maxNameLength(limits.maxNameLength)
                    .maxNumberLength(limits.maxNumberLength)
                    .maxDocumentLength(limits.maxBodyBytes.toLong())
                    .build(),
            )
            .build(),
    )
        .enable(
            DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
        )
        .build()

    /**
     * raw JSON body를 파싱하고 canonical UTF-8 byte 배열로 변환합니다.
     *
     * @throws IllegalArgumentException body가 비어 있거나 body 제한을 초과한 경우,
     * tree 값이 지원되지 않거나 canonical 출력 제한을 초과한 경우
     * @throws tools.jackson.core.JacksonException JSON이 잘못되었거나 duplicate key,
     * trailing token, parser 제한 위반이 발생한 경우
     */
    fun canonicalBytes(body: ByteArray): ByteArray {
        require(body.isNotEmpty()) { "JSON body는 비어 있을 수 없습니다" }
        require(body.size <= limits.maxBodyBytes) {
            "JSON body(${body.size} bytes)가 maxBodyBytes(${limits.maxBodyBytes})를 초과했습니다"
        }
        return canonicalBytes(readTree(body))
    }

    /**
     * 이미 파싱된 [JsonNode]를 canonical UTF-8 byte 배열로 변환합니다.
     *
     * raw body 길이는 tree에 보존되지 않으므로 [CanonicalJsonLimits.maxBodyBytes]는
     * 적용할 수 없습니다. 대신 모든 tree 값에 깊이, 문자열, field name, 숫자,
     * container 원소 수 및 canonical 출력 제한을 적용합니다.
     *
     * @throws IllegalArgumentException 제한을 초과했거나 지원하지 않는 tree 값인 경우
     */
    fun canonicalBytes(node: JsonNode): ByteArray {
        val output = CanonicalOutput(limits.maxOutputBytes)
        appendNode(node, depth = 0, output)
        return output.toString().toByteArray(UTF_8)
    }

    private fun readTree(body: ByteArray): JsonNode {
        return mapper.readTree(body)
            ?: throw IllegalArgumentException("JSON body는 비어 있을 수 없습니다")
    }

    private fun appendNode(node: JsonNode, depth: Int, output: CanonicalOutput) {
        require(depth <= limits.maxDepth) {
            "JSON depth($depth)가 maxDepth(${limits.maxDepth})를 초과했습니다"
        }
        when {
            node.isObject -> appendObject(node, depth, output)
            node.isArray -> appendArray(node, depth, output)
            node.isString -> appendString(node.stringValue(), output)
            node.isNumber -> appendNumber(node, output)
            node.isBoolean || node.isNull -> output.append(node.toString())
            else -> throw IllegalArgumentException("지원하지 않는 JSON 값입니다: ${node.nodeType}")
        }
    }

    private fun appendObject(node: JsonNode, depth: Int, output: CanonicalOutput) {
        require(node.size() <= limits.maxObjectEntries) {
            "JSON object field 수(${node.size()})가 maxObjectEntries(${limits.maxObjectEntries})를 초과했습니다"
        }
        output.append('{')
        node.properties().asSequence()
            .sortedBy { it.key }
            .forEachIndexed { index, property ->
                if (index > 0) output.append(CANONICAL_SEPARATOR)
                require(property.key.length <= limits.maxNameLength) {
                    "JSON field name 길이가 maxNameLength(${limits.maxNameLength})를 초과했습니다"
                }
                output.append(mapper.writeValueAsString(property.key))
                output.append(':')
                appendNode(property.value, depth + 1, output)
            }
        output.append('}')
    }

    private fun appendArray(node: JsonNode, depth: Int, output: CanonicalOutput) {
        require(node.size() <= limits.maxArrayElements) {
            "JSON array 원소 수(${node.size()})가 maxArrayElements(${limits.maxArrayElements})를 초과했습니다"
        }
        output.append('[')
        node.iterator().asSequence().forEachIndexed { index, child ->
            if (index > 0) output.append(CANONICAL_SEPARATOR)
            appendNode(child, depth + 1, output)
        }
        output.append(']')
    }

    private fun appendString(value: String, output: CanonicalOutput) {
        require(value.length <= limits.maxStringLength) {
            "JSON string 길이가 maxStringLength(${limits.maxStringLength})를 초과했습니다"
        }
        val normalized = when (stringNormalization) {
            CanonicalJsonStringNormalization.NONE -> value
            CanonicalJsonStringNormalization.NFC -> Normalizer.normalize(value, Normalizer.Form.NFC)
        }
        output.append(mapper.writeValueAsString(normalized))
    }

    private fun appendNumber(node: JsonNode, output: CanonicalOutput) {
        val decimal = try {
            node.decimalValue()
        } catch (failure: JsonNodeException) {
            throw IllegalArgumentException("JSON number는 finite 값이어야 합니다", failure)
        }
        require(decimal.precision() <= limits.maxNumberLength) {
            "JSON number 정밀도가 maxNumberLength(${limits.maxNumberLength})를 초과했습니다"
        }
        val sourceLength = decimal.toString().length
        require(sourceLength <= limits.maxNumberLength) {
            "JSON number 길이가 maxNumberLength(${limits.maxNumberLength})를 초과했습니다"
        }

        val normalized = decimal.stripTrailingZeros()
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            output.append("0")
            return
        }

        val plainLength = plainTextLength(normalized)
        output.ensureAscii(plainLength)
        output.append(normalized.toPlainString())
    }

    private fun plainTextLength(value: BigDecimal): Long {
        val precision = value.precision().toLong()
        val scale = value.scale().toLong()
        val signLength = if (value.signum() < 0) 1L else 0L
        return when {
            scale <= 0L -> signLength + precision + -scale
            precision > scale -> signLength + precision + 1L
            else -> signLength + scale + 2L
        }
    }

    private class CanonicalOutput(private val maxBytes: Int) {
        private val builder = StringBuilder()
        private var byteCount: Long = 0L

        fun append(value: String) {
            val valueBytes = value.toByteArray(UTF_8).size.toLong()
            ensure(valueBytes)
            builder.append(value)
            byteCount += valueBytes
        }

        fun append(value: Char) = append(value.toString())

        fun ensureAscii(valueLength: Long) {
            ensure(valueLength)
        }

        private fun ensure(valueLength: Long) {
            require(valueLength <= maxBytes.toLong() - byteCount) {
                "canonical JSON output이 maxOutputBytes($maxBytes)를 초과했습니다"
            }
        }

        override fun toString(): String = builder.toString()
    }
}
