package io.bluetape4k.exposed.lettuce.domain

import io.bluetape4k.exposed.lettuce.repository.AbstractJdbcLettuceRepository
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigDecimal

class ItemRepository(
    client: RedisClient,
    config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
): AbstractJdbcLettuceRepository<Long, ItemDto>(client, config) {
    override val table: IdTable<Long> = ItemTable

    override fun ResultRow.toEntity(): ItemDto =
        ItemDto(
            id = this[ItemTable.id].value,
            name = this[ItemTable.name],
            price = this[ItemTable.price]
        )

    override fun UpdateStatement.updateEntity(entity: ItemDto) {
        this[ItemTable.name] = entity.name
        this[ItemTable.price] = entity.price
    }

    override fun BatchInsertStatement.insertEntity(entity: ItemDto) {
        this[ItemTable.id] = entity.id
        this[ItemTable.name] = entity.name
        this[ItemTable.price] = entity.price
    }

    /** 테스트 편의: 직접 DB에 row 생성 후 ItemDto 반환 */
    fun createInDb(
        name: String,
        price: BigDecimal,
    ): ItemDto =
        transaction {
            val id =
                ItemTable
                    .insertAndGetId {
                        it[ItemTable.name] = name
                        it[ItemTable.price] = price
                    }.value
            ItemDto(id, name, price)
        }
}
