package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class LettuceMapTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var map: LettuceMap<String>

    @BeforeEach
    fun setup() {
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
    fun `TTL API는 null과 기존 필드 및 Hash key 수명을 구분한다`() {
        map.putTtl("plain", "value", null).shouldBeTrue()
        map.putTtl("expiring", "value", Duration.ofSeconds(30)).shouldBeTrue()
        map.putTtl("expiring", "updated", Duration.ofSeconds(30)).shouldBeFalse()

        connection.sync().ttl(map.mapKey) shouldBeGreaterThan 0L
        map.refreshTtl(null)
        map.refreshTtl(Duration.ofSeconds(45))
        connection.sync().ttl(map.mapKey) shouldBeGreaterThan 0L

        map.putAllTtl(emptyMap(), Duration.ofSeconds(10))
        map.putAllTtl(mapOf("batch-1" to "v1", "batch-2" to "v2"), null)
        map.get("batch-1") shouldBeEqualTo "v1"
        map.putAllTtl(mapOf("batch-3" to "v3"), Duration.ofSeconds(30))
        map.get("batch-3") shouldBeEqualTo "v3"
    }

    @Test
    fun `분산 락은 예외 후에도 소유권을 해제하고 인자 범위를 검증한다`() {
        assertFailsWith<IllegalArgumentException> {
            map.withDistributedLock("owner", leaseTime = Duration.ZERO) { }
        }
        assertFailsWith<IllegalArgumentException> {
            map.withDistributedLock("owner", waitTime = Duration.ofMillis(-1)) { }
        }
        assertFailsWith<IllegalStateException> {
            map.withDistributedLock("owner-1") {
                error("block failure")
            }
        }

        map.withDistributedLock("owner-2", waitTime = Duration.ofSeconds(1)) {
            map.put("after-failure", "released")
        }
        map.get("after-failure") shouldBeEqualTo "released"
    }

    @Test
    fun `withDistributedLock - 같은 mapKey의 연결 간 임계 구간을 직렬화`() {
        val secondConnection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val secondMap = LettuceMap<String>(secondConnection, map.mapKey)

        try {
            map.withDistributedLock("owner-1", waitTime = Duration.ZERO) {
                assertFailsWith<IllegalStateException> {
                    secondMap.withDistributedLock(
                        token = "owner-2",
                        waitTime = Duration.ofMillis(100),
                    ) { }
                }
            }

            secondMap.withDistributedLock("owner-2", waitTime = Duration.ofSeconds(1)) {
                secondMap.put("field", "value")
            }
            map.get("field") shouldBeEqualTo "value"
        } finally {
            secondConnection.close()
        }
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
