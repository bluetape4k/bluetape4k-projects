package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.Serializable
import java.io.StringReader

/**
 * [JsonMapperSupport] 확장 함수에 대한 테스트입니다.
 */
class JsonMapperSupportTest {
    companion object : KLogging()

    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    data class Item(
        val id: Int,
        val name: String,
    ) : Serializable

    private val sampleItem = Item(1, "hello")
    private val sampleJson = """{"id":1,"name":"hello"}"""

    // ─── readValueOrNull (String) ───────────────────────────────────────────

    @Test
    fun `readValueOrNull(String) - 정상 JSON 역직렬화`() {
        val result = mapper.readValueOrNull<Item>(sampleJson)
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    @Test
    fun `readValueOrNull(String) - 잘못된 JSON이면 null 반환`() {
        val result = mapper.readValueOrNull<Item>("{not-json")
        result.shouldBeNull()
    }

    @Test
    fun `readValueOrNull(String) - 빈 문자열이면 null 반환`() {
        val result = mapper.readValueOrNull<Item>("")
        result.shouldBeNull()
    }

    // ─── readValueOrNull (Reader) ───────────────────────────────────────────

    @Test
    fun `readValueOrNull(Reader) - 정상 JSON 역직렬화`() {
        val result = mapper.readValueOrNull<Item>(StringReader(sampleJson))
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    @Test
    fun `readValueOrNull(Reader) - 잘못된 JSON이면 null 반환`() {
        val result = mapper.readValueOrNull<Item>(StringReader("{not-json"))
        result.shouldBeNull()
    }

    // ─── readValueOrNull (InputStream) ─────────────────────────────────────

    @Test
    fun `readValueOrNull(InputStream) - 정상 JSON 역직렬화`() {
        val stream = ByteArrayInputStream(sampleJson.toByteArray())
        val result = mapper.readValueOrNull<Item>(stream)
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    @Test
    fun `readValueOrNull(InputStream) - 잘못된 JSON이면 null 반환`() {
        val stream = ByteArrayInputStream("{bad".toByteArray())
        val result = mapper.readValueOrNull<Item>(stream)
        result.shouldBeNull()
    }

    // ─── readValueOrNull (ByteArray) ────────────────────────────────────────

    @Test
    fun `readValueOrNull(ByteArray) - 정상 JSON 역직렬화`() {
        val result = mapper.readValueOrNull<Item>(sampleJson.toByteArray())
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    @Test
    fun `readValueOrNull(ByteArray) - 잘못된 JSON이면 null 반환`() {
        val result = mapper.readValueOrNull<Item>("{bad".toByteArray())
        result.shouldBeNull()
    }

    @Test
    fun `readValueOrNull(ByteArray) - 빈 배열이면 null 반환`() {
        val result = mapper.readValueOrNull<Item>(ByteArray(0))
        result.shouldBeNull()
    }

    // ─── convertValueOrNull ─────────────────────────────────────────────────

    @Test
    fun `convertValueOrNull - Map을 data class로 변환`() {
        val map = mapOf("id" to 1, "name" to "hello")
        val result = mapper.convertValueOrNull<Item>(map)
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    @Test
    fun `convertValueOrNull - 변환 실패 시 null 반환`() {
        val result = mapper.convertValueOrNull<Item>("invalid-string")
        result.shouldBeNull()
    }

    // ─── treeToValueOrNull ──────────────────────────────────────────────────

    @Test
    fun `treeToValueOrNull - JsonNode를 data class로 변환`() {
        val node = mapper.readTree(sampleJson)
        val result = mapper.treeToValueOrNull<Item>(node)
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    // ─── writeAsString (graph) ──────────────────────────────────────────────

    @Test
    fun `writeAsString - null 입력이면 null 반환`() {
        val result = mapper.writeAsString(null as Item?)
        result.shouldBeNull()
    }

    @Test
    fun `writeAsString - 정상 객체 직렬화`() {
        val result = mapper.writeAsString(sampleItem)
        result.shouldNotBeNull() shouldBeEqualTo sampleJson
    }

    // ─── writeAsString (JsonNode) ────────────────────────────────────────────

    @Test
    fun `writeAsString(JsonNode) - 정상 직렬화`() {
        val node = mapper.readTree(sampleJson)
        val result = mapper.writeAsString(node)
        result shouldBeEqualTo sampleJson
    }

    // ─── writeAsBytes ───────────────────────────────────────────────────────

    @Test
    fun `writeAsBytes - null 입력이면 null 반환`() {
        val result = mapper.writeAsBytes(null as Item?)
        result.shouldBeNull()
    }

    @Test
    fun `writeAsBytes - 정상 객체 직렬화`() {
        val result = mapper.writeAsBytes(sampleItem)
        result.shouldNotBeNull()
        val restored = mapper.readValueOrNull<Item>(result)
        restored.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    // ─── prettyWriteAsString ────────────────────────────────────────────────

    @Test
    fun `prettyWriteAsString - null 입력이면 null 반환`() {
        val result = mapper.prettyWriteAsString(null as Item?)
        result.shouldBeNull()
    }

    @Test
    fun `prettyWriteAsString - 결과에 줄바꿈 포함`() {
        val result = mapper.prettyWriteAsString(sampleItem)
        result.shouldNotBeNull()
        result.contains("\n").shouldBeTrue()
    }

    // ─── prettyWriteAsBytes ──────────────────────────────────────────────────

    @Test
    fun `prettyWriteAsBytes - null 입력이면 null 반환`() {
        val result = mapper.prettyWriteAsBytes(null as Item?)
        result.shouldBeNull()
    }

    @Test
    fun `prettyWriteAsBytes - 바이트 배열 역직렬화 왕복`() {
        val bytes = mapper.prettyWriteAsBytes(sampleItem)
        bytes.shouldNotBeNull()
        val restored = mapper.readValueOrNull<Item>(bytes)
        restored.shouldNotBeNull() shouldBeEqualTo sampleItem
    }

    // ─── jsonMapper DSL ──────────────────────────────────────────────────────

    @Test
    fun `jsonMapper DSL로 JsonMapper 생성`() {
        val customMapper =
            jsonMapper {
                findAndAddModules()
            }
        customMapper.shouldNotBeNull()
        val result = customMapper.readValueOrNull<Item>(sampleJson)
        result.shouldNotBeNull() shouldBeEqualTo sampleItem
    }
}

private fun Boolean.shouldBeTrue() = org.amshove.kluent.shouldBeTrue(this)
