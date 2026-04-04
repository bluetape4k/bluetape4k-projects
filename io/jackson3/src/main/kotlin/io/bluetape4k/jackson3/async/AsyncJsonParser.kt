package io.bluetape4k.jackson3.async

import io.bluetape4k.jackson3.addBoolean
import io.bluetape4k.jackson3.addDouble
import io.bluetape4k.jackson3.addLong
import io.bluetape4k.jackson3.addNull
import io.bluetape4k.jackson3.addString
import io.bluetape4k.jackson3.createArray
import io.bluetape4k.jackson3.createNode
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import jakarta.json.stream.JsonParsingException
import tools.jackson.core.JsonToken
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.json.JsonFactory
import tools.jackson.core.json.async.NonBlockingByteArrayJsonParser
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.ValueNode
import java.io.Serializable
import java.util.*

/**
 * Non-blocking Jackson 3 파서로 바이트 청크를 받아 JSON 루트 노드를 완성하는 파서입니다.
 *
 * ## 동작/계약
 * - [consume] 호출 시 입력 청크를 공급하고 가능한 토큰을 즉시 처리합니다.
 * - 루트 노드가 완성될 때마다 [onNodeDone] 콜백을 호출합니다.
 * - 루트가 객체/배열뿐 아니라 문자열, 숫자, 불리언, null 같은 스칼라여도 [JsonNode]로 전달합니다.
 * - 비정상 토큰 시퀀스는 [JsonParsingException]으로 전파됩니다.
 *
 * ## 이런 경우에 적합합니다
 * - Netty, WebSocket, TCP 리스너처럼 `ByteArray` 청크를 콜백으로 받는 push 스타일 코드
 * - NDJSON/연속 JSON 객체 스트림처럼 메시지 경계를 직접 복원해야 하는 경우
 * - 전체 응답을 모두 버퍼링하지 않고 루트 노드 단위로 바로 처리하고 싶은 경우
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
 * @param jsonFactory JSON 파서 팩토리
 * @param onNodeDone JSON 노드가 완성될 때 호출되는 콜백
 * @see SuspendJsonParser
 */
class AsyncJsonParser(
    private val jsonFactory: JsonFactory = JsonFactory(),
    private val onNodeDone: (root: JsonNode) -> Unit,
) {
    companion object : KLogging()

    private class Stack : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }

        private val nodes = LinkedList<StackFrame>()

        fun push(
            node: JsonNode,
            fieldName: String? = null,
        ) = nodes.add(StackFrame(node, fieldName))

        fun pop(): StackFrame = nodes.removeLast()

        fun top(): StackFrame = nodes.last()

        fun topOrNull(): StackFrame? = nodes.lastOrNull()

        val isEmpty: Boolean get() = nodes.isEmpty()
        val isNotEmpty: Boolean get() = !nodes.isEmpty()
    }

    private data class StackFrame(
        val node: JsonNode,
        val fieldName: String? = null,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private val parser: NonBlockingByteArrayJsonParser by lazy {
        jsonFactory.createNonBlockingByteArrayParser(ObjectReadContext.empty()) as NonBlockingByteArrayJsonParser
    }

    private val stack = Stack()
    private var currentFieldName: String? = null

    /**
     * 현재 처리 중인 필드 이름을 반환하고 사용 후 초기화합니다.
     * 일회성 사용을 보장하여 잘못된 컨텍스트에서의 재사용을 방지합니다.
     */
    private fun getCurrentFieldName(): String? {
        val result = currentFieldName
        currentFieldName = null // 사용 후 초기화하여 재사용 방지
        return result
    }

    /**
     * 바이트 배열 청크를 파서에 공급합니다.
     *
     * ## 동작/계약
     * - [length]만큼 입력을 공급합니다.
     * - 루트 노드가 완성되면 [onNodeDone]을 호출합니다.
     * - 연속된 여러 루트 JSON 객체/배열/스칼라도 순서대로 처리할 수 있습니다.
     * - 파싱 중 오류는 [jakarta.json.stream.JsonParsingException]으로 전파됩니다.
     *
     * ```kotlin
     * parser.consume("{\"name\":\"debop\"}".toByteArray())
     * // onNodeDone이 1회 호출됨
     * ```
     *
     * @param bytes JSON 데이터 청크
     * @param length 처리할 바이트 수
     */
    fun consume(
        bytes: ByteArray,
        length: Int = bytes.size,
    ) {
        val feeder = parser.nonBlockingInputFeeder()

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
     * @return Json Object의 root node or null if not yet built
     */
    private fun buildTree(token: JsonToken): JsonNode? {
        try {
            return parseJsonToken(token)
        } catch (e: Exception) {
            log.error(e) { "JSON 파싱 오류" }
            throw JsonParsingException("JSON 파싱 오류", e, null)
        }
    }

    private fun parseJsonToken(token: JsonToken): JsonNode? =
        when (token) {
            JsonToken.PROPERTY_NAME -> {
                requireNotEmptyStack()
                currentFieldName = parser.currentName()
                null
            }

            JsonToken.START_OBJECT -> {
                val fieldName = getCurrentFieldName()
                stack.push(
                    stack.topOrNull()?.node?.createNode(fieldName) ?: JsonNodeFactory.instance.objectNode(),
                    fieldName,
                )
                null
            }

            JsonToken.START_ARRAY -> {
                val fieldName = getCurrentFieldName()
                stack.push(
                    stack.topOrNull()?.node?.createArray(fieldName) ?: JsonNodeFactory.instance.arrayNode(),
                    fieldName,
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

            else -> {
                error("Unknown json token $token")
            }
        }

    private fun requireNotEmptyStack() {
        if (stack.isEmpty) {
            error("JSON 파싱 오류: 예상치 못한 토큰을 발견했습니다. 파서 상태가 올바르지 않을 수 있습니다.")
        }
    }

    private fun buildScalarNode(token: JsonToken): ValueNode =
        when (token) {
            JsonToken.VALUE_NUMBER_INT -> JsonNodeFactory.instance.numberNode(parser.longValue)
            JsonToken.VALUE_STRING -> JsonNodeFactory.instance.stringNode(parser.valueAsString)
            JsonToken.VALUE_NUMBER_FLOAT -> JsonNodeFactory.instance.numberNode(parser.doubleValue)
            JsonToken.VALUE_NULL -> JsonNodeFactory.instance.nullNode()
            JsonToken.VALUE_TRUE -> JsonNodeFactory.instance.booleanNode(true)
            JsonToken.VALUE_FALSE -> JsonNodeFactory.instance.booleanNode(false)
            else -> error("Unsupported scalar token $token")
        }
}
