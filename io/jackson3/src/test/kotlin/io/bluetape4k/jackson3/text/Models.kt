package io.bluetape4k.jackson3.text

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.io.Serializable
import java.util.*

data class Box(
    val x: Int,
    val y: Int,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class Container(
    val boxes: List<Box>,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

@JsonPropertyOrder(value = ["x", "y"])
data class Point(
    val x: Int,
    val y: Int,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class Points(
    val p: List<Point>,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    constructor(vararg points: Point): this(points.toList())
}

@JsonPropertyOrder(value = ["topLeft", "bottomRight"])
data class Rectangle(
    val topLeft: Point,
    val bottomRight: Point,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

enum class Gender {
    MALE,
    FEMALE,
}

data class FiveMinuteUser(
    val firstName: String,
    val lastName: String,
    var verified: Boolean,
    var gender: Gender,
    var userImage: ByteArray,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

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
data class IdDesc(
    var id: String,
    val desc: String,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class Outer(
    val name: Name,
    val age: Int,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class Name(
    val first: String,
    val last: String,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class Database(
    val dataSource: DataSource,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class DataSource(
    val driverClass: String,
    val url: String,
    val username: String,
    val password: String,
    val properties: Set<String>,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
