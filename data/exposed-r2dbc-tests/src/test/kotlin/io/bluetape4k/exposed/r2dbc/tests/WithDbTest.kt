package io.bluetape4k.exposed.r2dbc.tests

import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.exists
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class WithDbTest: AbstractExposedR2dbcTest() {

    object SampleTable: IntIdTable("with_db_sample_table") {
        val name = varchar("name", 64)
    }

    /**
     * withDb 는 호출 후 TestDB.db 에 연결 인스턴스를 설정합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withDb 는 TestDB db 필드를 초기화한다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            testDB.db.shouldNotBeNull()
        }
    }

    /**
     * withDb 블록 내부에서 currentTestDB 가 올바르게 설정됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withDb 블록 내부에서 currentTestDB 가 설정된다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            currentTestDB shouldBeEqualTo testDB
        }
    }

    /**
     * withDb 는 중복 호출 시에도 동일한 DB 인스턴스를 재사용합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withDb 는 동일한 TestDB 에 대해 db 인스턴스를 재사용한다`(testDB: TestDB) = runTest {
        withDb(testDB) { /* 첫 번째 연결 초기화 */ }
        val firstDb = testDB.db

        withDb(testDB) { /* 두 번째 호출 */ }
        val secondDb = testDB.db

        firstDb shouldBeEqualTo secondDb
    }

    /**
     * withTables 는 테이블을 생성하고 statement 종료 후 테이블을 삭제합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withTables 는 테이블을 생성하고 종료 시 삭제한다`(testDB: TestDB) = runTest {
        withTables(testDB, SampleTable) {
            SampleTable.exists().shouldBeTrue()

            SampleTable.insert { it[name] = "hello" }
            SampleTable.selectAll().count() shouldBeEqualTo 1L
        }

        // 테이블이 삭제되었는지 확인
        withDb(testDB) {
            SampleTable.exists().shouldBeFalse()
        }
    }

    /**
     * withTables 에 dropTables=false 를 지정하면 종료 후 테이블이 남아 있습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withTables 에 dropTables false 지정 시 테이블이 유지된다`(testDB: TestDB) = runTest {
        try {
            withTables(testDB, SampleTable, dropTables = false) {
                SampleTable.exists().shouldBeTrue()
            }

            withDb(testDB) {
                SampleTable.exists().shouldBeTrue()
            }
        } finally {
            // 정리
            withDb(testDB) {
                runCatching { SchemaUtils.drop(SampleTable) }
            }
        }
    }

    /**
     * withTables 블록에서 예외가 발생하면 호출자로 예외가 전파됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withTables 블록 예외는 호출자에게 전파된다`(testDB: TestDB) = runTest {
        assertFailsWith<IllegalStateException> {
            withTables(testDB, SampleTable) {
                throw IllegalStateException("forced failure")
            }
        }
    }

    /**
     * withDb 는 configure 람다를 전달받아 일시적으로 DB 설정을 변경할 수 있습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withDb 에 configure 를 전달하면 해당 호출에만 임시 적용된다`(testDB: TestDB) = runTest {
        // 먼저 DB를 초기화
        withDb(testDB) { /* init */ }
        val originalDb = testDB.db

        withDb(testDB, configure = { /* no-op additional config */ }) {
            testDB.db.shouldNotBeNull()
        }

        // 이전 db 인스턴스로 복원됨
        testDB.db shouldBeEqualTo originalDb
    }
}
