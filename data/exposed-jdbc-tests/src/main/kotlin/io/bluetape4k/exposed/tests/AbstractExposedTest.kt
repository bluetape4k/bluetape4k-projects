package io.bluetape4k.exposed.tests

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import java.util.*

/**
 * Exposed JDBC 테스트 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - 테스트 시작 시 기본 타임존을 UTC로 고정합니다.
 * - companion에서 `faker`, `enableDialects`, `ENABLE_DIALECTS_METHOD`를 공용으로 제공합니다.
 * - `prepareSchemaForTest`는 Oracle 스타일 tablespace 옵션이 포함된 [Schema]를 생성합니다.
 *
 * ```kotlin
 * class MyTest: AbstractExposedTest() {
 *     @ParameterizedTest
 *     @MethodSource(ENABLE_DIALECTS_METHOD)
 *     fun run(testDB: TestDB) = withDb(testDB) { }
 * }
 * // testDB 파라미터로 활성 dialect를 순회한다.
 * ```
 */
abstract class AbstractExposedTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        fun enableDialects() = TestDB.enabledDialects()

        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private object CurrentTestDBInterceptor: StatementInterceptor {
        override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
            return userData.filterValues { it is TestDB }
        }
    }

    /**
     * 현재 dialect가 `IF NOT EXISTS`를 지원하면 해당 SQL 조각을 반환합니다.
     *
     * ## 동작/계약
     * - 지원 시 `"IF NOT EXISTS "`를, 미지원 시 빈 문자열을 반환합니다.
     * - 호출 시점의 [currentDialectTest] 상태에만 의존합니다.
     *
     * ```kotlin
     * val clause = addIfNotExistsIfSupported()
     * // clause == "IF NOT EXISTS " || clause == ""
     * ```
     */
    fun addIfNotExistsIfSupported() = if (currentDialectTest.supportsIfNotExists) {
        "IF NOT EXISTS "
    } else {
        ""
    }

    /**
     * 테스트용 스키마 객체를 생성합니다.
     *
     * ## 동작/계약
     * - 전달한 [schemaName]으로 [Schema]를 새로 생성합니다.
     * - 기본/임시 tablespace 및 quota 값은 고정값(`USERS`, `TEMP`, `20M`)을 사용합니다.
     *
     * ```kotlin
     * val schema = prepareSchemaForTest("test_schema")
     * // schema.name == "test_schema"
     * ```
     */
    protected fun prepareSchemaForTest(schemaName: String): Schema = Schema(
        schemaName,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )
}
