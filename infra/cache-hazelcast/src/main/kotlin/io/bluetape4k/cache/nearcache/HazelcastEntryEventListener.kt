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
 * ## 이벤트 처리 전략
 * - **로컬 멤버 이벤트는 무시**: 자기 자신의 write(put/remove)가 발행하는 이벤트로
 *   로컬 캐시를 self-invalidate하면 race condition이 발생한다.
 *   (`setAsync().await()` 완료 후에도 Hazelcast 이벤트는 비동기 스레드에서 지연 전달됨)
 * - **원격 멤버 이벤트만 invalidate**: 다른 노드의 변경은 로컬 캐시를 stale하게 만드므로 무효화.
 * - **ADD 이벤트는 항상 무시**: 새 항목 추가 시 로컬 캐시에 해당 키가 없으므로 invalidate 불필요.
 *
 * ```kotlin
 * val localCache = CaffeineHazelcastLocalCache<String>(config)
 * val listener = HazelcastEntryEventListener(localCache)
 * imap.addEntryListener(listener, true)
 * // 원격 노드의 IMap 변경 시만 localCache 자동 무효화
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
class HazelcastEntryEventListener<V: Any>(
    private val localCache: HazelcastLocalCache<String, V>,
): EntryAddedListener<String, V>,
   EntryUpdatedListener<String, V>,
   EntryRemovedListener<String, V>,
   EntryExpiredListener<String, V> {

    companion object: KLogging()

    override fun entryAdded(event: EntryEvent<String, V>) {
        // ADD 이벤트는 무시: 로컬에 없는 항목을 invalidate할 필요 없음.
        // 자기 자신의 put()이 유발하는 ADD 이벤트가 로컬 캐시를 self-invalidate하는 것을 방지.
    }

    override fun entryUpdated(event: EntryEvent<String, V>) {
        // 로컬 멤버의 update는 이미 frontCache.put()으로 처리됨 — self-invalidate 방지
        if (event.member.localMember()) return
        log.debug { "Remote IMap entry updated: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }

    override fun entryRemoved(event: EntryEvent<String, V>) {
        // 로컬 멤버의 remove는 이미 frontCache.remove()로 처리됨 — self-invalidate 방지
        if (event.member.localMember()) return
        log.debug { "Remote IMap entry removed: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }

    override fun entryExpired(event: EntryEvent<String, V>) {
        log.debug { "IMap entry expired: key=${event.key}, invalidating local cache" }
        localCache.invalidate(event.key)
    }
}
