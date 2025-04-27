package io.bluetape4k.exposed.redisson

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import java.io.Serializable
import java.time.Instant

object CacheSchema: KLogging() {

    object UserTable: LongIdTable("users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val age = integer("age")
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    class UserEntity(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<UserEntity>(UserTable)

        var firstName by UserTable.firstName
        var lastName by UserTable.lastName
        var age by UserTable.age
        var createdAt by UserTable.createdAt
        var updatedAt by UserTable.updatedAt

        override fun equals(other: Any?) = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("age", age)
            .add("createdAt", createdAt)
            .add("updatedAt", updatedAt)
            .toString()
    }

    data class UserDTO(
        override val id: Long,
        val firstName: String,
        val lastName: String,
        val age: Int,
        val createdAt: Instant,
        val updatedAt: Instant? = null,
    ): HasIdentifier<Long>

    fun ResultRow.toUserDTO(): UserDTO = UserDTO(
        id = this[UserTable.id].value,
        firstName = this[UserTable.firstName],
        lastName = this[UserTable.lastName],
        age = this[UserTable.age],
        createdAt = this[UserTable.createdAt],
        updatedAt = this[UserTable.updatedAt],
    )

    fun UserEntity.toDTO() = UserDTO(
        id = this.id.value,
        firstName = this.firstName,
        lastName = this.lastName,
        age = this.age,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun AbstractExposedTest.withUserTables(
        testDB: TestDB,
        statement: Transaction.() -> Unit,
    ) {
        withTables(testDB, UserTable) {
            UserEntity.new {
                firstName = "Sunghyouk"
                lastName = "Bae"
                age = 56
            }
            UserEntity.new {
                firstName = "Misook"
                lastName = "Kwon"
                age = 55
            }
            UserEntity.new {
                firstName = "Jehyoung"
                lastName = "Bae"
                age = 29
            }
            commit()
            entityCache.clear()

            statement()
        }
    }
}
