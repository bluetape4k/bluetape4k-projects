package io.bluetape4k.examples.mapstruct

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.io.Serializable
import java.time.LocalDate

class PersonConverterTest: AbstractMapstructTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    val converter = mapper<PersonConverter>()

    @RepeatedTest(REPEAT_SIZE)
    fun `convert to person dto`() {

        val person = Person(
            faker.name().firstName(),
            faker.name().lastName(),
            faker.phoneNumber().cellPhone(),
            LocalDate.ofEpochDay(faker.number().numberBetween(3000L, 9000L))
        )

        val personDto = converter.convertToDto(person)

        personDto.shouldNotBeNull()
        personDto.firstName shouldBeEqualTo person.firstName
        personDto.lastName shouldBeEqualTo person.lastName
        personDto.phone shouldBeEqualTo person.phoneNumber
        personDto.birthDate shouldBeEqualTo person.birthDate

        val actual = converter.convertToModel(personDto)
        actual shouldBeEqualTo person
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert to person model`() {
        val personDto = PersonDto(
            faker.name().firstName(),
            faker.name().lastName(),
            faker.phoneNumber().cellPhone(),
            LocalDate.ofEpochDay(faker.number().numberBetween(3000L, 9000L))
        )

        val person = converter.convertToModel(personDto)

        person.shouldNotBeNull()
        person.firstName shouldBeEqualTo personDto.firstName
        person.lastName shouldBeEqualTo personDto.lastName
        person.phoneNumber shouldBeEqualTo personDto.phone
        person.birthDate shouldBeEqualTo personDto.birthDate

        val actual = converter.convertToDto(person)
        actual shouldBeEqualTo personDto
    }

    data class Person(
        var firstName: String?,
        var lastName: String?,
        var phoneNumber: String?,
        var birthDate: LocalDate?,
    ): Serializable

    data class PersonDto(
        var firstName: String?,
        var lastName: String?,
        var phone: String?,
        var birthDate: LocalDate?,
    ): Serializable

    @Mapper
    interface PersonConverter {

        @Mapping(source = "phoneNumber", target = "phone")
        fun convertToDto(person: Person): PersonDto

        @InheritInverseConfiguration
        fun convertToModel(personDto: PersonDto): Person

    }
}
