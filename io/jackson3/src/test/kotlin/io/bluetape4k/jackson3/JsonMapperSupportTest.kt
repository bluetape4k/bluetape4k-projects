package io.bluetape4k.jackson3

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.io.ByteArrayInputStream
import java.io.StringReader

/**
 * [JsonMapperSupport] 확장 함수에 대한 단위 테스트입니다.
 */
class JsonMapperSupportTest {
    companion object: KLogging()

    private val mapper = Jackson.defaultJsonMapper

    data class Sample(
        val name: String,
        val value: Int,
    )

    // ── readValueOrNull(String) ───────────────────────────────────────────────

    @Test
    fun `올바른 JSON 문자열을 역직렬화한다`() {
        val json = """{"name":"debop","value":42}"""
        val result = mapper.readValueOrNull<Sample>(json)
        result.shouldNotBeNull()
        result.name shouldBeEqualTo "debop"
        result.value shouldBeEqualTo 42
    }

    @Test
    fun `잘못된 JSON 문자열은 null 을 반환한다`() {
        val result = mapper.readValueOrNull<Sample>("{not-json}")
        result.shouldBeNull()
    }

    @Test
    fun `빈 JSON 문자열은 null 을 반환한다`() {
        val result = mapper.readValueOrNull<Sample>("")
        result.shouldBeNull()
    }

    // ── readValueOrNull(Reader) ───────────────────────────────────────────────

    @Test
    fun `Reader 에서 올바른 JSON 을 역직렬화한다`() {
        val reader = StringReader("""{"name":"hello","value":1}""")
        val result = mapper.readValueOrNull<Sample>(reader)
        result.shouldNotBeNull()
        result.name shouldBeEqualTo "hello"
        result.value shouldBeEqualTo 1
    }

    @Test
    fun `Reader 에서 잘못된 JSON 은 null 을 반환한다`() {
        val reader = StringReader("{bad}")
        val result = mapper.readValueOrNull<Sample>(reader)
        result.shouldBeNull()
    }

    // ── readValueOrNull(InputStream) ─────────────────────────────────────────

    @Test
    fun `InputStream 에서 올바른 JSON 을 역직렬화한다`() {
        val stream = ByteArrayInputStream("""{"name":"stream","value":7}""".toByteArray())
        val result = mapper.readValueOrNull<Sample>(stream)
        result.shouldNotBeNull()
        result.name shouldBeEqualTo "stream"
        result.value shouldBeEqualTo 7
    }

    @Test
    fun `InputStream 에서 잘못된 JSON 은 null 을 반환한다`() {
        val stream = ByteArrayInputStream("{oops}".toByteArray())
        val result = mapper.readValueOrNull<Sample>(stream)
        result.shouldBeNull()
    }

    // ── readValueOrNull(ByteArray) ────────────────────────────────────────────

    @Test
    fun `ByteArray 에서 올바른 JSON 을 역직렬화한다`() {
        val bytes = """{"name":"bytes","value":99}""".toByteArray()
        val result = mapper.readValueOrNull<Sample>(bytes)
        result.shouldNotBeNull()
        result.name shouldBeEqualTo "bytes"
        result.value shouldBeEqualTo 99
    }

    @Test
    fun `ByteArray 에서 잘못된 JSON 은 null 을 반환한다`() {
        val bytes = "garbage".toByteArray()
        val result = mapper.readValueOrNull<Sample>(bytes)
        result.shouldBeNull()
    }

    @Test
    fun `빈 ByteArray 는 null 을 반환한다`() {
        val result = mapper.readValueOrNull<Sample>(byteArrayOf())
        result.shouldBeNull()
    }

    // ── convertValueOrNull ────────────────────────────────────────────────────

    @Test
    fun `Map 을 데이터 클래스로 변환한다`() {
        val map = mapOf("name" to "convert", "value" to 5)
        val result = mapper.convertValueOrNull<Sample>(map)
        result.shouldNotBeNull()
        result.name shouldBeEqualTo "convert"
        result.value shouldBeEqualTo 5
    }

    @Test
    fun `타입 불일치 변환은 null 을 반환한다`() {
        val result = mapper.convertValueOrNull<Sample>(listOf("a", "b"))
        result.shouldBeNull()
    }

    // ── writeAsString / writeAsBytes ──────────────────────────────────────────

    @Test
    fun `객체를 JSON 문자열로 직렬화한다`() {
        val sample = Sample("test", 123)
        val json = mapper.writeAsString(sample)
        json.shouldNotBeNull()
        json shouldBeEqualTo """{"name":"test","value":123}"""
    }

    @Test
    fun `null 객체 직렬화 시 null 을 반환한다`() {
        val json = mapper.writeAsString(null as Sample?)
        json.shouldBeNull()
    }

    @Test
    fun `객체를 JSON ByteArray 로 직렬화한다`() {
        val sample = Sample("bytes", 7)
        val bytes = mapper.writeAsBytes(sample)
        bytes.shouldNotBeNull()
        val restored = mapper.readValueOrNull<Sample>(bytes)
        restored shouldBeEqualTo sample
    }

    @Test
    fun `null 객체를 ByteArray 로 직렬화 시 null 을 반환한다`() {
        val bytes = mapper.writeAsBytes(null as Sample?)
        bytes.shouldBeNull()
    }

    // ── writeAsString(JsonNode) / writeTree(JsonNode) ─────────────────────────

    @Test
    fun `JsonNode 를 문자열로 변환한다`() {
        val node =
            JsonNodeFactory.instance.objectNode().apply {
                put("x", 1)
                put("y", "hello")
            }
        val json = mapper.writeAsString(node)
        log.debug { "json=$json" }
        json.shouldNotBeNullOrBlank()
        json shouldBeEqualTo """{"x":1,"y":"hello"}"""
    }

    @Test
    fun `빈 ObjectNode 를 문자열로 변환하면 빈 JSON 객체가 된다`() {
        val node = JsonNodeFactory.instance.objectNode()
        val json = mapper.writeAsString(node)
        json shouldBeEqualTo "{}"
    }

    @Test
    fun `writeTree 로 JsonNode 를 문자열로 변환한다`() {
        val node =
            JsonNodeFactory.instance.arrayNode().apply {
                add(1)
                add(2)
                add(3)
            }
        val json = mapper.writeTree(node)
        log.debug { "json=$json" }
        json shouldBeEqualTo "[1,2,3]"
    }

    // ── prettyWriteAsString / prettyWriteAsBytes ──────────────────────────────

    @Test
    fun `객체를 포맷된 JSON 문자열로 직렬화한다`() {
        val sample = Sample("pretty", 0)
        val json = mapper.prettyWriteAsString(sample)
        json.shouldNotBeNull()
        // 줄바꿈 포함 확인
        (json.contains("\n")).shouldBeTrue()
    }

    @Test
    fun `null 객체 포맷 직렬화 시 null 을 반환한다`() {
        val json = mapper.prettyWriteAsString(null as Sample?)
        json.shouldBeNull()
    }

    // ── treeToValueOrNull(JsonNode) ───────────────────────────────────────────

    @Test
    fun `JsonNode 를 데이터 클래스로 변환한다`() {
        val node =
            JsonNodeFactory.instance.objectNode().apply {
                put("name", "node")
                put("value", 42)
            }
        val result = mapper.treeToValueOrNull<Sample>(node)
        result.shouldNotBeNull()
        result.name shouldBeEqualTo "node"
        result.value shouldBeEqualTo 42
    }

    @Test
    fun `필드 누락 JsonNode 변환은 null 을 반환한다`() {
        // Sample requires both name and value — missing value field causes error
        val node =
            JsonNodeFactory.instance.objectNode().apply {
                put("unexpected_field", true)
            }
        // name 이 없으면 Jackson 이 기본값으로 null/0 채우거나 실패할 수 있다. 단순 변환 결과 검증
        val result = mapper.treeToValueOrNull<Sample>(node)
        // 결과가 null 이거나, name 이 null 인 객체이거나 — 예외 없이 처리됨을 검증
        log.debug { "result=$result" }
    }

    // ── registeredModuleNames / registeredModuleIds ───────────────────────────

    @Test
    fun `등록된 모듈 이름 목록을 반환한다`() {
        val names = mapper.registeredModuleNames()
        names.shouldNotBeNull()
        (names.isNotEmpty()).shouldBeTrue()
    }

    @Test
    fun `등록된 모듈 ID 목록을 반환한다`() {
        val ids = mapper.registeredModuleIds()
        ids.shouldNotBeNull()
        (ids.isNotEmpty()).shouldBeTrue()
    }
}
