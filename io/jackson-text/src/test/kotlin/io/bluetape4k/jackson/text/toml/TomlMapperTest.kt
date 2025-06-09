package io.bluetape4k.jackson.text.toml

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.text.AbstractJacksonTextTest
import io.bluetape4k.jackson.text.FiveMinuteUser
import io.bluetape4k.jackson.text.Gender
import io.bluetape4k.jackson.text.JacksonText
import io.bluetape4k.jackson.text.Point
import io.bluetape4k.jackson.text.Rectangle
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.asDouble
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TomlMapperTest: AbstractJacksonTextTest() {

    companion object: KLogging()

    private val tomlMapper = JacksonText.Toml.defaultMapper
    private val tomlFactory = JacksonText.Toml.defaultFactory
    private val jsonMapper = JacksonText.Toml.defaultJsonMapper

    class MapWrapper {
        var map: MutableMap<String, String> = mutableMapOf()
    }

    @Nested
    inner class Parsing {
        @Test
        fun `parse from text`() {
            val toml =
                """
                |[map]
                |name = "name"
                |b = "b"
                |xyz = "xyz"
                """.trimMargin()

            val wrapper = tomlMapper.readValue<MapWrapper>(toml)

            wrapper.map.forEach { (k, v) ->
                log.debug { "key=$k, value=$v" }
            }

            wrapper.map.shouldNotBeNull()
            wrapper.map.size shouldBeEqualTo 3
        }
    }

    @Nested
    inner class Serialization {
        @Test
        fun `serialize simple employee`() {
            val input = FiveMinuteUser(
                faker.name().firstName(),
                faker.name().lastName(),
                false,
                Gender.MALE,
                byteArrayOf(1, 2, 3, 4)
            )

            val output = tomlMapper.writeValueAsString(input)
            log.debug { "output=\n$output\n----------" }

            val expected = """
                |firstName = '${input.firstName}'
                |lastName = '${input.lastName}'
                |verified = false
                |gender = 'MALE'
                |userImage = 'AQIDBA=='
                |
                """.trimMargin()

            output shouldBeEqualTo expected
        }

        @Test
        fun `deserialize simple POJO`() {
            val input =
                """
                |firstName = "Bob"
                |lastName = "Palmer"
                |verified = true
                |gender = "FEMALE"
                |userImage = "AQIDBA=="  # base64 encoded byte array
                """.trimMargin()

            val expected = FiveMinuteUser("Bob", "Palmer", true, Gender.FEMALE, byteArrayOf(1, 2, 3, 4))

            val actual = tomlMapper.readValue<FiveMinuteUser>(input)
            actual shouldBeEqualTo expected
        }

        @Test
        fun `serialize rectangle`() {
            val input = Rectangle(Point(1, -2), Point(5, 10))
            val output = tomlMapper.writeValueAsString(input)
            log.debug { "output=\n$output\n------" }

            val expected = """
                |topLeft.x = 1
                |topLeft.y = -2
                |bottomRight.x = 5
                |bottomRight.y = 10
                |
                """.trimMargin()

            output shouldBeEqualTo expected

            val rectangle = tomlMapper.readValue<Rectangle>(output)
            rectangle shouldBeEqualTo input
        }

        @Suppress("UNCHECKED_CAST")
        @Test
        fun `deserialize sections`() {
            val input = """
                # 데이터베이스 설정
                [database]
                host = "localhost"
                port = 5432
                username = "db_user"
                password = "secure_password"
                max_connections = 100
                timeout = 30.5 # 초 단위

                # 서버 설정
                [server]
                ip = "192.168.1.1"
                port = 8080
                
                """.trimIndent()

            val map = tomlMapper.readValue<Map<String, Any?>>(input)
            log.debug { "map=$map" }
            map.size shouldBeEqualTo 2      // database, server

            val database = map["database"] as Map<*, *>
            database.size shouldBeEqualTo 6
            database["port"] shouldBeEqualTo 5432
            database["timeout"].asDouble() shouldBeEqualTo 30.5

            val server = map["server"] as Map<*, *>
            server.size shouldBeEqualTo 2
            server["ip"] shouldBeEqualTo "192.168.1.1"
            server["port"] shouldBeEqualTo 8080
        }
    }
}
