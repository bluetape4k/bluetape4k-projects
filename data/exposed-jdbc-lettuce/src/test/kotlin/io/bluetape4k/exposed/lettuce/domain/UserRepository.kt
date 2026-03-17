package io.bluetape4k.exposed.lettuce.domain

import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.AbstractJdbcLettuceRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * [AbstractJdbcLettuceRepository] 구현체 — 테스트용 UserRecord 레포지토리.
 */
class UserRepository(
    client: RedisClient,
    config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
) : AbstractJdbcLettuceRepository<Long, UserRecord>(client, config) {
    override val table: IdTable<Long> = UserTable

    override fun ResultRow.toEntity(): UserRecord =
        UserRecord(
            id = this[UserTable.id].value,
            firstName = this[UserTable.firstName],
            lastName = this[UserTable.lastName],
            email = this[UserTable.email],
            createdAt = this[UserTable.createdAt]
        )

    override fun UpdateStatement.updateEntity(entity: UserRecord) {
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
    }

    override fun BatchInsertStatement.insertEntity(entity: UserRecord) {
        this[UserTable.id] = entity.id
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
    }

    /** DB에 직접 row를 삽입하고 UserRecord를 반환한다 (테스트 편의용). */
    fun createInDb(record: UserRecord): UserRecord =
        transaction {
            val id =
                UserTable
                    .insertAndGetId {
                        it[UserTable.id] = record.id
                        it[UserTable.firstName] = record.firstName
                        it[UserTable.lastName] = record.lastName
                        it[UserTable.email] = record.email
                    }.value
            record.copy(id = id)
        }

    /** DB에서 직접 조회한다 (캐시를 거치지 않음, 테스트 검증용). */
    fun findFromDb(id: Long): UserRecord? =
        transaction {
            UserTable
                .selectAll()
                .where { UserTable.id eq id }
                .singleOrNull()
                ?.let {
                    UserRecord(
                        id = it[UserTable.id].value,
                        firstName = it[UserTable.firstName],
                        lastName = it[UserTable.lastName],
                        email = it[UserTable.email],
                        createdAt = it[UserTable.createdAt]
                    )
                }
        }

    /** DB의 전체 레코드 수를 반환한다 (테스트 검증용). */
    override fun countFromDb(): Long =
        transaction {
            UserTable.selectAll().count()
        }
}
