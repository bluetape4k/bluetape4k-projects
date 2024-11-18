package io.bluetape4k.jdbc.sql

import io.bluetape4k.logging.KLogging
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.RowId
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp
import java.util.*

/**
 * [PreparedStatement]에 parameter를 설정하는 방식을 사용자가 쉽게 이해할 수 있도록 해주는 클래스입니다.
 *
 * ```
 * val ps = connection.preparedStatement("update test_bean set created_date=?")
 * ps.arguments {
 *     date[1] = Date()      // 첫번째 인자를 Date 형식으로 설정합니다. (setDate(value))
 *     string[2] = "Hello"   // 두번째 인자를 String 형식으로 설정합니다. (setString(value))
 *     long[3] = 100L        // 세번째 인자를 Long 형식으로 설정합니다. (setLong(value))
 *     timestamp[4] = Timestamp() // 네번째 인자를 Timestamp 형식으로 설정합니다. (setTimestamp(value))
 * }
 * ps.executeUpdate()
 * ```
 *
 * @property preparedStatement 인자를 설정할 [PreparedStatement] 인스턴스
 */
class PreparedStatementArgumentSetter private constructor(
    private val preparedStatement: PreparedStatement,
): PreparedStatement by preparedStatement {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(preparedStatement: PreparedStatement): PreparedStatementArgumentSetter {
            return PreparedStatementArgumentSetter(preparedStatement)
        }
    }

    val array: ArgumentSetter<Array> by lazy {
        DefaultArgumentSetter { index: Int, value: Array -> setArray(index, value) }
    }

    val asciiStream: ArgumentWithLengthSetter<InputStream> by lazy {
        ArgumentWithLengthSetter(
            { index: Int, stream: InputStream -> setAsciiStream(index, stream) },
            { index: Int, stream: InputStream, length -> setAsciiStream(index, stream, length) },
            { index: Int, stream: InputStream, length: Long -> setAsciiStream(index, stream, length) }
        )
    }

    val bigDecimal: ArgumentSetter<BigDecimal> by lazy {
        DefaultArgumentSetter { index: Int, value: BigDecimal -> setBigDecimal(index, value) }
    }

    val binaryStream: ArgumentWithLengthSetter<InputStream> by lazy {
        ArgumentWithLengthSetter(
            { index: Int, stream: InputStream -> setBinaryStream(index, stream) },
            { index: Int, stream: InputStream, length -> setBinaryStream(index, stream, length) },
            { index: Int, stream: InputStream, length: Long -> setBinaryStream(index, stream, length) }
        )
    }

    /**
     * ```
     * blob[2] = Blob()
     * blob[3] = InputStream
     * ```
     */
    val blob: BlobArgumentSetter by lazy {
        BlobArgumentSetter(
            { index: Int, blob: Blob -> setBlob(index, blob) },
            { index: Int, stream: InputStream -> setBlob(index, stream) },
            { index: Int, stream: InputStream, length: Long -> setBlob(index, stream, length) }
        )
    }

    val boolean: ArgumentSetter<Boolean> by lazy {
        DefaultArgumentSetter { index: Int, value: Boolean -> setBoolean(index, value) }
    }

    val byte: ArgumentSetter<Byte> by lazy {
        DefaultArgumentSetter { index: Int, value: Byte -> setByte(index, value) }
    }

    val bytes: ArgumentSetter<ByteArray> by lazy {
        DefaultArgumentSetter { index: Int, value: ByteArray -> setBytes(index, value) }
    }

    val characterStream: ArgumentWithLengthSetter<Reader> by lazy {
        ArgumentWithLengthSetter(
            { index: Int, reader: Reader -> setCharacterStream(index, reader) },
            { index: Int, reader: Reader, length: Int -> setCharacterStream(index, reader, length) },
            { index: Int, reader: Reader, length: Long -> setCharacterStream(index, reader, length) }
        )
    }

    val clob: ClobArgumentSetter by lazy {
        ClobArgumentSetter(
            { index: Int, clob: Clob -> setClob(index, clob) },
            { index: Int, reader: Reader -> setClob(index, reader) },
            { index: Int, reader: Reader, length: Long -> setClob(index, reader, length) }
        )
    }

    val date: CombinedArgumentSetter<Date, Calendar> by lazy {
        CombinedArgumentSetter(
            { index: Int, date: Date -> setDate(index, date) },
            { index: Int, date: Date, calendar: Calendar -> setDate(index, date, calendar) }
        )
    }

    val double: ArgumentSetter<Double> by lazy {
        DefaultArgumentSetter { index: Int, value: Double -> setDouble(index, value) }
    }

    val float: ArgumentSetter<Float> by lazy {
        DefaultArgumentSetter { index: Int, value: Float -> setFloat(index, value) }
    }

    val int: ArgumentSetter<Int> by lazy {
        DefaultArgumentSetter { index: Int, value: Int -> setInt(index, value) }
    }

    val long: ArgumentSetter<Long> by lazy {
        DefaultArgumentSetter { index: Int, value: Long -> setLong(index, value) }
    }

    val ncharacterStream: CombinedArgumentSetter<Reader, Long> by lazy {
        CombinedArgumentSetter(
            { index: Int, reader: Reader -> setNCharacterStream(index, reader) },
            { index: Int, reader: Reader, length: Long -> setNCharacterStream(index, reader, length) }
        )
    }

    val nclob: NClobArgumentSetter by lazy {
        NClobArgumentSetter(
            { index: Int, nclob: NClob -> setNClob(index, nclob) },
            { index: Int, reader: Reader -> setNClob(index, reader) },
            { index: Int, reader: Reader, length: Long -> setNClob(index, reader, length) }
        )
    }

    val nstring: ArgumentSetter<String> by lazy {
        DefaultArgumentSetter { index: Int, value: String -> setNString(index, value) }
    }

    val `null`: CombinedArgumentSetter<Int, String> by lazy {
        CombinedArgumentSetter(
            { index: Int, sqlType: Int -> setNull(index, sqlType) },
            { index: Int, sqlType: Int, typeName: String -> setNull(index, sqlType, typeName) }
        )
    }

    val `object`: ObjectArgumentSetter by lazy {
        ObjectArgumentSetter(
            { index: Int, value: Any -> setObject(index, value) },
            { index: Int, value: Any, sqlType: Int -> setObject(index, value, sqlType) },
            { index: Int, value: Any, sqlType: Int, scaleOrLength: Int ->
                setObject(
                    index,
                    value,
                    sqlType,
                    scaleOrLength
                )
            }
        )
    }

    val ref: ArgumentSetter<Ref> by lazy {
        DefaultArgumentSetter { index: Int, value: Ref -> setRef(index, value) }
    }

    val rowId: ArgumentSetter<RowId> by lazy {
        DefaultArgumentSetter { index: Int, value: RowId -> setRowId(index, value) }
    }

    val sqlxml: ArgumentSetter<SQLXML> by lazy {
        DefaultArgumentSetter { index: Int, value: SQLXML -> setSQLXML(index, value) }
    }

    val string: ArgumentSetter<String> by lazy {
        DefaultArgumentSetter { index: Int, value: String -> setString(index, value) }
    }

    val time: CombinedArgumentSetter<Time, Calendar> by lazy {
        CombinedArgumentSetter(
            { index: Int, time: Time -> setTime(index, time) },
            { index: Int, time: Time, calendar: Calendar -> setTime(index, time, calendar) }
        )
    }

    val timestamp: CombinedArgumentSetter<Timestamp, Calendar> by lazy {
        CombinedArgumentSetter(
            { index: Int, timestamp: Timestamp -> setTimestamp(index, timestamp) },
            { index: Int, timestamp: Timestamp, calendar: Calendar -> setTimestamp(index, timestamp, calendar) }
        )
    }

    val url: ArgumentSetter<URL> by lazy {
        DefaultArgumentSetter { index: Int, value: URL -> setURL(index, value) }
    }
}
