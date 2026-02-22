package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResultRowExtensionsTest: AbstractExposedTest() {

    object ResultRowExtTable: Table("result_row_ext_test") {
        val text = varchar("text", 64)
        val numberText = varchar("number_text", 32)
        val boolText = varchar("bool_text", 16)
        val uuidText = varchar("uuid_text", 36)
        val nullableText = varchar("nullable_text", 64).nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow 확장으로 문자열 기반 값을 다양한 타입으로 변환한다`(testDB: TestDB) {
        val uuid = UUID.randomUUID()

        withTables(testDB, ResultRowExtTable) {
            ResultRowExtTable.insert {
                it[text] = "hello"
                it[numberText] = "123"
                it[boolText] = "true"
                it[uuidText] = uuid.toString()
                it[nullableText] = null
            }

            val row = ResultRowExtTable.selectAll().single()

            row.getString(ResultRowExtTable.text) shouldBeEqualTo "hello"
            row.getInt(ResultRowExtTable.numberText) shouldBeEqualTo 123
            row.getLong(ResultRowExtTable.numberText) shouldBeEqualTo 123L
            row.getBigDecimal(ResultRowExtTable.numberText) shouldBeEqualTo BigDecimal("123")
            row.getBoolean(ResultRowExtTable.boolText).shouldBeTrue()
            row.getUuid(ResultRowExtTable.uuidText) shouldBeEqualTo uuid
            row.getByteArray(ResultRowExtTable.text).shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow null getter는 null을 반환하고 non-null getter는 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, ResultRowExtTable) {
            ResultRowExtTable.insert {
                it[text] = "hello"
                it[numberText] = "123"
                it[boolText] = "true"
                it[uuidText] = UUID.randomUUID().toString()
                it[nullableText] = null
            }

            val row = ResultRowExtTable.selectAll().single()

            row.getStringOrNull(ResultRowExtTable.nullableText) shouldBeEqualTo null
            row.getIntOrNull(ResultRowExtTable.nullableText) shouldBeEqualTo null

            val ex = assertFailsWith<IllegalStateException> {
                row.getInt(ResultRowExtTable.nullableText)
            }
            assertTrue(ex.message?.contains("nullable_text") == true)
            assertTrue(ex.message?.contains("not convertible to Int") == true)
        }
    }
}
