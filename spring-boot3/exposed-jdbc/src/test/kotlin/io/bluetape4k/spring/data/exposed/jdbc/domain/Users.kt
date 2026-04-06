package io.bluetape4k.spring.data.exposed.jdbc.domain

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.spring.data.exposed.jdbc.annotation.ExposedEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object Users: LongIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val age = integer("age")
}

@ExposedEntity
class UserEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserEntity>(Users)

    var name: String by Users.name
    var email: String by Users.email
    var age: Int by Users.age

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = entityToStringBuilder()
        .add("name", name)
        .add("email", email)
        .add("age", age)
        .toString()
}
