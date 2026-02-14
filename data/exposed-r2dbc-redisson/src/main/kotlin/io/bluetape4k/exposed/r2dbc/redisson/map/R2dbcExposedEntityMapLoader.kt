package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll

/**
 * [HasIdentifier]를 구현한 엔티티를 위한 [R2dbcEntityMapLoader] 기본 구현체입니다.
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param entityTable `EntityID<ID>` 를 id 컬럼으로 가진 [IdTable] 입니다.
 * @param scope CoroutineScope
 * @param batchSize 배치 사이즈
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
open class R2dbcExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapLoaderCoroutineScope,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: suspend ResultRow.() -> E,
): R2dbcEntityMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        entityTable.selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    },
    loadAllIdsFromDB = { channel ->
        // 성능 문제를 피하기 위해 배치 단위로 모든 ID를 로드합니다.

        var loadedIds = 0
        var offset = 0L

        try {
            while (true) {
                val chunk = entityTable
                    .select(entityTable.id)
                    .orderBy(entityTable.id, SortOrder.ASC)
                    .limit(batchSize)
                    .offset(offset)
                    .mapNotNull { it[entityTable.id]?.value }
                    .toList()

                if (chunk.isEmpty()) {
                    break
                }

                chunk.forEach { id ->
                    loadedIds++
                    channel.send(id)
                }
                offset += batchSize.toLong()
                log.debug { "DB에서 모든 ID 로딩 중... 로딩된 id 수=$loadedIds, offset=$offset" }
            }
            log.debug { "DB에서 모든 ID 로딩 완료. 로딩된 id 수=$loadedIds" }
        } catch (cause: Throwable) {
            log.error(cause) { "R2dbc를 이용하여 DB에서 모든 ID 로딩 중 오류 발생" }
            throw cause
        }
    },
    scope = scope,
) {
    companion object: KLoggingChannel() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    init {
        batchSize.requirePositiveNumber("batchSize")
    }
}
