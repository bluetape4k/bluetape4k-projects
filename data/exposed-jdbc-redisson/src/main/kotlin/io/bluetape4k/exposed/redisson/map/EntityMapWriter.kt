package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.redisson.api.map.MapWriter

/**
 *  Redisson의 Write-through [MapWriter] 를 Exposed 를 사용하여 구현한 추상화 클래스입니다.
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
