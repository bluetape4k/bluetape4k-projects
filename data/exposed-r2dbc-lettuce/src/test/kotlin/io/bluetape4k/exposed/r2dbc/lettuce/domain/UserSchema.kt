package io.bluetape4k.exposed.r2dbc.lettuce.domain

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.time.Instant
import java.util.*

/**
 * exposed-r2dbc-lettuce 통합 테스트용 User 도메인 스키마.
 */
object UserSchema : KLoggingChannel() {
    private val faker = Fakers.faker

    /**
     * Auto-increment Long ID를 가진 User 테이블.
     * 테이블명은 `r2dbc_lettuce_users`로 다른 테스트와 충돌하지 않는다.
     */
    object UserTable : LongIdTable("r2dbc_lettuce_users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    /**
     * User 엔티티 DTO.
     * Lettuce 직렬화를 위해 [java.io.Serializable] 구현 필수.
     */
    data class UserRecord(
        val id: Long = 0L,
        val firstName: String,
        val lastName: String,
        val email: String,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ) : java.io.Serializable {
        fun withId(id: Long) = copy(id = id)
    }

    fun ResultRow.toUserRecord(): UserRecord =
        UserRecord(
            id = this[UserTable.id].value,
            firstName = this[UserTable.firstName],
            lastName = this[UserTable.lastName],
            email = this[UserTable.email],
            createdAt = this[UserTable.createdAt],
            updatedAt = this[UserTable.updatedAt]
        )

    private val lastUserId = atomic(1000L)

    /**
     * 테스트용 새 [UserRecord]를 생성한다. DB에는 저장하지 않는다.
     */
    fun newUserRecord(): UserRecord =
        UserRecord(
            id = lastUserId.getAndIncrement(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress()
        )

    private suspend fun insertUser(user: UserRecord) {
        UserTable.insertAndGetId {
            it[UserTable.firstName] = user.firstName
            it[UserTable.lastName] = user.lastName
            it[UserTable.email] = user.email
        }
    }

    /**
     * [UserTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행한다.
     */
    suspend fun withUserTable(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserTable) {
            insertUser(
                UserRecord(firstName = "Sunghyouk", lastName = "Bae", email = faker.internet().safeEmailAddress())
            )
            insertUser(
                UserRecord(firstName = "Midoogi", lastName = "Kwon", email = faker.internet().safeEmailAddress())
            )
            insertUser(
                UserRecord(firstName = "Jehyoung", lastName = "Bae", email = faker.internet().safeEmailAddress())
            )
            commit()
            statement()
        }
    }

    /**
     * DB에서 직접 [id]에 해당하는 [UserRecord]를 조회한다.
     */
    suspend fun findUserById(id: Long): UserRecord? =
        UserTable
            .selectAll()
            .where { UserTable.id eq id }
            .singleOrNull()
            ?.toUserRecord()

    // -------------------------------------------------------------------------
    // UserCredentials — Client-generated UUID PK
    // -------------------------------------------------------------------------

    /**
     * Client에서 UUID를 직접 생성하여 PK로 사용하는 UserCredentials 테이블.
     * [TimebasedUUIDTable]을 사용하며 auto-increment 없이 클라이언트가 ID를 제공한다.
     */
    object UserCredentialsTable : TimebasedUUIDTable("r2dbc_lettuce_user_credentials") {
        val loginId = varchar("login_id", 255).uniqueIndex()
        val email = varchar("email", 255)
        val lastLoginAt = timestamp("last_login_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    /**
     * UserCredentials 엔티티 DTO.
     * Lettuce 직렬화를 위해 [java.io.Serializable] 구현 필수.
     */
    data class UserCredentialsRecord(
        val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ) : java.io.Serializable

    fun ResultRow.toUserCredentialsRecord(): UserCredentialsRecord =
        UserCredentialsRecord(
            id = this[UserCredentialsTable.id].value,
            loginId = this[UserCredentialsTable.loginId],
            email = this[UserCredentialsTable.email],
            lastLoginAt = this[UserCredentialsTable.lastLoginAt],
            createdAt = this[UserCredentialsTable.createdAt],
            updatedAt = this[UserCredentialsTable.updatedAt]
        )

    private suspend fun insertUserCredentials(loginId: String): UUID =
        UserCredentialsTable
            .insertAndGetId {
                it[UserCredentialsTable.loginId] = loginId
                it[UserCredentialsTable.email] = faker.internet().safeEmailAddress()
                it[UserCredentialsTable.lastLoginAt] = Instant.now().minusSeconds(3600)
            }.value

    /**
     * [UserCredentialsTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행한다.
     */
    suspend fun withUserCredentialsTable(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserCredentialsTable) {
            insertUserCredentials("debop")
            insertUserCredentials("midoogi")
            insertUserCredentials(faker.credentials().username())
            commit()
            statement()
        }
    }

    /**
     * 테스트용 새 [UserCredentialsRecord]를 생성한다. DB에는 저장하지 않는다.
     */
    fun newUserCredentialsRecord(): UserCredentialsRecord =
        UserCredentialsRecord(
            id = Uuid.V7.nextId(),
            loginId = faker.credentials().username() + "_" + Base58.randomString(8),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress(),
            lastLoginAt = Instant.now().minusSeconds(86400)
        )

    /**
     * DB에서 직접 [id]에 해당하는 [UserCredentialsRecord]를 조회한다.
     */
    suspend fun findUserCredentialsById(id: UUID): UserCredentialsRecord? =
        UserCredentialsTable
            .selectAll()
            .where { UserCredentialsTable.id eq id }
            .singleOrNull()
            ?.toUserCredentialsRecord()
}
