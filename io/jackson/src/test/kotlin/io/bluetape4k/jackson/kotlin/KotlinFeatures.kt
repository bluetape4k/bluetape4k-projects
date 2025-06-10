package io.bluetape4k.jackson.kotlin

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.readValueOrNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

class KotlinFeatures {

    companion object: KLogging()

    private val mapper = Jackson.defaultJsonMapper.apply {
        configure(SerializationFeature.INDENT_OUTPUT, false)
    }

    private data class BasicPerson(
        val name: String,
        val age: Int,
    ): Serializable

    @Test
    fun `all inferenece forms`() {
        val json = """{"name":"John Smith", "age":30}"""

        val inferRightSide = mapper.readValueOrNull<BasicPerson>(json)
        val inferLeftSide: BasicPerson = mapper.readValueOrNull(json)!!
        val person = mapper.readValueOrNull<BasicPerson>(json)!!

        val expected = BasicPerson("John Smith", 30)

        inferRightSide shouldBeEqualTo expected
        inferLeftSide shouldBeEqualTo expected
        person shouldBeEqualTo expected
    }

    @Test
    fun `read array person`() {
        val json = """[{"name":"John Smith", "age":30}, {"name":"Sunghyouk Bae", "age":54}]"""
        val expected = listOf(
            BasicPerson("John Smith", 30),
            BasicPerson("Sunghyouk Bae", 54)
        )

        val persons: List<BasicPerson> = mapper.readValueOrNull(json)!!
        persons shouldHaveSize 2
        persons shouldBeEqualTo expected
    }

    private data class ClassWithPair(
        val name: Pair<String, String>,
        val age: Int,
    ): Serializable

    @Test
    fun `read Pair`() {
        val expected = """{"name":{"first":"John","second":"Smith"},"age":30}"""
        val input = ClassWithPair(Pair("John", "Smith"), 30)

        val json = mapper.writeValueAsString(input)
        log.debug { "json=$json" }
        json shouldBeEqualTo expected

        val output = mapper.readValueOrNull<ClassWithPair>(json)
        output shouldBeEqualTo input
    }

    private data class ClassWithPairMixedTypes(
        val person: Pair<String, Int>,
    ): Serializable

    @Test
    fun `read pair mixed types`() {
        val json = """{"person":{"first":"John","second":30}}"""
        val expected = ClassWithPairMixedTypes("John" to 30)

        mapper.writeValueAsString(expected) shouldBeEqualTo json
        mapper.readValueOrNull<ClassWithPairMixedTypes>(json) shouldBeEqualTo expected
    }

    private data class ClassWithRanges(
        val ages: IntRange,
        val distance: LongRange,
    ): Serializable

    @Test
    fun `read ranges`() {
        val expected = ClassWithRanges(18..40, 5L..50L)
        val expectedJson = """{"ages":{"start":18,"end":40},"distance":{"start":5,"end":50}}"""

        val json = mapper.writeValueAsString(expected)
        log.trace { "json=$json" }

        json shouldBeEqualTo expectedJson
        mapper.readValueOrNull<ClassWithRanges>(json) shouldBeEqualTo expected
    }

    data class ClassWithPairMixedNullableTypes(
        val person: Pair<String?, Int?>,
    ): Serializable

    @Test
    fun `bind pair mixed nullable types`() {
        val expected = ClassWithPairMixedNullableTypes(Pair("John", null))
        val expectedJson = """{"person":{"first":"John","second":null}}"""

        val json = mapper.writeValueAsString(expected)
        log.trace { "json=$json" }
        json shouldBeEqualTo expectedJson

        mapper.readValueOrNull<ClassWithPairMixedNullableTypes>(json) shouldBeEqualTo expected
    }

    data class GenericParametersClass<A, B: Any>(
        val one: A,
        val two: B,
    ): Serializable

    data class GenericParameterConsumer(
        val thing: GenericParametersClass<String?, Int>,
    ): Serializable

    @Test
    fun `generic parameters in constructor`() {
        val expected = GenericParameterConsumer(GenericParametersClass(null, 123))
        val expectedJson = """{"thing":{"one":null,"two":123}}"""

        val json = mapper.writeValueAsString(expected)
        log.debug { "json=$json" }
        json shouldBeEqualTo expectedJson

        val actual = mapper.readValueOrNull<GenericParameterConsumer>(json)
        actual shouldBeEqualTo expected
    }

    data class TinyPerson(val name: String, val age: Int): Serializable
    class KotlinPersonIterator(val people: List<TinyPerson>): Iterator<TinyPerson> by people.iterator()

    @Test
    fun `generate iterator to json`() {
        val expected = KotlinPersonIterator(
            listOf(
                TinyPerson("Fred", 10),
                TinyPerson("Max", 11)
            )
        )
        val expectedJson = """[{"name":"Fred","age":10},{"name":"Max","age":11}]"""

        val typeRef = jacksonTypeRef<Iterator<TinyPerson>>()

        val json = mapper.writerFor(typeRef).writeValueAsString(expected)
        log.trace { "json=$json" }
        json shouldBeEqualTo expectedJson

        val actual = mapper.readValueOrNull<List<TinyPerson>>(json)
        actual.shouldNotBeNull()
        actual shouldContainAll expected.people
    }

    data class Company(
        val name: String,
        @JsonSerialize(`as` = java.util.Iterator::class, contentAs = TinyPerson::class)
        @JsonDeserialize(`as` = KotlinPersonIterator::class)
        val people: KotlinPersonIterator,
    ): Serializable

    @Test
    fun `generate iterator as field`() {
        val people = KotlinPersonIterator(
            listOf(
                TinyPerson("Fred", 10),
                TinyPerson("Max", 11)
            )
        )
        val company = Company("KidVille", people)
        val expectedJson = """{"name":"KidVille","people":[{"name":"Fred","age":10},{"name":"Max","age":11}]}"""

        val json = mapper.writeValueAsString(company)
        log.trace { "json=$json" }
        json shouldBeEqualTo expectedJson

        // NOTE: Iterator를 읽어드릴 수는 없네 ...
        //        val actual = mapper.readValue<Company>(json)
        //        actual shouldBeEqualTo company
    }

    @JvmInline
    value class PersonId(val id: Long): Serializable

    data class Person(
        val id: PersonId,
        val name: String,
    ): Serializable

    @Test
    fun `read value class`() {
        val person = Person(PersonId(1L), "debop")
        val expectedJson = """{"id":1,"name":"debop"}"""

        val json = mapper.writeValueAsString(person)
        log.debug { "json=$json" }
        json shouldBeEqualTo expectedJson

        val parsed = mapper.readValueOrNull<Person>(json)
        parsed shouldBeEqualTo person
    }
}
