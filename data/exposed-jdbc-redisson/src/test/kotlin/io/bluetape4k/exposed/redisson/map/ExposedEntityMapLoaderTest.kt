package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ExposedEntityMapLoaderTest: AbstractExposedTest() {

    private data class LoaderEntity(
        override val id: Long,
        val name: String,
    ): HasIdentifier<Long>

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
