package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.redis.lettuce.map.MapLoader
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * DB에서 엔티티를 로드하는 추상 [MapLoader] 구현체.
 *
 * 서브클래스는 [loadById]와 [loadAllIds]를 구현한다.
 *
 * ```kotlin
 * class MyLoader : EntityMapLoader<Long, MyEntity>() {
 *     override fun loadById(id: Long): MyEntity? =
 *         MyTable.selectAll().where { MyTable.id eq id }.singleOrNull()?.toMyEntity()
 *     override fun loadAllIds(): Iterable<Long> =
 *         MyTable.select(MyTable.id).map { it[MyTable.id].value }
 * }
 * ```
 *
 * @param ID 키 타입
 * @param E 엔티티 타입
 */
abstract class EntityMapLoader<ID: Any, E: Any>: MapLoader<ID, E> {
    override fun load(key: ID): E? = transaction { loadById(key) }

    override fun loadAllKeys(): Iterable<ID> = transaction { loadAllIds() }

    protected abstract fun loadById(id: ID): E?

    protected abstract fun loadAllIds(): Iterable<ID>
}
