package io.bluetape4k.spring.data.exposed.r2dbc.domain

import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Users : LongIdTable("coroutine_users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val age = integer("age")
}

data class User(
    override val id: Long? = null,
    val name: String,
    val email: String,
    val age: Int,
) : HasIdentifier<Long>
