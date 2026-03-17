package io.bluetape4k.exposed.lettuce.map

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Exposed DSL을 사용해 DB에서 엔티티를 로드하는 [EntityMapLoader] 구현체.
 *
 * @param ID PK 타입
 * @param E 반환 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param toEntity [ResultRow] → [E] 변환 함수
 * @param batchSize 페이징 배치 크기
 */
class ExposedEntityMapLoader<ID: Any, E: Any>(
    private val table: IdTable<ID>,
    private val toEntity: (ResultRow) -> E,
    private val batchSize: Int = 1000,
) : EntityMapLoader<ID, E>() {
    init {
        require(batchSize > 0) { "batchSize는 0보다 커야 합니다. batchSize=$batchSize" }
    }

    override fun loadById(id: ID): E? =
        table
            .selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.let(toEntity)

    override fun loadAllIds(): Iterable<ID> =
        buildList {
            var offset = 0L
            while (true) {
                val batch =
                    table
                        .select(table.id)
                        .limit(batchSize)
                        .offset(offset)
                        .map { it[table.id].value }
                addAll(batch)
                if (batch.size < batchSize) break
                offset += batchSize
            }
        }
}
