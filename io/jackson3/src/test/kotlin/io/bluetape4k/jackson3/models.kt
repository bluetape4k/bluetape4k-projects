package io.bluetape4k.jackson3

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.hashOf
import java.time.Instant
import java.util.*
import kotlin.random.Random

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
    val props: List<String> = emptyList(),
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
    val options: List<Optional<String>> = listOf(),
): Person

internal data class CollectionItem(
    val id: Int,
    val name: String,
)


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
open class User: AbstractValueObject(), Comparable<User> {

    lateinit var firstname: String
    lateinit var lastname: String
    var addressStr: String? = null
    var city: String? = null
    var state: String? = null
    var zipcode: String? = null
    var email: String? = null
    var username: String? = null
    var password: String? = null

    var age: Int = 0
    var generation: Generation = Generation.FOURTY
    var updateTime: Instant = Instant.now()

    var byteArray = Random.nextBytes(1024)

    var homeAddr = Address()
    var officeAddr = Address()
    var favoriteMovies: MutableList<String> = mutableListOf()

    override fun compareTo(other: User): Int {
        return firstname.compareTo(other.firstname)
    }

    override fun equalProperties(other: Any): Boolean =
        other is User &&
                firstname == other.firstname &&
                lastname == other.lastname

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = hashOf(firstname, lastname)

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("firstname", firstname)
            .add("lastname", lastname)
            .add("addressStr", addressStr)
}

fun createSampleUser(favoriteMovieSize: Int = 100): User {

    return User().apply {
        firstname = "성혁"
        lastname = "배"
        addressStr = "성북구 정릉 292 정릉힐스테이트 107동 301호"
        city = "서울"
        state = "서울"
        email = "sunghyouk.bae@gmail.com"
        username = "debop"

        homeAddr = Address("정릉", "555-5555")
        officeAddr = Address("강남", "111-1111")

        (0..favoriteMovieSize).forEach {
            favoriteMovies.add("Favorite Movie number-$it")
        }
    }
}
