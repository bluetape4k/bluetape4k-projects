package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
import io.bluetape4k.ignite3.cache.coroutines.igniteNearSuspendCache
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.table.KeyValueView

/**
 * Ignite 3.x 클라이언트에서 [KeyValueView]를 가져오는 확장 함수입니다.
 */
fun <K: Any, V: Any> IgniteClient.keyValueView(
    tableName: String,
    keyType: Class<K>,
    valueType: Class<V>,
): KeyValueView<K, V> {
    val table = tables().table(tableName)
        ?: error("Ignite 3.x 테이블을 찾을 수 없습니다. tableName=$tableName")
    return table.keyValueView(keyType, valueType)
}

/**
 * reified 타입을 사용하는 [KeyValueView] 조회 확장 함수입니다.
 */
inline fun <reified K: Any, reified V: Any> IgniteClient.keyValueView(
    tableName: String,
): KeyValueView<K, V> = keyValueView(tableName, K::class.java, V::class.java)

/**
 * [IgniteNearCache] ([NearCache])를 생성하는 확장 함수입니다.
 */
fun <K: Any, V: Any> IgniteClient.nearCache(
    config: IgniteNearCacheConfig<K, V>,
): NearCache<K, V> = igniteNearCache(this, config)

/**
 * [io.bluetape4k.ignite3.cache.coroutines.IgniteSuspendNearCache] ([NearSuspendCache])를 생성하는 확장 함수입니다.
 */
fun <K: Any, V: Any> IgniteClient.suspendNearCache(
    config: IgniteNearCacheConfig<K, V>,
): NearSuspendCache<K, V> = igniteNearSuspendCache(this, config)
