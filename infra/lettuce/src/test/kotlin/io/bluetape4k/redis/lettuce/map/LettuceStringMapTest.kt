package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceStringMapTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var map: LettuceStringMap

    @BeforeEach
    fun setup() {
        map = LettuceStringMap(connection, randomName())
    }

    @AfterEach
    fun teardown() {
        map.clear()
    }

    @Test
    fun `put and get - 기본 CRUD`() {
        map.put("key1", "value1").shouldBeTrue()
        map.get("key1") shouldBeEqualTo "value1"

        map.put("key1", "updated").shouldBeFalse()
        map.get("key1") shouldBeEqualTo "updated"
    }

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() {
        map.get("notexist").shouldBeNull()
    }

    @Test
    fun `putIfAbsent - 없을 때만 설정`() {
        map.putIfAbsent("key1", "first").shouldBeTrue()
        map.putIfAbsent("key1", "second").shouldBeFalse()
        map.get("key1") shouldBeEqualTo "first"
    }

    @Test
    fun `size and isEmpty`() {
        map.isEmpty().shouldBeTrue()
        map.put("a", "1")
        map.put("b", "2")
        map.size() shouldBeEqualTo 2L
        map.isEmpty().shouldBeFalse()
    }

    @Test
    fun `putAll and keys`() {
        map.putAll(mapOf("x" to "1", "y" to "2", "z" to "3"))
        map.keySet() shouldHaveSize 3
    }

    @Test
    fun `remove - 필드 삭제`() {
        map.put("key1", "val")
        map.remove("key1") shouldBeEqualTo 1L
        map.containsKey("key1").shouldBeFalse()
    }
}
