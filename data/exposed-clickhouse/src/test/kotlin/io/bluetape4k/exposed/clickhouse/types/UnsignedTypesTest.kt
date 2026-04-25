package io.bluetape4k.exposed.clickhouse.types

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * ClickHouse Basic / Signed Int / Float / Unsigned 컬럼 타입의 단위 테스트.
 *
 * 실 DB 연결이 필요하지 않은 [ColumnType.sqlType] / [ColumnType.valueFromDB] /
 * [ColumnType.notNullValueToDB] 의 동작만을 검증합니다.
 */
class UnsignedTypesTest {

    // ────────────────────────────────────────────────────────────
    // sqlType()
    // ────────────────────────────────────────────────────────────

    @Test
    fun `String sqlType is String`() {
        ClickHouseStringColumnType().sqlType() shouldBeEqualTo "String"
    }

    @Test
    fun `FixedString sqlType is FixedString(n)`() {
        ClickHouseFixedStringColumnType(16).sqlType() shouldBeEqualTo "FixedString(16)"
        ClickHouseFixedStringColumnType(1).sqlType() shouldBeEqualTo "FixedString(1)"
    }

    @Test
    fun `Float32 sqlType is Float32`() {
        ClickHouseFloat32ColumnType().sqlType() shouldBeEqualTo "Float32"
    }

    @Test
    fun `Float64 sqlType is Float64`() {
        ClickHouseFloat64ColumnType().sqlType() shouldBeEqualTo "Float64"
    }

    @Test
    fun `Int8 sqlType is Int8`() {
        ClickHouseInt8ColumnType().sqlType() shouldBeEqualTo "Int8"
    }

    @Test
    fun `Int16 sqlType is Int16`() {
        ClickHouseInt16ColumnType().sqlType() shouldBeEqualTo "Int16"
    }

    @Test
    fun `Int32 sqlType is Int32`() {
        ClickHouseInt32ColumnType().sqlType() shouldBeEqualTo "Int32"
    }

    @Test
    fun `Int64 sqlType is Int64`() {
        ClickHouseInt64ColumnType().sqlType() shouldBeEqualTo "Int64"
    }

    @Test
    fun `UByte sqlType is UInt8`() {
        ClickHouseUByteColumnType().sqlType() shouldBeEqualTo "UInt8"
    }

    @Test
    fun `UShort sqlType is UInt16`() {
        ClickHouseUShortColumnType().sqlType() shouldBeEqualTo "UInt16"
    }

    @Test
    fun `UInt sqlType is UInt32`() {
        ClickHouseUIntColumnType().sqlType() shouldBeEqualTo "UInt32"
    }

    @Test
    fun `ULong sqlType is UInt64`() {
        ClickHouseULongColumnType().sqlType() shouldBeEqualTo "UInt64"
    }

    @Test
    fun `UInt64BigInt sqlType is UInt64`() {
        ClickHouseUInt64BigIntColumnType().sqlType() shouldBeEqualTo "UInt64"
    }

    @Test
    fun `Nullable wraps inner sqlType`() {
        ClickHouseNullableColumnType(ClickHouseInt32ColumnType()).sqlType() shouldBeEqualTo "Nullable(Int32)"
        ClickHouseNullableColumnType(ClickHouseStringColumnType()).sqlType() shouldBeEqualTo "Nullable(String)"
        ClickHouseNullableColumnType(ClickHouseFixedStringColumnType(8)).sqlType() shouldBeEqualTo "Nullable(FixedString(8))"
    }

    // ────────────────────────────────────────────────────────────
    // valueFromDB() — cross-cast 방어 로직
    // ────────────────────────────────────────────────────────────

    @Test
    fun `UByte valueFromDB accepts Short`() {
        val col = ClickHouseUByteColumnType()
        col.valueFromDB(255.toShort()) shouldBeEqualTo 255.toUByte()
        col.valueFromDB(0.toShort()) shouldBeEqualTo 0.toUByte()
    }

    @Test
    fun `UShort valueFromDB accepts Int`() {
        val col = ClickHouseUShortColumnType()
        col.valueFromDB(65535) shouldBeEqualTo 65535.toUShort()
        col.valueFromDB(0) shouldBeEqualTo 0.toUShort()
    }

    @Test
    fun `UInt valueFromDB accepts Long`() {
        val col = ClickHouseUIntColumnType()
        col.valueFromDB(4_294_967_295L) shouldBeEqualTo 4_294_967_295L.toUInt()
        col.valueFromDB(0L) shouldBeEqualTo 0u
    }

    @Test
    fun `ULong valueFromDB accepts BigInteger`() {
        val col = ClickHouseULongColumnType()
        col.valueFromDB(java.math.BigInteger.valueOf(123L)) shouldBeEqualTo 123uL
    }

    @Test
    fun `UInt64BigInt valueFromDB accepts Long`() {
        val col = ClickHouseUInt64BigIntColumnType()
        col.valueFromDB(123L) shouldBeEqualTo java.math.BigInteger.valueOf(123L)
    }

    @Test
    fun `Int32 valueFromDB accepts Long`() {
        val col = ClickHouseInt32ColumnType()
        col.valueFromDB(42L) shouldBeEqualTo 42
    }

    @Test
    fun `Float32 valueFromDB accepts Double`() {
        val col = ClickHouseFloat32ColumnType()
        col.valueFromDB(1.5) shouldBeEqualTo 1.5f
    }

    @Test
    fun `Float64 valueFromDB accepts Float`() {
        val col = ClickHouseFloat64ColumnType()
        col.valueFromDB(1.5f) shouldBeEqualTo 1.5
    }

    @Test
    fun `String valueFromDB falls back to toString`() {
        val col = ClickHouseStringColumnType()
        col.valueFromDB(123) shouldBeEqualTo "123"
        col.valueFromDB("abc") shouldBeEqualTo "abc"
    }

    // ────────────────────────────────────────────────────────────
    // notNullValueToDB() — JDBC 변환
    // ────────────────────────────────────────────────────────────

    @Test
    fun `UByte notNullValueToDB returns Short`() {
        val col = ClickHouseUByteColumnType()
        col.notNullValueToDB(200.toUByte()) shouldBeEqualTo 200.toShort()
    }

    @Test
    fun `UShort notNullValueToDB returns Int`() {
        val col = ClickHouseUShortColumnType()
        col.notNullValueToDB(60000.toUShort()) shouldBeEqualTo 60000
    }

    @Test
    fun `UInt notNullValueToDB returns Long`() {
        val col = ClickHouseUIntColumnType()
        col.notNullValueToDB(4_000_000_000u) shouldBeEqualTo 4_000_000_000L
    }

    @Test
    fun `ULong notNullValueToDB returns Long`() {
        val col = ClickHouseULongColumnType()
        col.notNullValueToDB(123uL) shouldBeEqualTo 123L
    }
}
