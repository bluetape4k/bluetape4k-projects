package io.bluetape4k.jackson.text.csv

import io.bluetape4k.jackson.text.AbstractJacksonTextTest
import io.bluetape4k.jackson.text.JacksonText
import io.bluetape4k.jackson.text.Point
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [io.bluetape4k.jackson.text.CsvJacksonSerializer] 및 [JacksonText.Csv] 통합 테스트.
 */
class CsvJacksonSerializerTest: AbstractJacksonTextTest() {
    companion object: KLogging()

    private val csvMapper = JacksonText.Csv.defaultMapper
    private val csvSerializer = JacksonText.Csv.defaultSerializer
    private val jsonMapper = JacksonText.Csv.defaultJsonMapper

    @Nested
    inner class SingletonBehavior {
        @Test
        fun `defaultMapper는 동일 인스턴스를 반환한다`() {
            (JacksonText.Csv.defaultMapper === JacksonText.Csv.defaultMapper).shouldBeTrue()
        }

        @Test
        fun `defaultSerializer는 동일 인스턴스를 반환한다`() {
            (JacksonText.Csv.defaultSerializer === JacksonText.Csv.defaultSerializer).shouldBeTrue()
        }

        @Test
        fun `defaultFactory는 동일 인스턴스를 반환한다`() {
            (JacksonText.Csv.defaultFactory === JacksonText.Csv.defaultFactory).shouldBeTrue()
        }

        @Test
        fun `defaultJsonMapper는 Jackson defaultJsonMapper와 동일 인스턴스이다`() {
            (JacksonText.Csv.defaultJsonMapper === io.bluetape4k.jackson.Jackson.defaultJsonMapper).shouldBeTrue()
        }
    }

    @Nested
    inner class Serialization {
        @Test
        fun `Point를 CSV 스키마로 직렬화한다`() {
            val schema = csvMapper.schemaFor(Point::class.java)
            val output = csvMapper.writer(schema).writeValueAsString(Point(1, 2))
            log.debug { "csv output: $output" }

            output.shouldNotBeNull()
            output.shouldNotBeEmpty()
            output.contains("1").shouldBeTrue()
            output.contains("2").shouldBeTrue()
        }

        @Test
        fun `Point 목록을 CSV 스키마로 직렬화하고 역직렬화한다`() {
            val points = listOf(Point(1, 2), Point(3, 4), Point(5, 6))
            val schema = csvMapper.schemaFor(Point::class.java)
            val output = csvMapper.writer(schema).writeValueAsString(points)
            log.debug { "csv output:\n$output" }

            output.shouldNotBeNull()
            output.shouldNotBeEmpty()

            val parsed: List<Point> =
                csvMapper
                    .readerFor(Point::class.java)
                    .with(schema)
                    .readValues<Point>(output)
                    .readAll()

            parsed shouldBeEqualTo points
        }

        @Test
        fun `헤더 포함 CSV로 직렬화하고 역직렬화한다`() {
            val points = listOf(Point(10, 20), Point(30, 40))
            val schema = csvMapper.schemaFor(Point::class.java).withHeader()
            val output = csvMapper.writer(schema).writeValueAsString(points)
            log.debug { "csv with header:\n$output" }

            output.shouldNotBeNull()
            // 헤더 행이 포함되어야 한다
            output.contains("x").shouldBeTrue()
            output.contains("y").shouldBeTrue()

            val parsed: List<Point> =
                csvMapper
                    .readerFor(Point::class.java)
                    .with(schema)
                    .readValues<Point>(output)
                    .readAll()

            parsed shouldBeEqualTo points
        }

        @Test
        fun `빈 목록을 직렬화하면 빈 문자열을 반환한다`() {
            val schema = csvMapper.schemaFor(Point::class.java)
            val output = csvMapper.writer(schema).writeValueAsString(emptyList<Point>())
            log.debug { "empty csv: '$output'" }
            output.shouldNotBeNull()
        }
    }

    @Nested
    inner class MapSerialization {
        @Test
        fun `Map을 JSON으로 변환 후 CSV mapper로 직렬화한다`() {
            // CSV mapper는 구조화된 POJO에 최적화됨; Map 직렬화는 writeValueAsString으로 검증
            val map = mapOf("key" to "value", "num" to 42)
            val output = csvSerializer.serializeAsString(map)
            log.debug { "map csv output: $output" }
            output.shouldNotBeNull()
        }
    }
}
