package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * [LettuceLoadedMap] Read-through / Write-through / Write-behind / NONE 모드 테스트.
 */
class LettuceLoadedMapTest: AbstractLettuceTest() {
    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // 헬퍼: 테스트마다 고유한 keyPrefix를 가진 맵 생성
    // -------------------------------------------------------------------------

    private fun newMap(
        loader: MapLoader<String, String>? = null,
        writer: MapWriter<String, String>? = null,
        config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
    ): LettuceLoadedMap<String, String> =
        LettuceLoadedMap(
            client = client,
            loader = loader,
            writer = writer,
            config = config.copy(keyPrefix = "loaded-map-test:${randomName()}")
        )

    // =========================================================================
    // Read-through
    // =========================================================================

    @Test
    fun `get - 캐시 미스 시 loader를 통해 Read-through 후 Redis에 캐싱한다`() {
        val loaderCallCount = AtomicInteger(0)
        val loader =
            object: MapLoader<String, String> {
                override fun load(key: String): String {
                    loaderCallCount.incrementAndGet()
                    return "loaded-$key"
                }

                override fun loadAllKeys(): Iterable<String> = emptyList()
            }

        newMap(loader = loader).use { map ->
            val value = map["key1"]
            value shouldBeEqualTo "loaded-key1"
            loaderCallCount.get() shouldBeEqualTo 1

            // 두 번째 조회는 캐시 히트 → loader 미호출
            map["key1"] shouldBeEqualTo "loaded-key1"
            loaderCallCount.get() shouldBeEqualTo 1
        }
    }

    @Test
    fun `get - loader가 null이면 캐시 미스 시 null을 반환한다`() {
        newMap().use { map ->
            map["nonexistent"].shouldBeNull()
        }
    }

    @Test
    fun `get - loader가 null을 반환하면 Redis에 캐싱하지 않는다`() {
        val loader =
            object: MapLoader<String, String> {
                override fun load(key: String): String? = null

                override fun loadAllKeys(): Iterable<String> = emptyList()
            }

        newMap(loader = loader).use { map ->
            map["key1"].shouldBeNull()
            // 다시 조회해도 null (캐싱되지 않았으므로 loader 재호출)
            map["key1"].shouldBeNull()
        }
    }

    // =========================================================================
    // NONE mode
    // =========================================================================

    @Test
    fun `set - NONE 모드에서는 Redis에만 저장하고 writer는 호출하지 않는다`() {
        val writerCallCount = AtomicInteger(0)
        val writer =
            object: MapWriter<String, String> {
                override fun write(map: Map<String, String>) {
                    writerCallCount.incrementAndGet()
                }

                override fun delete(keys: Collection<String>) {
                    writerCallCount.incrementAndGet()
                }
            }

        newMap(writer = writer, config = LettuceCacheConfig.READ_ONLY).use { map ->
            map["k1"] = "v1"
            map["k1"] shouldBeEqualTo "v1"
            writerCallCount.get() shouldBeEqualTo 0
        }
    }

    // =========================================================================
    // Write-through
    // =========================================================================

    @Test
    fun `set - WRITE_THROUGH 모드에서 writer와 Redis를 모두 갱신한다`() {
        val written = mutableMapOf<String, String>()
        val writer =
            object: MapWriter<String, String> {
                override fun write(map: Map<String, String>) {
                    written.putAll(map)
                }

                override fun delete(keys: Collection<String>) {
                    keys.forEach { written.remove(it) }
                }
            }

        newMap(writer = writer, config = LettuceCacheConfig.READ_WRITE_THROUGH).use { map ->
            map["k1"] = "v1"

            written["k1"] shouldBeEqualTo "v1"
            map["k1"] shouldBeEqualTo "v1"
        }
    }

    @Test
    fun `set - WRITE_THROUGH 모드에서 writer 실패 시 Redis는 갱신되지 않는다`() {
        val writer =
            object: MapWriter<String, String> {
                override fun write(map: Map<String, String>) {
                    error("write failure")
                }

                override fun delete(keys: Collection<String>) = Unit
            }

        newMap(writer = writer, config = LettuceCacheConfig.READ_WRITE_THROUGH).use { map ->
            val ex = runCatching { map["k1"] = "v1" }.exceptionOrNull()
            ex.shouldNotBeNull()
            ex.message shouldBeEqualTo "write failure"
            map["k1"].shouldBeNull()
        }
    }

    @Test
    fun `delete - WRITE_THROUGH 모드에서 writer delete와 Redis 삭제를 모두 수행한다`() {
        val deleted = mutableListOf<String>()
        val written = mutableMapOf<String, String>()
        val writer =
            object: MapWriter<String, String> {
                override fun write(map: Map<String, String>) {
                    written.putAll(map)
                }

                override fun delete(keys: Collection<String>) {
                    deleted.addAll(keys)
                }
            }

        newMap(writer = writer, config = LettuceCacheConfig.READ_WRITE_THROUGH).use { map ->
            map["k1"] = "v1"
            map.delete("k1")

            deleted.contains("k1") shouldBeEqualTo true
            map["k1"].shouldBeNull()
        }
    }

    // =========================================================================
    // Write-behind
    // =========================================================================

    @Test
    fun `set - WRITE_BEHIND 모드에서 Redis를 즉시 갱신하고 writer를 비동기로 호출한다`() {
        val writerCallCount = AtomicInteger(0)
        val writer =
            object: MapWriter<String, String> {
                override fun write(map: Map<String, String>) {
                    writerCallCount.incrementAndGet()
                }

                override fun delete(keys: Collection<String>) = Unit
            }

        val config =
            LettuceCacheConfig.WRITE_BEHIND.copy(
                writeBehindDelay = java.time.Duration.ofMillis(200)
            )

        newMap(writer = writer, config = config).use { map ->
            map["k1"] = "v1"

            // Redis는 즉시 갱신
            map["k1"] shouldBeEqualTo "v1"

            // writer는 비동기로 호출됨 — 최대 3초 대기
            val deadline = System.currentTimeMillis() + 3000L
            while (writerCallCount.get() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50L)
            }
            writerCallCount.get() shouldBeEqualTo 1
        }
    }

    // =========================================================================
    // getAll
    // =========================================================================

    @Test
    fun `getAll - 일부 캐시 미스 키는 loader로 Read-through한다`() {
        val loaderCallCount = AtomicInteger(0)
        val loader =
            object: MapLoader<String, String> {
                override fun load(key: String): String {
                    loaderCallCount.incrementAndGet()
                    return "from-db-$key"
                }

                override fun loadAllKeys(): Iterable<String> = emptyList()
            }

        newMap(loader = loader).use { map ->
            // k1은 미리 캐싱
            map["k1"] = "cached-v1"

            val result = map.getAll(setOf("k1", "k2", "k3"))
            result["k1"] shouldBeEqualTo "cached-v1"
            result["k2"] shouldBeEqualTo "from-db-k2"
            result["k3"] shouldBeEqualTo "from-db-k3"
            loaderCallCount.get() shouldBeEqualTo 2
        }
    }

    @Test
    fun `getAll - 빈 키 집합은 빈 맵을 반환한다`() {
        newMap().use { map ->
            map.getAll(emptySet()) shouldBeEqualTo emptyMap()
        }
    }

    // =========================================================================
    // deleteAll / clear
    // =========================================================================

    @Test
    fun `deleteAll - 여러 키를 한번에 삭제한다`() {
        val config = LettuceCacheConfig.READ_ONLY

        newMap(config = config).use { map ->
            map["k1"] = "v1"
            map["k2"] = "v2"
            map["k3"] = "v3"

            map.deleteAll(listOf("k1", "k2"))

            map["k1"].shouldBeNull()
            map["k2"].shouldBeNull()
            map["k3"] shouldBeEqualTo "v3"
        }
    }

    @Test
    fun `clear - keyPrefix에 해당하는 모든 Redis 키를 삭제한다`() {
        val config = LettuceCacheConfig.READ_ONLY

        newMap(config = config).use { map ->
            map["k1"] = "v1"
            map["k2"] = "v2"

            map.clear()

            map["k1"].shouldBeNull()
            map["k2"].shouldBeNull()
        }
    }
}
