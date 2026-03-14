package io.bluetape4k.feign.services

import java.io.Serializable

data class Post(
    val userId: Int,
    val id: Int,
    val title: String?,
    val body: String?,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val email: String,
    val body: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val address: Address,
    val phone: String,
    val website: String,
    val company: Company,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Address(
    val street: String,
    val suite: String,
    val city: String,
    val zipcode: String,
    val geo: Geo,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Geo(
    val lat: Double,
    val lng: Double,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Company(
    val name: String,
    val catchPhrase: String,
    val bs: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Todo(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Album(
    val userId: Int,
    val id: Int,
    val title: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class Photo(
    val albumId: Int,
    val id: Int,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
