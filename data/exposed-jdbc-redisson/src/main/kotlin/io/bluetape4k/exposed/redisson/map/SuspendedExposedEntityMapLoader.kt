package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.onFailure
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll


/**
 * [HasIdentifier]를 구현한 엔티티를 위한 [SuspendedEntityMapLoader] 기본 구현체입니다.
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param entityTable `EntityID<ID>` 를 id 컬럼으로 가진 [IdTable] 입니다.
 * @param scope CoroutineScope
 * @param batchSize 배치 사이즈
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
open class SuspendedExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapLoaderCoroutineScope,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: ResultRow.() -> E,
): SuspendedEntityMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        entityTable.selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    },
    loadAllIdsFromDB = { channel ->
        var rowCount = 0
        var offset = 0L

        try {
            while (true) {
                val rows = entityTable.select(entityTable.id)
                    .orderBy(entityTable.id, SortOrder.ASC)
                    .limit(batchSize)
                    .offset(offset)
                    .toList()

                if (rows.isEmpty()) {
                    break
                }

                rows.forEach { row ->
                    val id = row[entityTable.id].value
                    channel.trySend(id).onFailure { cause ->
                        throw IllegalStateException("채널 전송 실패. id=$id", cause)
                    }
                    rowCount += 1
                }
                log.trace { "DB에서 모든 ID 로딩 중... 로딩된 id 수=$rowCount, offset=$offset" }
                offset += batchSize.toLong()
            }
            log.debug { "DB에서 모든 ID 로딩 완료. 로딩된 id 수=$rowCount" }
        } catch (cause: Throwable) {
            log.error(cause) { "DB에서 모든 ID 로딩 중 오류 발생" }
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
