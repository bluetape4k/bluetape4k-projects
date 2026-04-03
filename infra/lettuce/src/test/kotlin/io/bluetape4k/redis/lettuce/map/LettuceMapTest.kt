package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceMapTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var map: LettuceMap<String>

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        map = LettuceMap(connection, randomName())
    }

    @AfterEach
    fun teardown() {
        map.clear()
    }

    // =========================================================================
    // 동기 테스트
    // =========================================================================

    @Test
    fun `put and get - 기본 CRUD`() {
        map.put("field1", "value1").shouldBeTrue()  // 새 필드 추가
        map.get("field1") shouldBeEqualTo "value1"

        map.put("field1", "updated").shouldBeFalse()  // 기존 필드 업데이트
        map.get("field1") shouldBeEqualTo "updated"
    }

    @Test
    fun `get - 존재하지 않는 필드는 null 반환`() {
        map.get("nonexistent").shouldBeNull()
    }

    @Test
    fun `putIfAbsent - 없을 때만 설정`() {
        map.putIfAbsent("field1", "first").shouldBeTrue()   // 새로 설정
        map.putIfAbsent("field1", "second").shouldBeFalse() // 이미 존재하므로 실패
        map.get("field1") shouldBeEqualTo "first"           // 변경되지 않음
    }

    @Test
    fun `remove - 필드 삭제`() {
        map.put("field1", "value1")
        map.containsKey("field1").shouldBeTrue()

        map.remove("field1") shouldBeEqualTo 1L
        map.containsKey("field1").shouldBeFalse()
        map.remove("field1") shouldBeEqualTo 0L  // 이미 없는 필드 삭제 시 0
    }

    @Test
    fun `containsKey - 필드 존재 확인`() {
        map.containsKey("field1").shouldBeFalse()
        map.put("field1", "value1")
        map.containsKey("field1").shouldBeTrue()
    }

    @Test
    fun `size and isEmpty`() {
        map.isEmpty().shouldBeTrue()
        map.size() shouldBeEqualTo 0L

        map.put("f1", "v1")
        map.put("f2", "v2")
        map.put("f3", "v3")

        map.isEmpty().shouldBeFalse()
        map.size() shouldBeEqualTo 3L
    }

    @Test
    fun `keySet, values, entries`() {
        map.put("f1", "v1")
        map.put("f2", "v2")
        map.put("f3", "v3")

        map.keySet() shouldContainAll listOf("f1", "f2", "f3")
        map.values() shouldContainAll listOf("v1", "v2", "v3")

        val entries = map.entries()
        entries.shouldHaveSize(3)
        entries["f1"] shouldBeEqualTo "v1"
        entries["f2"] shouldBeEqualTo "v2"
        entries["f3"] shouldBeEqualTo "v3"
    }

    @Test
    fun `putAll - 일괄 설정`() {
        val data = mapOf("f1" to "v1", "f2" to "v2", "f3" to "v3")
        map.putAll(data)

        map.size() shouldBeEqualTo 3L
        map.get("f1") shouldBeEqualTo "v1"
        map.get("f2") shouldBeEqualTo "v2"
        map.get("f3") shouldBeEqualTo "v3"
    }

    @Test
    fun `getAll - 일괄 조회`() {
        map.put("f1", "v1")
        map.put("f2", "v2")

        val result = map.getAll(listOf("f1", "f2", "nonexistent"))
        result.shouldHaveSize(3)
        result["f1"] shouldBeEqualTo "v1"
        result["f2"] shouldBeEqualTo "v2"
        result["nonexistent"].shouldBeNull()
    }

    @Test
    fun `clear - 전체 삭제`() {
        map.put("f1", "v1")
        map.put("f2", "v2")
        map.isEmpty().shouldBeFalse()

        map.clear() shouldBeEqualTo 1L
        map.isEmpty().shouldBeTrue()
    }

    @Test
    fun `putAll - 빈 맵은 무시`() {
        map.putAll(emptyMap())
        map.isEmpty().shouldBeTrue()
    }

    @Test
    fun `getAll - 빈 컬렉션은 빈 맵 반환`() {
        map.getAll(emptyList()) shouldBeEqualTo emptyMap()
    }

    // =========================================================================
    // 비동기 테스트
    // =========================================================================

    @Test
    fun `putAsync and getAsync`() {
        map.putAsync("field1", "value1").get().shouldBeTrue()
        map.getAsync("field1").get() shouldBeEqualTo "value1"
    }

    @Test
    fun `putIfAbsentAsync`() {
        map.putIfAbsentAsync("field1", "first").get().shouldBeTrue()
        map.putIfAbsentAsync("field1", "second").get().shouldBeFalse()
        map.getAsync("field1").get() shouldBeEqualTo "first"
    }

    @Test
    fun `removeAsync and containsKeyAsync`() {
        map.putAsync("field1", "value1").get()
        map.containsKeyAsync("field1").get().shouldBeTrue()
        map.removeAsync("field1").get() shouldBeEqualTo 1L
        map.containsKeyAsync("field1").get().shouldBeFalse()
    }

    @Test
    fun `sizeAsync and isEmptyAsync`() {
        map.isEmptyAsync().get().shouldBeTrue()
        map.putAsync("f1", "v1").get()
        map.putAsync("f2", "v2").get()
        map.sizeAsync().get() shouldBeEqualTo 2L
        map.isEmptyAsync().get().shouldBeFalse()
    }

    @Test
    fun `entriesAsync, keySetAsync, valuesAsync`() {
        val data = mapOf("f1" to "v1", "f2" to "v2")
        map.putAllAsync(data).get()

        map.keySetAsync().get() shouldContainAll listOf("f1", "f2")
        map.valuesAsync().get() shouldContainAll listOf("v1", "v2")

        val entries = map.entriesAsync().get()
        entries["f1"] shouldBeEqualTo "v1"
        entries["f2"] shouldBeEqualTo "v2"
    }

    @Test
    fun `getAllAsync`() {
        map.putAsync("f1", "v1").get()
        val result = map.getAllAsync(listOf("f1", "missing")).get()
        result["f1"] shouldBeEqualTo "v1"
        result["missing"].shouldBeNull()
    }

    @Test
    fun `clearAsync`() {
        map.putAllAsync(mapOf("f1" to "v1", "f2" to "v2")).get()
        map.clearAsync().get() shouldBeEqualTo 1L
        map.isEmptyAsync().get().shouldBeTrue()
    }
}
