package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.map.MapWriter

/**
 * JDBC 트랜잭션 안에서 DB 쓰기/삭제 함수를 실행하는 Redisson [MapWriter] 구현입니다.
 *
 * ## 동작/계약
 * - [write] 호출 시 전달된 map 전체를 하나의 트랜잭션에서 [writeToDB]로 위임합니다.
 * - [delete] 호출 시 전달된 키 컬렉션을 하나의 트랜잭션에서 [deleteFromDB]로 위임합니다.
 * - 캐시 엔트리 자체를 변형하지 않고 DB 반영 함수만 호출합니다.
 *
 * ```kotlin
 * val writer = EntityMapWriter<Long, UserRecord>(
 *     writeToDB = { batch -> repo.saveAll(batch.values) },
 *     deleteFromDB = { ids -> repo.deleteAllByIds(ids) }
 * )
 * // writer.write(mapOf(1L to user))
 * ```
 *
 * @param writeToDB DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDB DB에서 데이터를 삭제하는 함수입니다.
 */
open class EntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val writeToDB: (map: Map<ID, E>) -> Unit,
    private val deleteFromDB: (ids: Collection<ID>) -> Unit,
): MapWriter<ID, E> {

    companion object: KLogging()

    override fun write(map: Map<ID, E>) = transaction {
        writeToDB(map)
    }

    override fun delete(keys: Collection<ID>) = transaction {
        deleteFromDB(keys)
    }
}
