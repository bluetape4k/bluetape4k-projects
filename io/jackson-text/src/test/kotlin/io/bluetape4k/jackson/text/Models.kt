package io.bluetape4k.jackson.text

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.io.Serializable
import java.util.*


data class Box(val x: Int, val y: Int): Serializable

data class Container(val boxes: List<Box>): Serializable

@JsonPropertyOrder(value = ["x", "y"])
data class Point(val x: Int, val y: Int): Serializable

data class Points(val p: List<Point>): Serializable {
    constructor(vararg points: Point): this(points.toList())
}

@JsonPropertyOrder(value = ["topLeft", "bottomRight"])
data class Rectangle(val topLeft: Point, val bottomRight: Point): Serializable

enum class Gender {
    MALE, FEMALE;
}

data class FiveMinuteUser(
    val firstName: String,
    val lastName: String,
    var verified: Boolean,
    var gender: Gender,
    var userImage: ByteArray,
): Serializable {

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
data class IdDesc(var id: String, val desc: String): Serializable

data class Outer(val name: Name, val age: Int): Serializable

data class Name(val first: String, val last: String): Serializable

data class Database(val dataSource: DataSource): Serializable

data class DataSource(
    val driverClass: String,
    val url: String,
    val username: String,
    val password: String,
    val properties: Set<String>,
): Serializable
