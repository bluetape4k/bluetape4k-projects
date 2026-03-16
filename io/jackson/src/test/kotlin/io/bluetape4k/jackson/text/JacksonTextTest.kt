package io.bluetape4k.jackson.text

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [JacksonText] object의 모든 포맷(CSV/Props/TOML/YAML) singleton 및 왕복(serde) 테스트.
 */
class JacksonTextTest : AbstractJacksonTextTest() {
    companion object : KLogging()

    // ── Singleton 동일성 ──────────────────────────────────────────────────────

    @Nested
    inner class SingletonBehavior {
        @Test
        fun `Yaml defaultMapper는 lazy singleton이다`() {
            (JacksonText.Yaml.defaultMapper === JacksonText.Yaml.defaultMapper).shouldBeTrue()
        }

        @Test
        fun `Yaml defaultSerializer는 lazy singleton이다`() {
            (JacksonText.Yaml.defaultSerializer === JacksonText.Yaml.defaultSerializer).shouldBeTrue()
        }

        @Test
        fun `Props defaultMapper는 lazy singleton이다`() {
            (JacksonText.Props.defaultMapper === JacksonText.Props.defaultMapper).shouldBeTrue()
        }

        @Test
        fun `Props defaultSerializer는 lazy singleton이다`() {
            (JacksonText.Props.defaultSerializer === JacksonText.Props.defaultSerializer).shouldBeTrue()
        }

        @Test
        fun `Toml defaultMapper는 lazy singleton이다`() {
            (JacksonText.Toml.defaultMapper === JacksonText.Toml.defaultMapper).shouldBeTrue()
        }

        @Test
        fun `Toml defaultSerializer는 lazy singleton이다`() {
            (JacksonText.Toml.defaultSerializer === JacksonText.Toml.defaultSerializer).shouldBeTrue()
        }

        @Test
        fun `Csv defaultMapper는 lazy singleton이다`() {
            (JacksonText.Csv.defaultMapper === JacksonText.Csv.defaultMapper).shouldBeTrue()
        }
    }

    // ── YAML 왕복 테스트 ─────────────────────────────────────────────────────

    @Nested
    inner class YamlRoundTrip {
        private val mapper = JacksonText.Yaml.defaultMapper

        @Test
        fun `Point를 YAML로 직렬화하고 역직렬화한다`() {
            val input = Point(3, 7)
            val yaml = mapper.writeValueAsString(input).trimYamlDocMarker()
            log.debug { "yaml=$yaml" }

            yaml.shouldNotBeEmpty()
            val parsed = mapper.readValue<Point>(yaml)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `Rectangle을 YAML로 직렬화하고 역직렬화한다`() {
            val input = Rectangle(Point(0, 0), Point(10, 20))
            val yaml = mapper.writeValueAsString(input).trimYamlDocMarker()
            log.debug { "yaml=\n$yaml" }

            val parsed = mapper.readValue<Rectangle>(yaml)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `null 허용 필드를 포함한 Map을 YAML로 왕복한다`() {
            val input = mapOf("a" to "hello", "b" to null)
            val yaml = mapper.writeValueAsString(input)
            yaml.shouldNotBeNull()
            log.debug { "yaml=$yaml" }
        }
    }

    // ── Props 왕복 테스트 ─────────────────────────────────────────────────────

    @Nested
    inner class PropsRoundTrip {
        private val mapper = JacksonText.Props.defaultMapper

        @Test
        fun `Point를 Properties로 직렬화하고 역직렬화한다`() {
            val input = Point(5, 9)
            val props = mapper.writeValueAsString(input)
            log.debug { "props=$props" }

            props.shouldNotBeEmpty()
            val parsed = mapper.readValue<Point>(props)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `Rectangle을 Properties로 직렬화하고 역직렬화한다`() {
            val input = Rectangle(Point(1, 2), Point(3, 4))
            val props = mapper.writeValueAsString(input)
            log.debug { "props=\n$props" }

            val parsed = mapper.readValue<Rectangle>(props)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `Container(Box 목록)를 Properties로 직렬화하고 역직렬화한다`() {
            val input = Container(listOf(Box(1, 2), Box(3, 4)))
            val props = mapper.writeValueAsString(input)
            log.debug { "props=\n$props" }

            val parsed = mapper.readValue<Container>(props)
            parsed shouldBeEqualTo input
        }
    }

    // ── TOML 왕복 테스트 ──────────────────────────────────────────────────────

    @Nested
    inner class TomlRoundTrip {
        private val mapper = JacksonText.Toml.defaultMapper

        @Test
        fun `Point를 TOML로 직렬화하고 역직렬화한다`() {
            val input = Point(11, 22)
            val toml = mapper.writeValueAsString(input)
            log.debug { "toml=$toml" }

            toml.shouldNotBeEmpty()
            val parsed = mapper.readValue<Point>(toml)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `Rectangle을 TOML로 직렬화하고 역직렬화한다`() {
            val input = Rectangle(Point(0, 1), Point(9, 8))
            val toml = mapper.writeValueAsString(input)
            log.debug { "toml=\n$toml" }

            val parsed = mapper.readValue<Rectangle>(toml)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `잘못된 TOML 입력은 예외를 발생시킨다`() {
            org.junit.jupiter.api.assertThrows<Exception> {
                mapper.readValue<Point>("NOT_VALID_TOML_KEY_WITHOUT_VALUE")
            }
        }
    }
}
