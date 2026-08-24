package io.bluetape4k.jdbc.sql

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.net.URI
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
    fun `PreparedStatementArgumentSetter forwards exact delegated arguments`() {
        val recording = RecordingJdbcInvocationHandler()
        val setter = PreparedStatementArgumentSetter(recording.proxy(PreparedStatement::class.java))
        val calendar = GregorianCalendar()

        setter.string[1] = "value"
        setter.int[2] = 42
        setter.`object`.set(3, Types.VARCHAR, 2, "scaled")
        setter.date.set(4, calendar, Date(0))
        setter.`object`.set(5, Types.VARCHAR, "typed")

        recording.calls.single { it.method == "setString" }.args shouldBeEqualTo listOf(1, "value")
        recording.calls.single { it.method == "setInt" }.args shouldBeEqualTo listOf(2, 42)
        recording.calls.single { it.method == "setObject" && it.args.size == 4 }.args shouldBeEqualTo
            listOf(3, "scaled", Types.VARCHAR, 2)
        recording.calls.single { it.method == "setDate" }.args shouldBeEqualTo
            listOf(4, Date(0), calendar)
        recording.calls.single { it.method == "setObject" && it.args.size == 3 }.args shouldBeEqualTo
            listOf(5, "typed", Types.VARCHAR)
    }

    @Test
    fun `ResultSetGetColumnTokens returns delegated values and records column access`() {
        val recording = RecordingJdbcInvocationHandler()
        val tokens = ResultSetGetColumnTokens(recording.proxy(ResultSet::class.java))

        tokens.int[1] shouldBeEqualTo 42
        tokens.string["name"] shouldBeEqualTo "result"
        tokens.intOrNull[2] shouldBeEqualTo 42
        tokens.stringOrNull["name"] shouldBeEqualTo "result"

        recording.calls.map { it.method } shouldBeEqualTo
            listOf("getInt", "getString", "getInt", "wasNull", "getString", "wasNull")
        recording.calls[0].args shouldBeEqualTo listOf(1)
        recording.calls[1].args shouldBeEqualTo listOf("name")
    }

    @Test
    fun `PreparedStatementArgumentSetter records every typed setter and overload`() {
        val recording = RecordingJdbcInvocationHandler()
        val preparedStatement = recording.proxy(PreparedStatement::class.java)
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
        setter.url[46] = URI("https://example.com").toURL()

        val exposedSetters = listOf(
            setter.array, setter.asciiStream, setter.bigDecimal, setter.binaryStream,
            setter.blob, setter.boolean, setter.byte, setter.bytes, setter.characterStream,
            setter.clob, setter.date, setter.double, setter.float, setter.int, setter.long,
            setter.ncharacterStream, setter.nclob, setter.nstring, setter.`null`, setter.`object`,
            setter.ref, setter.rowId, setter.sqlxml, setter.string, setter.time, setter.timestamp,
            setter.url
        )
        exposedSetters.size shouldBeEqualTo 27

        recording.calls.map { it.method } shouldBeEqualTo EXPECTED_PREPARED_STATEMENT_CALLS

        allowKnownUnsupported("setObject") { setter.setObject(47, "value", JDBCType.VARCHAR) }
        allowKnownUnsupported("setObject") { setter.setObject(48, "value", JDBCType.VARCHAR, 2) }
        allowKnownUnsupported("executeLargeUpdate") { setter.executeLargeUpdate() }
        allowKnownUnsupported("getLargeUpdateCount") { setter.getLargeUpdateCount() }
        allowKnownUnsupported("setLargeMaxRows") { setter.setLargeMaxRows(10L) }
        assertKnownDefault(0L) { setter.getLargeMaxRows() }
        allowKnownUnsupported("executeLargeBatch") { setter.executeLargeBatch() }
        allowKnownUnsupported("executeLargeUpdate") {
            setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'")
        }
        allowKnownUnsupported("executeLargeUpdate") {
            setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'", 1)
        }
        allowKnownUnsupported("executeLargeUpdate") {
            setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'", intArrayOf(1))
        }
        allowKnownUnsupported("executeLargeUpdate") {
            setter.executeLargeUpdate("UPDATE Actors SET lastname='delegate'", arrayOf("id"))
        }
        assertKnownDefault("'value'") { setter.enquoteLiteral("value") }
        assertKnownDefault("\"value\"") { setter.enquoteIdentifier("value", true) }
        assertKnownDefault(true) { setter.isSimpleIdentifier("value") }
        assertKnownDefault("N'value'") { setter.enquoteNCharLiteral("value") }
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
        allowKnownUnsupported("updateObject") { tokens.updateObject(1, "value", JDBCType.VARCHAR) }
        allowKnownUnsupported("updateObject") { tokens.updateObject(2, "value", JDBCType.VARCHAR, 2) }
        allowKnownUnsupported("updateObject") { tokens.updateObject("value", "value", JDBCType.VARCHAR) }
        allowKnownUnsupported("updateObject") { tokens.updateObject("value", "value", JDBCType.VARCHAR, 2) }
    }

    private fun allowKnownUnsupported(methodName: String, action: () -> Unit) {
        try {
            action()
        } catch (e: SQLFeatureNotSupportedException) {
            e.message shouldBeEqualTo "$methodName not implemented"
            return
        } catch (e: UnsupportedOperationException) {
            e.message shouldBeEqualTo "$methodName not implemented"
            return
        }
        throw AssertionError("$methodName unexpectedly succeeded")
    }

    private fun <T> assertKnownDefault(expected: T, action: () -> T) {
        action() shouldBeEqualTo expected
    }

    private fun resultSetProxy(): ResultSet = proxyFor(ResultSet::class.java)

    private inline fun <reified T> interfaceProxy(): T = proxyFor(T::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxyFor(type: Class<T>): T = RecordingJdbcInvocationHandler().proxy(type)
}

private data class JdbcInvocation(
    val method: String,
    val args: List<Any?>,
)

private val EXPECTED_PREPARED_STATEMENT_CALLS = listOf(
    "setArray", "setAsciiStream", "setAsciiStream", "setAsciiStream", "setBigDecimal",
    "setBinaryStream", "setBinaryStream", "setBinaryStream", "setBlob", "setBlob", "setBlob",
    "setBoolean", "setByte", "setBytes", "setCharacterStream", "setCharacterStream",
    "setCharacterStream", "setClob", "setClob", "setClob", "setDate", "setDate", "setDouble",
    "setFloat", "setInt", "setLong", "setNCharacterStream", "setNCharacterStream", "setNClob",
    "setNClob", "setNClob", "setNString", "setNull", "setNull", "setObject", "setObject",
    "setObject", "setRef", "setRowId", "setSQLXML", "setString", "setTime", "setTime",
    "setTimestamp", "setTimestamp", "setURL"
)

private class RecordingJdbcInvocationHandler: InvocationHandler {

    val calls = mutableListOf<JdbcInvocation>()

    @Suppress("UNCHECKED_CAST")
    fun <T> proxy(type: Class<T>): T =
        Proxy.newProxyInstance(type.classLoader, arrayOf(type), this) as T

    override fun invoke(proxy: Any, method: Method, args: kotlin.Array<out Any?>?): Any? {
        val arguments = args?.toList().orEmpty()
        calls += JdbcInvocation(method.name, arguments)

        return when (method.name) {
            "getInt" -> 42
            "getString" -> "result"
            "wasNull" -> false
            "toString" -> "RecordingJdbcInvocationHandler"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments.firstOrNull()
            else -> {
                check(method.returnType == Void.TYPE) {
                    "Unexpected delegated method ${method.name}"
                }
                null
            }
        }
    }
}
