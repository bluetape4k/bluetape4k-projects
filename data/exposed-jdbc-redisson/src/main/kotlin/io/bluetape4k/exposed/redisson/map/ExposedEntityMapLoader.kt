package io.bluetape4k.exposed.redisson.map

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
 * Exposed [IdTable]에서 엔티티를 읽어 Redisson read-through에 공급하는 [EntityMapLoader] 구현입니다.
 *
 * ## 동작/계약
 * - 단건 조회는 `selectAll().where { id eq ... }.singleOrNull()` 결과를 [toEntity]로 변환합니다.
 * - 전체 키 조회는 [batchSize] 단위 `limit/offset` 반복으로 모든 ID를 수집합니다.
 * - [batchSize]가 0 이하이면 초기화 시 [IllegalArgumentException]이 발생합니다.
 * - `loadAllKeys()`는 DB 오류를 로깅 후 예외를 다시 던집니다.
 *
 * ```kotlin
 * val loader = ExposedEntityMapLoader(
 *     entityTable = LoaderTable,
 *     batchSize = 2,
 *     toEntity = { toLoaderEntity() },
 * )
 * val ids = loader.loadAllKeys()!!.toList()
 * // ids.size == 3
 * ```
 *
 * @param entityTable `EntityID<ID>` 를 id 컬럼으로 가진 [IdTable] 입니다.
 * @param batchSize 배치 사이즈
 * @param toEntity ResultRow를 엔티티로 변환하는 함수
 */
open class ExposedEntityMapLoader<ID : Comparable<ID>, E : Any>(
    private val entityTable: IdTable<ID>,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: ResultRow.() -> E,
) : EntityMapLoader<ID, E>(
        loadByIdFromDB = { id: ID ->
            entityTable
                .selectAll()
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
                    val chunk =
                        entityTable
                            .select(entityTable.id)
                            .orderBy(entityTable.id, SortOrder.ASC)
                            .limit(batchSize)
                            .offset(offset)
                            .mapNotNull { it[entityTable.id].value }

                    if (chunk.isEmpty()) {
                        break
                    }
                    loadedIds += chunk
                    offset += batchSize.toLong()
                    log.debug { "DB에서 모든 ID 로딩 중... 로딩된 id 수=${loadedIds.size}, offset=$offset" }
                }

                log.debug { "DB에서 모든 ID 로딩 완료. 로딩된 id 수=${loadedIds.size}" }
                loadedIds
            } catch (cause: Throwable) {
                log.error(cause) { "DB에서 모든 ID 로딩 중 오류 발생" }
                throw cause
            }
        }
    ) {
    companion object : KLogging() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    init {
        batchSize.requirePositiveNumber("batchSize")
    }
}
