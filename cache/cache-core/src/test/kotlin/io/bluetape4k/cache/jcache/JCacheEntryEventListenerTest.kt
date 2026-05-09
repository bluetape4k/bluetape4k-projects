package io.bluetape4k.cache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import javax.cache.configuration.MutableCacheEntryListenerConfiguration

class JCacheEntryEventListenerTest {

    companion object: KLogging()

    private fun frontCache(name: String = "front-${System.nanoTime()}") =
        JCaching.Caffeine.getOrCreate<String, String>(name)

    private fun backCache(name: String = "back-${System.nanoTime()}") =
        JCaching.Caffeine.getOrCreate<String, String>(name)

    @Test
    fun `onCreated - back cache에 추가되면 front cache에 동기화`() {
        val front = frontCache()
        val back = backCache()

        val listener = JCacheEntryEventListener(front)
        val config = MutableCacheEntryListenerConfiguration({ listener }, null, false, true)
        back.registerCacheEntryListener(config)

        back.put("key1", "value1")
        Thread.sleep(100) // listener 전파 대기

        front.containsKey("key1").shouldBeTrue()
        front.get("key1") shouldBeEqualTo "value1"

        front.close()
        back.close()
    }

    @Test
    fun `onUpdated - back cache 업데이트가 front에 반영`() {
        val front = frontCache()
        val back = backCache()

        val listener = JCacheEntryEventListener(front)
        val config = MutableCacheEntryListenerConfiguration({ listener }, null, false, true)
        back.registerCacheEntryListener(config)

        back.put("k", "v1")
        Thread.sleep(100)
        back.put("k", "v2")
        Thread.sleep(100)

        front.get("k") shouldBeEqualTo "v2"

        front.close()
        back.close()
    }

    @Test
    fun `onRemoved - back cache 삭제가 front에 반영`() {
        val front = frontCache()
        val back = backCache()

        front.put("key1", "value1")
        val listener = JCacheEntryEventListener(front)
        val config = MutableCacheEntryListenerConfiguration({ listener }, null, false, true)
        back.registerCacheEntryListener(config)

        back.put("key1", "value1")
        Thread.sleep(100)
        back.remove("key1")
        Thread.sleep(100)

        front.containsKey("key1").shouldBeFalse()

        front.close()
        back.close()
    }

    @Test
    fun `closed front cache - 이벤트를 무시한다`() {
        val front = frontCache()
        front.close()

        val back = backCache()
        val listener = JCacheEntryEventListener(front)
        val config = MutableCacheEntryListenerConfiguration({ listener }, null, false, true)
        back.registerCacheEntryListener(config)

        // 예외 없이 완료되어야 함
        back.put("key1", "value1")
        Thread.sleep(100)

        back.close()
    }
}
