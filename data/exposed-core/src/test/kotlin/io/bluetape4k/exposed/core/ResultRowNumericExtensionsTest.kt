package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFailsWith

/**
 * [ResultRow] 확장 함수 중 숫자/바이너리 타입 변환 함수에 대한 단위 테스트.
 *
 * `getByte`, `getShort`, `getFloat`, `getDouble`, `getBigInt`, `getBigDecimal`,
 * `getByteArray`, `getChar` 등의 변환 함수의 정상 동작과 null/예외 처리를 검증한다.
 */
class ResultRowNumericExtensionsTest: AbstractExposedTest() {

    companion object: KLogging()

    object NumericTable: Table("result_row_numeric_test") {
        val byteVal = varchar("byte_val", 8)
        val shortVal = varchar("short_val", 8)
        val floatVal = varchar("float_val", 32)
        val doubleVal = varchar("double_val", 64)
        val bigIntVal = varchar("bigint_val", 64)
        val bigDecimalVal = varchar("bigdecimal_val", 64)
        val charVal = varchar("char_val", 4)
        val nullableVal = varchar("nullable_val", 64).nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getByte와 getByteOrNull은 Byte 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "42"
                it[shortVal] = "100"
                it[floatVal] = "3.14"
                it[doubleVal] = "2.71828"
                it[bigIntVal] = "999999999999"
                it[bigDecimalVal] = "12345.6789"
                it[charVal] = "A"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getByte(NumericTable.byteVal) shouldBeEqualTo 42.toByte()
            row.getByteOrNull(NumericTable.byteVal) shouldBeEqualTo 42.toByte()
            row.getByteOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getShort와 getShortOrNull은 Short 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "32000"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "B"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getShort(NumericTable.shortVal) shouldBeEqualTo 32000.toShort()
            row.getShortOrNull(NumericTable.shortVal) shouldBeEqualTo 32000.toShort()
            row.getShortOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getFloat와 getFloatOrNull은 Float 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "3.14"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "C"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getFloat(NumericTable.floatVal).shouldNotBeNull()
            row.getFloatOrNull(NumericTable.floatVal).shouldNotBeNull()
            row.getFloatOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getDouble와 getDoubleOrNull은 Double 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "2.71828"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "D"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getDouble(NumericTable.doubleVal).shouldNotBeNull()
            row.getDoubleOrNull(NumericTable.doubleVal).shouldNotBeNull()
            row.getDoubleOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getBigInt와 getBigIntOrNull은 BigInteger 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "999999999999"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "E"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getBigInt(NumericTable.bigIntVal) shouldBeEqualTo BigInteger("999999999999")
            row.getBigIntOrNull(NumericTable.bigIntVal) shouldBeEqualTo BigInteger("999999999999")
            row.getBigIntOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getBigDecimal와 getBigDecimalOrNull은 BigDecimal 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "12345.6789"
                it[charVal] = "F"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getBigDecimal(NumericTable.bigDecimalVal) shouldBeEqualTo BigDecimal("12345.6789")
            row.getBigDecimalOrNull(NumericTable.bigDecimalVal) shouldBeEqualTo BigDecimal("12345.6789")
            row.getBigDecimalOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getChar와 getCharOrNull은 Char 값을 올바르게 변환한다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "Z"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            row.getChar(NumericTable.charVal) shouldBeEqualTo 'Z'
            row.getCharOrNull(NumericTable.charVal) shouldBeEqualTo 'Z'
            row.getCharOrNull(NumericTable.nullableVal).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getByte는 null 값에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "A"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getByte(NumericTable.nullableVal)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getShort는 null 값에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "A"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getShort(NumericTable.nullableVal)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getChar는 null 값에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "A"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getChar(NumericTable.nullableVal)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getFloat는 null 값에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "A"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getFloat(NumericTable.nullableVal)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getDouble는 null 값에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, NumericTable) {
            NumericTable.insert {
                it[byteVal] = "1"
                it[shortVal] = "1"
                it[floatVal] = "1.0"
                it[doubleVal] = "1.0"
                it[bigIntVal] = "1"
                it[bigDecimalVal] = "1.0"
                it[charVal] = "A"
                it[nullableVal] = null
            }

            val row = NumericTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getDouble(NumericTable.nullableVal)
            }
        }
    }
}
