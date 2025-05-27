package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.map.MapLoader

/**
 * ExposedMapLoader는 Exposed를 사용하여 DB에서 데이터를 로드하는 [MapLoader]입니다.
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param loadByIdFromDB ID로 엔티티를 로드하는 함수
 * @param loadAllIdsFromDB 모든 ID를 로드하는 함수
 */
open class EntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val loadByIdFromDB: (ID) -> E?,
    private val loadAllIdsFromDB: () -> Collection<ID>,
): MapLoader<ID, E> {

    companion object: KLogging() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds
    }

    override fun load(id: ID): E? = transaction {
        log.debug { "DB에서 엔티티를 로드합니다... id=$id" }
        loadByIdFromDB(id)
            .apply {
                log.debug { "DB에서 엔티티를 로드했습니다. id=$id, entity=$this" }
            }
    }

    override fun loadAllKeys(): Iterable<ID>? = transaction {
        log.debug { "DB에서 모든 id 를 로드합니다..." }
        queryTimeout = DEFAULT_QUERY_TIMEOUT
        loadAllIdsFromDB()
    }
}
