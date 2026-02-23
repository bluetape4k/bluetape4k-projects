package io.bluetape4k.exposed.r2dbc.hazelcast.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Hazelcast + R2DBC 캐시 Repository 테스트에 사용하는 DB 스키마 및 엔티티 정의입니다.
 */
object UserSchema: KLoggingChannel() {

    private val faker = Fakers.faker

    /**
     * 테스트용 사용자 테이블 (Auto Increment Long PK).
     */
    object UserTable: LongIdTable("users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    /**
     * 캐시에 저장할 사용자 레코드입니다.
     */
    data class UserRecord(
        override val id: Long,
        val firstName: String,
        val lastName: String,
        val email: String,
    ): HasIdentifier<Long>

    /**
     * [ResultRow]를 [UserRecord]로 변환합니다.
     */
    fun ResultRow.toUserRecord(): UserRecord = UserRecord(
        id = this[UserTable.id].value,
        firstName = this[UserTable.firstName],
        lastName = this[UserTable.lastName],
        email = this[UserTable.email],
    )

    /**
     * 테스트용 사용자 테이블을 생성하고 샘플 데이터를 삽입한 뒤 테스트 블록을 실행합니다.
     */
    suspend fun withUserTable(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, UserTable) {
            UserTable.insert {
                it[firstName] = "Sunghyouk"
                it[lastName] = "Bae"
                it[email] = faker.internet().safeEmailAddress()
            }
            UserTable.insert {
                it[firstName] = "Midoogi"
                it[lastName] = "Kwon"
                it[email] = faker.internet().safeEmailAddress()
            }
            UserTable.insert {
                it[firstName] = "Jehyoung"
                it[lastName] = "Bae"
                it[email] = faker.internet().safeEmailAddress()
            }
            commit()

            statement()
        }
    }

    /**
     * DB에서 첫 번째 사용자 ID를 반환합니다.
     */
    suspend fun getExistingId(): Long = suspendTransaction {
        UserTable.selectAll().limit(1).first()[UserTable.id].value
    }

    /**
     * DB에서 전체 사용자 ID 목록을 반환합니다.
     */
    suspend fun getExistingIds(): List<Long> = suspendTransaction {
        UserTable.selectAll().map { it[UserTable.id].value }.toList()
    }

    /**
     * 존재하지 않는 사용자 ID를 반환합니다.
     */
    fun getNonExistentId(): Long = Long.MIN_VALUE
}
