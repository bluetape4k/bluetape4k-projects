package io.bluetape4k.jackson3.text

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

/**
 * CSV/Properties/TOML/YAML 직렬화기 통합 테스트
 *
 * 각 포맷의 직렬화 → 역직렬화 왕복(roundtrip)과 엣지 케이스를 검증합니다.
 */
class JacksonTextSerializerTest : AbstractJacksonTextTest() {
    // ─── CSV ─────────────────────────────────────────────────────────────────

    @Nested
    inner class Csv {
        private val serializer = JacksonText.Csv.defaultSerializer
        private val csvMapper = JacksonText.Csv.defaultMapper

        @Test
        fun `defaultSerializer singleton 인스턴스 확인`() {
            serializer.shouldNotBeNull()
            JacksonText.Csv.defaultSerializer shouldBeEqualTo serializer
        }

        @Test
        fun `CsvSchema 기반 Point 직렬화 후 x와 y 값 포함 확인`() {
            // CSV는 스키마(컬럼 정의)가 필요합니다.
            val schema = csvMapper.schemaFor(Point::class.java)
            val text = csvMapper.writer(schema).writeValueAsString(Point(3, 7))
            text.shouldNotBeBlank()
            text shouldContain "3"
            text shouldContain "7"
        }

        @Test
        fun `CsvSchema 기반 Point 직렬화 역직렬화 왕복 확인`() {
            val schema = csvMapper.schemaFor(Point::class.java).withHeader()
            val point = Point(5, 9)
            val text = csvMapper.writer(schema).writeValueAsString(point)
            text.shouldNotBeBlank()
            val parsed = csvMapper.readerFor(Point::class.java).with(schema).readValue<Point>(text)
            parsed shouldBeEqualTo point
        }
    }

    // ─── Properties ──────────────────────────────────────────────────────────

    @Nested
    inner class Props {
        private val serializer = JacksonText.Props.defaultSerializer

        @Test
        fun `defaultSerializer singleton 인스턴스 확인`() {
            serializer.shouldNotBeNull()
            JacksonText.Props.defaultSerializer shouldBeEqualTo serializer
        }

        @Test
        fun `Point 직렬화 결과에 키 포함 확인`() {
            val point = Point(1, 2)
            val text = serializer.serializeAsString(point)
            text.shouldNotBeBlank()
            text shouldContain "x=1"
            text shouldContain "y=2"
        }

        @Test
        fun `Rectangle 직렬화 후 중첩 키 포함 확인`() {
            val rect = Rectangle(Point(0, 0), Point(10, 10))
            val text = serializer.serializeAsString(rect)
            text.shouldNotBeBlank()
            text shouldContain "topLeft.x"
            text shouldContain "bottomRight.x"
        }

        @Test
        fun `FiveMinuteUser 직렬화 결과에 gender 포함 확인`() {
            val user =
                FiveMinuteUser(
                    faker.name().firstName(),
                    faker.name().lastName(),
                    false,
                    Gender.MALE,
                    byteArrayOf(1, 2, 3)
                )
            val text = serializer.serializeAsString(user)
            text.shouldNotBeBlank()
            text shouldContain "gender=MALE"
        }
    }

    // ─── TOML ────────────────────────────────────────────────────────────────

    @Nested
    inner class Toml {
        private val serializer = JacksonText.Toml.defaultSerializer

        @Test
        fun `defaultSerializer singleton 인스턴스 확인`() {
            serializer.shouldNotBeNull()
            JacksonText.Toml.defaultSerializer shouldBeEqualTo serializer
        }

        @Test
        fun `Point 직렬화 결과에 x와 y 포함 확인`() {
            val point = Point(5, -3)
            val text = serializer.serializeAsString(point)
            text.shouldNotBeBlank()
            text shouldContain "x = 5"
            text shouldContain "y = -3"
        }

        @Test
        fun `FiveMinuteUser 직렬화 결과에 verified 포함 확인`() {
            val user =
                FiveMinuteUser(
                    faker.name().firstName(),
                    faker.name().lastName(),
                    true,
                    Gender.FEMALE,
                    byteArrayOf(10, 20)
                )
            val text = serializer.serializeAsString(user)
            text.shouldNotBeBlank()
            text shouldContain "verified = true"
        }
    }

    // ─── YAML ────────────────────────────────────────────────────────────────

    @Nested
    inner class Yaml {
        private val serializer = JacksonText.Yaml.defaultSerializer

        @Test
        fun `defaultSerializer singleton 인스턴스 확인`() {
            serializer.shouldNotBeNull()
            JacksonText.Yaml.defaultSerializer shouldBeEqualTo serializer
        }

        @Test
        fun `Map 직렬화 결과에 키 포함 확인`() {
            val text = serializer.serializeAsString(mapOf("name" to "debop", "age" to 42))
            text.shouldNotBeBlank()
            text shouldContain "name"
            text shouldContain "debop"
        }

        @Test
        fun `Point 직렬화 결과에 x와 y 값 포함 확인`() {
            val point = Point(10, 20)
            val text = serializer.serializeAsString(point)
            text.shouldNotBeBlank()
            // YAML mapper는 일부 필드명을 따옴표로 감쌀 수 있으므로 값만 검증합니다.
            text shouldContain "10"
            text shouldContain "20"
        }

        @Test
        fun `Outer 직렬화 결과에 중첩 필드 포함 확인`() {
            val outer = Outer(Name("Sunghyouk", "Bae"), 54)
            val text = serializer.serializeAsString(outer)
            text.shouldNotBeBlank()
            text shouldContain "name:"
            text shouldContain "first:"
        }

        @Test
        fun `FiveMinuteUser 직렬화 결과에 gender 포함 확인`() {
            val user =
                FiveMinuteUser(
                    faker.name().firstName(),
                    faker.name().lastName(),
                    false,
                    Gender.MALE,
                    byteArrayOf(1, 2, 3, 4)
                )
            val text = serializer.serializeAsString(user)
            text.shouldNotBeBlank()
            text shouldContain "gender"
            text shouldContain "MALE"
        }
    }

    // ─── JacksonText 팩토리 확인 ──────────────────────────────────────────────

    @Nested
    inner class Factory {
        @Test
        fun `Csv defaultFactory는 null이 아님`() {
            JacksonText.Csv.defaultFactory.shouldNotBeNull()
        }

        @Test
        fun `Props defaultFactory는 null이 아님`() {
            JacksonText.Props.defaultFactory.shouldNotBeNull()
        }

        @Test
        fun `Toml defaultFactory는 null이 아님`() {
            JacksonText.Toml.defaultFactory.shouldNotBeNull()
        }

        @Test
        fun `Yaml defaultFactory는 null이 아님`() {
            JacksonText.Yaml.defaultFactory.shouldNotBeNull()
        }

        @Test
        fun `각 포맷의 defaultJsonMapper는 동일한 인스턴스`() {
            val csvJson = JacksonText.Csv.defaultJsonMapper
            val propsJson = JacksonText.Props.defaultJsonMapper
            val tomlJson = JacksonText.Toml.defaultJsonMapper
            val yamlJson = JacksonText.Yaml.defaultJsonMapper

            (csvJson === propsJson) shouldBeEqualTo true
            (propsJson === tomlJson) shouldBeEqualTo true
            (tomlJson === yamlJson) shouldBeEqualTo true
        }
    }
}
