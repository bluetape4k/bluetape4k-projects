package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62Table
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityClass
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withSuspendedTables
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.javatimes.toInstant
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.CoroutineContext

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
        // NOTE: EntityClass 는 직렬화/역직렬화가 불가능합니다. --> UserRecord 를 이용하여 캐시해야 합니다.
        companion object: LongEntityClass<UserEntity>(UserTable)

        var firstName by UserTable.firstName
        var lastName by UserTable.lastName
        var email by UserTable.email

        var createdAt by UserTable.createdAt
        var updatedAt by UserTable.updatedAt

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("email", email)
            .toString()
    }

    data class UserRecord(
        override val id: Long,
        val firstName: String,
        val lastName: String,
        val email: String,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<Long> {
        fun withId(id: Long) = copy(id = id)
    }

    fun ResultRow.toUserRecord(): UserRecord = UserRecord(
        id = this[UserTable.id].value,
        firstName = this[UserTable.firstName],
        lastName = this[UserTable.lastName],
        email = this[UserTable.email],
        createdAt = this[UserTable.createdAt],
        updatedAt = this[UserTable.updatedAt]
    )

    fun UserEntity.toUserRecord(): UserRecord = UserRecord(
        id = this.id.value,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun withUserTable(testDB: TestDB, statement: JdbcTransaction.() -> Unit) {
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
        statement: suspend JdbcTransaction.() -> Unit,
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

    private val lastUserId = atomic(1000L)

    fun newUserRecord(): UserRecord = UserRecord(
        id = lastUserId.getAndIncrement(),
        firstName = faker.name().firstName(),
        lastName = faker.name().lastName(),
        email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
    )

    fun findUserById(id: Long): UserRecord? {
        return UserTable.selectAll()
            .where { UserTable.id eq id }
            .singleOrNull()
            ?.toUserRecord()
    }

    /**
     * Client 에서 ID 값을 설정하는 [TimebasedUUIDBase62Table]을 구현한 `IdTable<String>` 테이블입니다.
     */
    object UserCredentialsTable: TimebasedUUIDTable("user_credentials") {
        val loginId = varchar("login_id", 255).uniqueIndex()
        val email = varchar("email", 255)
        val lastLoginAt = timestamp("last_login_at").nullable()

        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    class UserCredentialsEntity(id: EntityID<UUID>): TimebasedUUIDEntity(id) {
        // NOTE: EntityClass 는 직렬화/역직렬화가 불가능합니다. --> UserRecord 를 이용하여 캐시해야 합니다.
        companion object: TimebasedUUIDEntityClass<UserCredentialsEntity>(UserCredentialsTable)

        var loginId by UserCredentialsTable.loginId
        var email by UserCredentialsTable.email
        var lastLoginAt by UserCredentialsTable.lastLoginAt

        var createdAt by UserCredentialsTable.createdAt
        var updatedAt by UserCredentialsTable.updatedAt

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("loginId", loginId)
            .add("email", email)
            .toString()
    }

    data class UserCredentialsRecord(
        override val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<UUID>

    fun ResultRow.toUserCredentialsRecord(): UserCredentialsRecord = UserCredentialsRecord(
        id = this[UserCredentialsTable.id].value,
        loginId = this[UserCredentialsTable.loginId],
        email = this[UserCredentialsTable.email],
        lastLoginAt = this[UserCredentialsTable.lastLoginAt],
        createdAt = this[UserCredentialsTable.createdAt],
        updatedAt = this[UserCredentialsTable.updatedAt],
    )

    fun withUserCredentialsTable(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserCredentialsTable) {
            UserCredentialsTable.insert {
                it[UserCredentialsTable.loginId] = "debop"
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(5).toInstant()
            }
            UserCredentialsTable.insert {
                it[UserCredentialsTable.loginId] = "midoogi"
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(100).toInstant()
            }
            UserCredentialsTable.insert {
                it[UserCredentialsTable.loginId] = faker.credentials().username()
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
            }
            flushCache()
            entityCache.clear()
            commit()

            statement()
        }
    }

    suspend fun withSuspendedUserCredentialsTable(
        testDB: TestDB,
        context: CoroutineContext = Dispatchers.IO,
        statement: suspend JdbcTransaction.() -> Unit,
    ) {
        // NOTE: 코루틴 작업은 작업이 완료 시까지 대기해야 해서, dropTables = false 로 설정합니다.
        withSuspendedTables(testDB, UserCredentialsTable, context = context, dropTables = false) {
            UserCredentialsTable.insert {
                it[UserCredentialsTable.loginId] = "debop"
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(5).toInstant()
            }
            UserCredentialsTable.insert {
                it[UserCredentialsTable.loginId] = "midoogi"
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(100).toInstant()
            }
            UserCredentialsTable.insert {
                it[UserCredentialsTable.loginId] = faker.credentials().username()
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
            }

            flushCache()
            entityCache.clear()
            commit()

            statement()
        }
    }

    fun newUserCredentialsRecord(loginId: String? = null): UserCredentialsRecord {
        return UserCredentialsRecord(
            id = TimebasedUuid.Epoch.nextId(),
            loginId = loginId ?: (faker.credentials().username() + "_" + Base58.randomString(8)),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
            lastLoginAt = LocalDateTime.now().minusDays(200).toInstant()
        )
    }

    fun insertUserCredentials(loginId: String? = null): UUID {
        return UserCredentialsTable.insertAndGetId {
            it[UserCredentialsTable.loginId] = loginId ?: faker.credentials().username()
            it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
            it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(200).toInstant()
        }.value
    }

    fun findUserCredentialsById(id: UUID): UserCredentialsRecord? {
        return UserCredentialsTable.selectAll()
            .where { UserCredentialsTable.id eq id }
            .singleOrNull()
            ?.toUserCredentialsRecord()
    }

    fun findUserCredentialsByLoginid(loginId: String): UserCredentialsRecord? {
        return UserCredentialsTable.selectAll()
            .where { UserCredentialsTable.loginId eq loginId }
            .singleOrNull()
            ?.toUserCredentialsRecord()
    }
}
