package io.bluetape4k.fastjson2.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.bluetape4k.junit5.faker.Fakers
import java.io.Serializable
import java.util.*

internal val faker = Fakers.faker

data class User(
    val id: Int,
    val name: String,
): Serializable

internal fun newUser(): User = User(
    id = faker.random().nextInt(1, 100),
    name = faker.name().fullName()
)


data class Movie(
    val name: String,
    val studio: String,
    val rating: Float? = 1.0F,
): Serializable

internal fun newMovie(): Movie = Movie(
    name = faker.book().title(),
    studio = faker.company().name(),
    rating = faker.random().nextDouble(0.0, 10.0).toFloat()
)


enum class Generation {
    TEENAGE,
    TWENTY,
    THIRTY,
    FOURTY
}

// DefaultObjectMapper를 사용해도 @JsonTypeInfo 를 지정하면 JSON에 class 정보를 포함시켜줍니다.
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
data class Address(
    var street: String? = null,
    var phone: String? = null,
    val props: MutableList<String> = mutableListOf(),
)

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
interface Person {
    val name: String
    val age: Int
}

data class Professor(
    override val name: String,
    override val age: Int,
    val spec: String? = null,
): Person

data class Student(
    override val name: String,
    override val age: Int,
    val degree: String? = null,
): Person

data class OptionalData(
    override val name: String,
    override val age: Int,
    val spec: Optional<String>,
): Person


data class OptionalCollection(
    override val name: String,
    override val age: Int,
    val spec: Optional<String>,
    val options: List<Optional<String>> = emptyList(),
): Person
