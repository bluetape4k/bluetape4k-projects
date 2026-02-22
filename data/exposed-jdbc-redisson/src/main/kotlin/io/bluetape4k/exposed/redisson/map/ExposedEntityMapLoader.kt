package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll


/**
 * [HasIdentifier]를 구현한 엔티티를 위한 [EntityMapLoader]입니다.
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param entityTable `EntityID<ID>` 를 id 컬럼으로 가진 [IdTable] 입니다.
 * @param batchSize 배치 사이즈
 * @param toEntity ResultRow를 엔티티로 변환하는 함수
 */
open class ExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: ResultRow.() -> E,
): EntityMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        entityTable.selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    },
    loadAllIdsFromDB = {
        // 성능 문제를 피하기 위해 배치 단위로 모든 ID를 로드합니다.
        val recordCount = entityTable.selectAll().count()
        var offset = 0L
        val loadedIds = mutableListOf<ID>()

        try {

            while (offset < recordCount) {
                val chunk = entityTable.select(entityTable.id)
                    .orderBy(entityTable.id, SortOrder.ASC)
                    .limit(batchSize)
                    .offset(offset)
                    .mapNotNull { it[entityTable.id].value }

                if (chunk.isEmpty()) {
                    break
                }
                loadedIds += chunk
                offset += batchSize.toLong()
                log.debug { "DB에서 모든 ID 로딩 중... 로딩된 id 수=$loadedIds, offset=$offset" }
            }

            log.debug { "DB에서 모든 ID 로딩 완료. 로딩된 id 수=$loadedIds" }
            loadedIds
        } catch (cause: Throwable) {
            log.error(cause) { "DB에서 모든 ID 로딩 중 오류 발생" }
            throw cause
        }
    }
) {
    companion object: KLogging() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    init {
        batchSize.requirePositiveNumber("batchSize")
    }
}
