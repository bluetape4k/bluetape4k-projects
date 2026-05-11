package io.bluetape4k.jackson.async

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
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
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import jakarta.json.stream.JsonParsingException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import java.io.Serializable
import java.util.*

/**
 * `Flow<ByteArray>` 입력을 코루틴 방식으로 파싱해 JSON 루트 노드를 순차 전달하는 파서입니다.
 *
 * ## 동작/계약
 * - [consume]는 Flow를 수집하며 입력 청크를 non-blocking parser에 공급합니다.
 * - 완료된 입력 Flow라면 [consumeComplete]를 사용해 마지막 JSON이 잘리지 않았는지 검증합니다.
 * - [endOfInput] 또는 [consumeComplete] 호출 후에는 같은 파서를 재사용할 수 없습니다.
 * - 루트 노드가 완성될 때마다 suspend 콜백 [onNodeDone]을 호출합니다.
 * - 루트가 객체/배열뿐 아니라 문자열, 숫자, 불리언, null 같은 스칼라여도 [JsonNode]로 전달합니다.
 * - 토큰 시퀀스가 비정상적이면 [JsonParsingException]을 발생시킵니다.
 *
 * ## 이런 경우에 적합합니다
 * - `Flow<ByteArray>`로 이미 모델링된 스트리밍 파이프라인
 * - `WebClient`, RSocket, 파일 스트림 등 Reactor/Coroutine 브리지를 통해 바이트 청크를 순차 소비하는 경우
 * - 루트 JSON 단위마다 suspend 후처리(저장, 채널 전송, 추가 비동기 호출)가 필요한 경우
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
 * parser.consumeComplete(chunkFlow)
 * ```
 *
 * ```kotlin
 * val roots = mutableListOf<JsonNode>()
 * val parser = SuspendJsonParser(onNodeDone = { roots += it })
 * parser.consume(flowOf("{\"id\":1}".toByteArray()))
 * // roots.first()["id"].asInt() == 1
 * ```
 *
 * @param jsonFactory 사용할 JsonFactory
 * @param onNodeDone 루트 노드 완성 시 호출할 suspend 콜백
 * @see AsyncJsonParser
 */
class SuspendJsonParser(
    private val jsonFactory: JsonFactory = JsonFactory(),
    private val onNodeDone: suspend (root: JsonNode) -> Unit,
) {

    companion object: KLoggingChannel()

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
     * [Flow]에서 바이트 배열 청크를 수집하여 JSON을 점진적으로 파싱합니다.
     *
     * ## 동작/계약
     * - Flow 요소를 순서대로 소비해 파서 입력으로 제공합니다.
     * - 루트 노드가 완성될 때마다 [onNodeDone]을 suspend 호출합니다.
     * - 파싱 예외는 [JsonParsingException]으로 전파됩니다.
     * - 하나의 Flow 안에 여러 JSON 루트가 연속으로 들어와도 순서대로 콜백을 호출합니다.
     * - 이 메서드는 입력 종료를 의미하지 않습니다. 완료된 단일 입력 스트림은 [consumeComplete]를 사용하세요.
     *
     * ```kotlin
     * parser.consume(flowOf("{\"name\":\"debop\"}".toByteArray()))
     * // onNodeDone이 1회 호출됨
     * ```
     * @param flow JSON 바이트 청크 Flow
     * @throws IllegalStateException 이미 [endOfInput]을 호출했거나 Jackson 파서가 더 이상 입력을 받지 않는 경우
     * @throws JsonParseException Jackson이 입력 바이트를 파싱할 수 없는 경우
     * @throws JsonParsingException 토큰 시퀀스가 비정상적인 경우
     */
    suspend fun consume(flow: Flow<ByteArray>) {
        flow.collect { bytes ->
            // consume은 증분 공급 API라서 Flow 완료를 EOF로 보지 않습니다.
            // 기존 호출자는 여러 Flow를 이어 붙여 하나의 논리 스트림을 구성할 수 있습니다.
            feedInput(bytes)
            drainAvailableTokens()
        }
    }

    private suspend fun feedInput(bytes: ByteArray) {
        currentCoroutineContext().ensureActive()
        val feeder = parser.nonBlockingInputFeeder

        // 이전 청크가 남아 있으면 먼저 비워서 새 입력을 조용히 버리는 상황을 막습니다.
        if (!feeder.needMoreInput()) {
            drainAvailableTokens()
        }

        check(feeder.needMoreInput()) {
            "Jackson non-blocking parser is not accepting more input. endOfInput() was likely called; create a new parser for the next logical stream."
        }

        // Jackson non-blocking parser는 이전 청크를 모두 소비한 뒤에만 다음 청크를 받을 수 있습니다.
        feeder.feedInput(bytes, 0, bytes.size)
    }

    /**
     * [Flow]를 하나의 완료된 JSON 입력 스트림으로 소비합니다.
     *
     * [consume]과 달리 Flow 수집이 끝난 뒤 [endOfInput]을 호출하므로 마지막 청크가
     * `{"id":`처럼 잘린 경우 Jackson 파싱 예외가 발생합니다. 부분 Flow를 여러 번 이어 붙이는
     * 사용법에서는 [consume]을 호출하고, 마지막 입력 뒤 [endOfInput]을 직접 호출하세요.
     *
     * ```kotlin
     * val roots = mutableListOf<JsonNode>()
     * val parser = SuspendJsonParser { roots += it }
     * parser.consumeComplete(flowOf("""{"id":1}""".toByteArray()))
     * ```
     *
     * @param flow 완료된 JSON 입력을 구성하는 바이트 청크 Flow
     * @throws IllegalStateException 이미 [endOfInput]을 호출했거나 Jackson 파서가 더 이상 입력을 받지 않는 경우
     * @throws JsonParseException Jackson이 입력 바이트를 파싱할 수 없는 경우
     * @throws JsonParsingException 토큰 시퀀스가 비정상적인 경우
     */
    suspend fun consumeComplete(flow: Flow<ByteArray>) {
        consume(flow)
        endOfInput()
    }

    /**
     * 더 이상 입력이 없음을 파서에 알리고 남은 토큰을 모두 처리합니다.
     *
     * Jackson non-blocking parser는 명시적인 EOF 신호를 받아야 미완성 JSON을 오류로 확정합니다.
     * 모든 [consume] 호출이 끝난 뒤 한 번만 호출하세요.
     *
     * ```kotlin
     * val roots = mutableListOf<JsonNode>()
     * val parser = SuspendJsonParser { roots += it }
     * parser.consume(flowOf("""{"id":1}""".toByteArray()))
     * parser.endOfInput()
     * ```
     *
     * @throws JsonParseException 마지막 JSON 입력이 잘렸거나 Jackson이 입력 바이트를 파싱할 수 없는 경우
     * @throws JsonParsingException 토큰 시퀀스가 비정상적인 경우
     */
    suspend fun endOfInput() {
        parser.nonBlockingInputFeeder.endOfInput()
        drainAvailableTokens()
    }

    private suspend fun drainAvailableTokens() {
        while (true) {
            currentCoroutineContext().ensureActive()

            val token = parser.nextToken()
            if (token == null || token == JsonToken.NOT_AVAILABLE) {
                break
            }

            buildTree(token)?.let { onNodeDone(it) }
        }
    }

    /**
     * 전체 Json Tree가 빌드되면, root node 를 반환합니다.
     *
     * @param token
     * @return JSON Object의 root node or null if not yet built
     */
    private fun buildTree(token: JsonToken): JsonNode? {
        try {
            when (token) {
                JsonToken.FIELD_NAME         -> {
                    requireNotEmptyStack()
                    currentFieldName = parser.currentName()
                    return null
                }

                JsonToken.START_OBJECT       -> {
                    val fieldName = getCurrentFieldName()
                    stack.push(
                        stack.topOrNull()?.node?.createNode(fieldName) ?: JsonNodeFactory.instance.objectNode(),
                        fieldName
                    )
                    return null
                }

                JsonToken.START_ARRAY        -> {
                    val fieldName = getCurrentFieldName()
                    stack.push(
                        stack.topOrNull()?.node?.createArray(fieldName) ?: JsonNodeFactory.instance.arrayNode(),
                        fieldName
                    )
                    return null
                }

                JsonToken.END_OBJECT, JsonToken.END_ARRAY -> {
                    requireNotEmptyStack()
                    val current = stack.pop().node
                    return if (stack.isEmpty) current else null
                }

                JsonToken.VALUE_NUMBER_INT   -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addLong(parser.longValue, getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_STRING       -> {
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

                JsonToken.VALUE_NULL         -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addNull(getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_TRUE         -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addBoolean(true, getCurrentFieldName())
                        null
                    }
                }

                JsonToken.VALUE_FALSE        -> {
                    return if (stack.isEmpty) {
                        buildScalarNode(token)
                    } else {
                        stack.top().node.addBoolean(false, getCurrentFieldName())
                        null
                    }
                }

                else                         -> error("Unknown json token $token")
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

    private fun buildScalarNode(token: JsonToken): ValueNode = when (token) {
        JsonToken.VALUE_NUMBER_INT -> JsonNodeFactory.instance.numberNode(parser.longValue)
        JsonToken.VALUE_STRING     -> JsonNodeFactory.instance.textNode(parser.valueAsString)
        JsonToken.VALUE_NUMBER_FLOAT -> JsonNodeFactory.instance.numberNode(parser.doubleValue)
        JsonToken.VALUE_NULL       -> JsonNodeFactory.instance.nullNode()
        JsonToken.VALUE_TRUE       -> JsonNodeFactory.instance.booleanNode(true)
        JsonToken.VALUE_FALSE      -> JsonNodeFactory.instance.booleanNode(false)
        else                       -> error("Unsupported scalar token $token")
    }
}
