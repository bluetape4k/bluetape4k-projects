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

    val array: GetColumnToken<Array> by lazy {
        GetColumnToken({ getArrayOrNull(it) }, { getArrayOrNull(it) })
    }

    val asciiStream: GetColumnToken<InputStream> by lazy {
        GetColumnToken({ getAsciiStreamOrNull(it) }, { getAsciiStreamOrNull(it) })
    }

    val bigDecimal: GetColumnToken<BigDecimal> by lazy {
        GetColumnToken({ getBigDecimalOrNull(it) }, { getBigDecimalOrNull(it) })
    }

    val binaryStream: GetColumnToken<InputStream> by lazy {
        GetColumnToken({ getBinaryStreamOrNull(it) }, { getBinaryStreamOrNull(it) })
    }

    val blob: GetColumnToken<Blob> by lazy {
        GetColumnToken({ getBlobOrNull(it) }, { getBlobOrNull(it) })
    }

    val boolean: GetColumnToken<Boolean> by lazy {
        GetColumnToken({ getBooleanOrNull(it) }, { getBooleanOrNull(it) })
    }

    val bytes: GetColumnToken<ByteArray> by lazy {
        GetColumnToken({ getBytesOrNull(it) }, { getBytesOrNull(it) })
    }

    val characterStream: GetColumnToken<Reader> by lazy {
        GetColumnToken({ getCharacterStreamOrNull(it) }, { getCharacterStreamOrNull(it) })
    }

    val clob: GetColumnToken<Clob> by lazy {
        GetColumnToken({ getClobOrNull(it) }, { getClobOrNull(it) })
    }

    val date: GetColumnToken<Date> by lazy {
        GetColumnToken({ getDateOrNull(it) }, { getDateOrNull(it) })
    }

    val double: GetColumnToken<Double> by lazy {
        GetColumnToken({ getDoubleOrNull(it) }, { getDoubleOrNull(it) })
    }

    val float: GetColumnToken<Float> by lazy {
        GetColumnToken({ getFloatOrNull(it) }, { getFloatOrNull(it) })
    }

    val int: GetColumnToken<Int> by lazy {
        GetColumnToken({ getIntOrNull(it) }, { getIntOrNull(it) })
    }

    val long: GetColumnToken<Long> by lazy {
        GetColumnToken({ getLongOrNull(it) }, { getLongOrNull(it) })
    }

    val ncharacterStream: GetColumnToken<Reader> by lazy {
        GetColumnToken({ getNCharacterStreamOrNull(it) }, { getNCharacterStreamOrNull(it) })
    }

    val nclob: GetColumnToken<NClob> by lazy {
        GetColumnToken({ getNClobOrNull(it) }, { getNClobOrNull(it) })
    }

    val nstring: GetColumnToken<String> by lazy {
        GetColumnToken({ getNStringOrNull(it) }, { getNStringOrNull(it) })
    }

    val ref: GetColumnToken<Ref> by lazy {
        GetColumnToken({ getRefOrNull(it) }, { getRefOrNull(it) })
    }

    val rowId: GetColumnToken<RowId> by lazy {
        GetColumnToken({ getRowIdOrNull(it) }, { getRowIdOrNull(it) })
    }

    val short: GetColumnToken<Short> by lazy {
        GetColumnToken({ getShortOrNull(it) }, { getShortOrNull(it) })
    }

    val sqlxml: GetColumnToken<SQLXML> by lazy {
        GetColumnToken({ getSQLXMLOrNull(it) }, { getSQLXMLOrNull(it) })
    }

    val string: GetColumnToken<String> by lazy {
        GetColumnToken({ getStringOrNull(it) }, { getStringOrNull(it) })
    }

    val time: GetColumnToken<Time> by lazy {
        GetColumnToken({ getTimeOrNull(it) }, { getTimeOrNull(it) })
    }

    val timestamp: GetColumnToken<Timestamp> by lazy {
        GetColumnToken({ getTimestampOrNull(it) }, { getTimestampOrNull(it) })
    }

    val url: GetColumnToken<URL> by lazy {
        GetColumnToken({ getURLOrNull(it) }, { getURLOrNull(it) })
    }
}
