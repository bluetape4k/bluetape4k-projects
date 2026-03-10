package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedisMapTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var map: RedisMap

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        map = RedisMap(connection, randomName())
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

    // =========================================================================
    // 코루틴 테스트
    // =========================================================================

    @Test
    fun `putSuspending and getSuspending`() = runSuspendIO {
        map.putSuspending("field1", "value1").shouldBeTrue()
        map.getSuspending("field1") shouldBeEqualTo "value1"
        map.getSuspending("nonexistent").shouldBeNull()
    }

    @Test
    fun `putIfAbsentSuspending`() = runSuspendIO {
        map.putIfAbsentSuspending("field1", "first").shouldBeTrue()
        map.putIfAbsentSuspending("field1", "second").shouldBeFalse()
        map.getSuspending("field1") shouldBeEqualTo "first"
    }

    @Test
    fun `removeSuspending and containsKeySuspending`() = runSuspendIO {
        map.putSuspending("field1", "value1")
        map.containsKeySuspending("field1").shouldBeTrue()
        map.removeSuspending("field1") shouldBeEqualTo 1L
        map.containsKeySuspending("field1").shouldBeFalse()
    }

    @Test
    fun `sizeSuspending and isEmptySuspending`() = runSuspendIO {
        map.isEmptySuspending().shouldBeTrue()
        map.putSuspending("f1", "v1")
        map.putSuspending("f2", "v2")
        map.sizeSuspending() shouldBeEqualTo 2L
        map.isEmptySuspending().shouldBeFalse()
    }

    @Test
    fun `entriesSuspending, keySetSuspending, valuesSuspending`() = runSuspendIO {
        map.putAllSuspending(mapOf("f1" to "v1", "f2" to "v2", "f3" to "v3"))

        map.keySetSuspending() shouldContainAll listOf("f1", "f2", "f3")
        map.valuesSuspending() shouldContainAll listOf("v1", "v2", "v3")

        val entries = map.entriesSuspending()
        entries.shouldHaveSize(3)
        entries["f1"] shouldBeEqualTo "v1"
    }

    @Test
    fun `getAllSuspending`() = runSuspendIO {
        map.putSuspending("f1", "v1")
        map.putSuspending("f2", "v2")

        val result = map.getAllSuspending(listOf("f1", "f2", "missing"))
        result["f1"] shouldBeEqualTo "v1"
        result["f2"] shouldBeEqualTo "v2"
        result["missing"].shouldBeNull()
    }

    @Test
    fun `clearSuspending`() = runSuspendIO {
        map.putAllSuspending(mapOf("f1" to "v1", "f2" to "v2"))
        map.isEmptySuspending().shouldBeFalse()
        map.clearSuspending() shouldBeEqualTo 1L
        map.isEmptySuspending().shouldBeTrue()
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 동시 put`() = runSuspendIO {
        val itemCount = 100

        val jobs = List(itemCount) { index ->
            async {
                map.putSuspending("field-$index", "value-$index")
            }
        }
        jobs.awaitAll()

        map.sizeSuspending() shouldBeEqualTo itemCount.toLong()
    }
}
