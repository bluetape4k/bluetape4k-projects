package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.javatimes.toInstant
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDateTime

object UserSchema: KLogging() {

    private val faker = Fakers.faker

    object UserTable: LongIdTable("users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255).uniqueIndex()

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    class UserEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<UserEntity>(UserTable)

        var firstName by UserTable.firstName
        var lastName by UserTable.lastName
        var email by UserTable.email

        var createdAt by UserTable.createdAt
        var updatedAt by UserTable.updatedAt

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("email", email)
            .toString()
    }

    data class UserDTO(
        override val id: Long,
        val firstName: String,
        val lastName: String,
        val email: String,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<Long>

    fun ResultRow.toUserDTO(): UserDTO = UserDTO(
        id = this[UserTable.id].value,
        firstName = this[UserTable.firstName],
        lastName = this[UserTable.lastName],
        email = this[UserTable.email],
        createdAt = this[UserTable.createdAt],
        updatedAt = this[UserTable.updatedAt]
    )

    fun UserEntity.toUserDTO(): UserDTO = UserDTO(
        id = this.id.value,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun AbstractExposedTest.withUserTable(testDB: TestDB, statement: Transaction.() -> Unit) {
        withTables(testDB, UserTable) {
            UserEntity.new {
                firstName = "Sunghyouk"
                lastName = "Bae"
                email = faker.internet().safeEmailAddress()
            }
            UserEntity.new {
                firstName = "Midoogi"
                lastName = "Kwon"
                email = faker.internet().safeEmailAddress()
            }
            UserEntity.new {
                firstName = "Jehyoung"
                lastName = "Bae"
                email = faker.internet().safeEmailAddress()
            }

            commit()
            entityCache.clear()
            statement()
        }
    }


    object UserCredentialTable: SnowflakeIdTable("user_credentials") {
        val loginId = varchar("login_id", 255).uniqueIndex()
        val email = varchar("email", 255).uniqueIndex()
        val lastLoginAt = timestamp("last_login_at").nullable()

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()

    }

    data class UserCredential(
        override val id: Long,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<Long>

    fun ResultRow.toUserCredential(): UserCredential = UserCredential(
        id = this[UserCredentialTable.id].value,
        loginId = this[UserCredentialTable.loginId],
        email = this[UserCredentialTable.email],
        lastLoginAt = this[UserCredentialTable.lastLoginAt],
        createdAt = this[UserCredentialTable.createdAt],
        updatedAt = this[UserCredentialTable.updatedAt],
    )

    fun AbstractExposedTest.withUserCredentialTable(
        testDB: TestDB,
        statement: Transaction.() -> Unit,
    ) {
        withTables(testDB, UserCredentialTable) {
            UserCredentialTable.insert {
                it[UserCredentialTable.id] = 1L
                it[UserCredentialTable.loginId] = "debop"
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(5).toInstant()
            }
            UserCredentialTable.insert {
                it[UserCredentialTable.id] = 2L
                it[UserCredentialTable.loginId] = "midoogi"
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(100).toInstant()
            }
            UserCredentialTable.insert {
                it[UserCredentialTable.id] = 3L
                it[UserCredentialTable.loginId] = faker.internet().username()
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
            }

            commit()
            entityCache.clear()

            statement()
        }
    }

    fun newUserCredential(loginId: String? = null): UserCredential {
        return UserCredential(
            id = Snowflakers.Global.nextId(),
            loginId = loginId ?: faker.internet().username(),
            email = faker.internet().safeEmailAddress(),
            lastLoginAt = LocalDateTime.now().minusDays(200).toInstant()
        )
    }

    fun insertUserCredential(loginId: String? = null): Long {
        return UserCredentialTable.insertAndGetId {
            it[UserCredentialTable.id] = Snowflakers.Global.nextId()
            it[UserCredentialTable.loginId] = loginId ?: faker.internet().username()
            it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
            it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
        }.value
    }

    fun findUserCredentialById(id: Long): UserCredential? {
        return UserCredentialTable.selectAll()
            .where { UserCredentialTable.id eq id }
            .singleOrNull()
            ?.toUserCredential()
    }

    fun findUserCredentialByLoginid(loginId: String): UserCredential? {
        return UserCredentialTable.selectAll()
            .where { UserCredentialTable.loginId eq loginId }
            .singleOrNull()
            ?.toUserCredential()
    }
}
