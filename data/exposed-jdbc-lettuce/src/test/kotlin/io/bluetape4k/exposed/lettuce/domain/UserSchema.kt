package io.bluetape4k.exposed.lettuce.domain

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDTable
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.junit5.faker.Fakers
import kotlinx.atomicfu.atomic
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.UUID

/**
 * exposed-jdbc-lettuce 통합 테스트용 User 도메인 스키마.
 *
 * - AutoIncrement Long ID: [UserTable] / [UserRecord]
 * - Client-generated UUID ID: [UserCredentialsTable] / [UserCredentialsRecord]
 */
object UserSchema {
    private val faker = Fakers.faker

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    object UserTable : LongIdTable("lettuce_users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    data class UserRecord(
        val id: Long,
        val firstName: String,
        val lastName: String,
        val email: String,
        val createdAt: Instant = Instant.now(),
    ) : java.io.Serializable

    private val lastUserId = atomic(10_000L)

    fun newUserRecord(): UserRecord =
        UserRecord(
            id = lastUserId.getAndIncrement(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            email = Base58.randomString(4) + "." + faker.internet().safeEmailAddress()
        )

    /**
     * [UserTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행한다 (JDBC 동기 버전).
     */
    fun withUserTable(statement: () -> Unit) {
        transaction {
            SchemaUtils.create(UserTable)
            UserTable.deleteAll()
            repeat(3) {
                UserTable.insert {
                    it[firstName] = faker.name().firstName()
                    it[lastName] = faker.name().lastName()
                    it[email] = Base58.randomString(4) + "." + faker.internet().safeEmailAddress()
                }
            }
        }
        statement()
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    object UserCredentialsTable : TimebasedUUIDTable("lettuce_user_credentials") {
        val loginId = varchar("login_id", 255).uniqueIndex()
        val email = varchar("email", 255)
        val lastLoginAt = timestamp("last_login_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    data class UserCredentialsRecord(
        val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
    ) : java.io.Serializable

    fun newUserCredentialsRecord(): UserCredentialsRecord =
        UserCredentialsRecord(
            id = Uuid.V7.nextId(),
            loginId = faker.internet().domainWord() + "_" + Base58.randomString(6),
            email = Base58.randomString(4) + "." + faker.internet().safeEmailAddress(),
            lastLoginAt = Instant.now().minusSeconds(3600)
        )

    /**
     * [UserCredentialsTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행한다 (JDBC 동기 버전).
     */
    fun withUserCredentialsTable(statement: () -> Unit) {
        transaction {
            SchemaUtils.create(UserCredentialsTable)
            UserCredentialsTable.deleteAll()
            repeat(3) {
                UserCredentialsTable.insertAndGetId {
                    it[loginId] = faker.internet().domainWord() + "_" + Base58.randomString(6)
                    it[email] = Base58.randomString(4) + "." + faker.internet().safeEmailAddress()
                    it[lastLoginAt] = Instant.now().minusSeconds(3600)
                }
            }
        }
        statement()
    }

    /** DB에서 [UserTable]의 모든 레코드를 조회한다. */
    fun loadAllUsers(): List<UserRecord> =
        transaction {
            UserTable.selectAll().map { row ->
                UserRecord(
                    id = row[UserTable.id].value,
                    firstName = row[UserTable.firstName],
                    lastName = row[UserTable.lastName],
                    email = row[UserTable.email],
                    createdAt = row[UserTable.createdAt]
                )
            }
        }

    /** DB에서 [UserCredentialsTable]의 모든 레코드를 조회한다. */
    fun loadAllUserCredentials(): List<UserCredentialsRecord> =
        transaction {
            UserCredentialsTable.selectAll().map { row ->
                UserCredentialsRecord(
                    id = row[UserCredentialsTable.id].value,
                    loginId = row[UserCredentialsTable.loginId],
                    email = row[UserCredentialsTable.email],
                    lastLoginAt = row[UserCredentialsTable.lastLoginAt],
                    createdAt = row[UserCredentialsTable.createdAt]
                )
            }
        }
}
