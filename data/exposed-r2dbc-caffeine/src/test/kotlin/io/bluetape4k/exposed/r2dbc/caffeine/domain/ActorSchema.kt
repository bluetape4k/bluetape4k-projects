package io.bluetape4k.exposed.r2dbc.caffeine.domain

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
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
import java.io.Serializable
import java.time.Instant

/**
 * exposed-r2dbc-caffeine 통합 테스트용 Actor 도메인 스키마.
 */
object ActorSchema: KLoggingChannel() {
    private val faker = Fakers.faker

    /**
     * Auto-increment Long ID를 가진 Actor 테이블.
     * 테이블명은 `r2dbc_caffeine_actors`로 다른 테스트와 충돌하지 않습니다.
     */
    object ActorTable: LongIdTable("r2dbc_caffeine_actors") {
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
        companion object: KLoggingChannel() {
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

    private suspend fun insertActor(actor: ActorRecord) {
        ActorTable.insertAndGetId {
            it[ActorTable.firstName] = actor.firstName
            it[ActorTable.lastName] = actor.lastName
            it[ActorTable.email] = actor.email
        }
    }

    /**
     * [ActorTable]을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행합니다.
     */
    suspend fun withActorTable(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.() -> Unit,
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
     * DB에서 직접 [id]에 해당하는 [ActorRecord]를 조회합니다.
     */
    suspend fun findActorById(id: Long): ActorRecord? =
        ActorTable
            .selectAll()
            .where { ActorTable.id eq id }
            .singleOrNull()
            ?.toActorRecord()
}
