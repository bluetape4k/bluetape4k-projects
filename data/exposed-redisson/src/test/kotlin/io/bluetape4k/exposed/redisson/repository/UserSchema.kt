package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.dao.HasIdentifier
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62Table
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityClass
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withSuspendedTables
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.javatimes.toInstant
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
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
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

@Suppress("ExposedReference")
object UserSchema: KLogging() {

    private val faker = Fakers.faker

    /**
     * Auto Incremented ID 를 가진 [LongIdTable]을 구현한 `IdTable<Long>` 테이블입니다.
     */
    object UserTable: LongIdTable("users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255)

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    class UserEntity(id: EntityID<Long>): LongEntity(id) {
        // NOTE: EntityClass 는 직렬화/역직렬화가 불가능합니다. --> UserDTO 를 이용하여 캐시해야 합니다.
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

    fun withUserTable(testDB: TestDB, statement: Transaction.() -> Unit) {
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

            flushCache()
            entityCache.clear()
            commit()

            statement()
        }
    }

    suspend fun withSuspendedUserTable(
        testDB: TestDB,
        context: CoroutineContext = Dispatchers.IO,
        statement: suspend Transaction.() -> Unit,
    ) {
        // NOTE: 코루틴 작업은 작업이 완료 시까지 대기해야 해서, dropTables = false 로 설정합니다.
        withSuspendedTables(testDB, UserTable, context = context, dropTables = false) {
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
            flushCache()
            entityCache.clear()
            commit()

            statement()
        }
    }

    private val lastUserId = AtomicLong(1000L)

    fun newUserDTO(): UserDTO = UserDTO(
        id = lastUserId.andIncrement,
        firstName = faker.name().firstName(),
        lastName = faker.name().lastName(),
        email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
    )

    fun findUserDTOById(id: Long): UserDTO? {
        return UserTable.selectAll()
            .where { UserTable.id eq id }
            .singleOrNull()
            ?.toUserDTO()
    }

    /**
     * Client 에서 ID 값을 설정하는 [TimebasedUUIDBase62Table]을 구현한 `IdTable<String>` 테이블입니다.
     */
    object UserCredentialTable: TimebasedUUIDTable("user_credentials") {
        val loginId = varchar("login_id", 255).uniqueIndex()
        val email = varchar("email", 255)
        val lastLoginAt = timestamp("last_login_at").nullable()

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    class UserCredentialEntity(id: EntityID<UUID>): TimebasedUUIDEntity(id) {
        // NOTE: EntityClass 는 직렬화/역직렬화가 불가능합니다. --> UserDTO 를 이용하여 캐시해야 합니다.
        companion object: TimebasedUUIDEntityClass<UserCredentialEntity>(UserCredentialTable)

        var loginId by UserCredentialTable.loginId
        var email by UserCredentialTable.email
        var lastLoginAt by UserCredentialTable.lastLoginAt

        var createdAt by UserCredentialTable.createdAt
        var updatedAt by UserCredentialTable.updatedAt

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("loginId", loginId)
            .add("email", email)
            .toString()
    }

    data class UserCredentialDTO(
        override val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<UUID>

    fun ResultRow.toUserCredential(): UserCredentialDTO = UserCredentialDTO(
        id = this[UserCredentialTable.id].value,
        loginId = this[UserCredentialTable.loginId],
        email = this[UserCredentialTable.email],
        lastLoginAt = this[UserCredentialTable.lastLoginAt],
        createdAt = this[UserCredentialTable.createdAt],
        updatedAt = this[UserCredentialTable.updatedAt],
    )

    fun withUserCredentialTable(
        testDB: TestDB,
        statement: Transaction.() -> Unit,
    ) {
        withTables(testDB, UserCredentialTable) {
            UserCredentialTable.insert {
                it[UserCredentialTable.loginId] = "debop"
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(5).toInstant()
            }
            UserCredentialTable.insert {
                it[UserCredentialTable.loginId] = "midoogi"
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(100).toInstant()
            }
            UserCredentialTable.insert {
                it[UserCredentialTable.loginId] = faker.internet().username()
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
            }
            flushCache()
            entityCache.clear()
            commit()

            statement()
        }
    }

    suspend fun withSuspendedUserCredentialTable(
        testDB: TestDB,
        context: CoroutineContext = Dispatchers.IO,
        statement: suspend Transaction.() -> Unit,
    ) {
        // NOTE: 코루틴 작업은 작업이 완료 시까지 대기해야 해서, dropTables = false 로 설정합니다.
        withSuspendedTables(testDB, UserCredentialTable, context = context, dropTables = false) {
            UserCredentialTable.insert {
                it[UserCredentialTable.loginId] = "debop"
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(5).toInstant()
            }
            UserCredentialTable.insert {
                it[UserCredentialTable.loginId] = "midoogi"
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(100).toInstant()
            }
            UserCredentialTable.insert {
                it[UserCredentialTable.loginId] = faker.internet().username()
                it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
            }

            flushCache()
            entityCache.clear()
            commit()

            statement()
        }
    }

    fun newUserCredentialDTO(loginId: String? = null): UserCredentialDTO {
        return UserCredentialDTO(
            id = TimebasedUuid.Reordered.nextId(),
            loginId = loginId ?: (faker.internet().username() + "_" + Base58.randomString(8)),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
            lastLoginAt = LocalDateTime.now().minusDays(200).toInstant()
        )
    }

    fun insertUserCredential(loginId: String? = null): UUID {
        return UserCredentialTable.insertAndGetId {
            it[UserCredentialTable.loginId] = loginId ?: faker.internet().username()
            it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
            it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
        }.value
    }

    fun findUserCredentialDTOById(id: UUID): UserCredentialDTO? {
        return UserCredentialTable.selectAll()
            .where { UserCredentialTable.id eq id }
            .singleOrNull()
            ?.toUserCredential()
    }

    fun findUserCredentialDTOByLoginid(loginId: String): UserCredentialDTO? {
        return UserCredentialTable.selectAll()
            .where { UserCredentialTable.loginId eq loginId }
            .singleOrNull()
            ?.toUserCredential()
    }
}
