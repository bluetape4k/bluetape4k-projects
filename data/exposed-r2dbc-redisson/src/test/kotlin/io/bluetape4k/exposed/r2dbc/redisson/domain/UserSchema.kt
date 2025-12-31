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
import io.bluetape4k.logging.KLogging
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
import java.util.concurrent.atomic.AtomicLong
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

    data class UserDTO(
        override val id: Long = 0L,
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

    private suspend fun insertUser(user: UserDTO): EntityID<Long> {
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
                UserDTO(
                    id = 0L,
                    firstName = "Sunghyouk",
                    lastName = "Bae",
                    email = faker.internet().safeEmailAddress()
                )
            )
            insertUser(
                UserDTO(
                    id = 0L,
                    firstName = "Midoogi",
                    lastName = "Kwon",
                    email = faker.internet().safeEmailAddress()
                )
            )
            insertUser(
                UserDTO(
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

    private val lastUserId = AtomicLong(1000L)

    fun newUserDTO(): UserDTO = UserDTO(
        id = lastUserId.andIncrement,
        firstName = faker.name().firstName(),
        lastName = faker.name().lastName(),
        email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
    )

    suspend fun findUserDTOById(id: Long): UserDTO? {
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

    data class UserCredentialDTO(
        override val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): HasIdentifier<UUID>

    fun ResultRow.toUserCredentialDTO(): UserCredentialDTO = UserCredentialDTO(
        id = this[UserCredentialTable.id].value,
        loginId = this[UserCredentialTable.loginId],
        email = this[UserCredentialTable.email],
        lastLoginAt = this[UserCredentialTable.lastLoginAt],
        createdAt = this[UserCredentialTable.createdAt],
        updatedAt = this[UserCredentialTable.updatedAt],
    )

    suspend fun withUserCredentialTable(
        testDB: TestDB,
        context: CoroutineContext,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserCredentialTable) {
            insertUserCredential("debop", 5L)
            insertUserCredential("midoogi", 100L)
            insertUserCredential(faker.credentials().username(), 200L)
            commit()

            statement()
        }
    }

    fun newUserCredentialDTO(loginId: String? = null): UserCredentialDTO {
        return UserCredentialDTO(
            id = TimebasedUuid.Reordered.nextId(),
            loginId = loginId ?: (faker.credentials().username() + "_" + Base58.randomString(8)),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
            lastLoginAt = LocalDateTime.now().minusDays(200).toInstant()
        )
    }

    suspend fun insertUserCredential(loginId: String? = null, lastDays: Long = 100L): UUID {
        return UserCredentialTable.insertAndGetId {
            it[UserCredentialTable.loginId] = loginId ?: faker.credentials().username()
            it[UserCredentialTable.email] = faker.internet().safeEmailAddress()
            it[UserCredentialTable.lastLoginAt] = LocalDateTime.now().minusDays(lastDays).toInstant()
        }.value
    }

    suspend fun findUserCredentialDTOById(id: UUID): UserCredentialDTO? {
        return UserCredentialTable.selectAll()
            .where { UserCredentialTable.id eq id }
            .singleOrNull()
            ?.toUserCredentialDTO()
    }

    suspend fun findUserCredentialDTOByLoginid(loginId: String): UserCredentialDTO? {
        return UserCredentialTable.selectAll()
            .where { UserCredentialTable.loginId eq loginId }
            .singleOrNull()
            ?.toUserCredentialDTO()
    }
}
