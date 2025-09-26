package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.R2dbcExposedTestBase
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.statements.api.R2dbcResult
import org.jetbrains.exposed.v1.r2dbc.statements.api.origin
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
class ImplicitSelectAllTest: R2dbcExposedTestBase() {

    companion object: KLoggingChannel()

    // 컬럼 주석을 지원하는 DB - H2, MySQL 8, NOT Postgres
    val columnCommentSupportedDB = TestDB.ALL_H2 + TestDB.MYSQL_V8


    /**
     * 묵시적으로 컬럼 전체를 뜻하는 `*` 를 사용하면, INVISIBLE 컬럼은 반환되지 않습니다.
     *
     * ```sql
     * -- MySQL 8
     * CREATE TABLE IF NOT EXISTS tester (
     *      amount INT NOT NULL,
     *      active BOOLEAN INVISIBLE NULL   -- INVISIBLE 컬럼
     * )
     * ```
     *
     * ```sql
     * -- MySQL 8
     * SELECT *                    -- active 는 INVISIBLE 컬럼이므로 반환되지 않음
     *   FROM TESTER
     *  WHERE TESTER.AMOUNT > 100
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼의 visibility를 변경할 수 있다`(testDB: TestDB) = runTest {
        Assumptions.assumeTrue { testDB in columnCommentSupportedDB }

        val tester = object: Table("tester") {
            val amount = integer("amount")
            val active = bool("active")
                .nullable()
                .withDefinition("INVISIBLE")  // Implicit 조회 시 (select * from tester), 컬럼을 숨김
        }

        withTables(testDB, tester) {
            if (testDB == TestDB.MYSQL_V8) {
                SchemaUtils.statementsRequiredToActualizeScheme(tester).shouldBeEmpty()
            }

            val ddl = tester.ddl.single()
            log.info { "tester ddl: $ddl" }

            tester.insert {
                it[amount] = 999
                it[active] = true
            }

            /**
             * 묵시적으로 컬럼 전체를 뜻하는 `*` 를 사용하면, INVISIBLE 컬럼은 반환되지 않습니다.
             *
             * ```sql
             * SELECT *         -- active 는 INVISIBLE 컬럼이므로 반환되지 않음
             *   FROM TESTER
             *  WHERE TESTER.AMOUNT > 100
             * ```
             */
            // HINT: 이렇게 Statement.execute(R2dbcTransaction) 을 수행하면 org.jetbrains.exposed.v1.r2dbc.statements.api.R2dbcResult 를 반환합니다.
            val result1: R2dbcResult = tester.selectImplicitAll()
                .where { tester.amount greater 100 }
                .execute(this) as R2dbcResult

            result1.mapRows { row ->
                row.origin.getInt(tester.amount.name) shouldBeEqualTo 999

                assertFailsWith<NoSuchElementException> {
                    row.origin.getBoolean(tester.active.name)
                }
            }.single()

            // 전체 컬럼 수와 INVISIBLE 컬럼을 제외한 컬럼 수가 달라서 [ResultRow] 로 변환하는데 문제가 생깁니다.
            // 그래서 위와 같이 execute() 를 사용하여 [R2dbcResult] 를 반환받아야 합니다.
//            tester.selectImplicitAll()
//                .where { tester.amount greater 100 }
//                .collect {
//                    it[tester.amount] shouldBeEqualTo 999
//                }

            assertFails {
                val row = tester.selectImplicitAll()
                    .where { tester.amount greater 100 }
                    .single()

                row[tester.amount] shouldBeEqualTo 999
                row.getOrNull(tester.active).shouldBeNull()   // INVISIBLE 컬럼은 반환되지 않음
            }

            /**
             * 명시적으로 INVISIBLE 컬럼을 지정해야만 반환됩니다.
             *
             * ```sql
             * SELECT TESTER.AMOUNT,
             *        TESTER.ACTIVE
             *   FROM TESTER
             *  WHERE TESTER.AMOUNT > 100
             * ```
             */
            // HINT: 이렇게 Statement.execute(R2dbcTransaction) 을 수행하면 org.jetbrains.exposed.v1.r2dbc.statements.api.R2dbcResult 를 반환합니다.
            val result2 = tester.selectAll()
                .where { tester.amount greater 100 }
                .execute(this) as R2dbcResult

            result2
                .mapRows { row ->
                    row.origin.getInt(tester.amount.name) shouldBeEqualTo 999
                    row.origin.getBoolean(tester.active.name).shouldBeTrue()
                }
                .single()

            tester.selectAll()
                .where { tester.amount greater 100 }
                .collect {
                    it[tester.amount] shouldBeEqualTo 999
                    it[tester.active].shouldNotBeNull().shouldBeTrue()
                }
        }
    }
}
