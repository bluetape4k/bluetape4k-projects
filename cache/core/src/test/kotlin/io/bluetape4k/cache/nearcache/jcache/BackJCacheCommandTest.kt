package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class BackJCacheCommandTest {

    companion object: KLogging()

    @Test
    fun `Put - key와 value 보관`() {
        val cmd = BackJCacheCommand.Put("hello", 5)
        cmd.key shouldBeEqualTo "hello"
        cmd.value shouldBeEqualTo 5
        (cmd is BackJCacheCommand<*, *>).shouldBeTrue()
    }

    @Test
    fun `PutAll - entries 보관`() {
        val entries = mapOf("a" to 1, "b" to 2)
        val cmd = BackJCacheCommand.PutAll<String, Int>(entries)
        cmd.entries shouldBeEqualTo entries
    }

    @Test
    fun `Remove - key 보관`() {
        val cmd = BackJCacheCommand.Remove<String, Int>("key1")
        cmd.key shouldBeEqualTo "key1"
    }

    @Test
    fun `RemoveAll - keys 보관`() {
        val keys = setOf("k1", "k2", "k3")
        val cmd = BackJCacheCommand.RemoveAll<String, Int>(keys)
        cmd.keys shouldBeEqualTo keys
    }

    @Test
    fun `ClearBack - 인스턴스 생성`() {
        val cmd = BackJCacheCommand.ClearBack<String, Int>()
        (cmd is BackJCacheCommand.ClearBack).shouldBeTrue()
    }

    @Test
    fun `sealed interface - when 분기`() {
        val cmds: List<BackJCacheCommand<String, Int>> = listOf(
            BackJCacheCommand.Put("k", 1),
            BackJCacheCommand.PutAll(mapOf("a" to 2)),
            BackJCacheCommand.Remove("r"),
            BackJCacheCommand.RemoveAll(setOf("x")),
            BackJCacheCommand.ClearBack(),
        )
        val types = cmds.map { cmd ->
            when (cmd) {
                is BackJCacheCommand.Put -> "Put"
                is BackJCacheCommand.PutAll -> "PutAll"
                is BackJCacheCommand.Remove -> "Remove"
                is BackJCacheCommand.RemoveAll -> "RemoveAll"
                is BackJCacheCommand.ClearBack -> "ClearBack"
            }
        }
        types shouldBeEqualTo listOf("Put", "PutAll", "Remove", "RemoveAll", "ClearBack")
    }
}
