package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll

/**
 * Exposed R2DBC DSL을 사용해 DB에서 엔티티를 로드하는 [R2dbcEntityMapLoader] 구현체.
 *
 * `suspendTransaction` 내부에서 실행되며, [R2dbcEntityMapLoader]가 `runBlocking(Dispatchers.IO)`으로 래핑한다.
 *
 * @param ID PK 타입
 * @param E 반환 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param toEntity [ResultRow] → [E] 변환 suspend 함수
 * @param batchSize 페이징 배치 크기
 */
class R2dbcExposedEntityMapLoader<ID : Comparable<ID>, E : Any>(
    private val table: IdTable<ID>,
    private val toEntity: suspend ResultRow.() -> E,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) : R2dbcEntityMapLoader<ID, E>() {
    companion object {
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    init {
        batchSize.requirePositiveNumber("batchSize")
    }

    override suspend fun loadById(id: ID): E? =
        table
            .selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

    override suspend fun loadAllIds(): Iterable<ID> =
        buildList {
            var offset = 0L
            while (true) {
                val batch =
                    table
                        .select(table.id)
                        .orderBy(table.id, SortOrder.ASC)
                        .limit(batchSize)
                        .offset(offset)
                        .map { it[table.id].value }
                        .toList()
                addAll(batch)
                if (batch.size < batchSize) break
                offset += batchSize
            }
        }
}
