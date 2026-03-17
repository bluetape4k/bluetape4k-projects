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
 * `suspendTransaction` 내에서 실행되며 `runBlocking` 없이 코루틴 네이티브로 동작한다.
 * [loadAllIds]는 대용량 테이블을 위해 [batchSize] 단위로 페이징하여 모든 PK를 로드한다.
 *
 * ### 사용 예시
 * ```kotlin
 * val loader = R2dbcExposedEntityMapLoader(
 *     table = UserTable,
 *     toEntity = { toUserRecord() },
 *     batchSize = 500,
 * )
 * val user: UserRecord? = loader.load(userId)
 * val allIds: List<Long> = loader.loadAllKeys()
 * ```
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 반환 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param toEntity [ResultRow] → [E] 변환 suspend 함수
 * @param batchSize 페이징 배치 크기 (기본: 1000). 0 이하이면 [IllegalArgumentException] 발생
 * @see R2dbcEntityMapLoader
 */
class R2dbcExposedEntityMapLoader<ID: Any, E: Any>(
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

    /**
     * [id]에 해당하는 단일 엔티티를 DB에서 조회한다. 존재하지 않으면 `null`을 반환한다.
     */
    override suspend fun loadById(id: ID): E? =
        table
            .selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

    /**
     * 테이블의 모든 PK를 [batchSize] 단위로 페이징하여 반환한다.
     */
    override suspend fun loadAllIds(): List<ID> =
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
