package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceSuspendStringMapTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var map: LettuceSuspendStringMap

    @BeforeEach
    fun setup() {
        map = LettuceSuspendStringMap(connection, randomName())
    }

    @AfterEach
    fun teardown() = runSuspendIO {
        map.clear()
    }

    @Test
    fun `put and get - 기본 CRUD`() = runSuspendIO {
        map.put("key1", "value1").shouldBeTrue()
        map.get("key1") shouldBeEqualTo "value1"

        map.put("key1", "updated").shouldBeFalse()
        map.get("key1") shouldBeEqualTo "updated"
    }

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() = runSuspendIO {
        map.get("notexist").shouldBeNull()
    }

    @Test
    fun `putIfAbsent - 없을 때만 설정`() = runSuspendIO {
        map.putIfAbsent("key1", "first").shouldBeTrue()
        map.putIfAbsent("key1", "second").shouldBeFalse()
        map.get("key1") shouldBeEqualTo "first"
    }

    @Test
    fun `size and isEmpty`() = runSuspendIO {
        map.isEmpty().shouldBeTrue()
        map.put("a", "1")
        map.put("b", "2")
        map.size() shouldBeEqualTo 2L
        map.isEmpty().shouldBeFalse()
    }

    @Test
    fun `putAll and keySet`() = runSuspendIO {
        map.putAll(mapOf("x" to "1", "y" to "2", "z" to "3"))
        map.keySet() shouldHaveSize 3
    }

    @Test
    fun `remove - 필드 삭제`() = runSuspendIO {
        map.put("key1", "val")
        map.remove("key1") shouldBeEqualTo 1L
        map.containsKey("key1").shouldBeFalse()
    }
}
