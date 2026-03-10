package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.AbstractHazelcastTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach

/**
 * [HazelcastNearCache] / [HazelcastSuspendNearCache] 공통 테스트 베이스.
 * Testcontainers Hazelcast를 사용하며, IMap 기반 NearCache 기능을 검증한다.
 */
abstract class AbstractHazelcastNearCacheTest: AbstractHazelcastTest() {

    companion object: KLogging() {
        const val REPEAT_SIZE = 3
    }

    @BeforeEach
    fun setup() {
        // 테스트마다 캐시를 새로 생성하므로 별도 초기화 불필요
    }

    protected fun verifyGetMiss(get: (String) -> String?) {
        get("missing-key").shouldBeNull()
    }

    protected fun verifyPutAndGet(put: (String, String) -> Unit, get: (String) -> String?) {
        put("key1", "value1")
        get("key1") shouldBeEqualTo "value1"
    }

    protected fun verifyRemove(
        put: (String, String) -> Unit,
        get: (String) -> String?,
        remove: (String) -> Unit,
    ) {
        put("key1", "value1")
        get("key1").shouldNotBeNull()
        remove("key1")
        get("key1").shouldBeNull()
    }

    protected fun verifyContainsKey(
        put: (String, String) -> Unit,
        containsKey: (String) -> Boolean,
        remove: (String) -> Unit,
    ) {
        put("keyX", "valX")
        containsKey("keyX") shouldBeEqualTo true
        containsKey("nonexistent") shouldBeEqualTo false
        remove("keyX")
        containsKey("keyX") shouldBeEqualTo false
    }

    protected fun verifyPutIfAbsent(
        putIfAbsent: (String, String) -> String?,
        get: (String) -> String?,
    ) {
        putIfAbsent("key", "first").shouldBeNull()
        get("key") shouldBeEqualTo "first"
        putIfAbsent("key", "second") shouldBeEqualTo "first"
        get("key") shouldBeEqualTo "first"
    }

    protected fun verifyGetAll(
        putAll: (Map<String, String>) -> Unit,
        getAll: (Set<String>) -> Map<String, String>,
    ) {
        val data = mapOf("a" to "1", "b" to "2", "c" to "3")
        putAll(data)
        val result = getAll(setOf("a", "b", "c", "x"))
        result["a"] shouldBeEqualTo "1"
        result["b"] shouldBeEqualTo "2"
        result["c"] shouldBeEqualTo "3"
        result["x"].shouldBeNull()
    }

    protected fun verifyReplace(
        put: (String, String) -> Unit,
        replace: (String, String) -> Boolean,
        get: (String) -> String?,
    ) {
        replace("noKey", "val") shouldBeEqualTo false
        put("key", "old")
        replace("key", "new") shouldBeEqualTo true
        get("key") shouldBeEqualTo "new"
    }

    protected fun verifyGetAndRemove(
        put: (String, String) -> Unit,
        getAndRemove: (String) -> String?,
        get: (String) -> String?,
    ) {
        put("key", "value")
        getAndRemove("key") shouldBeEqualTo "value"
        get("key").shouldBeNull()
        getAndRemove("key").shouldBeNull()
    }

    protected fun verifyGetAndReplace(
        put: (String, String) -> Unit,
        getAndReplace: (String, String) -> String?,
        get: (String) -> String?,
    ) {
        getAndReplace("missing", "val").shouldBeNull()
        put("key", "old")
        getAndReplace("key", "new") shouldBeEqualTo "old"
        get("key") shouldBeEqualTo "new"
    }

    protected fun verifyRemoveAll(
        putAll: (Map<String, String>) -> Unit,
        removeAll: (Set<String>) -> Unit,
        get: (String) -> String?,
    ) {
        putAll(mapOf("a" to "1", "b" to "2", "c" to "3"))
        removeAll(setOf("a", "b"))
        get("a").shouldBeNull()
        get("b").shouldBeNull()
        get("c") shouldBeEqualTo "3"
    }

    protected fun verifyClearLocal(
        put: (String, String) -> Unit,
        clearLocal: () -> Unit,
        localSize: () -> Long,
        containsKeyInBack: (String) -> Boolean,
    ) {
        put("k1", "v1")
        put("k2", "v2")
        clearLocal()
        localSize() shouldBeEqualTo 0L
        containsKeyInBack("k1") shouldBeEqualTo true
    }
}
