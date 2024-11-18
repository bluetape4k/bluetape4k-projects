package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.hashOf
import net.datafaker.Faker
import java.time.Instant
import java.util.*
import kotlin.random.Random


data class Box(val x: Int, val y: Int)

data class Container(val boxes: List<Box>)

@JsonPropertyOrder(value = ["x", "y"])
data class Point(val x: Int, val y: Int)

data class Points(val p: List<Point>) {
    constructor(vararg points: Point): this(points.toList())
}

@JsonPropertyOrder(value = ["topLeft", "bottomRight"])
data class Rectangle(
    val topLeft: Point,
    val bottomRight: Point,
)

enum class Gender {
    MALE, FEMALE;
}

data class FiveMinuteUser(
    val firstName: String,
    val lastName: String,
    var verified: Boolean,
    var gender: Gender,
    var userImage: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other == null || other !is FiveMinuteUser) {
            return false
        }

        if (verified != other.verified) return false
        if (gender != other.gender) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (!userImage.contentEquals(other.userImage)) return false

        return true
    }

    override fun hashCode(): Int = Objects.hashCode(firstName)
}

@JsonPropertyOrder(value = ["id", "desc"])
data class IdDesc(var id: String, val desc: String)

data class Outer(val name: Name, val age: Int)

data class Name(val first: String, val last: String)

data class Database(val dataSource: DataSource)

data class DataSource(
    val driverClass: String,
    val url: String,
    val username: String,
    val password: String,
    val properties: Set<String>,
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
        var result = firstname.compareTo(other.firstname)
        if (result == 0) {
            result = lastname.compareTo(other.lastname)
        }
        return result
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

private val faker = Faker(Locale.getDefault())

fun createSampleUser(favoriteMovieSize: Int = 100): User {
    return User().apply {
        firstname = faker.name().firstName()
        lastname = faker.name().lastName()
        addressStr = faker.address().secondaryAddress()
        city = faker.address().city()
        state = faker.address().state()
        email = faker.internet().emailAddress()
        username = faker.internet().username()

        homeAddr = io.bluetape4k.jackson.binary.Address(
            faker.address().fullAddress(),
            faker.phoneNumber().phoneNumber(),
            mutableListOf("home")
        )
        officeAddr = io.bluetape4k.jackson.binary.Address(
            faker.address().fullAddress(),
            faker.phoneNumber().phoneNumber(),
            mutableListOf("office")
        )

        (0..favoriteMovieSize).forEach {
            favoriteMovies.add("Favorite Movie number-$it")
        }
    }
}
