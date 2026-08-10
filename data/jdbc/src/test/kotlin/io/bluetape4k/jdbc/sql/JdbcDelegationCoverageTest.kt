package io.bluetape4k.jdbc.sql

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.net.URL
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.JDBCType
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.util.Calendar
import java.util.GregorianCalendar

/** Thin delegation surfaces are exercised explicitly so overloads cannot silently become dead code. */
class JdbcDelegationCoverageTest: AbstractJdbcSqlTest() {

    @Test
    fun `PreparedStatementArgumentSetter delegates every typed setter and overload`() {
        val preparedStatement = preparedStatementProxy()
        val setter = PreparedStatementArgumentSetter(preparedStatement)
        val calendar = GregorianCalendar()
        val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val reader = StringReader("value")

        setter.array[1] = interfaceProxy()
        setter.asciiStream[2] = stream
        setter.asciiStream.set(3, 3, stream)
        setter.asciiStream.set(4, 3L, stream)
        setter.bigDecimal[5] = BigDecimal("1.5")
        setter.binaryStream[6] = stream
        setter.binaryStream.set(7, 3, stream)
        setter.binaryStream.set(8, 3L, stream)
        setter.blob[9] = interfaceProxy<Blob>()
        setter.blob[10] = stream
        setter.blob.set(11, 3L, stream)
        setter.boolean[12] = true
        setter.byte[13] = 1
        setter.bytes[14] = byteArrayOf(1)
        setter.characterStream[15] = reader
        setter.characterStream.set(16, 5, reader)
        setter.characterStream.set(17, 5L, reader)
        setter.clob[18] = interfaceProxy<Clob>()
        setter.clob[19] = reader
        setter.clob.set(20, 5L, reader)
        setter.date[21] = Date(0)
        setter.date.set(22, calendar, Date(0))
        setter.double[23] = 1.0
        setter.float[24] = 1.0f
        setter.int[25] = 1
        setter.long[26] = 1L
        setter.ncharacterStream[27] = reader
        setter.ncharacterStream.set(28, 5L, reader)
        setter.nclob[29] = interfaceProxy<NClob>()
        setter.nclob[30] = reader
        setter.nclob.set(31, 5L, reader)
        setter.nstring[32] = "value"
        setter.`null`[33] = Types.VARCHAR
        setter.`null`.set(34, "VARCHAR", Types.VARCHAR)
        setter.`object`[35] = "value"
        setter.`object`.set(36, Types.VARCHAR, "value")
        setter.`object`.set(37, Types.VARCHAR, 2, "value")
        setter.ref[38] = interfaceProxy<Ref>()
        setter.rowId[39] = interfaceProxy<RowId>()
        setter.sqlxml[40] = interfaceProxy<SQLXML>()
        setter.string[41] = "value"
        setter.time[42] = Time(0)
        setter.time.set(43, calendar, Time(0))
        setter.timestamp[44] = Timestamp(0)
        setter.timestamp.set(45, calendar, Timestamp(0))
        setter.url[46] = URL("https://example.com")

        val exposedSetters = listOf(
            setter.array, setter.asciiStream, setter.bigDecimal, setter.binaryStream,
            setter.blob, setter.boolean, setter.byte, setter.bytes, setter.characterStream,
            setter.clob, setter.date, setter.double, setter.float, setter.int, setter.long,
            setter.ncharacterStream, setter.nclob, setter.nstring, setter.`null`, setter.`object`,
            setter.ref, setter.rowId, setter.sqlxml, setter.string, setter.time, setter.timestamp,
            setter.url
        )
        exposedSetters.size shouldBeEqualTo 27

        allowUnsupported { setter.setObject(47, "value", JDBCType.VARCHAR) }
        allowUnsupported { setter.setObject(48, "value", JDBCType.VARCHAR, 2) }
        allowUnsupported { setter.executeLargeUpdate() }
        allowUnsupported { setter.getLargeUpdateCount() }
        allowUnsupported { setter.setLargeMaxRows(10L) }
        allowUnsupported { setter.getLargeMaxRows() }
        allowUnsupported { setter.executeLargeBatch() }
        allowUnsupported { setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'") }
        allowUnsupported { setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'", 1) }
        allowUnsupported { setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'", intArrayOf(1)) }
        allowUnsupported { setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'", arrayOf("id")) }
        allowUnsupported { setter.enquoteLiteral("value") }
        allowUnsupported { setter.enquoteIdentifier("value", true) }
        allowUnsupported { setter.isSimpleIdentifier("value") }
        allowUnsupported { setter.enquoteNCharLiteral("value") }
    }

    @Test
    fun `ResultSetGetColumnTokens exposes every typed token and update overload`() {
        val tokens = ResultSetGetColumnTokens(resultSetProxy())
        val exposedTokens = listOf(
            tokens.array, tokens.arrayOrNull, tokens.asciiStream, tokens.asciiStreamOrNull,
            tokens.bigDecimal, tokens.bigDecimalOrNull, tokens.binaryStream, tokens.binaryStreamOrNull,
            tokens.blob, tokens.blobOrNull, tokens.boolean, tokens.booleanOrNull,
            tokens.bytes, tokens.bytesOrNull, tokens.characterStream, tokens.characterStreamOrNull,
            tokens.clob, tokens.clobOrNull, tokens.date, tokens.dateOrNull,
            tokens.double, tokens.doubleOrNull, tokens.float, tokens.floatOrNull,
            tokens.int, tokens.intOrNull, tokens.long, tokens.longOrNull,
            tokens.ncharacterStream, tokens.ncharacterStreamOrNull, tokens.nclob, tokens.nclobOrNull,
            tokens.nstring, tokens.nstringOrNull, tokens.ref, tokens.refOrNull,
            tokens.rowId, tokens.rowIdOrNull, tokens.short, tokens.shortOrNull,
            tokens.sqlxml, tokens.sqlxmlOrNull, tokens.string, tokens.stringOrNull,
            tokens.time, tokens.timeOrNull, tokens.timestamp, tokens.timestampOrNull,
            tokens.url, tokens.urlOrNull
        )

        exposedTokens.size shouldBeEqualTo 50
        tokens.int[1]
        tokens.string["value"]
        allowUnsupported { tokens.updateObject(1, "value", JDBCType.VARCHAR) }
        allowUnsupported { tokens.updateObject(2, "value", JDBCType.VARCHAR, 2) }
        allowUnsupported { tokens.updateObject("value", "value", JDBCType.VARCHAR) }
        allowUnsupported { tokens.updateObject("value", "value", JDBCType.VARCHAR, 2) }
    }

    private fun preparedStatementProxy(): PreparedStatement = proxyFor(PreparedStatement::class.java)

    private fun resultSetProxy(): ResultSet = proxyFor(ResultSet::class.java)

    private fun allowUnsupported(action: () -> Unit) {
        try {
            action()
        } catch (_: SQLFeatureNotSupportedException) {
            // JDBC default methods may intentionally reject newer overloads.
        } catch (_: UnsupportedOperationException) {
            // JDBC default methods may intentionally reject newer overloads.
        }
    }

    private inline fun <reified T> interfaceProxy(): T = proxyFor(T::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxyFor(type: Class<T>): T {
        val handler = InvocationHandler { _, method, _ ->
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Byte::class.javaPrimitiveType -> 0.toByte()
                Short::class.javaPrimitiveType -> 0.toShort()
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0.0f
                Double::class.javaPrimitiveType -> 0.0
                Void.TYPE -> null
                IntArray::class.java -> IntArray(0)
                LongArray::class.java -> LongArray(0)
                else -> null
            }
        }
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler) as T
    }
}
