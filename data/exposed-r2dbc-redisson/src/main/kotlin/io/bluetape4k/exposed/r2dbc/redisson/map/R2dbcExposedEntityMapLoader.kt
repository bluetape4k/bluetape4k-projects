package io.bluetape4k.exposed.r2dbc.redisson.map

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
 * Exposed [IdTable]에서 엔티티/ID를 읽어 Redisson read-through에 공급하는 R2DBC loader입니다.
 *
 * ## 동작/계약
 * - 단건 조회는 `selectAll().where { id eq ... }.singleOrNull()` 결과를 [toEntity]로 변환합니다.
 * - 전체 키 조회는 [batchSize] 단위 `limit/offset` 반복으로 채널에 키를 전송합니다.
 * - [batchSize]가 0 이하이면 초기화 시 [IllegalArgumentException]이 발생합니다.
 * - 테스트 기준으로 `batchSize=2`일 때도 3건 키를 모두 로드합니다.
 *
 * ```kotlin
 * val loader = R2dbcExposedEntityMapLoader(
 *     entityTable = LoaderTable,
 *     batchSize = 2,
 * ) { toLoaderEntity() }
 * val ids = loader.loadAllKeys().toList()
 * // ids.size == 3
 * ```
 *
 * @param entityTable `EntityID<ID>` 를 id 컬럼으로 가진 [IdTable] 입니다.
 * @param scope CoroutineScope
 * @param batchSize 배치 사이즈
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
open class R2dbcExposedEntityMapLoader<ID: Any, E: Any>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapLoaderCoroutineScope,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: suspend ResultRow.() -> E,
) : R2dbcEntityMapLoader<ID, E>(
        loadByIdFromDB = { id: ID ->
            entityTable
                .selectAll()
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
                    val chunk =
                        entityTable
                            .select(entityTable.id)
                            .orderBy(entityTable.id, SortOrder.ASC)
                            .limit(batchSize)
                            .offset(offset)
                            .mapNotNull { it[entityTable.id].value }
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
        scope = scope
    ) {
    companion object : KLoggingChannel() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    init {
        batchSize.requirePositiveNumber("batchSize")
    }
}
