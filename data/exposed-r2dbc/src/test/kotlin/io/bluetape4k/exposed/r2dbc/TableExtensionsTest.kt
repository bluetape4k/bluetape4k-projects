package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldContainIgnoringCase
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [suspendColumnMetadata], [suspendIndexes], [suspendPrimaryKeyMetadata], [suspendSequences] 통합 테스트입니다.
 *
 * 각 suspend 확장 함수가 올바른 메타데이터를 반환하는지 검증합니다.
 */
class TableExtensionsTest : AbstractExposedR2dbcTest() {

    companion object : KLoggingChannel()

    private val tester = object : IntIdTable("r2dbc_table_ext_tester") {
        val name = varchar("name", 255)
        val price = integer("price")

        init {
            index("r2dbc_tester_by_name", isUnique = false, name)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendColumnMetadata 는 테이블의 컬럼 메타데이터를 반환한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, tester) {
            val metadatas = tester.suspendColumnMetadata()
            metadatas.forEach {
                log.debug { "columnMetadata=$it" }
            }
            metadatas.shouldNotBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendIndexes 는 테이블의 인덱스 목록을 반환한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, tester) {
            val indices = tester.suspendIndexes()
            indices.forEach {
                log.debug { "index=$it" }
            }
            // r2dbc_tester_by_name 비유니크 인덱스 1개가 반환된다 (PK는 별도)
            indices shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendPrimaryKeyMetadata 는 기본키 메타데이터를 반환한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, tester) {
            val pk = tester.suspendPrimaryKeyMetadata()
            log.debug { "primaryKey=$pk" }
            pk.shouldNotBeNull()
            pk.columnNames shouldContainIgnoringCase "ID"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendSequences 는 PostgreSQL 에서 시퀀스 목록을 반환한다`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES_LIKE }

        val identityTable = object : IntIdTable("r2dbc_identity_seq_table") {}

        withTables(testDB, identityTable) {
            val sequences = identityTable.suspendSequences()
            log.debug { "sequences=$sequences" }
            sequences.shouldNotBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendSequences 는 시퀀스가 없는 테이블에서 빈 리스트를 반환한다`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_POSTGRES_LIKE }

        withTables(testDB, tester) {
            val sequences = tester.suspendSequences()
            log.debug { "sequences=$sequences" }
            sequences.shouldBeEmpty()
        }
    }
}
