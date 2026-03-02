package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Schema
import java.util.*

/**
 * Exposed R2DBC 테스트 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - 테스트 시작 시 기본 타임존을 UTC로 고정합니다.
 * - companion에서 `faker`, `enableDialects`, `ENABLE_DIALECTS_METHOD`를 공용으로 제공합니다.
 * - `prepareSchemaForTest`는 테스트 스키마 생성에 사용하는 고정 옵션 스키마를 반환합니다.
 *
 * ```kotlin
 * class MyTest: AbstractExposedR2dbcTest() {
 *     @ParameterizedTest
 *     @MethodSource(ENABLE_DIALECTS_METHOD)
 *     fun run(testDB: TestDB) = runTest { withDb(testDB) { } }
 * }
 * // 활성 dialect 기준으로 테스트 실행
 * ```
 */
abstract class AbstractExposedR2dbcTest {

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        fun enableDialects() = TestDB.enabledDialects()

        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }

    /**
     * 현재 dialect가 `IF NOT EXISTS`를 지원하면 SQL 조각을 반환합니다.
     */
    fun addIfNotExistsIfSupported() = if (currentDialectTest.supportsIfNotExists) {
        "IF NOT EXISTS "
    } else {
        ""
    }

    /**
     * 테스트용 스키마 객체를 생성합니다.
     */
    protected fun prepareSchemaForTest(schemaName: String): Schema = Schema(
        schemaName,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )

}
