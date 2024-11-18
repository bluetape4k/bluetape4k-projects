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
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * 비동기 방식으로 Json을 파싱하는 클래스입니다.
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
class AsyncJsonParser(
    private val jsonFactory: JsonFactory = JsonFactory(),
    private val onNodeDone: (root: JsonNode) -> Unit,
) {

    companion object: KLogging()

    private class Stack {
        private val nodes = LinkedList<JsonNode>()

        fun push(node: JsonNode) = nodes.add(node)
        fun pop(): JsonNode = nodes.removeLast()
        fun top(): JsonNode = nodes.last()
        fun topOrNull(): JsonNode? = nodes.lastOrNull()
        val isEmpty: Boolean get() = nodes.isEmpty()
        val isNotEmpty: Boolean get() = !nodes.isEmpty()
    }

    private val parser: NonBlockingJsonParser by lazy {
        jsonFactory.createNonBlockingByteArrayParser() as NonBlockingJsonParser
    }
    private var fieldName: String? = null
    private val stack = Stack()

    fun consume(bytes: ByteArray, length: Int = bytes.size) {
        val feeder = parser.nonBlockingInputFeeder
        var consumed = false
        while (!consumed) {
            if (feeder.needMoreInput()) {
                feeder.feedInput(bytes, 0, length)
                consumed = true
            }

            do {
                val token = parser.nextToken()
                if (token != JsonToken.NOT_AVAILABLE) {
                    buildTree(token)?.let { onNodeDone(it) }
                }
            } while (token != JsonToken.NOT_AVAILABLE)
        }
    }

    /**
     * 전체 Json Tree가 빌드되면, root node 를 반환합니다.
     *
     * @param token
     * @return Json Object의 root node or null if not yet built
     */
    private fun buildTree(token: JsonToken): JsonNode? {
        when (token) {
            JsonToken.FIELD_NAME                      -> {
                assert(stack.isNotEmpty)
                fieldName = parser.currentName()
                return null
            }

            JsonToken.START_OBJECT                    -> {
                stack.push(stack.topOrNull()?.createNode(fieldName) ?: JsonNodeFactory.instance.objectNode())
                return null
            }

            JsonToken.START_ARRAY                     -> {
                stack.push(stack.topOrNull()?.createArray(fieldName) ?: JsonNodeFactory.instance.arrayNode())
                return null
            }

            JsonToken.END_OBJECT, JsonToken.END_ARRAY -> {
                assert(stack.isNotEmpty)
                val current: JsonNode = stack.pop()
                return if (stack.isEmpty) current else null
            }

            JsonToken.VALUE_NUMBER_INT                -> {
                assert(stack.isNotEmpty)
                stack.top().addLong(parser.longValue, fieldName)
                return null
            }

            JsonToken.VALUE_STRING                    -> {
                assert(stack.isNotEmpty)
                stack.top().addString(parser.valueAsString, fieldName)
                return null
            }

            JsonToken.VALUE_NUMBER_FLOAT              -> {
                assert(stack.isNotEmpty)
                stack.top().addDouble(parser.doubleValue, fieldName)
                return null
            }

            JsonToken.VALUE_NULL                      -> {
                assert(stack.isNotEmpty)
                stack.top().addNull(fieldName)
                return null
            }

            JsonToken.VALUE_TRUE                      -> {
                assert(stack.isNotEmpty)
                stack.top().addBoolean(true, fieldName)
                return null
            }

            JsonToken.VALUE_FALSE                     -> {
                assert(stack.isNotEmpty)
                stack.top().addBoolean(false, fieldName)
                return null
            }

            else                                      ->
                throw RuntimeException("Unknown json token $token")
        }
    }
}
