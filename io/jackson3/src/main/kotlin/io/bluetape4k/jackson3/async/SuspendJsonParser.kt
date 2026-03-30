package io.bluetape4k.jackson3.async

import io.bluetape4k.jackson3.addBoolean
import io.bluetape4k.jackson3.addDouble
import io.bluetape4k.jackson3.addLong
import io.bluetape4k.jackson3.addNull
import io.bluetape4k.jackson3.addString
import io.bluetape4k.jackson3.createArray
import io.bluetape4k.jackson3.createNode
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import jakarta.json.stream.JsonParsingException
import kotlinx.coroutines.flow.Flow
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
 * `Flow<ByteArray>` 입력을 코루틴 방식으로 파싱해 JSON 루트 노드를 순차 전달하는 파서입니다.
 *
 * ## 동작/계약
 * - [consume]는 Flow를 수집하며 청크를 non-blocking parser에 공급합니다.
 * - 루트 노드 완성 시마다 [onNodeDone] suspend 콜백을 호출합니다.
 * - 루트가 객체/배열뿐 아니라 문자열, 숫자, 불리언, null 같은 스칼라여도 [JsonNode]로 전달합니다.
 * - 비정상 토큰 시퀀스는 [JsonParsingException]으로 전파됩니다.
 *
 * ## 이런 경우에 적합합니다
 * - `Flow<ByteArray>` 기반 스트리밍 파이프라인
 * - `WebClient`, 파일, 메시지 브로커 응답을 Coroutine으로 이어받아 순차 처리하는 경우
 * - 루트 JSON 단위마다 suspend 후처리가 필요한 경우
 *
 * ## WebClient 연동 예시
 * ```kotlin
 * val parser = SuspendJsonParser { root ->
 *    processNode(root)
 * }
 *
 * val chunkFlow: Flow<ByteArray> = webClient.get()
 *    .uri("/stream/3")
 *    .retrieve()
 *    .bodyToFlux(DataBuffer::class.java)
 *    .map { buffer ->
 *        val bytes = ByteArray(buffer.readableByteCount())
 *        buffer.read(bytes)
 *        DataBufferUtils.release(buffer)
 *        bytes
 *    }
 *    .asFlow()
 *
 * parser.consume(chunkFlow)
 * ```
 *
 * ```kotlin
 * val roots = mutableListOf<JsonNode>()
 * val parser = SuspendJsonParser(onNodeDone = { roots += it })
 * parser.consume(flowOf("{\"id\":1}".toByteArray()))
 * // roots.first()["id"].asInt() == 1
 * ```
 *
 * @param jsonFactory JSON 파서 팩토리
 * @param onNodeDone JSON 노드가 완성될 때 호출되는 suspend 콜백
 * @see AsyncJsonParser
 */
class SuspendJsonParser(
    private val jsonFactory: JsonFactory = JsonFactory(),
    private val onNodeDone: suspend (root: JsonNode) -> Unit,
) {
    companion object : KLoggingChannel()

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
     * [Flow]에서 바이트 배열 청크를 수집하여 JSON을 점진적으로 파싱합니다.
     *
     * ## 동작/계약
     * - Flow 요소를 순서대로 읽어 파서 입력으로 사용합니다.
     * - 루트 완성 시 [onNodeDone]을 호출합니다.
     * - 하나의 Flow 안에 여러 JSON 루트가 연속으로 들어와도 순서대로 처리합니다.
     */
    suspend fun consume(flow: Flow<ByteArray>) {
        val feeder = parser.nonBlockingInputFeeder()

        flow.collect { bytes ->
            if (feeder.needMoreInput()) {
                feeder.feedInput(bytes, 0, bytes.size)
            }

            while (true) {
                val token = parser.nextToken()
                if (token == null || token == JsonToken.NOT_AVAILABLE) {
                    break
                }

                // JsonNode 빌드되면 onNodeDone 호출
                buildTree(token)?.let { onNodeDone(it) }
            }
        }
    }

    /**
     * 전체 Json Tree가 빌드되면, root node 를 반환합니다.
     *
     * @param token
     * @return Json Object의 root node or null if not yet built
     */
    private fun buildTree(token: JsonToken): JsonNode? {
        try {
            when (token) {
                JsonToken.PROPERTY_NAME -> {
                    requireNotEmptyStack()
                    currentFieldName = parser.currentName()
                    return null
                }

                JsonToken.START_OBJECT -> {
                    val fieldName = getCurrentFieldName()
                    stack.push(
                        stack.topOrNull()?.node?.createNode(fieldName) ?: JsonNodeFactory.instance.objectNode(),
                        fieldName,
                    )
                    return null
                }

                JsonToken.START_ARRAY -> {
                    val fieldName = getCurrentFieldName()
                    stack.push(
                        stack.topOrNull()?.node?.createArray(fieldName) ?: JsonNodeFactory.instance.arrayNode(),
                        fieldName,
                    )
                    return null
                }

                JsonToken.END_OBJECT, JsonToken.END_ARRAY -> {
                    requireNotEmptyStack()
                    val current = stack.pop().node
                    return if (stack.isEmpty) current else null
                }

                JsonToken.VALUE_NUMBER_INT -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addLong(parser.longValue, getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_STRING -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addString(parser.valueAsString, getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_NUMBER_FLOAT -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addDouble(parser.doubleValue, getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_NULL -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addNull(getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_TRUE -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addBoolean(true, getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_FALSE -> {
                    return if (stack.isEmpty) {
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
        } catch (e: Exception) {
            log.error(e) { "JSON 파싱 오류: ${e.message}" }
            throw JsonParsingException("JSON 파싱 오류: ${e.message}", e, null)
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
