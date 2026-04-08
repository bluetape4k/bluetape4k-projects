package io.bluetape4k.exposed.jdbc.caffeine.domain

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.core.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.exposed.tests.withTablesSuspending
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable
import java.time.Instant
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * exposed-jdbc-caffeine 통합 테스트용 Actor 및 Credential 도메인 스키마.
 *
 * - AutoIncrement Long ID: [ActorTable] / [ActorRecord]
 * - Client-generated UUID ID: [CredentialTable] / [CredentialRecord]
 */
object ActorSchema: KLogging() {
    private val faker = Fakers.faker

    /**
     * Auto-increment Long ID를 가진 Actor 테이블.
     * 테이블명은 `jdbc_caffeine_actors`로 다른 테스트와 충돌하지 않습니다.
     */
    object ActorTable: LongIdTable("jdbc_caffeine_actors") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        val updatedAt = timestamp("updated_at").nullable()
    }

    /**
     * Actor 엔티티 DTO.
     * 캐시 직렬화를 위해 [Serializable] 구현 필수.
     */
    data class ActorRecord(
        val id: Long = 0L,
        val firstName: String,
        val lastName: String,
        val email: String,
        val createdAt: Instant = Instant.now(),
        val updatedAt: Instant? = null,
    ): Serializable {
        companion object: KLogging() {
            private const val serialVersionUID = 1L
        }

        fun withId(id: Long) = copy(id = id)
    }

    /**
     * [ResultRow]를 [ActorRecord]로 변환합니다.
     */
    fun ResultRow.toActorRecord(): ActorRecord =
        ActorRecord(
            id = this[ActorTable.id].value,
            firstName = this[ActorTable.firstName],
            lastName = this[ActorTable.lastName],
            email = this[ActorTable.email],
            createdAt = this[ActorTable.createdAt],
            updatedAt = this[ActorTable.updatedAt]
        )

    private val lastActorId = atomic(1000L)

    /**
     * 테스트용 새 [ActorRecord]를 생성합니다. DB에는 저장하지 않습니다.
     */
    fun newActorRecord(): ActorRecord =
        ActorRecord(
            id = lastActorId.getAndIncrement(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            email = Base58.randomString(4) + "." + faker.internet().emailAddress()
        )

    private fun insertActor(actor: ActorRecord) {
        ActorTable.insert {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            it[email] = actor.email
        }
    }

    /**
     * [ActorTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행합니다.
     */
    fun withActorTable(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    ) {
        withTables(testDB, ActorTable) {
            insertActor(
                ActorRecord(firstName = "Sunghyouk", lastName = "Bae", email = faker.internet().safeEmailAddress())
            )
            insertActor(
                ActorRecord(firstName = "Midoogi", lastName = "Kwon", email = faker.internet().safeEmailAddress())
            )
            insertActor(
                ActorRecord(firstName = "Jehyoung", lastName = "Bae", email = faker.internet().safeEmailAddress())
            )
            commit()
            statement()
        }
    }

    /**
     * [ActorTable]을 생성하고 초기 데이터를 삽입한 뒤 suspend [statement]를 실행합니다.
     */
    suspend fun withSuspendedActorTable(
        testDB: TestDB,
        context: CoroutineContext = Dispatchers.IO,
        statement: suspend JdbcTransaction.() -> Unit,
    ) {
        withTablesSuspending(testDB, ActorTable, context = context, dropTables = false) {
            insertActor(
                ActorRecord(firstName = "Sunghyouk", lastName = "Bae", email = faker.internet().safeEmailAddress())
            )
            insertActor(
                ActorRecord(firstName = "Midoogi", lastName = "Kwon", email = faker.internet().safeEmailAddress())
            )
            insertActor(
                ActorRecord(firstName = "Jehyoung", lastName = "Bae", email = faker.internet().safeEmailAddress())
            )
            commit()
            statement()
        }
    }

    /**
     * DB에서 직접 [id]에 해당하는 [ActorRecord]를 조회합니다.
     */
    fun findActorById(id: Long): ActorRecord? =
        transaction {
            ActorTable
                .selectAll()
                .where { ActorTable.id eq id }
                .singleOrNull()
                ?.toActorRecord()
        }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    /**
     * Client-generated UUID ID를 가진 Credential 테이블.
     * Write-Behind 캐시 테스트를 위해 [TimebasedUUIDTable]을 사용합니다.
     */
    object CredentialTable: TimebasedUUIDTable("jdbc_caffeine_credentials") {
        val loginId = varchar("login_id", 255).uniqueIndex()
        val email = varchar("email", 255)
        val lastLoginAt = timestamp("last_login_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    /**
     * Credential 엔티티 DTO.
     * 캐시 직렬화를 위해 [Serializable] 구현 필수.
     */
    data class CredentialRecord(
        val id: UUID,
        val loginId: String,
        val email: String,
        val lastLoginAt: Instant? = null,
        val createdAt: Instant = Instant.now(),
    ): Serializable {
        companion object: KLogging() {
            private const val serialVersionUID = 1L
        }
    }

    /**
     * [ResultRow]를 [CredentialRecord]로 변환합니다.
     */
    fun ResultRow.toCredentialRecord(): CredentialRecord =
        CredentialRecord(
            id = this[CredentialTable.id].value,
            loginId = this[CredentialTable.loginId],
            email = this[CredentialTable.email],
            lastLoginAt = this[CredentialTable.lastLoginAt],
            createdAt = this[CredentialTable.createdAt]
        )

    /**
     * 테스트용 새 [CredentialRecord]를 생성합니다. DB에는 저장하지 않습니다.
     */
    fun newCredentialRecord(): CredentialRecord =
        CredentialRecord(
            id = Uuid.V7.nextId(),
            loginId = faker.internet().domainWord() + "_" + Base58.randomString(6),
            email = Base58.randomString(4) + "." + faker.internet().safeEmailAddress(),
            lastLoginAt = Instant.now().minusSeconds(3600)
        )

    private fun insertCredential() {
        CredentialTable.insert {
            it[loginId] = faker.internet().domainWord() + "_" + Base58.randomString(6)
            it[email] = Base58.randomString(4) + "." + faker.internet().safeEmailAddress()
            it[lastLoginAt] = Instant.now().minusSeconds(3600)
        }
    }

    /**
     * [CredentialTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행합니다.
     */
    fun withCredentialTable(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    ) {
        withTables(testDB, CredentialTable) {
            insertCredential()
            insertCredential()
            insertCredential()
            commit()
            statement()
        }
    }

    /**
     * [CredentialTable]을 생성하고 초기 데이터를 삽입한 뒤 suspend [statement]를 실행합니다.
     */
    suspend fun withSuspendedCredentialTable(
        testDB: TestDB,
        context: CoroutineContext = Dispatchers.IO,
        statement: suspend JdbcTransaction.() -> Unit,
    ) {
        withTablesSuspending(testDB, CredentialTable, context = context, dropTables = false) {
            insertCredential()
            insertCredential()
            insertCredential()
            commit()
            statement()
        }
    }
}
