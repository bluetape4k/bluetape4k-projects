package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * Redis RESP3 CLIENT TRACKING invalidation 검증 테스트.
 *
 * ## 핵심 원칙
 * CLIENT TRACKING은 READ(GET) 명령어로 키를 조회했을 때만 tracking이 활성화된다.
 * 따라서 테스트 패턴은:
 * 1. `directCommands`로 Redis에 직접 값을 쓴다 (prefix key 사용)
 * 2. nearCache1.get() → local miss → Redis READ → CLIENT TRACKING 활성화
 * 3. 같은 cacheName의 다른 인스턴스나 외부 연결이 해당 key를 수정
 * 4. nearCache1의 local cache에 invalidation이 비동기적으로 전파됨을 검증
 *
 * ## Cross-instance 테스트 설계
 * 같은 cacheName을 가진 두 인스턴스(nearCache1, nearCache2)가
 * 동일한 Redis key 공간을 공유하므로, 한 인스턴스의 쓰기가
 * 다른 인스턴스의 local cache를 invalidate한다.
 */
class LettuceNearCacheTrackingTest: AbstractLettuceNearCacheTest() {
    companion object: KLogging()

    private lateinit var nearCache1: LettuceNearCache<String>
    private lateinit var nearCache2: LettuceNearCache<String>

    private lateinit var nearSuspendCache1: LettuceSuspendNearCache<String>
    private lateinit var nearSuspendCache2: LettuceSuspendNearCache<String>

    @BeforeEach
    fun createCaches() {
        // 같은 cacheName → 같은 Redis key 공간 → cross-instance invalidation 동작
        nearCache1 =
            LettuceNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "tracking-cache"))
        nearCache2 =
            LettuceNearCache(resp3Client, StringCodec.UTF8, LettuceNearCacheConfig(cacheName = "tracking-cache"))
        nearSuspendCache1 =
            LettuceSuspendNearCache(
                resp3Client,
                StringCodec.UTF8,
                LettuceNearCacheConfig(cacheName = "suspend-tracking-cache")
            )
        nearSuspendCache2 =
            LettuceSuspendNearCache(
                resp3Client,
                StringCodec.UTF8,
                LettuceNearCacheConfig(cacheName = "suspend-tracking-cache")
            )
    }

    @AfterEach
    fun closeCaches() {
        runCatching { nearCache1.close() }
        runCatching { nearCache2.close() }
        runCatching { nearSuspendCache1.close() }
        runCatching { nearSuspendCache2.close() }
    }

    @AfterAll
    fun cleanup() {
        directCommands.flushdb()
    }

    // ---- Sync 교차 invalidation ----

    @Test
    fun `cross-instance invalidation - nearCache1이 읽은 키를 nearCache2가 쓰면 nearCache1의 local이 invalidated`() {
        val key = "cross-key"
        val cacheName = nearCache1.cacheName

        // Step 1: prefix key로 Redis에 직접 값 설정
        directCommands.set("$cacheName:$key", "initial")

        // Step 2: nearCache1이 Redis에서 읽음 (cache miss) → CLIENT TRACKING이 이 키를 추적 시작
        nearCache1.get(key) shouldBeEqualTo "initial"
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // Step 3: nearCache2가 같은 키를 수정 → Redis가 nearCache1에 invalidation push 전송
        nearCache2.put(key, "updated-by-cache2")

        // Step 4: nearCache1의 local cache가 비동기로 invalidated되기를 기다림
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        // Step 5: nearCache1이 다시 읽으면 새 값을 가져옴
        nearCache1.get(key) shouldBeEqualTo "updated-by-cache2"
    }

    @Test
    fun `noloop - 자신이 쓴 키는 자신의 local을 invalidate하지 않음`() {
        val key = "noloop-key"
        val cacheName = nearCache1.cacheName

        // directCommands로 prefix key 설정 후 nearCache1이 읽어 tracking 활성화
        directCommands.set("$cacheName:$key", "initial")
        nearCache1.get(key) shouldBeEqualTo "initial"
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // 자신이 다시 write → noloop이므로 자신의 local은 invalidate되지 않아야 함
        nearCache1.put(key, "updated-by-self")

        // 약간 기다린 후에도 local size 유지 (noloop이므로 invalidated 안 됨)
        Thread.sleep(300)
        nearCache1.localCacheSize() shouldBeEqualTo 1L
        nearCache1.get(key) shouldBeEqualTo "updated-by-self"
    }

    @Test
    fun `external writer invalidation - 외부 연결이 직접 Redis 쓰기 시 invalidation 전파`() {
        val key = "external-key"
        val cacheName = nearCache1.cacheName

        // Step 1: prefix key로 초기값 설정
        directCommands.set("$cacheName:$key", "initial")

        // Step 2: nearCache1이 읽어 local에 populate + tracking 활성화
        nearCache1.get(key) shouldBeEqualTo "initial"
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // Step 3: 외부 Redis 클라이언트(tracking 없는 연결)가 prefix key를 직접 수정
        directCommands.set("$cacheName:$key", "updated-by-external")

        // Step 4: nearCache1의 local이 invalidated되기를 기다림
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        // Step 5: 다시 get 하면 새 값을 반환해야 함
        nearCache1.get(key) shouldBeEqualTo "updated-by-external"
    }

    @Test
    fun `remove invalidation - nearCache2가 삭제하면 nearCache1의 local이 invalidated`() {
        val key = "remove-key"
        val cacheName = nearCache1.cacheName

        // Step 1: prefix key로 설정
        directCommands.set("$cacheName:$key", "to-be-removed")

        // Step 2: nearCache1이 읽어 local populate + tracking 활성화
        nearCache1.get(key) shouldBeEqualTo "to-be-removed"
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // Step 3: nearCache2가 삭제 (같은 cacheName이므로 같은 Redis key 삭제)
        nearCache2.remove(key)

        // Step 4: nearCache1의 local이 invalidated되기를 기다림
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        // Step 5: nearCache1이 읽으면 null (키 자체가 삭제됨)
        nearCache1.get(key).shouldBeNull()
    }

    @Test
    fun `cacheName 격리 - 다른 cacheName 인스턴스의 쓰기는 invalidation을 발생시키지 않음`() {
        val key = "isolation-key"
        val cacheName1 = nearCache1.cacheName // "tracking-cache"

        // 다른 cacheName 인스턴스 생성
        val isolatedCache =
            LettuceNearCache(
                resp3Client,
                StringCodec.UTF8,
                LettuceNearCacheConfig(cacheName = "isolated-cache")
            )

        isolatedCache.use { isolated ->
            // nearCache1이 키를 읽어 tracking 활성화
            directCommands.set("$cacheName1:$key", "initial")
            nearCache1.get(key) shouldBeEqualTo "initial"
            nearCache1.localCacheSize() shouldBeEqualTo 1L

            // 다른 cacheName의 같은 key 이름 수정 (실제 Redis key는 다름)
            isolated.put(key, "from-isolated")

            // 약간 기다려도 nearCache1의 local은 invalidated되지 않아야 함
            Thread.sleep(300)
            nearCache1.localCacheSize() shouldBeEqualTo 1L
            nearCache1.get(key) shouldBeEqualTo "initial"
        }
    }

    // ---- Coroutine (Suspend) 교차 invalidation ----

    @Test
    fun `suspend - cross-instance invalidation`() =
        runTest {
            val key = "suspend-cross-key"
            val cacheName = nearSuspendCache1.cacheName

            // prefix key로 Redis에 직접 값 설정
            directCommands.set("$cacheName:$key", "initial")

            // nearSuspendCache1이 읽어 local populate + tracking 활성화
            nearSuspendCache1.get(key) shouldBeEqualTo "initial"
            nearSuspendCache1.localCacheSize() shouldBeEqualTo 1L

            // nearSuspendCache2가 수정 → Redis가 invalidation push 전송
            nearSuspendCache2.put(key, "updated-by-suspend-cache2")

            // nearSuspendCache1의 local이 비동기로 invalidated되기를 기다림
            await.atMost(5, TimeUnit.SECONDS).untilAsserted {
                nearSuspendCache1.localCacheSize() shouldBeEqualTo 0L
            }

            nearSuspendCache1.get(key) shouldBeEqualTo "updated-by-suspend-cache2"
        }

    @Test
    fun `suspend - external writer invalidation`() =
        runTest {
            val key = "suspend-external-key"
            val cacheName = nearSuspendCache1.cacheName

            // prefix key로 초기값 설정
            directCommands.set("$cacheName:$key", "initial")

            // nearSuspendCache1이 읽어 local populate + tracking 활성화
            nearSuspendCache1.get(key) shouldBeEqualTo "initial"
            nearSuspendCache1.localCacheSize() shouldBeEqualTo 1L

            // 외부 연결이 prefix key를 직접 수정
            directCommands.set("$cacheName:$key", "external-update")

            // nearSuspendCache1의 local이 invalidated되기를 기다림
            await.atMost(5, TimeUnit.SECONDS).untilAsserted {
                nearSuspendCache1.localCacheSize() shouldBeEqualTo 0L
            }

            nearSuspendCache1.get(key) shouldBeEqualTo "external-update"
        }

    // ---- putIfAbsent / replace tracking ----

    @Test
    fun `putIfAbsent success 경로도 tracking을 등록해서 외부 변경 시 invalidation 된다`() {
        val key = "pia-tracking-key"
        val cacheName = nearCache1.cacheName

        // putIfAbsent로 저장 (내부에서 tracking 활성화)
        nearCache1.putIfAbsent(key, "initial").shouldBeNull()
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // 외부 변경 → nearCache1 local이 invalidated 되어야 함
        directCommands.set("$cacheName:$key", "updated-externally")

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        nearCache1.get(key) shouldBeEqualTo "updated-externally"
    }

    @Test
    fun `replace success 경로도 tracking을 등록해서 외부 변경 시 invalidation 된다`() {
        val key = "replace-tracking-key"
        val cacheName = nearCache1.cacheName

        // 초기값 설정 후 replace (내부 get이 tracking 활성화)
        nearCache1.put(key, "initial")
        nearCache1.replace(key, "replaced").shouldBeTrue()
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // 외부 변경 → nearCache1 local이 invalidated 되어야 함
        directCommands.set("${nearCache1.cacheName}:$key", "updated-externally")

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        nearCache1.get(key) shouldBeEqualTo "updated-externally"
    }

    // ---- Suspend putIfAbsent / replace tracking ----

    @Test
    fun `suspend - putIfAbsent success 경로도 tracking을 등록한다`() = runSuspendIO {
        val key = "susp-pia-tracking-key"
        val cacheName = nearSuspendCache1.cacheName

        nearSuspendCache1.putIfAbsent(key, "initial").shouldBeNull()
        nearSuspendCache1.localCacheSize() shouldBeEqualTo 1L

        directCommands.set("$cacheName:$key", "updated-externally")

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearSuspendCache1.localCacheSize() shouldBeEqualTo 0L
        }

        nearSuspendCache1.get(key) shouldBeEqualTo "updated-externally"
    }


    @Test
    fun `suspend - replace success 경로도 tracking을 등록한다`() = runSuspendIO {
        val key = "susp-replace-tracking-key"
        val cacheName = nearSuspendCache1.cacheName

        directCommands.set("$cacheName:$key", "initial")
        nearSuspendCache1.replace(key, "replaced").shouldBeTrue()
        nearSuspendCache1.localCacheSize() shouldBeEqualTo 1L

        directCommands.set("$cacheName:$key", "updated-externally")

        await.atMost(5, TimeUnit.SECONDS).untilSuspending {
            nearSuspendCache1.get(key) == "updated-externally"
        }

        nearSuspendCache1.get(key) shouldBeEqualTo "updated-externally"
    }

    // ---- 추가 시나리오: putAll / removeAll / replace cross-instance ----

    @Test
    fun `putAll - cross-instance invalidation`() {
        val keys = listOf("putall-k1", "putall-k2", "putall-k3")
        val cacheName = nearCache1.cacheName

        // nearCache1이 읽어 tracking 활성화
        keys.forEach { key -> directCommands.set("$cacheName:$key", "initial") }
        keys.forEach { key -> nearCache1.get(key) shouldBeEqualTo "initial" }
        nearCache1.localCacheSize() shouldBeEqualTo 3L

        // nearCache2가 putAll로 한번에 수정 → nearCache1 local invalidated
        nearCache2.putAll(keys.associateWith { "updated-$it" })

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        // nearCache1이 다시 읽으면 새 값을 가져옴 (read-through)
        keys.forEach { key -> nearCache1.get(key) shouldBeEqualTo "updated-$key" }
    }

    @Test
    fun `removeAll - cross-instance invalidation`() {
        val keys = listOf("rmall-k1", "rmall-k2", "rmall-k3")
        val cacheName = nearCache2.cacheName

        // nearCache2가 읽어 tracking 활성화
        keys.forEach { key -> directCommands.set("$cacheName:$key", "value") }
        keys.forEach { key -> nearCache2.get(key) shouldBeEqualTo "value" }
        nearCache2.localCacheSize() shouldBeEqualTo 3L

        // nearCache1이 removeAll → Redis 삭제 → nearCache2 local invalidated
        nearCache1.removeAll(keys.toSet())

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache2.localCacheSize() shouldBeEqualTo 0L
        }

        // nearCache2가 읽으면 null (Redis에서도 삭제됨)
        keys.forEach { key -> nearCache2.get(key).shouldBeNull() }
    }

    @Test
    fun `replace - cross-instance invalidation`() {
        val key = "replace-inv-key"
        val cacheName = nearCache2.cacheName

        // nearCache2가 읽어 tracking 활성화
        directCommands.set("$cacheName:$key", "initial")
        nearCache2.get(key) shouldBeEqualTo "initial"
        nearCache2.localCacheSize() shouldBeEqualTo 1L

        // nearCache1이 put 후 replace → nearCache2 local invalidated
        nearCache1.put(key, "initial")
        nearCache1.replace(key, "replaced")

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache2.localCacheSize() shouldBeEqualTo 0L
        }

        // nearCache2가 다시 읽으면 새 값
        nearCache2.get(key) shouldBeEqualTo "replaced"
    }

    @Test
    fun `read-through after invalidation - 무효화 후 Redis에서 최신 값 조회`() {
        val key = "readthrough-key"
        val cacheName = nearCache1.cacheName

        // nearCache1이 읽어 local populate + tracking 활성화
        directCommands.set("$cacheName:$key", "v1")
        nearCache1.get(key) shouldBeEqualTo "v1"
        nearCache1.localCacheSize() shouldBeEqualTo 1L

        // 외부에서 값 변경 → nearCache1 local invalidated
        directCommands.set("$cacheName:$key", "v2")
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            nearCache1.localCacheSize() shouldBeEqualTo 0L
        }

        // 다시 get → local miss → Redis에서 v2 read-through + re-populate
        nearCache1.get(key) shouldBeEqualTo "v2"
        nearCache1.localCacheSize() shouldBeEqualTo 1L
    }

    // ---- Suspend 추가 시나리오 ----

    @Test
    fun `suspend - putAll cross-instance invalidation`() =
        runTest {
            val keys = listOf("susp-putall-k1", "susp-putall-k2")
            val cacheName = nearSuspendCache1.cacheName

            keys.forEach { key -> directCommands.set("$cacheName:$key", "initial") }
            keys.forEach { key -> nearSuspendCache1.get(key) shouldBeEqualTo "initial" }
            nearSuspendCache1.localCacheSize() shouldBeEqualTo 2L

            nearSuspendCache2.putAll(keys.associateWith { "updated-$it" })

            await.atMost(5, TimeUnit.SECONDS).untilAsserted {
                nearSuspendCache1.localCacheSize() shouldBeEqualTo 0L
            }

            keys.forEach { key -> nearSuspendCache1.get(key) shouldBeEqualTo "updated-$key" }
        }

    @Test
    fun `suspend - removeAll cross-instance invalidation`() =
        runTest {
            val keys = listOf("susp-rmall-k1", "susp-rmall-k2")
            val cacheName = nearSuspendCache2.cacheName

            keys.forEach { key -> directCommands.set("$cacheName:$key", "value") }
            keys.forEach { key -> nearSuspendCache2.get(key) shouldBeEqualTo "value" }
            nearSuspendCache2.localCacheSize() shouldBeEqualTo 2L

            nearSuspendCache1.removeAll(keys.toSet())

            await.atMost(5, TimeUnit.SECONDS).untilAsserted {
                nearSuspendCache2.localCacheSize() shouldBeEqualTo 0L
            }

            keys.forEach { key -> nearSuspendCache2.get(key).shouldBeNull() }
        }

    @Test
    fun `suspend - replace cross-instance invalidation`() =
        runTest {
            val key = "susp-replace-key"
            val cacheName = nearSuspendCache2.cacheName

            directCommands.set("$cacheName:$key", "initial")
            nearSuspendCache2.get(key) shouldBeEqualTo "initial"
            nearSuspendCache2.localCacheSize() shouldBeEqualTo 1L

            nearSuspendCache1.put(key, "initial")
            nearSuspendCache1.replace(key, "replaced")

            await.atMost(5, TimeUnit.SECONDS).untilAsserted {
                nearSuspendCache2.localCacheSize() shouldBeEqualTo 0L
            }

            nearSuspendCache2.get(key) shouldBeEqualTo "replaced"
        }
}
