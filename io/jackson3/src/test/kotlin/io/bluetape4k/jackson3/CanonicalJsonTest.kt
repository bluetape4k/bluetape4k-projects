package io.bluetape4k.jackson3

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.HexFormat

class CanonicalJsonTest {

    @Test
    fun `canonical JSON은 key를 정렬하고 배열 순서와 기존 separator를 보존한다`() {
        val body = """{"z":[1,2,3],"a":{"value":1.00e+2,"zero":-0.0},"text":"e\u0301","escaped":"quote\""}"""

        CanonicalJson().canonicalString(body.toByteArray()) shouldBeEqualTo
            """{"a":{"value":100, "zero":0}, "escaped":"quote\"", "text":"é", "z":[1, 2, 3]}"""
    }

    @Test
    fun `canonical JSON은 선택적으로 문자열 value만 NFC 정규화한다`() {
        val body = """{"value":"e\u0301","é":"e\u0301"}""".toByteArray()

        CanonicalJson().canonicalString(body) shouldBeEqualTo """{"é":"é", "value":"é"}"""
        CanonicalJson(stringNormalization = CanonicalJsonStringNormalization.NFC)
            .canonicalString(body) shouldBeEqualTo """{"é":"é", "value":"é"}"""
    }

    @Test
    fun `canonical JSON은 duplicate key와 trailing token을 거부하고 기본 mapper를 변경하지 않는다`() {
        val canonicalJson = CanonicalJson()

        val duplicate = assertFailsWith<Exception> {
            canonicalJson.canonicalBytes("""{"value":1,"value":2}""".toByteArray())
        }
        duplicate.message.orEmpty() shouldContain "Duplicate"

        val trailing = assertFailsWith<Exception> {
            canonicalJson.canonicalBytes("{} {}".toByteArray())
        }
        trailing.message.orEmpty() shouldContain "Trailing token"

        Jackson.defaultJsonMapper.readTree("""{"value":1,}""")
    }

    @Test
    fun `raw JSON 입력은 body와 구조 제한을 적용한다`() {
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxBodyBytes = 6)).canonicalBytes("""{"value":1}""".toByteArray())
        }
        assertFailsWith<Exception> {
            CanonicalJson(CanonicalJsonLimits(maxDepth = 1)).canonicalBytes("""{"a":{"b":1}}""".toByteArray())
        }
        assertFailsWith<Exception> {
            CanonicalJson(CanonicalJsonLimits(maxStringLength = 3)).canonicalBytes("""{"value":"abcd"}""".toByteArray())
        }
        assertFailsWith<Exception> {
            CanonicalJson(CanonicalJsonLimits(maxNameLength = 3)).canonicalBytes("""{"value":1}""".toByteArray())
        }
        assertFailsWith<Exception> {
            CanonicalJson(CanonicalJsonLimits(maxNumberLength = 2)).canonicalBytes("""{"x":123}""".toByteArray())
        }
    }

    @Test
    fun `JsonNode 입력도 구조와 output 제한을 우회하지 않는다`() {
        val node = JsonNodeFactory.instance.objectNode().apply {
            put("value", "abcd")
        }

        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxStringLength = 3)).canonicalBytes(node)
        }
        val deep = JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.arrayNode().add(1))
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxDepth = 1)).canonicalBytes(deep)
        }
        val longName = JsonNodeFactory.instance.objectNode().put("long", 1)
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxNameLength = 3)).canonicalBytes(longName)
        }
        val preciseNumber = JsonNodeFactory.instance.numberNode(BigDecimal("12345"))
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxNumberLength = 4)).canonicalBytes(preciseNumber)
        }
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxOutputBytes = 4)).canonicalBytes(node)
        }

        val hugeNumber = JsonNodeFactory.instance.numberNode(BigDecimal("1E+10"))
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxOutputBytes = 5)).canonicalBytes(hugeNumber)
        }

        val negativeHugeNumber = JsonNodeFactory.instance.numberNode(BigDecimal("-1E+10"))
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxOutputBytes = 11)).canonicalBytes(negativeHugeNumber)
        }
    }

    @Test
    fun `JsonNode container 원소 제한은 정렬과 순회 전에 적용한다`() {
        val objectNode = JsonNodeFactory.instance.objectNode().apply {
            put("a", 1)
            put("b", 2)
            put("c", 3)
        }
        val arrayNode = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3)

        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxObjectEntries = 2)).canonicalBytes(objectNode)
        }
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxArrayElements = 2)).canonicalBytes(arrayNode)
        }
    }

    @Test
    fun `canonical JSON은 finite number만 허용한다`() {
        assertFailsWith<Exception> {
            CanonicalJson().canonicalBytes("""{"value":NaN}""".toByteArray())
        }
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson().canonicalBytes(JsonNodeFactory.instance.numberNode(Double.NaN))
        }
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson().canonicalBytes(JsonNodeFactory.instance.numberNode(Double.POSITIVE_INFINITY))
        }
    }

    @Test
    fun `floating number는 BigDecimal precision을 보존한다`() {
        val body = """{"value":0.123456789012345678901234567890}""".toByteArray()

        CanonicalJson().canonicalString(body) shouldBeEqualTo
            """{"value":0.12345678901234567890123456789}"""
    }

    @Test
    fun `maxDepth 0은 root container를 허용하고 nested value를 거부한다`() {
        val canonical = CanonicalJson(CanonicalJsonLimits(maxDepth = 0))

        canonical.canonicalString("{}".toByteArray()) shouldBeEqualTo "{}"
        assertFailsWith<Exception> {
            canonical.canonicalBytes("""{"value":1}""".toByteArray())
        }
    }

    @Test
    fun `canonical JSON output 제한은 UTF-8 byte 기준으로 적용한다`() {
        val body = """{"a":"가"}""".toByteArray()

        CanonicalJson(CanonicalJsonLimits(maxOutputBytes = 11)).canonicalBytes(body).size shouldBeEqualTo 11
        assertFailsWith<IllegalArgumentException> {
            CanonicalJson(CanonicalJsonLimits(maxOutputBytes = 10)).canonicalBytes(body)
        }
    }

    @Test
    fun `canonical JSON은 JsonNode 입력과 raw 입력에 동일한 UTF-8 bytes를 반환한다`() {
        val body = """{"b":1.000,"a":"현장"}""".toByteArray()
        val canonicalJson = CanonicalJson()
        val node = Jackson.defaultJsonMapper.readTree(body)

        canonicalJson.canonicalBytes(body) shouldBeEqualTo canonicalJson.canonicalBytes(node)
        canonicalJson.canonicalBytes(body).decodeToString() shouldBeEqualTo """{"a":"현장", "b":1}"""
    }

    @Test
    fun `세 Workshop canonical bytes와 digest golden vector를 보존한다`() {
        val commonBody = """{"b":1.00,"a":[true,null,"e\u0301"],"z":-0.0}""".toByteArray()
        val fieldServiceBytes = CanonicalJson().canonicalBytes(commonBody)
        fieldServiceBytes.decodeToString() shouldBeEqualTo
            """{"a":[true, null, "é"], "b":1, "z":0}"""
        fieldServiceBytes.sha256Hex() shouldBeEqualTo
            "a79fd84206152d61159dbea8876798156529edf969397498e874b3d57756f5b2"

        val warehouseBytes = CanonicalJson(
            stringNormalization = CanonicalJsonStringNormalization.NFC,
        ).canonicalBytes(commonBody)
        warehouseBytes.decodeToString() shouldBeEqualTo
            """{"a":[true, null, "é"], "b":1, "z":0}"""
        warehouseBytes.sha256Hex() shouldBeEqualTo
            "e309eacc795d011e0c919e93906394dee9f7a412106a8c74f3a47d0b3293ca8b"

        val shiftCoverageBytes = CanonicalJson()
            .canonicalBytes("""{ "event" : "availability.changed" }""".toByteArray())
        shiftCoverageBytes.decodeToString() shouldBeEqualTo """{"event":"availability.changed"}"""
        shiftCoverageBytes.sha256Hex() shouldBeEqualTo
            "d7632712981f06afc7c03b6a190a15c81c445e02e2dbc9595796ad4960f1f82f"
    }

    private fun ByteArray.sha256Hex(): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(this))
}
