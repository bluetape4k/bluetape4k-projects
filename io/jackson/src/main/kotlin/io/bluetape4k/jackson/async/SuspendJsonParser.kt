package io.bluetape4k.jackson.async

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
import kotlinx.coroutines.flow.Flow
import java.io.Serializable
import java.util.*

/**
 * 코루틴 방식으로 Json을 파싱하는 클래스입니다.
 *
 * ```
 * val parser = AsyncJsonParser { root ->
 *     // root node 가 빌드되면 호출됩니다.
 *     println(root)
 * }
 *
 * parser.consume(bytes)
 * ```
 *
 * @param jsonFactory JsonFactory 인스턴스
 * @param onNodeDone Json Node가 빌드되면 호출되는 콜백
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

    suspend fun consume(flow: Flow<ByteArray>) {
        val feeder = parser.nonBlockingInputFeeder

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
     * @return JSON Object의 root node or null if not yet built
     */
    private fun buildTree(token: JsonToken): JsonNode? {
        try {
            when (token) {
                JsonToken.FIELD_NAME -> {
                    requireNotEmptyStack()
                    currentFieldName = parser.currentName()
                    return null
                }

                JsonToken.START_OBJECT -> {
                    val fieldName = getCurrentFieldName()
                    stack.push(
                        stack.topOrNull()?.node?.createNode(fieldName) ?: JsonNodeFactory.instance.objectNode(),
                        fieldName
                    )
                    return null
                }

                JsonToken.START_ARRAY -> {
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

                JsonToken.VALUE_NUMBER_INT -> {
                    requireNotEmptyStack()
                    stack.top().node.addLong(parser.longValue, getCurrentFieldName())
                    return null
                }

                JsonToken.VALUE_STRING -> {
                    requireNotEmptyStack()
                    stack.top().node.addString(parser.valueAsString, getCurrentFieldName())
                    return null
                }

                JsonToken.VALUE_NUMBER_FLOAT -> {
                    requireNotEmptyStack()
                    stack.top().node.addDouble(parser.doubleValue, getCurrentFieldName())
                    return null
                }

                JsonToken.VALUE_NULL -> {
                    requireNotEmptyStack()
                    stack.top().node.addNull(getCurrentFieldName())
                    return null
                }

                JsonToken.VALUE_TRUE -> {
                    requireNotEmptyStack()
                    stack.top().node.addBoolean(true, getCurrentFieldName())
                    return null
                }

                JsonToken.VALUE_FALSE -> {
                    requireNotEmptyStack()
                    stack.top().node.addBoolean(false, getCurrentFieldName())
                    return null
                }

                else -> error("Unknown json token $token")
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
}
