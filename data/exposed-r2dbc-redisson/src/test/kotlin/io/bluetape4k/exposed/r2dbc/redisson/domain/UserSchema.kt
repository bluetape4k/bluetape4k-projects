package io.bluetape4k.exposed.r2dbc.redisson.domain

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62Table
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.javatimes.toInstant
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.CoroutineContext

object UserSchema: KLoggingChannel() {

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

    data class UserRecord(
        override val id: Long = 0L,
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

    private suspend fun insertUser(user: UserRecord): EntityID<Long> {
        return UserTable.insertAndGetId {
            it[UserTable.firstName] = user.firstName
            it[UserTable.lastName] = user.lastName
            it[UserTable.email] = user.email
        }
    }

    suspend fun withUserTable(
        testDB: TestDB,
        context: CoroutineContext,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserTable) {
            insertUser(
                UserRecord(
                    id = 0L,
                    firstName = "Sunghyouk",
                    lastName = "Bae",
                    email = faker.internet().safeEmailAddress()
                )
            )
            insertUser(
                UserRecord(
                    id = 0L,
                    firstName = "Midoogi",
                    lastName = "Kwon",
                    email = faker.internet().safeEmailAddress()
                )
            )
            insertUser(
                UserRecord(
                    id = 0L,
                    firstName = "Jehyoung",
                    lastName = "Bae",
                    email = faker.internet().safeEmailAddress()
                )
            )
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

    suspend fun findUserById(id: Long): UserRecord? {
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

    data class UserCredentialsRecord(
        override val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<UUID> {
        fun withId(id: UUID) = copy(id = id)
    }

    fun ResultRow.toUserCredentialsRecord(): UserCredentialsRecord = UserCredentialsRecord(
        id = this[UserCredentialsTable.id].value,
        loginId = this[UserCredentialsTable.loginId],
        email = this[UserCredentialsTable.email],
        lastLoginAt = this[UserCredentialsTable.lastLoginAt],
        createdAt = this[UserCredentialsTable.createdAt],
        updatedAt = this[UserCredentialsTable.updatedAt],
    )

    suspend fun withUserCredentialsTable(
        testDB: TestDB,
        context: CoroutineContext,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserCredentialsTable) {
            insertUserCredentials("debop", 5L)
            insertUserCredentials("midoogi", 100L)
            insertUserCredentials(faker.credentials().username(), 200L)
            commit()

            statement()
        }
    }

    fun newUserCredentialsRecord(loginId: String? = null): UserCredentialsRecord {
        return UserCredentialsRecord(
            id = TimebasedUuid.Reordered.nextId(),
            loginId = loginId ?: (faker.credentials().username() + "_" + Base58.randomString(8)),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
            lastLoginAt = LocalDateTime.now().minusDays(200).toInstant()
        )
    }

    suspend fun insertUserCredentials(loginId: String? = null, lastDays: Long = 100L): UUID {
        return UserCredentialsTable.insertAndGetId {
            it[UserCredentialsTable.loginId] = loginId ?: faker.credentials().username()
            it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
            it[UserCredentialsTable.lastLoginAt] = LocalDateTime.now().minusDays(lastDays).toInstant()
        }.value
    }

    suspend fun findUserCredentialsById(id: UUID): UserCredentialsRecord? {
        return UserCredentialsTable.selectAll()
            .where { UserCredentialsTable.id eq id }
            .singleOrNull()
            ?.toUserCredentialsRecord()
    }

    suspend fun findUserCredentialsByLoginid(loginId: String): UserCredentialsRecord? {
        return UserCredentialsTable.selectAll()
            .where { UserCredentialsTable.loginId eq loginId }
            .singleOrNull()
            ?.toUserCredentialsRecord()
    }
}
