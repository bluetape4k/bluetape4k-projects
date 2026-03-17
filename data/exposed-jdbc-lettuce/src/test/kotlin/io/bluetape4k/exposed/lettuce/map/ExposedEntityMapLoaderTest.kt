package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [ExposedEntityMapLoader] 단위 테스트.
 */
class ExposedEntityMapLoaderTest : AbstractExposedTest() {
    companion object : KLogging()

    private data class LoaderEntity(
        override val id: Long,
        val name: String,
    ) : HasIdentifier<Long>

    private object LoaderTable : LongIdTable("lettuce_loader_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toLoaderEntity(): LoaderEntity =
        LoaderEntity(
            id = this[LoaderTable.id].value,
            name = this[LoaderTable.name]
        )

    @Test
    fun `load - 단건 조회 성공`() {
        withTables(TestDB.H2, LoaderTable) {
            val insertedId =
                LoaderTable.insert {
                    it[name] = "alice"
                } get LoaderTable.id

            val loader =
                ExposedEntityMapLoader(
                    table = LoaderTable,
                    toEntity = { row -> row.toLoaderEntity() }
                )

            val entity = loader.load(insertedId.value)
            entity.shouldNotBeNull()
            entity.name shouldBeEqualTo "alice"
        }
    }

    @Test
    fun `load - 존재하지 않는 ID는 null을 반환한다`() {
        withTables(TestDB.H2, LoaderTable) {
            val loader =
                ExposedEntityMapLoader(
                    table = LoaderTable,
                    toEntity = { row -> row.toLoaderEntity() }
                )

            loader.load(Long.MIN_VALUE).shouldBeNull()
        }
    }

    @Test
    fun `loadAllKeys - 빈 테이블은 빈 컬렉션을 반환한다`() {
        withTables(TestDB.H2, LoaderTable) {
            val loader =
                ExposedEntityMapLoader(
                    table = LoaderTable,
                    toEntity = { row -> row.toLoaderEntity() }
                )

            loader.loadAllKeys().toList().shouldBeEmpty()
        }
    }

    @Test
    fun `loadAllKeys - 배치 경계를 넘어 모든 ID를 로드한다`() {
        withTables(TestDB.H2, LoaderTable) {
            repeat(5) { index ->
                LoaderTable.insert { it[name] = "user-$index" }
            }

            val loader =
                ExposedEntityMapLoader(
                    table = LoaderTable,
                    batchSize = 2,
                    toEntity = { row -> row.toLoaderEntity() }
                )

            val ids = loader.loadAllKeys().toList()
            ids.size shouldBeEqualTo 5
        }
    }

    @Test
    fun `batchSize는 0보다 커야 한다`() {
        withTables(TestDB.H2, LoaderTable) {
            assertFailsWith<IllegalArgumentException> {
                ExposedEntityMapLoader(
                    table = LoaderTable,
                    batchSize = 0,
                    toEntity = { row -> row.toLoaderEntity() }
                )
            }
        }
    }
}
