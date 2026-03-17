package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.MapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * R2DBC `suspendTransaction`을 사용해 DB에서 엔티티를 로드하는 추상 [MapLoader] 구현체.
 *
 * [LettuceLoadedMap]의 동기 [MapLoader] 인터페이스와 R2DBC suspend API를 브리지한다.
 * 내부적으로 `runBlocking(Dispatchers.IO)`를 사용하여 suspend 호출을 동기로 래핑한다.
 *
 * @param ID 키 타입
 * @param E 엔티티 타입
 */
abstract class R2dbcEntityMapLoader<ID : Any, E : Any> : MapLoader<ID, E> {
    override fun load(key: ID): E? =
        runBlocking(Dispatchers.IO) {
            suspendTransaction { loadById(key) }
        }

    override fun loadAllKeys(): Iterable<ID> =
        runBlocking(Dispatchers.IO) {
            suspendTransaction { loadAllIds() }
        }

    /**
     * 주어진 [id]에 해당하는 엔티티를 DB에서 로드한다.
     */
    protected abstract suspend fun loadById(id: ID): E?

    /**
     * DB에 존재하는 모든 키를 반환한다.
     */
    protected abstract suspend fun loadAllIds(): Iterable<ID>
}
