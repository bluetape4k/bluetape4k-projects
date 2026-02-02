package io.bluetape4k.jdbc.sql

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.toFastList
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLException
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp

/**
 * column index 를 이용하여 column 값을 가져옵니다.
 *
 * ```
 * val id = rs[1] as? Long
 * val name = rs[2] as? String
 * ```
 */
operator fun ResultSet.get(columnIndex: Int): Any? = retrieveValueOrNull(this.getObject(columnIndex))

/**
 * column label 를 이용하여 column 값을 가져옵니다.
 *
 * ```
 * val id = rs["id"] as? Long
 * val name = rs["name"] as? String
 * ```
 */
operator fun ResultSet.get(columnLabel: String): Any? = retrieveValueOrNull(this.getObject(columnLabel))

/**
 * [ResultSet]으로부터 정보를 읽어, 객체를 생성합니다.
 *
 * ```
 * val users = statement.executeQuery(sql) { rs: ResultSet ->
 *      rs.extract {
 *          User(long["id"], string["name"])
 *      }
 * }
 * ```
 *
 * @param T  결과 값의 수형
 * @param body ResultSet을 읽어 객체를 생성하는 코드
 * @return 객체 리스트
 */
inline fun <T> ResultSet.extract(crossinline body: ResultSetGetColumnTokens.() -> T): List<T> {
    val rs = ResultSetGetColumnTokens(this)
    return rs.map { body(rs) }
}

/**
 * [ResultSet] 작업 시 [SQLException]이 발생하면 null을 반환하도록 합니다.
 *
 * @param T  결과 값의 수형
 * @param body ResultSet을 읽어 객체를 생성하는 코드
 * @return 객체 또는 null
 */
inline fun <T> ResultSet.emptyResultToNull(body: (ResultSet) -> T): T? =
    try {
        body(this)
    } catch (e: SQLException) {
        null
    }

/**
 * [ResultSet]을 [Iterator]`<ResultSet>` 으로 변환합니다.
 *
 * ```
 * val rs = statement.executeQuery(sql)
 * for (row in rs) {
 *    println(row)
 *    // do something
 *    // ...
 * }
 * ```
 */
operator fun ResultSet.iterator(): Iterator<ResultSet> {
    val rs = this
    return object: Iterator<ResultSet> {
        override operator fun hasNext(): Boolean = rs.next()
        override operator fun next(): ResultSet = rs
    }
}

/**
 * [ResultSet]을 [mapper]를 이용하여 [Iterator]`<T>` 으로 변환합니다.
 *
 * ```
 * val rs = statement.executeQuery(sql)
 * val users = rs.iterator { rs -> User(rs.long["id"], rs.string["name"]) }
 * for (user in users) {
 *   println(user)
 *   // do something
 * }
 * ```
 *
 * @param mapper ResultSet를 이용하여 객체를 생성하는 코드
 */
inline fun <T> ResultSet.iterator(crossinline mapper: (ResultSet) -> T): Iterator<T> {
    val rs = this
    return object: Iterator<T> {
        override fun hasNext(): Boolean = rs.next()
        override fun next(): T = mapper(rs)
    }
}

/**
 * [ResultSet]을 [Sequence]`<T>` 으로 변환합니다.
 *
 * ```
 * val rs = statement.executeQuery(sql)
 * val users = rs.sequence { rs -> User(rs.long["id"], rs.string["name"]) }
 * users.forEach { user ->
 *   println(user)
 * }
 * ```
 * @param mapper ResultSet를 이용하여 객체를 생성하는 코드
 */
inline fun <T> ResultSet.sequence(crossinline mapper: (ResultSet) -> T): Sequence<T> {
    return iterator(mapper).asSequence()
}

/**
 * [ResultSet]을 [mapper]를 이용하여 [List]`<T>` 으로 변환합니다.
 *
 * ```
 * val rs = statement.executeQuery(sql)
 * val users = rs.map { rs -> User(rs.long["id"], rs.string["name"]) }
 * users.forEach { user ->
 *   println(user)
 * }
 * ```
 *
 * @param mapper ResultSet를 이용하여 객체를 생성하는 코드
 */
inline fun <T> ResultSet.map(crossinline mapper: ResultSet.() -> T): List<T> {
    return this@map.iterator(mapper).toFastList()
}

/**
 * [ResultSet]을 [mapper]를 이용하여 [Sequence]`<T>` 으로 변환합니다.
 *
 * ```
 * val rs = statement.executeQuery(sql)
 * val users = rs.mapAsSequence { rs -> User(rs.long["id"], rs.string["name"]) }
 * users.forEach { user ->
 *   println(user)
 * }
 * ```
 *
 * @param mapper ResultSet를 이용하여 객체를 생성하는 코드
 */
inline fun <T> ResultSet.mapAsSequence(crossinline mapper: ResultSet.() -> T): Sequence<T> {
    return this@mapAsSequence.sequence(mapper)
}

/**
 * [ResultSet]의 컬럼명들을 가져옵니다.
 */
val ResultSet.columnNames: List<String>
    get() {
        val meta = this.metaData
        return fastList(meta.columnCount) { meta.getColumnName(it + 1) ?: it.toString() }
    }

/**
 * [ResultSet]이 빈 결과가 아닌 Row를 가지고 있는지 확인합니다. 없으면 [IllegalStateException]을 발생시킵니다.
 */
private fun ResultSet.ensureHasRow(): ResultSet = apply {
    if (!this.next()) {
        error("There are no rows left in cursor.")
    }
}

/**
 * [ResultSet]으로부터 Int형 단일 값을 가져옵니다.
 */
fun ResultSet.singleInt(): Int = this.ensureHasRow().getInt(1)

/**
 * [ResultSet]으로부터 Long형 단일 값을 가져옵니다.
 */
fun ResultSet.singleLong(): Long = this.ensureHasRow().getLong(1)

/**
 * [ResultSet]으로부터 Double 형 단일 값을 가져옵니다.
 */
fun ResultSet.singleDouble(): Double = this.ensureHasRow().getDouble(1)

/**
 * [ResultSet]으로부터 [String]형 단일 값을 가져옵니다.
 */
fun ResultSet.singleString(): String = this.ensureHasRow().getString(1)


private fun <T> ResultSet.retrieveValueOrNull(columnValue: T): T? = if (this.wasNull()) null else columnValue

/**
 * [ResultSet]으로부터 Boolean 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBooleanOrNull(columnIndex: Int): Boolean? = retrieveValueOrNull(this.getBoolean(columnIndex))

/**
 * [ResultSet]으로부터 Boolean 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBooleanOrNull(columnLabel: String): Boolean? = retrieveValueOrNull(this.getBoolean(columnLabel))

/**
 * [ResultSet]으로부터 Byte 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getByteOrNull(columnIndex: Int): Byte? = retrieveValueOrNull(this.getByte(columnIndex))

/**
 * [ResultSet]으로부터 Byte 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getByteOrNull(columnLabel: String): Byte? = retrieveValueOrNull(this.getByte(columnLabel))

/**
 * [ResultSet]으로부터 Short 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getShortOrNull(columnIndex: Int): Short? = retrieveValueOrNull(this.getShort(columnIndex))

/**
 * [ResultSet]으로부터 Short 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getShortOrNull(columnLabel: String): Short? = retrieveValueOrNull(this.getShort(columnLabel))

/**
 * [ResultSet]으로부터 Int 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getIntOrNull(columnIndex: Int): Int? = retrieveValueOrNull(this.getInt(columnIndex))

/**
 * [ResultSet]으로부터 Int 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getIntOrNull(columnLabel: String): Int? = retrieveValueOrNull(this.getInt(columnLabel))

/**
 * [ResultSet]으로부터 Long 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getLongOrNull(columnIndex: Int): Long? = retrieveValueOrNull(this.getLong(columnIndex))

/**
 * [ResultSet]으로부터 Long 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getLongOrNull(columnLabel: String): Long? = retrieveValueOrNull(this.getLong(columnLabel))

/**
 * [ResultSet]으로부터 Float 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getFloatOrNull(columnIndex: Int): Float? = retrieveValueOrNull(this.getFloat(columnIndex))

/**
 * [ResultSet]으로부터 Float 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getFloatOrNull(columnLabel: String): Float? = retrieveValueOrNull(this.getFloat(columnLabel))

/**
 * [ResultSet]으로부터 Double 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getDoubleOrNull(columnIndex: Int): Double? = retrieveValueOrNull(this.getDouble(columnIndex))

/**
 * [ResultSet]으로부터 Double 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getDoubleOrNull(columnLabel: String): Double? = retrieveValueOrNull(this.getDouble(columnLabel))

/**
 * [ResultSet]으로부터 BigDecimal 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBigDecimalOrNull(columnIndex: Int): BigDecimal? =
    retrieveValueOrNull(this.getBigDecimal(columnIndex))

/**
 * [ResultSet]으로부터 BigDecimal 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBigDecimalOrNull(columnLabel: String): BigDecimal? =
    retrieveValueOrNull(this.getBigDecimal(columnLabel))

/**
 * [ResultSet]으로부터 ByteArray 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBytesOrNull(columnIndex: Int): ByteArray? = retrieveValueOrNull(this.getBytes(columnIndex))

/**
 * [ResultSet]으로부터 ByteArray 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBytesOrNull(columnLabel: String): ByteArray? = retrieveValueOrNull(this.getBytes(columnLabel))

/**
 * [ResultSet]으로부터 Any 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getObjectOrNull(columnIndex: Int): Any? = retrieveValueOrNull(this.getObject(columnIndex))

/**
 * [ResultSet]으로부터 Any 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getObjectOrNull(columnLabel: String): Any? = retrieveValueOrNull(this.getObject(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Array] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getArrayOrNull(columnIndex: Int): java.sql.Array? = retrieveValueOrNull(this.getArray(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Array] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getArrayOrNull(columnLabel: String): java.sql.Array? = retrieveValueOrNull(this.getArray(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Date] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getDateOrNull(columnIndex: Int): Date? = retrieveValueOrNull(this.getDate(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Date] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getDateOrNull(columnLabel: String): Date? = retrieveValueOrNull(this.getDate(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Time] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getTimeOrNull(columnIndex: Int): Time? = retrieveValueOrNull(this.getTime(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Time] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getTimeOrNull(columnLabel: String): Time? = retrieveValueOrNull(this.getTime(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Timestamp] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getTimestampOrNull(columnIndex: Int): Timestamp? = retrieveValueOrNull(this.getTimestamp(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Timestamp] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getTimestampOrNull(columnLabel: String): Timestamp? = retrieveValueOrNull(this.getTimestamp(columnLabel))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getAsciiStreamOrNull(columnIndex: Int): InputStream? =
    retrieveValueOrNull(this.getAsciiStream(columnIndex))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getAsciiStreamOrNull(columnLabel: String): InputStream? =
    retrieveValueOrNull(this.getAsciiStream(columnLabel))

/**
 * [ResultSet]으로부터 [ByteArray] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBinaryStreamOrNull(columnIndex: Int): InputStream? =
    retrieveValueOrNull(this.getBinaryStream(columnIndex))

/**
 * [ResultSet]으로부터 [ByteArray] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBinaryStreamOrNull(columnLabel: String): InputStream? =
    retrieveValueOrNull(this.getBinaryStream(columnLabel))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getCharacterStreamOrNull(columnIndex: Int): Reader? =
    retrieveValueOrNull(this.getCharacterStream(columnIndex))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getCharacterStreamOrNull(columnLabel: String): Reader? =
    retrieveValueOrNull(this.getCharacterStream(columnLabel))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getNCharacterStreamOrNull(columnIndex: Int): Reader? =
    retrieveValueOrNull(this.getNCharacterStream(columnIndex))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getNCharacterStreamOrNull(columnLabel: String): Reader? =
    retrieveValueOrNull(this.getNCharacterStream(columnLabel))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getStringOrNull(columnIndex: Int): String? = retrieveValueOrNull(this.getString(columnIndex))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getStringOrNull(columnLabel: String): String? = retrieveValueOrNull(this.getString(columnLabel))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getNStringOrNull(columnIndex: Int): String? = retrieveValueOrNull(this.getNString(columnIndex))

/**
 * [ResultSet]으로부터 [String] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getNStringOrNull(columnLabel: String): String? = retrieveValueOrNull(this.getNString(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Blob] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBlobOrNull(columnIndex: Int): Blob? = retrieveValueOrNull(this.getBlob(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Blob] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getBlobOrNull(columnLabel: String): Blob? = retrieveValueOrNull(this.getBlob(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Clob] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getClobOrNull(columnIndex: Int): Clob? = retrieveValueOrNull(this.getClob(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Clob] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getClobOrNull(columnLabel: String): Clob? = retrieveValueOrNull(this.getClob(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.NClob] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getNClobOrNull(columnIndex: Int): NClob? = retrieveValueOrNull(this.getNClob(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.NClob] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getNClobOrNull(columnLabel: String): NClob? = retrieveValueOrNull(this.getNClob(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.SQLXML] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getSQLXMLOrNull(columnIndex: Int): SQLXML? = retrieveValueOrNull(this.getSQLXML(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.SQLXML] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getSQLXMLOrNull(columnLabel: String): SQLXML? = retrieveValueOrNull(this.getSQLXML(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.Ref] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getRefOrNull(columnIndex: Int): Ref? = retrieveValueOrNull(this.getRef(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.Ref] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getRefOrNull(columnLabel: String): Ref? = retrieveValueOrNull(this.getRef(columnLabel))

/**
 * [ResultSet]으로부터 [java.sql.RowId] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getRowIdOrNull(columnIndex: Int): RowId? = retrieveValueOrNull(this.getRowId(columnIndex))

/**
 * [ResultSet]으로부터 [java.sql.RowId] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getRowIdOrNull(columnLabel: String): RowId? = retrieveValueOrNull(this.getRowId(columnLabel))

/**
 * [ResultSet]으로부터 [java.net.URL] 값을 가져옵니다. 해당 [columnIndex]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getURLOrNull(columnIndex: Int): URL? = retrieveValueOrNull(this.getURL(columnIndex))

/**
 * [ResultSet]으로부터 [java.net.URL] 값을 가져옵니다. 해당 [columnLabel]가 없으면 null 을 반환합니다.
 */
fun ResultSet.getURLOrNull(columnLabel: String): URL? = retrieveValueOrNull(this.getURL(columnLabel))
