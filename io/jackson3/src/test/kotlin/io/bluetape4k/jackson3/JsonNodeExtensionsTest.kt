package io.bluetape4k.jackson3

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import jakarta.json.JsonException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFailsWith

/**
 * [JsonNodeExtensions] 에 대한 단위 테스트입니다.
 */
class JsonNodeExtensionsTest {
    companion object : KLogging()

    // ── createNode / createArray ──────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 createNode 호출 시 중첩 ObjectNode 가 생성된다`() {
        val root = JsonNodeFactory.instance.objectNode()
        val child = root.createNode("child")
        child.shouldNotBeNull()
        root.has("child") shouldBeEqualTo true
    }

    @Test
    fun `ArrayNode 에 createNode 호출 시 배열 끝에 ObjectNode 가 추가된다`() {
        val arr = JsonNodeFactory.instance.arrayNode()
        arr.createNode(null)
        arr.size() shouldBeEqualTo 1
    }

    @Test
    fun `ObjectNode 에 createArray 호출 시 중첩 ArrayNode 가 생성된다`() {
        val root = JsonNodeFactory.instance.objectNode()
        val arr = root.createArray("items")
        arr.shouldNotBeNull()
        root.has("items") shouldBeEqualTo true
    }

    @Test
    fun `ArrayNode 에 createArray 호출 시 배열 끝에 ArrayNode 가 추가된다`() {
        val arr = JsonNodeFactory.instance.arrayNode()
        arr.createArray(null)
        arr.size() shouldBeEqualTo 1
    }

    // ── addLong ───────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addLong 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addLong(42L, "count")
        node.get("count").asLong() shouldBeEqualTo 42L
    }

    @Test
    fun `ArrayNode 에 addLong 호출 시 원소가 추가된다`() {
        val node = JsonNodeFactory.instance.arrayNode()
        node.addLong(99L, null)
        node.size() shouldBeEqualTo 1
        node.get(0).asLong() shouldBeEqualTo 99L
    }

    @Test
    fun `지원하지 않는 JsonNode 타입에 addLong 호출 시 JsonException 이 발생한다`() {
        val textNode = JsonNodeFactory.instance.textNode("hello")
        assertFailsWith<JsonException> {
            textNode.addLong(1L, "x")
        }
    }

    // ── addInt ────────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addInt 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addInt(7, "val")
        node.get("val").asInt() shouldBeEqualTo 7
    }

    @Test
    fun `ArrayNode 에 addInt 호출 시 원소가 추가된다`() {
        val node = JsonNodeFactory.instance.arrayNode()
        node.addInt(3, null)
        node.get(0).asInt() shouldBeEqualTo 3
    }

    // ── addString ─────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addString 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addString("hello", "greeting")
        node.get("greeting").asText() shouldBeEqualTo "hello"
    }

    @Test
    fun `ArrayNode 에 addString 호출 시 원소가 추가된다`() {
        val node = JsonNodeFactory.instance.arrayNode()
        node.addString("world", null)
        node.get(0).asText() shouldBeEqualTo "world"
    }

    @Test
    fun `빈 문자열도 정상적으로 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addString("", "empty")
        node.get("empty").asText() shouldBeEqualTo ""
    }

    // ── addDouble ─────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addDouble 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addDouble(3.14, "pi")
        node.get("pi").asDouble() shouldBeEqualTo 3.14
    }

    @Test
    fun `ArrayNode 에 addDouble 호출 시 원소가 추가된다`() {
        val node = JsonNodeFactory.instance.arrayNode()
        node.addDouble(2.71, null)
        (node.get(0).asDouble() - 2.71 < 1e-9) shouldBeEqualTo true
    }

    // ── addFloat ──────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addFloat 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addFloat(1.5f, "rate")
        log.debug { "rate=${node.get("rate")}" }
        node.has("rate") shouldBeEqualTo true
    }

    // ── addBigDecimal ─────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addBigDecimal 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addBigDecimal(BigDecimal("123.456"), "amount")
        node.get("amount").decimalValue() shouldBeEqualTo BigDecimal("123.456")
    }

    // ── addBigInteger ─────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addBigInteger 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addBigInteger(BigInteger("9999999999999999999"), "big")
        node.get("big").bigIntegerValue() shouldBeEqualTo BigInteger("9999999999999999999")
    }

    // ── addShort ──────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addShort 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addShort(32767.toShort(), "maxShort")
        node.get("maxShort").asInt() shouldBeEqualTo 32767
    }

    // ── addBoolean ────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addBoolean true 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addBoolean(true, "flag")
        node.get("flag").asBoolean() shouldBeEqualTo true
    }

    @Test
    fun `ObjectNode 에 addBoolean false 호출 시 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addBoolean(false, "flag")
        node.get("flag").asBoolean() shouldBeEqualTo false
    }

    @Test
    fun `ArrayNode 에 addBoolean 호출 시 원소가 추가된다`() {
        val node = JsonNodeFactory.instance.arrayNode()
        node.addBoolean(true, null)
        node.get(0).asBoolean() shouldBeEqualTo true
    }

    // ── addNull ───────────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 addNull 호출 시 null 필드가 추가된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        node.addNull("nothing")
        node.has("nothing") shouldBeEqualTo true
        node.get("nothing").isNull shouldBeEqualTo true
    }

    @Test
    fun `ArrayNode 에 addNull 호출 시 null 원소가 추가된다`() {
        val node = JsonNodeFactory.instance.arrayNode()
        node.addNull(null)
        node.size() shouldBeEqualTo 1
        node.get(0).isNull shouldBeEqualTo true
    }

    @Test
    fun `지원하지 않는 JsonNode 타입에 addNull 호출 시 JsonException 이 발생한다`() {
        val textNode = JsonNodeFactory.instance.textNode("hello")
        assertFailsWith<JsonException> {
            textNode.addNull("x")
        }
    }

    // ── 복합 시나리오 ─────────────────────────────────────────────────────────

    @Test
    fun `ObjectNode 에 여러 타입의 값을 추가하고 올바르게 읽는다`() {
        val node = JsonNodeFactory.instance.objectNode() as ObjectNode
        node.addString("debop", "name")
        node.addInt(42, "age")
        node.addBoolean(true, "active")
        node.addNull("address")

        node.get("name").asText() shouldBeEqualTo "debop"
        node.get("age").asInt() shouldBeEqualTo 42
        node.get("active").asBoolean() shouldBeEqualTo true
        node.get("address").isNull shouldBeEqualTo true
    }

    @Test
    fun `ArrayNode 에 여러 타입의 값을 추가하고 순서대로 읽는다`() {
        val node = JsonNodeFactory.instance.arrayNode() as ArrayNode
        node.addString("first", null)
        node.addLong(2L, null)
        node.addBoolean(false, null)
        node.addNull(null)

        node.size() shouldBeEqualTo 4
        node.get(0).asText() shouldBeEqualTo "first"
        node.get(1).asLong() shouldBeEqualTo 2L
        node.get(2).asBoolean() shouldBeEqualTo false
        node.get(3).isNull shouldBeEqualTo true
    }
}
