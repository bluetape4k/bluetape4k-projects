package io.bluetape4k.jackson3.text.properties

import io.bluetape4k.jackson3.text.AbstractJacksonTextTest
import io.bluetape4k.jackson3.text.Box
import io.bluetape4k.jackson3.text.Container
import io.bluetape4k.jackson3.text.FiveMinuteUser
import io.bluetape4k.jackson3.text.Gender
import io.bluetape4k.jackson3.text.JacksonText
import io.bluetape4k.jackson3.text.Point
import io.bluetape4k.jackson3.text.Points
import io.bluetape4k.jackson3.text.Rectangle
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.javaprop.JavaPropsFactory
import tools.jackson.dataformat.javaprop.JavaPropsMapper
import tools.jackson.module.kotlin.readValue
import java.util.*

@Suppress("UNCHECKED_CAST")
class PropertiesExample: AbstractJacksonTextTest() {

    companion object: KLogging()

    private val propsMapper: JavaPropsMapper by lazy { JacksonText.Props.defaultMapper }
    private val propsFactory: JavaPropsFactory by lazy { JacksonText.Props.defaultFactory }
    private val jsonMapper: JsonMapper by lazy { JacksonText.Props.defaultJsonMapper }

    class MapWrapper {
        var map: MutableMap<String, String> = mutableMapOf()
    }

    @Nested
    inner class Parsing {
        @Test
        fun `map with branch`() {
            val props =
                """
                |map=first
                |map.b = second
                |map.xyz = third
                """.trimMargin()

            val wrapper = propsMapper.readValue<MapWrapper>(props)

            wrapper.map.shouldNotBeNull()
            wrapper.map.size shouldBeEqualTo 3
        }

        @Suppress("UNCHECKED_CAST")
        @Test
        fun `parse properties`() {
            val props = Properties().apply {
                put("a.b", "14")
                put("x", "foo")
            }

            val result = propsMapper.readPropertiesAs(props, Map::class.java) as Map<String, Any?>
            result.shouldNotBeNull()
            result.size shouldBeEqualTo props.size
            result["x"] shouldBeEqualTo props["x"]

            val obj = result["a"]
            obj.shouldNotBeNull()
            obj shouldBeInstanceOf Map::class

            val m2 = obj as Map<String, Any?>
            m2.size shouldBeEqualTo 1
            m2["b"] shouldBeEqualTo "14"
        }
    }

    @Nested
    inner class Serialization {
        @Test
        fun `simple employee serialization`() {
            val input = FiveMinuteUser(
                faker.name().firstName(),
                faker.name().lastName(),
                false,
                Gender.MALE,
                byteArrayOf(1, 2, 3, 4)
            )
            val output = propsMapper.writeValueAsString(input)
            log.debug { "output=\n$output\n----------" }

            val expected = """
            |firstName=${input.firstName}
            |lastName=${input.lastName}
            |verified=false
            |gender=MALE
            |userImage=AQIDBA==
            |
            """.trimMargin()

            output shouldBeEqualTo expected

            val props = propsMapper.writeValueAsProperties(input)
            props.size shouldBeEqualTo 5
            props["verified"] shouldBeEqualTo "false"
            props["gender"] shouldBeEqualTo "MALE"
        }

        @Test
        fun `deserialize simple POJO`() {
            val input = """
            |firstName=Bob
            |lastName=Palmer
            |verified=true
            |gender=FEMALE
            |userImage=AQIDBA==
            |
            """.trimMargin()

            val expected = FiveMinuteUser("Bob", "Palmer", true, Gender.FEMALE, byteArrayOf(1, 2, 3, 4))

            val actual = propsMapper.readValue<FiveMinuteUser>(input)
            actual.shouldNotBeNull() shouldBeEqualTo expected
        }

        @Test
        fun `serialize rectangle`() {
            val input = Rectangle(Point(1, -2), Point(5, 10))
            val output = propsMapper.writeValueAsString(input)
            log.debug { "output=\n$output\n------" }

            val expected = """
            |topLeft.x=1
            |topLeft.y=-2
            |bottomRight.x=5
            |bottomRight.y=10
            |
            """.trimMargin()

            output shouldBeEqualTo expected

            val props = propsMapper.writeValueAsProperties(input)
            props.size shouldBeEqualTo 4
            props["topLeft.x"] shouldBeEqualTo "1"
            props["topLeft.y"] shouldBeEqualTo "-2"
            props["bottomRight.x"] shouldBeEqualTo "5"
            props["bottomRight.y"] shouldBeEqualTo "10"
        }

        @Test
        fun `deserialize rectange`() {
            val input = """
            |topLeft.x=1
            |topLeft.y=-2
            |bottomRight.x=5
            |bottomRight.y=10
            """.trimMargin()

            val expected = Rectangle(Point(1, -2), Point(5, 10))

            val actual = propsMapper.readValue<Rectangle>(input)
            actual shouldBeEqualTo expected

            val actual2 = propsMapper.readValue<Rectangle>(input.toByteArray())
            actual2 shouldBeEqualTo expected
        }

        @Test
        fun `deserialize nested map`() {
            val input = "root.comparison.source.database=test\n" +
                    "root.comparison.target.database=test2\n"

            val result = propsMapper.readValue<Map<Any, Any?>>(input)
            result.shouldNotBeNull()
            result.size shouldBeEqualTo 1

            log.debug { "result=$result" }

            val nested = result.getNode("root.comparison")
            nested.size shouldBeEqualTo 2

            log.debug { "nested=$nested" }

            val source = nested["source"] as Map<Any, Any?>
            source["database"] shouldBeEqualTo "test"

            val target = nested["target"] as Map<Any, Any?>
            target["database"] shouldBeEqualTo "test2"
        }
    }

    @Nested
    inner class Array {
        @Test
        fun `serde array of POJO`() {
            val input = Container(listOf(Box(5, 6), Box(-5, 15)))
            val expected = """
            |boxes.1.x=5
            |boxes.1.y=6
            |boxes.2.x=-5
            |boxes.2.y=15
            |
            """.trimMargin()

            val output = propsMapper.writeValueAsString(input)
            log.debug { "output=\n$output\n-------" }

            output shouldBeEqualTo expected

            val parsed = propsMapper.readValue<Container>(output)
            parsed shouldBeEqualTo input
        }

        @Test
        fun `serialize points`() {
            val input = Points(Point(1, 2), Point(3, 4), Point(5, 6))
            val expected = """
            |p.1.x=1
            |p.1.y=2
            |p.2.x=3
            |p.2.y=4
            |p.3.x=5
            |p.3.y=6
            |
            """.trimMargin()

            val output = propsMapper.writeValueAsString(input)
            log.debug { "output=\n$output\n-----------" }

            output shouldBeEqualTo expected

            val parsed = propsMapper.readValue<Points>(output)
            parsed shouldBeEqualTo input
        }
    }
}
