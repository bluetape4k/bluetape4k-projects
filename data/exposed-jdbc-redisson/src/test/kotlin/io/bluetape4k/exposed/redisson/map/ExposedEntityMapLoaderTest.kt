package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.test.assertFailsWith

class ExposedEntityMapLoaderTest: AbstractExposedTest() {

    private data class LoaderEntity(
        val id: Long,
        val name: String,
    ): Serializable

    private object LoaderTable: LongIdTable("redisson_loader_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toLoaderEntity(): LoaderEntity =
        LoaderEntity(
            id = this[LoaderTable.id].value,
            name = this[LoaderTable.name],
        )

    @Test
    fun `batch loader는 배치 경계를 넘어 모든 id를 로드한다`() {
        withTables(TestDB.H2, LoaderTable) {
            repeat(3) { index ->
                LoaderTable.insert {
                    it[name] = "user-$index"
                }
            }

            val loader = ExposedEntityMapLoader(
                entityTable = LoaderTable,
                batchSize = 2,
                toEntity = { toLoaderEntity() },
            )

            val ids = loader.loadAllKeys()!!.toList()
            ids.size shouldBeEqualTo 3
            ids shouldBeEqualTo ids.sorted()
        }
    }

    @Test
    fun `load - 단건 조회 성공`() {
        withTables(TestDB.H2, LoaderTable) {
            val insertedId = LoaderTable.insert {
                it[name] = "alice"
            } get LoaderTable.id

            val loader = ExposedEntityMapLoader(
                entityTable = LoaderTable,
                toEntity = { toLoaderEntity() },
            )

            val entity = loader.load(insertedId.value)
            entity.shouldNotBeNull()
            entity.name shouldBeEqualTo "alice"
        }
    }

    @Test
    fun `load - 존재하지 않는 ID는 null을 반환한다`() {
        withTables(TestDB.H2, LoaderTable) {
            val loader = ExposedEntityMapLoader(
                entityTable = LoaderTable,
                toEntity = { toLoaderEntity() },
            )

            loader.load(Long.MIN_VALUE).shouldBeNull()
        }
    }

    @Test
    fun `loadAllKeys - 빈 테이블은 빈 컬렉션을 반환한다`() {
        withTables(TestDB.H2, LoaderTable) {
            val loader = ExposedEntityMapLoader(
                entityTable = LoaderTable,
                toEntity = { toLoaderEntity() },
            )

            val ids = loader.loadAllKeys()!!.toList()
            ids.shouldBeEmpty()
        }
    }

    @Test
    fun `batchSize 는 0보다 커야 한다`() {
        withTables(TestDB.H2, LoaderTable) {
            assertFailsWith<IllegalArgumentException> {
                ExposedEntityMapLoader(
                    entityTable = LoaderTable,
                    batchSize = 0,
                    toEntity = { toLoaderEntity() },
                )
            }

            assertFailsWith<IllegalArgumentException> {
                SuspendedExposedEntityMapLoader(
                    entityTable = LoaderTable,
                    batchSize = 0,
                    toEntity = { toLoaderEntity() },
                )
            }
        }
    }

}
