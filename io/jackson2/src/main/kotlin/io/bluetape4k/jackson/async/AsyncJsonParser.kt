package io.bluetape4k.jackson.async

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ValueNode
import io.bluetape4k.jackson.addBoolean
import io.bluetape4k.jackson.addDouble
import io.bluetape4k.jackson.addLong
import io.bluetape4k.jackson.addNull
import io.bluetape4k.jackson.addString
import io.bluetape4k.jackson.createArray
import io.bluetape4k.jackson.createNode
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import jakarta.json.stream.JsonParsingException
import java.io.Serializable
import java.util.*

/**
 * Non-blocking Jackson 파서로 바이트 청크를 받아 JSON 노드를 점진적으로 완성하는 파서입니다.
 *
 * ## 동작/계약
 * - [consume] 호출 시 입력 청크를 파서에 공급하고 가능한 토큰을 즉시 처리합니다.
 * - 루트 노드가 완성될 때마다 [onNodeDone] 콜백을 호출합니다.
 * - 루트가 객체/배열뿐 아니라 문자열, 숫자, 불리언, null 같은 스칼라여도 [JsonNode]로 전달합니다.
 * - 토큰 시퀀스가 비정상적이면 [JsonParsingException]을 발생시킵니다.
 *
 * ## 이런 경우에 적합합니다
 * - Netty, WebSocket, TCP 리스너처럼 `ByteArray` 청크를 콜백으로 받는 push 스타일 코드
 * - NDJSON/연속 JSON 객체 스트림처럼 메시지 경계를 직접 복원해야 하는 경우
 * - 전체 응답을 문자열로 모두 모으지 않고 루트 노드 단위로 바로 처리하고 싶은 경우
 *
 * ## WebClient 연동 예시
 * ```kotlin
 * val roots = mutableListOf<JsonNode>()
 * val parser = AsyncJsonParser { root -> roots += root }
 *
 * WebClient.builder()
 *    .baseUrl(httpbin.url)
 *    .build()
 *    .get()
 *    .uri("/stream/3")
 *    .retrieve()
 *    .bodyToFlux(DataBuffer::class.java)
 *    .doOnNext { buffer ->
 *        val bytes = ByteArray(buffer.readableByteCount())
 *        buffer.read(bytes)
 *        DataBufferUtils.release(buffer)
 *        parser.consume(bytes)
 *    }
 *    .blockLast()
 * ```
 *
 * ```kotlin
 * val roots = mutableListOf<JsonNode>()
 * val parser = AsyncJsonParser(onNodeDone = { roots += it })
 * parser.consume("{\"id\":1}".toByteArray())
 * // roots.first()["id"].asInt() == 1
 * ```
 *
 * @param jsonFactory 사용할 JsonFactory
 * @param onNodeDone 루트 노드 완성 시 호출할 콜백
 * @see SuspendJsonParser
 */
class AsyncJsonParser(
    private val jsonFactory: JsonFactory = JsonFactory(),
    private val onNodeDone: (root: JsonNode) -> Unit,
) {

    companion object: KLogging()

    private class Stack {
        private val nodes = LinkedList<StackFrame>()

        fun push(node: JsonNode, fieldName: String? = null) = nodes.add(StackFrame(node, fieldName))
        fun pop(): StackFrame = nodes.removeLast()
        fun top(): StackFrame = nodes.last()
        fun topOrNull(): StackFrame? = nodes.lastOrNull()
        val isEmpty: Boolean get() = nodes.isEmpty()
        val isNotEmpty: Boolean get() = !nodes.isEmpty()
    }

    private data class StackFrame(
        val node: JsonNode,
        val fieldName: String? = null,
    ): Serializable

    private val parser: NonBlockingJsonParser by lazy {
        jsonFactory.createNonBlockingByteArrayParser() as NonBlockingJsonParser
    }

    private val stack = Stack()
    private var currentFieldName: String? = null

    /**
     * 현재 처리 중인 필드 이름을 반환하고 사용 후 초기화합니다.
     * 일회성 사용을 보장하여 잘못된 컨텍스트에서의 재사용을 방지합니다.
     */
    private fun getCurrentFieldName(): String? {
        val result = currentFieldName
        currentFieldName = null                 // 사용 후 초기화하여 재사용 방지
        return result
    }

    /**
     * 바이트 배열을 비동기 JSON 파서에 공급합니다.
     *
     * ## 동작/계약
     * - [length]만큼의 바이트를 파서 입력으로 전달합니다.
     * - 토큰이 완성될 때마다 내부 트리를 빌드하고 루트 완성 시 콜백을 실행합니다.
     * - 파싱 중 오류는 [JsonParsingException]으로 전파됩니다.
     * - 연속된 여러 루트 JSON 객체/배열/스칼라를 순서대로 처리할 수 있습니다.
     *
     * ```kotlin
     * parser.consume("{\"name\":\"debop\"}".toByteArray())
     * // onNodeDone이 1회 호출됨
     * ```
     *
     * @param bytes JSON 데이터 청크
     * @param length 처리할 바이트 수
     */
    fun consume(bytes: ByteArray, length: Int = bytes.size) {
        val feeder = parser.nonBlockingInputFeeder

        // 입력이 필요한 경우에만 데이터 제공
        if (feeder.needMoreInput()) {
            feeder.feedInput(bytes, 0, length)
        }

        var token: JsonToken?
        do {
            token = parser.nextToken()
            if (token != null && token != JsonToken.NOT_AVAILABLE) {
                buildTree(token)?.let { onNodeDone(it) }
            }
        } while (token != null && token != JsonToken.NOT_AVAILABLE)
    }

    /**
     * 전체 Json Tree가 빌드되면, root node 를 반환합니다.
     *
     * @param token
     * @return JSON object의 root node or null if not yet built
     */
    private fun buildTree(token: JsonToken): JsonNode? {
        try {
            return parseJsonToken(token)
        } catch (e: Exception) {
            log.error(e) { "JSON 파싱 오류: ${e.message}" }
            throw JsonParsingException("JSON 파싱 오류: ${e.message}", e, null)
        }
    }

    private fun parseJsonToken(token: JsonToken): JsonNode? = when (token) {
        JsonToken.FIELD_NAME -> {
            requireNotEmptyStack()
            currentFieldName = parser.currentName()
            null
        }

        JsonToken.START_OBJECT -> {
            val fieldName = getCurrentFieldName()
            stack.push(
                stack.topOrNull()?.node?.createNode(fieldName) ?: JsonNodeFactory.instance.objectNode(),
                fieldName
            )
            null
        }

        JsonToken.START_ARRAY -> {
            val fieldName = getCurrentFieldName()
            stack.push(
                stack.topOrNull()?.node?.createArray(fieldName) ?: JsonNodeFactory.instance.arrayNode(),
                fieldName
            )
            null
        }

        JsonToken.END_OBJECT, JsonToken.END_ARRAY -> {
            requireNotEmptyStack()
            val current = stack.pop().node
            if (stack.isEmpty) current else null
        }

        JsonToken.VALUE_NUMBER_INT -> {
            if (stack.isEmpty) {
                buildScalarNode(token)
            } else {
                stack.top().node.addLong(parser.longValue, getCurrentFieldName())
                null
            }
        }

        JsonToken.VALUE_STRING -> {
            if (stack.isEmpty) {
                buildScalarNode(token)
            } else {
                stack.top().node.addString(parser.valueAsString, getCurrentFieldName())
                null
            }
        }

        JsonToken.VALUE_NUMBER_FLOAT -> {
            if (stack.isEmpty) {
                buildScalarNode(token)
            } else {
                stack.top().node.addDouble(parser.doubleValue, getCurrentFieldName())
                null
            }
        }

        JsonToken.VALUE_NULL -> {
            if (stack.isEmpty) {
                buildScalarNode(token)
            } else {
                stack.top().node.addNull(getCurrentFieldName())
                null
            }
        }

        JsonToken.VALUE_TRUE -> {
            if (stack.isEmpty) {
                buildScalarNode(token)
            } else {
                stack.top().node.addBoolean(true, getCurrentFieldName())
                null
            }
        }

        JsonToken.VALUE_FALSE -> {
            if (stack.isEmpty) {
                buildScalarNode(token)
            } else {
                stack.top().node.addBoolean(false, getCurrentFieldName())
                null
            }
        }

        else -> error("Unknown json token $token")
    }

    private fun buildScalarNode(token: JsonToken): ValueNode = when (token) {
        JsonToken.VALUE_NUMBER_INT -> JsonNodeFactory.instance.numberNode(parser.longValue)
        JsonToken.VALUE_STRING -> JsonNodeFactory.instance.textNode(parser.valueAsString)
        JsonToken.VALUE_NUMBER_FLOAT -> JsonNodeFactory.instance.numberNode(parser.doubleValue)
        JsonToken.VALUE_NULL -> JsonNodeFactory.instance.nullNode()
        JsonToken.VALUE_TRUE -> JsonNodeFactory.instance.booleanNode(true)
        JsonToken.VALUE_FALSE -> JsonNodeFactory.instance.booleanNode(false)
        else -> error("Unsupported scalar token $token")
    }

    private fun requireNotEmptyStack() {
        if (stack.isEmpty) {
            error("JSON 파싱 오류: 예상치 못한 토큰을 발견했습니다. 파서 상태가 올바르지 않을 수 있습니다.")
        }
    }
}
