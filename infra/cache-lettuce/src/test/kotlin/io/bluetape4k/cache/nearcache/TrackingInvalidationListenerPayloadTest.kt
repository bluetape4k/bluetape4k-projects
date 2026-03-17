package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * [TrackingInvalidationListener.handleInvalidation] 페이로드 디코딩 로직 단위 테스트.
 *
 * reflection을 통해 private handleInvalidation()을 직접 호출하여
 * 다양한 payload 타입(ByteBuffer, ByteArray, String, mixed, null)에 대한
 * 처리가 올바른지 검증한다.
 */
class TrackingInvalidationListenerPayloadTest : AbstractLettuceNearCacheTest() {
    // ---- 테스트용 RecordingLocalCache ----

    private class RecordingLocalCache : LettuceLocalCache<String, String> {
        val invalidatedKeys = mutableListOf<String>()
        var clearCalled = false

        override fun get(key: String): String? = null

        override fun getAll(keys: Set<String>): Map<String, String> = emptyMap()

        override fun put(
            key: String,
            value: String,
        ) {}

        override fun putAll(map: Map<out String, String>) {}

        override fun remove(key: String) {}

        override fun removeAll(keys: Set<String>) {}

        override fun invalidate(key: String) {
            invalidatedKeys.add(key)
        }

        override fun invalidateAll(keys: Collection<String>) {
            invalidatedKeys.addAll(keys)
        }

        override fun containsKey(key: String): Boolean = false

        override fun clear() {
            clearCalled = true
        }

        override fun estimatedSize(): Long = 0L

        override fun stats(): CacheStats? = null
    }

    // ---- 헬퍼 ----

    private fun createListener(
        cacheName: String,
        localCache: RecordingLocalCache,
    ): TrackingInvalidationListener<String> {
        val conn = resp3Client.connect(StringCodec.UTF8)
        return TrackingInvalidationListener(localCache, conn, cacheName)
    }

    private fun callHandleInvalidation(
        listener: TrackingInvalidationListener<*>,
        content: List<Any?>,
    ) {
        val method =
            TrackingInvalidationListener::class.java
                .getDeclaredMethod("handleInvalidation", List::class.java)
        method.isAccessible = true
        method.invoke(listener, content)
    }

    private fun encodeKey(key: String): ByteBuffer = StringCodec.UTF8.encodeKey(key)

    // ---- 테스트 ----

    @Test
    fun `invalidation payload가 mixed type이어도 cacheName prefix 키만 무효화한다`() {
        val cacheName = "mixed-cache"
        val localCache = RecordingLocalCache()
        val listener = createListener(cacheName, localCache)

        val content =
            listOf<Any?>(
                encodeKey("invalidate"),
                listOf(
                    encodeKey("$cacheName:key1"), // ByteBuffer → 무효화
                    "some-non-bytebuffer-string", // String → 다른 cacheName이므로 무시
                    encodeKey("$cacheName:key2"), // ByteBuffer → 무효화
                    null, // null → filterNotNull로 제거
                    encodeKey("other-cache:key3") // 다른 cacheName → 무시
                )
            )

        callHandleInvalidation(listener, content)

        localCache.invalidatedKeys shouldBeEqualTo listOf("key1", "key2")
        localCache.clearCalled.shouldBeFalse()
    }

    @Test
    fun `invalidation payload가 null이면 local cache 전체 clear한다`() {
        val localCache = RecordingLocalCache()
        val listener = createListener("null-cache", localCache)

        val content = listOf<Any?>(encodeKey("invalidate"), null)

        callHandleInvalidation(listener, content)

        localCache.clearCalled.shouldBeTrue()
        localCache.invalidatedKeys.isEmpty().shouldBeTrue()
    }

    @Test
    fun `다른 cacheName 키만 전달되면 invalidate하지 않는다`() {
        val localCache = RecordingLocalCache()
        val listener = createListener("my-cache", localCache)

        val content =
            listOf<Any?>(
                encodeKey("invalidate"),
                listOf(
                    encodeKey("other-cache:key1"),
                    encodeKey("another:key2")
                )
            )

        callHandleInvalidation(listener, content)

        localCache.invalidatedKeys.isEmpty().shouldBeTrue()
        localCache.clearCalled.shouldBeFalse()
    }

    @Test
    fun `invalidation payload가 ByteArray면 정상 디코딩 후 무효화한다`() {
        val cacheName = "bytearray-cache"
        val localCache = RecordingLocalCache()
        val listener = createListener(cacheName, localCache)

        // content[1]이 단일 ByteArray
        val key = "$cacheName:mykey"
        val content =
            listOf<Any?>(
                encodeKey("invalidate"),
                key.toByteArray(Charsets.UTF_8)
            )

        callHandleInvalidation(listener, content)

        localCache.invalidatedKeys shouldBeEqualTo listOf("mykey")
    }

    @Test
    fun `invalidation payload가 단일 ByteBuffer면 해당 key만 무효화한다`() {
        val cacheName = "single-cache"
        val localCache = RecordingLocalCache()
        val listener = createListener(cacheName, localCache)

        // content[1]이 단일 ByteBuffer (리스트가 아님)
        val content =
            listOf<Any?>(
                encodeKey("invalidate"),
                encodeKey("$cacheName:singlekey")
            )

        callHandleInvalidation(listener, content)

        localCache.invalidatedKeys shouldBeEqualTo listOf("singlekey")
    }
}
