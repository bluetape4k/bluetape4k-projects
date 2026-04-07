package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFailsWith

/**
 * [JsonNodeExtensions] 확장 함수에 대한 테스트입니다.
 */
class JsonNodeExtensionsTest {
    companion object: KLogging()

    private val factory = JsonNodeFactory.instance

    // ─── createNode ────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode에 createNode 호출 시 자식 객체 노드 추가`() {
        val root = factory.objectNode()
        val child = root.createNode("data")
        child.isObject.shouldBeTrue()
        root.has("data").shouldBeTrue()
    }

    @Test
    fun `ArrayNode에 createNode 호출 시 배열 끝에 객체 노드 추가`() {
        val arr = factory.arrayNode()
        val child = arr.createNode(null)
        child.isObject.shouldBeTrue()
        (arr.size() == 1).shouldBeTrue()
    }

    @Test
    fun `기타 노드에 createNode 호출 시 독립 객체 노드 반환`() {
        val textNode = factory.textNode("hello")
        val result = textNode.createNode("x")
        result.isObject.shouldBeTrue()
    }

    // ─── createArray ───────────────────────────────────────────────────────

    @Test
    fun `ObjectNode에 createArray 호출 시 자식 배열 노드 추가`() {
        val root = factory.objectNode()
        val arr = root.createArray("items")
        arr.isArray.shouldBeTrue()
        root.has("items").shouldBeTrue()
    }

    @Test
    fun `ArrayNode에 createArray 호출 시 배열 끝에 배열 노드 추가`() {
        val arr = factory.arrayNode()
        val inner = arr.createArray(null)
        inner.isArray.shouldBeTrue()
        (arr.size() == 1).shouldBeTrue()
    }

    // ─── addValue ──────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode에 addValue 정상 추가`() {
        val root = factory.objectNode()
        root.addValue(42, "count")
        root["count"].asInt() shouldBeEqualTo 42
    }

    @Test
    fun `ArrayNode에 addValue fieldName null 허용`() {
        val arr = factory.arrayNode()
        arr.addValue("hello", null)
        arr[0].asText() shouldBeEqualTo "hello"
    }

    @Test
    fun `ObjectNode에 fieldName null이면 IllegalArgumentException`() {
        val root = factory.objectNode()
        assertFailsWith<IllegalArgumentException> {
            root.addValue(1, null)
        }
    }

    @Test
    fun `ObjectNode에 fieldName blank이면 IllegalArgumentException`() {
        val root = factory.objectNode()
        assertFailsWith<IllegalArgumentException> {
            root.addValue("x", "   ")
        }
    }

    @Test
    fun `addValue 지원하지 않는 타입이면 IllegalArgumentException`() {
        val arr = factory.arrayNode()
        assertFailsWith<IllegalArgumentException> {
            arr.addValue(object {}, null)
        }
    }

    @Test
    fun `addValue null 값은 nullNode로 추가`() {
        val root = factory.objectNode()
        root.addValue(null, "field")
        root["field"].isNull.shouldBeTrue()
    }

    @Test
    fun `addValue 각 기본 타입 왕복`() {
        val root = factory.objectNode()
        root.addValue(true, "bool")
        root.addValue('A', "char")
        root.addValue(1.toByte(), "byte")
        root.addValue(2.toShort(), "short")
        root.addValue(3, "int")
        root.addValue(4L, "long")
        root.addValue(1.5f, "float")
        root.addValue(2.5, "double")
        root.addValue(BigDecimal("9.99"), "bigdec")
        root.addValue(BigInteger("999"), "bigint")
        root.addValue("text", "str")
        root.addValue(byteArrayOf(0x01, 0x02), "bin")

        root["bool"].asBoolean().shouldBeTrue()
        root["char"].asText() shouldBeEqualTo "A"
        root["int"].asInt() shouldBeEqualTo 3
        root["long"].asLong() shouldBeEqualTo 4L
        root["str"].asText() shouldBeEqualTo "text"
    }

    // ─── addNull ───────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode에 addNull 정상 추가`() {
        val root = factory.objectNode()
        root.addNull("deletedAt")
        root["deletedAt"].isNull.shouldBeTrue()
    }

    @Test
    fun `ArrayNode에 addNull 추가`() {
        val arr = factory.arrayNode()
        arr.addNull(null)
        arr[0].isNull.shouldBeTrue()
    }

    @Test
    fun `ObjectNode에 addNull fieldName null이면 IllegalArgumentException`() {
        val root = factory.objectNode()
        assertFailsWith<IllegalArgumentException> {
            root.addNull(null)
        }
    }

    @Test
    fun `ObjectNode에 addNull fieldName blank이면 IllegalArgumentException`() {
        val root = factory.objectNode()
        assertFailsWith<IllegalArgumentException> {
            root.addNull("")
        }
    }

    // ─── 편의 addXxx 함수 ────────────────────────────────────────────────────

    @Test
    fun `addBoolean addString addInt addLong addDouble 편의 함수 동작 확인`() {
        val root = factory.objectNode() as ObjectNode
        root.addBoolean(true, "b")
        root.addString("hello", "s")
        root.addInt(10, "i")
        root.addLong(20L, "l")
        root.addDouble(3.14, "d")

        root["b"].asBoolean().shouldBeTrue()
        root["s"].asText() shouldBeEqualTo "hello"
        root["i"].asInt() shouldBeEqualTo 10
        root["l"].asLong() shouldBeEqualTo 20L
    }

    @Test
    fun `ArrayNode에 addInt 여러 값 추가`() {
        val arr = factory.arrayNode() as ArrayNode
        arr.addInt(1, null)
        arr.addInt(2, null)
        arr.addInt(3, null)
        arr.size() shouldBeEqualTo 3
        arr[1].asInt() shouldBeEqualTo 2
    }
}
