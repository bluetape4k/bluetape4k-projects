package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTablesSuspending
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Test
import java.io.Serializable
import io.bluetape4k.assertions.assertFailsWith

/**
 * [SuspendedExposedEntityMapLoader] 단위 테스트.
 *
 * [SuspendedEntityMapLoader.load]는 내부적으로 [suspendedTransactionAsync]를 열므로
 * 데이터가 커밋된 상태여야 새 트랜잭션에서 READ_COMMITTED 격리 수준으로 조회 가능하다.
 * 따라서 각 테스트에서 insert 후 `commit()`을 호출한다.
 */
class SuspendedExposedEntityMapLoaderTest: AbstractExposedTest() {
    companion object: KLogging()

    private data class SuspendedLoaderEntity(
        val id: Long,
        val name: String,
    ): Serializable

    private object SuspendedLoaderTable: LongIdTable("suspended_loader_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toSuspendedLoaderEntity(): SuspendedLoaderEntity =
        SuspendedLoaderEntity(
            id = this[SuspendedLoaderTable.id].value,
            name = this[SuspendedLoaderTable.name]
        )

    @Test
    fun `load - suspend 컨텍스트에서 단건 조회 성공`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedLoaderTable) {
            val insertedId =
                SuspendedLoaderTable.insert {
                    it[name] = "alice"
                } get SuspendedLoaderTable.id

            // suspendedTransactionAsync는 새 트랜잭션을 열므로 먼저 커밋해야 데이터가 보인다
            commit()

            val loader = SuspendedExposedEntityMapLoader(
                table = SuspendedLoaderTable,
                toEntity = { row -> row.toSuspendedLoaderEntity() }
            )

            val entity = loader.load(insertedId.value)
            entity.shouldNotBeNull()
            entity.name shouldBeEqualTo "alice"
        }
    }

    @Test
    fun `load - 존재하지 않는 ID는 null을 반환한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedLoaderTable) {
            val loader = SuspendedExposedEntityMapLoader(
                table = SuspendedLoaderTable,
                toEntity = { row -> row.toSuspendedLoaderEntity() }
            )

            loader.load(Long.MIN_VALUE).shouldBeNull()
        }
    }

    @Test
    fun `loadAllKeys - 빈 테이블은 빈 리스트를 반환한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedLoaderTable) {
            // 데이터 없이 바로 loadAllKeys 호출 — 빈 리스트 반환
            commit()

            val loader = SuspendedExposedEntityMapLoader(
                table = SuspendedLoaderTable,
                toEntity = { row -> row.toSuspendedLoaderEntity() }
            )

            loader.loadAllKeys().shouldBeEmpty()
        }
    }

    @Test
    fun `loadAllKeys - 배치 경계를 넘어 모든 ID를 로드한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedLoaderTable) {
            repeat(5) { index ->
                SuspendedLoaderTable.insert { it[name] = "user-$index" }
            }

            // suspendedTransactionAsync는 새 트랜잭션을 열므로 먼저 커밋해야 한다
            commit()

            // batchSize=2 로 설정하여 페이지 경계를 여러 번 넘도록 강제
            val loader = SuspendedExposedEntityMapLoader(
                table = SuspendedLoaderTable,
                batchSize = 2,
                toEntity = { row -> row.toSuspendedLoaderEntity() }
            )

            val ids = loader.loadAllKeys()
            ids.size shouldBeEqualTo 5
            ids shouldBeEqualTo ids.sorted()
        }
    }

    @Test
    fun `batchSize는 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            SuspendedExposedEntityMapLoader(
                table = SuspendedLoaderTable,
                batchSize = 0,
                toEntity = { row -> row.toSuspendedLoaderEntity() }
            )
        }
    }
}
