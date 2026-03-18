package io.bluetape4k.cache.nearcache

import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 복수 cacheName 인스턴스 간 격리 검증 테스트.
 *
 * 같은 Redis DB를 공유하는 여러 [LettuceNearCache] / [LettuceSuspendNearCache] 인스턴스가
 * 서로의 데이터를 침범하지 않는지 검증한다.
 *
 * ## 검증 시나리오
 * - 동일 key 이름을 서로 다른 cacheName으로 사용해도 독립적으로 동작
 * - clearAll()은 해당 cacheName의 key만 삭제 (다른 캐시 보존)
 * - remove(key)는 해당 캐시의 key만 삭제
 * - redisSize()는 해당 cacheName의 key만 카운트
 * - 동기/비동기 캐시 혼합 사용 시에도 격리 보장
 */
class LettuceNearCacheIsolationTest: AbstractLettuceNearCacheTest() {
    // ---- 동기 캐시 인스턴스 3개 ----
    private lateinit var cacheA: LettuceNearCache<String>
    private lateinit var cacheB: LettuceNearCache<String>
    private lateinit var cacheC: LettuceNearCache<String>

    // ---- 코루틴 캐시 인스턴스 2개 ----
    private lateinit var suspendCacheX: LettuceSuspendNearCache<String>
    private lateinit var suspendCacheY: LettuceSuspendNearCache<String>

    @BeforeEach
    fun createCaches() {
        cacheA = LettuceNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "isolation-a"))
        cacheB = LettuceNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "isolation-b"))
        cacheC = LettuceNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "isolation-c"))
        suspendCacheX =
            LettuceSuspendNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "isolation-x"))
        suspendCacheY =
            LettuceSuspendNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "isolation-y"))
    }

    @AfterEach
    fun closeCaches() {
        runCatching { cacheA.close() }
        runCatching { cacheB.close() }
        runCatching { cacheC.close() }
        runCatching { suspendCacheX.close() }
        runCatching { suspendCacheY.close() }
    }

    // ---- 기본 격리: 동일 key 이름 ----

    @Test
    fun `동일 key 이름을 서로 다른 cacheName에 저장하면 독립적으로 관리된다`() {
        val key = "shared-key-name"

        cacheA.put(key, "value-from-A")
        cacheB.put(key, "value-from-B")
        cacheC.put(key, "value-from-C")

        cacheA.get(key) shouldBeEqualTo "value-from-A"
        cacheB.get(key) shouldBeEqualTo "value-from-B"
        cacheC.get(key) shouldBeEqualTo "value-from-C"

        // Redis 직접 확인: 각각 별도 key로 저장됨
        directCommands.get("isolation-a:$key") shouldBeEqualTo "value-from-A"
        directCommands.get("isolation-b:$key") shouldBeEqualTo "value-from-B"
        directCommands.get("isolation-c:$key") shouldBeEqualTo "value-from-C"
    }

    @Test
    fun `한 캐시에서 remove해도 다른 캐시의 동일 key는 유지된다`() {
        val key = "key-to-remove"

        cacheA.put(key, "from-A")
        cacheB.put(key, "from-B")

        cacheA.remove(key)

        cacheA.get(key).shouldBeNull()
        cacheB.get(key) shouldBeEqualTo "from-B" // B는 영향 없음
    }

    @Test
    fun `clearAll()은 해당 cacheName의 key만 삭제하고 다른 캐시는 보존한다`() {
        // 세 캐시에 동일한 key들을 저장
        cacheA.putAll(mapOf("k1" to "a1", "k2" to "a2", "k3" to "a3"))
        cacheB.putAll(mapOf("k1" to "b1", "k2" to "b2", "k3" to "b3"))
        cacheC.putAll(mapOf("k1" to "c1", "k2" to "c2", "k3" to "c3"))

        // cacheB만 전체 삭제
        cacheB.clearAll()

        // cacheB는 비어 있어야 함
        cacheB.get("k1").shouldBeNull()
        cacheB.get("k2").shouldBeNull()
        cacheB.get("k3").shouldBeNull()
        cacheB.localCacheSize() shouldBeEqualTo 0L
        cacheB.backCacheSize() shouldBeEqualTo 0L

        // cacheA, cacheC는 그대로 유지
        cacheA.get("k1") shouldBeEqualTo "a1"
        cacheA.get("k2") shouldBeEqualTo "a2"
        cacheA.get("k3") shouldBeEqualTo "a3"

        cacheC.get("k1") shouldBeEqualTo "c1"
        cacheC.get("k2") shouldBeEqualTo "c2"
        cacheC.get("k3") shouldBeEqualTo "c3"
    }

    @Test
    fun `clearAll() 순차 호출 - 각 캐시가 독립적으로 삭제된다`() {
        cacheA.putAll(mapOf("x" to "ax", "y" to "ay"))
        cacheB.putAll(mapOf("x" to "bx", "y" to "by"))

        cacheA.clearAll()
        cacheA.get("x").shouldBeNull()
        cacheB.get("x") shouldBeEqualTo "bx" // B 영향 없음

        cacheB.clearAll()
        cacheB.get("x").shouldBeNull()
    }

    // ---- redisSize 격리 ----

    @Test
    fun `redisSize()는 해당 cacheName의 key만 카운트한다`() {
        cacheA.putAll(mapOf("r1" to "v", "r2" to "v", "r3" to "v"))
        cacheB.putAll(mapOf("r1" to "v", "r2" to "v"))

        cacheA.backCacheSize() shouldBeEqualTo 3L
        cacheB.backCacheSize() shouldBeEqualTo 2L

        cacheA.remove("r1")
        cacheA.backCacheSize() shouldBeEqualTo 2L
        cacheB.backCacheSize() shouldBeEqualTo 2L // B 영향 없음
    }

    // ---- containsKey 격리 ----

    @Test
    fun `containsKey()는 해당 캐시의 key만 조회한다`() {
        cacheA.put("exist-key", "value")

        cacheA.containsKey("exist-key").shouldBeTrue()
        cacheB.containsKey("exist-key").shouldBeFalse() // B에는 없음
        cacheC.containsKey("exist-key").shouldBeFalse()
    }

    // ---- clearLocal 격리: L1만 비워도 다른 캐시 영향 없음 ----

    @Test
    fun `clearLocal()은 해당 캐시의 L1만 비우고 다른 캐시는 영향 없다`() {
        cacheA.put("loc-key", "a-val")
        cacheB.put("loc-key", "b-val")

        cacheA.localCacheSize() shouldBeEqualTo 1L
        cacheB.localCacheSize() shouldBeEqualTo 1L

        cacheA.clearLocal()

        cacheA.localCacheSize() shouldBeEqualTo 0L
        cacheB.localCacheSize() shouldBeEqualTo 1L // B L1 유지

        // Redis 데이터도 영향 없음
        directCommands.get("isolation-a:loc-key").shouldNotBeNull()
        directCommands.get("isolation-b:loc-key").shouldNotBeNull()
    }

    // ---- key에 ':' 포함 허용 ----

    @Test
    fun `key에 콜론이 포함되어도 정상 동작한다`() {
        val key = "user:123:profile"

        cacheA.put(key, "alice")
        cacheB.put(key, "bob")

        cacheA.get(key) shouldBeEqualTo "alice"
        cacheB.get(key) shouldBeEqualTo "bob"

        // Redis key: "isolation-a:user:123:profile"
        directCommands.get("isolation-a:$key") shouldBeEqualTo "alice"
        directCommands.get("isolation-b:$key") shouldBeEqualTo "bob"

        // 한 캐시 제거해도 다른 캐시 유지
        cacheA.remove(key)
        cacheA.get(key).shouldBeNull()
        cacheB.get(key) shouldBeEqualTo "bob"
    }

    // ---- 동기/코루틴 혼합 격리 ----

    @Test
    fun `동기 캐시와 코루틴 캐시가 동일 key 이름으로 독립 동작한다`() = runTest {
        val key = "mixed-key"

        cacheA.put(key, "sync-value")
        suspendCacheX.put(key, "suspend-value")

        cacheA.get(key) shouldBeEqualTo "sync-value"
        suspendCacheX.get(key) shouldBeEqualTo "suspend-value"

        // Redis 직접 확인
        directCommands.get("isolation-a:$key") shouldBeEqualTo "sync-value"
        directCommands.get("isolation-x:$key") shouldBeEqualTo "suspend-value"
    }

    @Test
    fun `코루틴 캐시 clearAll()은 해당 cacheName만 삭제한다`() = runTest {
        suspendCacheX.putAll(mapOf("p" to "xp", "q" to "xq"))
        suspendCacheY.putAll(mapOf("p" to "yp", "q" to "yq"))

        suspendCacheX.clearAll()

        suspendCacheX.get("p").shouldBeNull()
        suspendCacheX.get("q").shouldBeNull()
        suspendCacheX.backCacheSize() shouldBeEqualTo 0L

        // Y는 영향 없음
        suspendCacheY.get("p") shouldBeEqualTo "yp"
        suspendCacheY.get("q") shouldBeEqualTo "yq"
        suspendCacheY.backCacheSize() shouldBeEqualTo 2L
    }

    @Test
    fun `코루틴 캐시 동일 key - 서로 독립적으로 관리된다`() = runTest {
        val key = "common-key"

        suspendCacheX.put(key, "x-value")
        suspendCacheY.put(key, "y-value")

        suspendCacheX.get(key) shouldBeEqualTo "x-value"
        suspendCacheY.get(key) shouldBeEqualTo "y-value"

        suspendCacheX.remove(key)
        suspendCacheX.get(key).shouldBeNull()
        suspendCacheY.get(key) shouldBeEqualTo "y-value" // Y 영향 없음
    }

    // ---- 대량 데이터 격리 ----

    @Test
    fun `대량 key 저장 후 clearAll()은 해당 캐시만 정확히 삭제한다`() {
        val countA = 50
        val countB = 30

        cacheA.putAll((1..countA).associate { "key-$it" to "val-a-$it" })
        cacheB.putAll((1..countB).associate { "key-$it" to "val-b-$it" })

        cacheA.backCacheSize() shouldBeEqualTo countA.toLong()
        cacheB.backCacheSize() shouldBeEqualTo countB.toLong()

        cacheA.clearAll()

        cacheA.backCacheSize() shouldBeEqualTo 0L
        cacheB.backCacheSize() shouldBeEqualTo countB.toLong() // B 영향 없음

        // B 데이터 무결성 확인
        cacheB.get("key-1") shouldBeEqualTo "val-b-1"
        cacheB.get("key-$countB") shouldBeEqualTo "val-b-$countB"
    }
}
