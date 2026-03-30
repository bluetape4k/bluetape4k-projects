package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.exposed.r2dbc.lettuce.AbstractR2dbcLettuceTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [R2dbcExposedEntityMapLoader] 단위 테스트.
 *
 * R2DBC `suspendTransaction` 기반으로 동작하며, `runBlocking` 없이 코루틴 네이티브로 실행한다.
 */
class R2dbcExposedEntityMapLoaderTest: AbstractR2dbcLettuceTest() {
    companion object: KLoggingChannel()

    private data class LoaderEntity(
        val id: Long,
        val name: String,
    )

    private object LoaderTable: LongIdTable("r2dbc_lettuce_loader_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toLoaderEntity(): LoaderEntity =
        LoaderEntity(
            id = this[LoaderTable.id].value,
            name = this[LoaderTable.name]
        )

    @Test
    fun `load - 단건 조회 성공`() =
        runTest {
            withTables(TestDB.H2, LoaderTable) {
                val insertedId = LoaderTable.insertAndGetId { it[name] = "alice" }
                commit()

                val loader =
                    R2dbcExposedEntityMapLoader(
                        table = LoaderTable,
                        toEntity = { toLoaderEntity() }
                    )

                val entity = loader.load(insertedId.value)
                entity.shouldNotBeNull()
                entity.name shouldBeEqualTo "alice"
            }
        }

    @Test
    fun `load - 존재하지 않는 ID는 null을 반환한다`() =
        runTest {
            withTables(TestDB.H2, LoaderTable) {
                val loader =
                    R2dbcExposedEntityMapLoader(
                        table = LoaderTable,
                        toEntity = { toLoaderEntity() }
                    )

                loader.load(Long.MIN_VALUE).shouldBeNull()
            }
        }

    @Test
    fun `loadAllKeys - 빈 테이블은 빈 컬렉션을 반환한다`() =
        runTest {
            withTables(TestDB.H2, LoaderTable) {
                val loader =
                    R2dbcExposedEntityMapLoader(
                        table = LoaderTable,
                        toEntity = { toLoaderEntity() }
                    )

                loader.loadAllKeys().shouldBeEmpty()
            }
        }

    @Test
    fun `loadAllKeys - 배치 경계를 넘어 모든 ID를 로드한다`() =
        runTest {
            withTables(TestDB.H2, LoaderTable) {
                repeat(5) { index ->
                    LoaderTable.insertAndGetId { it[name] = "user-$index" }
                }
                commit()

                val loader =
                    R2dbcExposedEntityMapLoader(
                        table = LoaderTable,
                        batchSize = 2,
                        toEntity = { toLoaderEntity() }
                    )

                val ids = loader.loadAllKeys()
                ids.size shouldBeEqualTo 5
                ids shouldBeEqualTo ids.sorted()
            }
        }

    @Test
    fun `batchSize는 0보다 커야 한다`() =
        runTest {
            withTables(TestDB.H2, LoaderTable) {
                assertFailsWith<IllegalArgumentException> {
                    R2dbcExposedEntityMapLoader(
                        table = LoaderTable,
                        batchSize = 0,
                        toEntity = { toLoaderEntity() }
                    )
                }
            }
        }
}
