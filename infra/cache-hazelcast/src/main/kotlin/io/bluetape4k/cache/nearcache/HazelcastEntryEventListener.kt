package io.bluetape4k.cache.nearcache

import com.hazelcast.core.EntryEvent
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryExpiredListener
import com.hazelcast.map.listener.EntryRemovedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug

/**
 * Hazelcast IMap 엔트리 이벤트를 수신해 로컬 캐시를 무효화하는 리스너.
 *
 * [com.hazelcast.map.IMap.addEntryListener]로 등록되며, 클라이언트 JVM에서 실행되어
 * non-serializable 객체(Caffeine front cache 등)를 캡처해도 직렬화 오류가 발생하지 않는다.
 *
 * @param V 값 타입 (키는 항상 String)
 */
class HazelcastEntryEventListener<V : Any>(
    private val localCache: HazelcastLocalCache<String, V>,
) : EntryAddedListener<String, V>,
    EntryUpdatedListener<String, V>,
    EntryRemovedListener<String, V>,
    EntryExpiredListener<String, V> {

    companion object : KLogging()

    override fun entryAdded(event: EntryEvent<String, V>) {
        // 다른 노드가 새 항목을 추가한 경우 로컬 캐시를 무효화
        log.debug { "IMap entry added: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }

    override fun entryUpdated(event: EntryEvent<String, V>) {
        log.debug { "IMap entry updated: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }

    override fun entryRemoved(event: EntryEvent<String, V>) {
        log.debug { "IMap entry removed: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }

    override fun entryExpired(event: EntryEvent<String, V>) {
        log.debug { "IMap entry expired: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }
}
