package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.channels.Channel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.r2dbc.insert
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.concurrent.CompletableFuture

/**
 * [R2dbcEntityMapLoader]의 단위 테스트입니다.
 *
 * - `load`: 정상 조회 및 없을 때 null 반환
 * - `loadAllKeys`: 채널에서 모든 ID 수집, 소진 후 `hasNext()` false 반환 (메모리 누수 방지)
 * - 빈 채널에서 `toList()`가 빈 결과 반환
 * - [AsyncIterator.toList] 확장 함수 정상 동작 확인
 */
class R2dbcEntityMapLoaderTest: AbstractExposedR2dbcTest() {

    private data class TestEntity(val id: Long, val name: String) : Serializable

    private object TestTable: LongIdTable("r2dbc_entity_map_loader_test") {
        val name = varchar("name", 64)
    }

    /**
     * load() 가 정상적으로 엔티티를 반환하는지 확인합니다.
     */
    @Test
    fun `load - ID로 엔티티를 정상 반환한다`() = runSuspendIO {
        withTables(TestDB.H2, TestTable) {
            TestTable.insert { it[name] = "entity-1" }

            val loader = R2dbcExposedEntityMapLoader(
                entityTable = TestTable,
            ) { TestEntity(this[TestTable.id].value, this[TestTable.name]) }

            val keys = loader.loadAllKeys().toList()
            val id = keys.first()

            val result = loader.load(id).toCompletableFuture().get()
            result shouldBeEqualTo TestEntity(id, "entity-1")
        }
    }

    /**
     * load() 가 존재하지 않는 ID에 대해 null을 반환하는지 확인합니다.
     */
    @Test
    fun `load - 존재하지 않는 ID면 null을 반환한다`() = runSuspendIO {
        withTables(TestDB.H2, TestTable) {
            val loader = R2dbcExposedEntityMapLoader(
                entityTable = TestTable,
            ) { TestEntity(this[TestTable.id].value, this[TestTable.name]) }

            val result = loader.load(Long.MIN_VALUE).toCompletableFuture().get()
            result.shouldBeNull()
        }
    }

    /**
     * loadAllKeys() 가 채널에서 소진 후 `hasNext()`가 false를 반환하고,
     * `pendingReceive`가 초기화되어 메모리 누수가 없는지 확인합니다.
     */
    @Test
    fun `loadAllKeys - 원소 소진 후 hasNext는 false를 반환한다`() = runSuspendIO {
        withTables(TestDB.H2, TestTable) {
            TestTable.insert { it[name] = "only-one" }

            val loader = R2dbcExposedEntityMapLoader(
                entityTable = TestTable,
            ) { TestEntity(this[TestTable.id].value, this[TestTable.name]) }

            val iterator = loader.loadAllKeys()

            iterator.hasNext().toCompletableFuture().get().shouldBeTrue()
            iterator.next().toCompletableFuture().get() // 유일한 원소 소비
            // 모두 소비했으므로 false 반환
            iterator.hasNext().toCompletableFuture().get().shouldBeFalse()
        }
    }

    /**
     * loadAllKeys() 가 빈 테이블에서 빈 리스트를 반환하는지 확인합니다.
     */
    @Test
    fun `loadAllKeys - 원소가 없으면 빈 목록을 반환한다`() = runSuspendIO {
        withTables(TestDB.H2, TestTable) {
            // 아무것도 insert하지 않음
            val loader = R2dbcExposedEntityMapLoader(
                entityTable = TestTable,
            ) { TestEntity(this[TestTable.id].value, this[TestTable.name]) }

            val ids = loader.loadAllKeys().toList()
            ids shouldBeEqualTo emptyList()
        }
    }

    /**
     * loadAllKeys() 가 채널에 전송된 모든 ID를 수집하는지 확인합니다.
     */
    @Test
    fun `loadAllKeys - 채널에 전송된 모든 ID를 수집한다`() = runSuspendIO {
        withTables(TestDB.H2, TestTable) {
            repeat(3) { i -> TestTable.insert { it[name] = "item-$i" } }

            val loader = R2dbcExposedEntityMapLoader(
                entityTable = TestTable,
            ) { TestEntity(this[TestTable.id].value, this[TestTable.name]) }

            val ids = loader.loadAllKeys().toList()
            ids.size shouldBeEqualTo 3
            ids shouldBeEqualTo ids.sorted()
        }
    }

    /**
     * [AsyncIterator.toList] 확장 함수가 CompletableFuture 기반 이터레이터를 정상 소비하는지 확인합니다.
     * 이 테스트는 DB 연결 없이 실행됩니다.
     */
    @Test
    fun `AsyncIterator toList - CompletableFuture 기반 이터레이터를 올바르게 소비한다`() = runSuspendIO {
        val items = listOf("a", "b", "c")
        val iter = items.iterator()
        var peek: String? = null

        val asyncIter = object : org.redisson.api.AsyncIterator<String> {
            override fun hasNext(): java.util.concurrent.CompletionStage<Boolean?> {
                if (peek != null) return CompletableFuture.completedFuture(true)
                return if (iter.hasNext()) {
                    peek = iter.next()
                    CompletableFuture.completedFuture(true)
                } else {
                    CompletableFuture.completedFuture(false)
                }
            }

            override fun next(): java.util.concurrent.CompletionStage<String> {
                val v = peek ?: throw NoSuchElementException()
                peek = null
                return CompletableFuture.completedFuture(v)
            }
        }

        asyncIter.toList() shouldBeEqualTo items
    }

    /**
     * loadAllKeys() 에서 R2dbcEntityMapLoader 에 직접 lambda를 전달했을 때
     * 채널 기반 이터레이터가 올바르게 동작하는지 확인합니다.
     * DB를 사용하지 않는 채널 로직만 검증합니다.
     */
    @Test
    fun `loadAllKeys - 직접 제공한 lambda 채널이 ID를 올바르게 전달한다`() = runSuspendIO {
        withTables(TestDB.H2, TestTable) {
            val expectedIds = (1L..4L).toList()

            // DB를 직접 쓰지 않고 loadAllIdsFromDB lambda에서 채널로 전달
            val loader = R2dbcEntityMapLoader<Long, TestEntity>(
                loadByIdFromDB = { id ->
                    // withTables 내부이므로 이미 DB 트랜잭션 컨텍스트가 없어도 여기선 단순 값 반환
                    // (suspendTransaction 호출 없이 람다 직접 값 반환은 R2dbcEntityMapLoader.load()가 감싸줌)
                    null // load() 테스트 용도가 아님
                },
                loadAllIdsFromDB = { channel: Channel<Long> ->
                    expectedIds.forEach { channel.send(it) }
                },
            )

            val ids = loader.loadAllKeys().toList()
            ids shouldBeEqualTo expectedIds
        }
    }
}
