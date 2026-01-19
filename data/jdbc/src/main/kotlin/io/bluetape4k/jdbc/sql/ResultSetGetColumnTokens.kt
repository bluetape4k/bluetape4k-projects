package io.bluetape4k.jdbc.sql

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLType
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp


/**
 * [ResultSet]으로부터 Column 값을 각 수형에 맞게 가져오는 클래스입니다.
 *
 * ```
 *
 * jdbcTemplare.query(selectQuery) { rs:ResultSet ->
 *     val id: Long? = long["id"]
 *     val description: String? = string["description"]
 *     val createdAt: Date? = date["createdAt"]
 * }
 * ```
 */
open class ResultSetGetColumnTokens(val resultSet: ResultSet): ResultSet by resultSet {

    val array: GetColumnToken<Array> =
        GetColumnToken(
            { getArrayOrNull(it) },
            { getArrayOrNull(it) }
        )

    val asciiStream: GetColumnToken<InputStream> =
        GetColumnToken(
            { getAsciiStreamOrNull(it) },
            { getAsciiStreamOrNull(it) }
        )

    val bigDecimal: GetColumnToken<BigDecimal> =
        GetColumnToken(
            { getBigDecimalOrNull(it) },
            { getBigDecimalOrNull(it) }
        )

    val binaryStream: GetColumnToken<InputStream> =
        GetColumnToken(
            { getBinaryStreamOrNull(it) },
            { getBinaryStreamOrNull(it) }
        )

    val blob: GetColumnToken<Blob> =
        GetColumnToken(
            { getBlobOrNull(it) },
            { getBlobOrNull(it) }
        )

    val boolean: GetColumnToken<Boolean> =
        GetColumnToken(
            { getBooleanOrNull(it) },
            { getBooleanOrNull(it) }
        )

    val bytes: GetColumnToken<ByteArray> =
        GetColumnToken(
            { getBytesOrNull(it) },
            { getBytesOrNull(it) }
        )

    val characterStream: GetColumnToken<Reader> =
        GetColumnToken(
            { getCharacterStreamOrNull(it) },
            { getCharacterStreamOrNull(it) }
        )

    val clob: GetColumnToken<Clob> =
        GetColumnToken(
            { getClobOrNull(it) },
            { getClobOrNull(it) }
        )

    val date: GetColumnToken<Date> =
        GetColumnToken(
            { getDateOrNull(it) },
            { getDateOrNull(it) }
        )

    val double: GetColumnToken<Double> =
        GetColumnToken(
            { getDoubleOrNull(it) },
            { getDoubleOrNull(it) }
        )

    val float: GetColumnToken<Float> =
        GetColumnToken(
            { getFloatOrNull(it) },
            { getFloatOrNull(it) }
        )

    val int: GetColumnToken<Int> =
        GetColumnToken(
            { getIntOrNull(it) },
            { getIntOrNull(it) }
        )

    val long: GetColumnToken<Long> =
        GetColumnToken(
            { getLongOrNull(it) },
            { getLongOrNull(it) }
        )

    val ncharacterStream: GetColumnToken<Reader> =
        GetColumnToken(
            { getNCharacterStreamOrNull(it) },
            { getNCharacterStreamOrNull(it) }
        )

    val nclob: GetColumnToken<NClob> =
        GetColumnToken(
            { getNClobOrNull(it) },
            { getNClobOrNull(it) }
        )

    val nstring: GetColumnToken<String> =
        GetColumnToken(
            { getNStringOrNull(it) },
            { getNStringOrNull(it) }
        )

    val ref: GetColumnToken<Ref> =
        GetColumnToken(
            { getRefOrNull(it) },
            { getRefOrNull(it) }
        )

    val rowId: GetColumnToken<RowId> =
        GetColumnToken(
            { getRowIdOrNull(it) },
            { getRowIdOrNull(it) }
        )

    val short: GetColumnToken<Short> =
        GetColumnToken(
            { getShortOrNull(it) },
            { getShortOrNull(it) }
        )

    val sqlxml: GetColumnToken<SQLXML> =
        GetColumnToken(
            { getSQLXMLOrNull(it) },
            { getSQLXMLOrNull(it) }
        )

    val string: GetColumnToken<String> =
        GetColumnToken(
            { getStringOrNull(it) },
            { getStringOrNull(it) }
        )

    val time: GetColumnToken<Time> =
        GetColumnToken(
            { getTimeOrNull(it) },
            { getTimeOrNull(it) }
        )

    val timestamp: GetColumnToken<Timestamp> =
        GetColumnToken(
            { getTimestampOrNull(it) },
            { getTimestampOrNull(it) }
        )

    val url: GetColumnToken<URL> =
        GetColumnToken(
            { getURLOrNull(it) },
            { getURLOrNull(it) }
        )

    override fun updateObject(
        columnIndex: Int,
        x: Any?,
        targetSqlType: SQLType?,
        scaleOrLength: Int,
    ) {
        super.updateObject(columnIndex, x, targetSqlType, scaleOrLength)
    }

    override fun updateObject(
        columnLabel: String?,
        x: Any?,
        targetSqlType: SQLType?,
        scaleOrLength: Int,
    ) {
        super.updateObject(columnLabel, x, targetSqlType, scaleOrLength)
    }

    override fun updateObject(columnIndex: Int, x: Any?, targetSqlType: SQLType?) {
        super.updateObject(columnIndex, x, targetSqlType)
    }

    override fun updateObject(columnLabel: String?, x: Any?, targetSqlType: SQLType?) {
        super.updateObject(columnLabel, x, targetSqlType)
    }
}
