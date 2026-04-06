package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.r2dbc.insert
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertFailsWith

class R2dbcExposedEntityMapLoaderTest: AbstractExposedR2dbcTest() {

    private data class LoaderEntity(
        val id: Long,
        val name: String,
    ): Serializable

    private object LoaderTable: LongIdTable("r2dbc_redisson_loader_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toLoaderEntity(): LoaderEntity =
        LoaderEntity(
            id = this[LoaderTable.id].value,
            name = this[LoaderTable.name],
        )

    @Test
    fun `batch loader는 배치 경계를 넘어 모든 id를 로드한다`() = runSuspendIO {
        withTables(TestDB.H2, LoaderTable) {
            repeat(3) { index ->
                LoaderTable.insert {
                    it[name] = "user-$index"
                }
            }

            val loader = R2dbcExposedEntityMapLoader(
                entityTable = LoaderTable,
                batchSize = 2,
            ) {
                toLoaderEntity()
            }

            val ids = loader.loadAllKeys().toList()
            ids shouldHaveSize 3
            ids shouldBeEqualTo ids.sorted()
        }
    }

    @Test
    fun `batchSize 는 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcExposedEntityMapLoader(
                entityTable = LoaderTable,
                batchSize = 0,
                toEntity = { toLoaderEntity() },
            )
        }
    }
}
