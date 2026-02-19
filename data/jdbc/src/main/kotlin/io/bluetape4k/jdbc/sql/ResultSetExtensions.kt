package io.bluetape4k.jdbc.sql

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
import java.sql.SQLException
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp

/**
 * 컬럼 인덱스를 이용하여 컬럼 값을 가져옵니다.
 *
 * 인덱스는 1부터 시작합니다. SQL NULL 값은 Kotlin null로 변환됩니다.
 *
 * ```kotlin
 * val id = rs[1] as? Long
 * val name = rs[2] as? String
 * ```
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return 컬럼 값 또는 null
 */
operator fun ResultSet.get(columnIndex: Int): Any? = retrieveValueOrNull(this.getObject(columnIndex))

/**
 * 컬럼 레이블을 이용하여 컬럼 값을 가져옵니다.
 *
 * SQL NULL 값은 Kotlin null로 변환됩니다.
 *
 * ```kotlin
 * val id = rs["id"] as? Long
 * val name = rs["name"] as? String
 * ```
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return 컬럼 값 또는 null
 */
operator fun ResultSet.get(columnLabel: String): Any? = retrieveValueOrNull(this.getObject(columnLabel))

/**
 * ResultSet에서 정보를 읽어 객체 리스트를 생성합니다.
 *
 * 이 함수는 ResultSetGetColumnTokens를 사용하여 타입 안전하게 컬럼 값을 조회할 수 있습니다.
 *
 * ```kotlin
 * val users = statement.executeQuery(sql).extract {
 *     User(long["id"]!!, string["name"]!!, date["created_at"])
 * }
 * ```
 *
 * @param T 결과 값의 타입
 * @param body ResultSet을 읽어 객체를 생성하는 코드 블록
 * @return 객체 리스트
 */
inline fun <T> ResultSet.extract(crossinline body: ResultSetGetColumnTokens.() -> T): List<T> {
    val rs = ResultSetGetColumnTokens(this)
    return rs.map { body(rs) }
}

/**
 * ResultSet 작업 시 SQLException이 발생하면 null을 반환합니다.
 *
 * ```kotlin
 * val result = rs.emptyResultToNull { resultSet ->
 *     resultSet.getInt("id")
 * }
 * ```
 *
 * @param T 결과 값의 타입
 * @param body ResultSet을 읽어 객체를 생성하는 코드
 * @return 객체 또는 null (예외 발생 시)
 */
inline fun <T> ResultSet.emptyResultToNull(body: (ResultSet) -> T): T? =
    try {
        body(this)
    } catch (e: SQLException) {
        null
    }

/**
 * ResultSet을 Iterator<ResultSet>으로 변환합니다.
 *
 * for-each 루프에서 ResultSet을 직접 사용할 수 있습니다.
 *
 * ```kotlin
 * val rs = statement.executeQuery(sql)
 * for (row in rs) {
 *     println(row.getString("name"))
 *     // ResultSet의 커서가 자동으로 이동됩니다.
 * }
 * ```
 *
 * @return ResultSet을 순회하는 Iterator
 */
operator fun ResultSet.iterator(): Iterator<ResultSet> {
    val rs = this
    val cursor = ResultSetCursorState(rs)
    return object : Iterator<ResultSet> {
        override operator fun hasNext(): Boolean = cursor.hasNext()

        override operator fun next(): ResultSet {
            cursor.consumeNext()
            return rs
        }
    }
}

/**
 * ResultSet을 mapper를 이용하여 Iterator<T>으로 변환합니다.
 *
 * ```kotlin
 * val rs = statement.executeQuery(sql)
 * val users = rs.iterator { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * for (user in users) {
 *     println(user)
 * }
 * ```
 *
 * @param T 변환할 결과 타입
 * @param mapper ResultSet을 이용하여 객체를 생성하는 코드
 * @return 객체를 순회하는 Iterator
 */
inline fun <T> ResultSet.iterator(crossinline mapper: (ResultSet) -> T): Iterator<T> {
    val rs = this
    val cursor = ResultSetCursorState(rs)
    return object : Iterator<T> {
        override fun hasNext(): Boolean = cursor.hasNext()

        override fun next(): T {
            cursor.consumeNext()
            return mapper(rs)
        }
    }
}

/**
 * JDBC 커서는 next() 호출마다 한 행씩 전진하므로, hasNext() 중복 호출 시 row skip이 발생할 수 있습니다.
 * 이를 방지하기 위해 다음 row 존재 여부를 1회만 계산해 캐싱합니다.
 *
 * @property rs 캐싱할 ResultSet
 */
@PublishedApi
internal class ResultSetCursorState(
    private val rs: ResultSet,
) {
    private var isReady = false
    private var hasNext = false

    /**
     * 다음 행이 있는지 확인합니다. 결과는 캐싱됩니다.
     */
    fun hasNext(): Boolean {
        if (!isReady) {
            hasNext = rs.next()
            isReady = true
        }
        return hasNext
    }

    /**
     * 다음 행으로 이동합니다. 더 이상 행이 없으면 예외를 발생시킵니다.
     */
    fun consumeNext() {
        if (!hasNext()) {
            throw NoSuchElementException("No more rows in ResultSet.")
        }
        isReady = false
    }
}

/**
 * ResultSet을 Sequence<T>으로 변환합니다.
 *
 * ```kotlin
 * val rs = statement.executeQuery(sql)
 * val users = rs.sequence { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * users.forEach { user ->
 *     println(user)
 * }
 * ```
 *
 * @param T 변환할 결과 타입
 * @param mapper ResultSet을 이용하여 객체를 생성하는 코드
 * @return 객체를 순회하는 Sequence
 */
inline fun <T> ResultSet.sequence(crossinline mapper: (ResultSet) -> T): Sequence<T> = iterator(mapper).asSequence()

/**
 * ResultSet을 mapper를 이용하여 List<T>으로 변환합니다.
 *
 * ```kotlin
 * val rs = statement.executeQuery(sql)
 * val users = rs.map { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 변환할 결과 타입
 * @param mapper ResultSet을 이용하여 객체를 생성하는 코드
 * @return 객체 리스트
 */
inline fun <T> ResultSet.map(crossinline mapper: ResultSet.() -> T): List<T> = this@map.iterator(mapper).asSequence().toList()

/**
 * ResultSet을 mapper를 이용하여 Sequence<T>으로 변환합니다.
 *
 * ```kotlin
 * val rs = statement.executeQuery(sql)
 * val users = rs.mapAsSequence { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 변환할 결과 타입
 * @param mapper ResultSet을 이용하여 객체를 생성하는 코드
 * @return 객체를 순회하는 Sequence
 */
inline fun <T> ResultSet.mapAsSequence(crossinline mapper: ResultSet.() -> T): Sequence<T> = this@mapAsSequence.sequence(mapper)

/**
 * ResultSet의 컬럼명들을 가져옵니다.
 *
 * ```kotlin
 * val columns = rs.columnNames
 * println(columns) // ["id", "name", "email"]
 * ```
 */
val ResultSet.columnNames: List<String>
    get() {
        val meta = this.metaData
        return List(meta.columnCount) { meta.getColumnName(it + 1) ?: (it + 1).toString() }
    }

/**
 * ResultSet의 컬럼 레이블(별칭)들을 가져옵니다.
 *
 * ```kotlin
 * val labels = rs.columnLabels
 * println(labels) // ["user_id", "user_name"]
 * ```
 */
val ResultSet.columnLabels: List<String>
    get() {
        val meta = this.metaData
        return List(meta.columnCount) { meta.getColumnLabel(it + 1) ?: (it + 1).toString() }
    }

/**
 * ResultSet의 컬럼 수를 가져옵니다.
 *
 * ```kotlin
 * val count = rs.columnCount
 * ```
 */
val ResultSet.columnCount: Int
    get() = this.metaData.columnCount

/**
 * ResultSet이 비어있지 않은 Row를 가지고 있는지 확인합니다.
 * 없으면 IllegalStateException을 발생시킵니다.
 */
private fun ResultSet.ensureHasRow(): ResultSet =
    apply {
        if (!this.next()) {
            error("ResultSet has no more rows.")
        }
    }

/**
 * ResultSet으로부터 Int형 단일 값을 가져옵니다.
 *
 * ```kotlin
 * val count = rs.singleInt()
 * ```
 *
 * @return 첫 번째 컬럼의 Int 값
 * @throws IllegalStateException ResultSet이 비어있을 경우
 */
fun ResultSet.singleInt(): Int = this.ensureHasRow().getInt(1)

/**
 * ResultSet으로부터 Long형 단일 값을 가져옵니다.
 *
 * ```kotlin
 * val id = rs.singleLong()
 * ```
 *
 * @return 첫 번째 컬럼의 Long 값
 * @throws IllegalStateException ResultSet이 비어있을 경우
 */
fun ResultSet.singleLong(): Long = this.ensureHasRow().getLong(1)

/**
 * ResultSet으로부터 Double 형 단일 값을 가져옵니다.
 *
 * ```kotlin
 * val value = rs.singleDouble()
 * ```
 *
 * @return 첫 번째 컬럼의 Double 값
 * @throws IllegalStateException ResultSet이 비어있을 경우
 */
fun ResultSet.singleDouble(): Double = this.ensureHasRow().getDouble(1)

/**
 * ResultSet으로부터 String 형 단일 값을 가져옵니다.
 *
 * ```kotlin
 * val name = rs.singleString()
 * ```
 *
 * @return 첫 번째 컬럼의 String 값 (null 가능)
 * @throws IllegalStateException ResultSet이 비어있을 경우
 */
fun ResultSet.singleString(): String? = this.ensureHasRow().getString(1)

/**
 * ResultSet으로부터 BigDecimal 형 단일 값을 가져옵니다.
 *
 * ```kotlin
 * val amount = rs.singleBigDecimal()
 * ```
 *
 * @return 첫 번째 컬럼의 BigDecimal 값 (null 가능)
 * @throws IllegalStateException ResultSet이 비어있을 경우
 */
fun ResultSet.singleBigDecimal(): BigDecimal? = this.ensureHasRow().getBigDecimal(1)

/**
 * 컬럼 값이 SQL NULL이었는지 확인하고, null이면 null을 반환합니다.
 *
 * @param T 컬럼 값의 타입
 * @param columnValue 컬럼에서 조회한 값
 * @return SQL NULL이었으면 null, 그렇지 않으면 원래 값
 */
private fun <T> ResultSet.retrieveValueOrNull(columnValue: T): T? = if (this.wasNull()) null else columnValue

// ==================== Boolean 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Boolean 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Boolean 값 또는 null
 */
fun ResultSet.getBooleanOrNull(columnIndex: Int): Boolean? = retrieveValueOrNull(this.getBoolean(columnIndex))

/**
 * ResultSet으로부터 Boolean 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Boolean 값 또는 null
 */
fun ResultSet.getBooleanOrNull(columnLabel: String): Boolean? = retrieveValueOrNull(this.getBoolean(columnLabel))

// ==================== Byte 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Byte 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Byte 값 또는 null
 */
fun ResultSet.getByteOrNull(columnIndex: Int): Byte? = retrieveValueOrNull(this.getByte(columnIndex))

/**
 * ResultSet으로부터 Byte 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Byte 값 또는 null
 */
fun ResultSet.getByteOrNull(columnLabel: String): Byte? = retrieveValueOrNull(this.getByte(columnLabel))

// ==================== Short 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Short 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Short 값 또는 null
 */
fun ResultSet.getShortOrNull(columnIndex: Int): Short? = retrieveValueOrNull(this.getShort(columnIndex))

/**
 * ResultSet으로부터 Short 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Short 값 또는 null
 */
fun ResultSet.getShortOrNull(columnLabel: String): Short? = retrieveValueOrNull(this.getShort(columnLabel))

// ==================== Int 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Int 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Int 값 또는 null
 */
fun ResultSet.getIntOrNull(columnIndex: Int): Int? = retrieveValueOrNull(this.getInt(columnIndex))

/**
 * ResultSet으로부터 Int 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Int 값 또는 null
 */
fun ResultSet.getIntOrNull(columnLabel: String): Int? = retrieveValueOrNull(this.getInt(columnLabel))

// ==================== Long 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Long 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Long 값 또는 null
 */
fun ResultSet.getLongOrNull(columnIndex: Int): Long? = retrieveValueOrNull(this.getLong(columnIndex))

/**
 * ResultSet으로부터 Long 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Long 값 또는 null
 */
fun ResultSet.getLongOrNull(columnLabel: String): Long? = retrieveValueOrNull(this.getLong(columnLabel))

// ==================== Float 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Float 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Float 값 또는 null
 */
fun ResultSet.getFloatOrNull(columnIndex: Int): Float? = retrieveValueOrNull(this.getFloat(columnIndex))

/**
 * ResultSet으로부터 Float 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Float 값 또는 null
 */
fun ResultSet.getFloatOrNull(columnLabel: String): Float? = retrieveValueOrNull(this.getFloat(columnLabel))

// ==================== Double 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Double 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Double 값 또는 null
 */
fun ResultSet.getDoubleOrNull(columnIndex: Int): Double? = retrieveValueOrNull(this.getDouble(columnIndex))

/**
 * ResultSet으로부터 Double 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Double 값 또는 null
 */
fun ResultSet.getDoubleOrNull(columnLabel: String): Double? = retrieveValueOrNull(this.getDouble(columnLabel))

// ==================== BigDecimal 타입 확장 함수 ====================

/**
 * ResultSet으로부터 BigDecimal 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return BigDecimal 값 또는 null
 */
fun ResultSet.getBigDecimalOrNull(columnIndex: Int): BigDecimal? = retrieveValueOrNull(this.getBigDecimal(columnIndex))

/**
 * ResultSet으로부터 BigDecimal 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return BigDecimal 값 또는 null
 */
fun ResultSet.getBigDecimalOrNull(columnLabel: String): BigDecimal? = retrieveValueOrNull(this.getBigDecimal(columnLabel))

// ==================== ByteArray 타입 확장 함수 ====================

/**
 * ResultSet으로부터 ByteArray 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return ByteArray 값 또는 null
 */
fun ResultSet.getBytesOrNull(columnIndex: Int): ByteArray? = retrieveValueOrNull(this.getBytes(columnIndex))

/**
 * ResultSet으로부터 ByteArray 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return ByteArray 값 또는 null
 */
fun ResultSet.getBytesOrNull(columnLabel: String): ByteArray? = retrieveValueOrNull(this.getBytes(columnLabel))

// ==================== Object 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Any 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Any 값 또는 null
 */
fun ResultSet.getObjectOrNull(columnIndex: Int): Any? = retrieveValueOrNull(this.getObject(columnIndex))

/**
 * ResultSet으로부터 Any 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Any 값 또는 null
 */
fun ResultSet.getObjectOrNull(columnLabel: String): Any? = retrieveValueOrNull(this.getObject(columnLabel))

// ==================== Array 타입 확장 함수 ====================

/**
 * ResultSet으로부터 java.sql.Array 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return java.sql.Array 값 또는 null
 */
fun ResultSet.getArrayOrNull(columnIndex: Int): Array? = retrieveValueOrNull(this.getArray(columnIndex))

/**
 * ResultSet으로부터 java.sql.Array 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return java.sql.Array 값 또는 null
 */
fun ResultSet.getArrayOrNull(columnLabel: String): Array? = retrieveValueOrNull(this.getArray(columnLabel))

// ==================== Date 타입 확장 함수 ====================

/**
 * ResultSet으로부터 java.sql.Date 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return java.sql.Date 값 또는 null
 */
fun ResultSet.getDateOrNull(columnIndex: Int): Date? = retrieveValueOrNull(this.getDate(columnIndex))

/**
 * ResultSet으로부터 java.sql.Date 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return java.sql.Date 값 또는 null
 */
fun ResultSet.getDateOrNull(columnLabel: String): Date? = retrieveValueOrNull(this.getDate(columnLabel))

// ==================== Time 타입 확장 함수 ====================

/**
 * ResultSet으로부터 java.sql.Time 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return java.sql.Time 값 또는 null
 */
fun ResultSet.getTimeOrNull(columnIndex: Int): Time? = retrieveValueOrNull(this.getTime(columnIndex))

/**
 * ResultSet으로부터 java.sql.Time 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return java.sql.Time 값 또는 null
 */
fun ResultSet.getTimeOrNull(columnLabel: String): Time? = retrieveValueOrNull(this.getTime(columnLabel))

// ==================== Timestamp 타입 확장 함수 ====================

/**
 * ResultSet으로부터 java.sql.Timestamp 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return java.sql.Timestamp 값 또는 null
 */
fun ResultSet.getTimestampOrNull(columnIndex: Int): Timestamp? = retrieveValueOrNull(this.getTimestamp(columnIndex))

/**
 * ResultSet으로부터 java.sql.Timestamp 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return java.sql.Timestamp 값 또는 null
 */
fun ResultSet.getTimestampOrNull(columnLabel: String): Timestamp? = retrieveValueOrNull(this.getTimestamp(columnLabel))

// ==================== Stream 타입 확장 함수 ====================

/**
 * ResultSet으로부터 ASCII 스트림을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return InputStream 값 또는 null
 */
fun ResultSet.getAsciiStreamOrNull(columnIndex: Int): java.io.InputStream? = retrieveValueOrNull(this.getAsciiStream(columnIndex))

/**
 * ResultSet으로부터 ASCII 스트림을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return InputStream 값 또는 null
 */
fun ResultSet.getAsciiStreamOrNull(columnLabel: String): java.io.InputStream? = retrieveValueOrNull(this.getAsciiStream(columnLabel))

/**
 * ResultSet으로부터 바이너리 스트림을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return InputStream 값 또는 null
 */
fun ResultSet.getBinaryStreamOrNull(columnIndex: Int): java.io.InputStream? = retrieveValueOrNull(this.getBinaryStream(columnIndex))

/**
 * ResultSet으로부터 바이너리 스트림을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return InputStream 값 또는 null
 */
fun ResultSet.getBinaryStreamOrNull(columnLabel: String): java.io.InputStream? = retrieveValueOrNull(this.getBinaryStream(columnLabel))

/**
 * ResultSet으로부터 문자 스트림을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Reader 값 또는 null
 */
fun ResultSet.getCharacterStreamOrNull(columnIndex: Int): java.io.Reader? = retrieveValueOrNull(this.getCharacterStream(columnIndex))

/**
 * ResultSet으로부터 문자 스트림을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Reader 값 또는 null
 */
fun ResultSet.getCharacterStreamOrNull(columnLabel: String): java.io.Reader? = retrieveValueOrNull(this.getCharacterStream(columnLabel))

/**
 * ResultSet으로부터 NCharacter 스트림을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Reader 값 또는 null
 */
fun ResultSet.getNCharacterStreamOrNull(columnIndex: Int): java.io.Reader? = retrieveValueOrNull(this.getNCharacterStream(columnIndex))

/**
 * ResultSet으로부터 NCharacter 스트림을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Reader 값 또는 null
 */
fun ResultSet.getNCharacterStreamOrNull(columnLabel: String): java.io.Reader? = retrieveValueOrNull(this.getNCharacterStream(columnLabel))

// ==================== String 타입 확장 함수 ====================

/**
 * ResultSet으로부터 String 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return String 값 또는 null
 */
fun ResultSet.getStringOrNull(columnIndex: Int): String? = retrieveValueOrNull(this.getString(columnIndex))

/**
 * ResultSet으로부터 String 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return String 값 또는 null
 */
fun ResultSet.getStringOrNull(columnLabel: String): String? = retrieveValueOrNull(this.getString(columnLabel))

/**
 * ResultSet으로부터 NString 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return String 값 또는 null
 */
fun ResultSet.getNStringOrNull(columnIndex: Int): String? = retrieveValueOrNull(this.getNString(columnIndex))

/**
 * ResultSet으로부터 NString 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return String 값 또는 null
 */
fun ResultSet.getNStringOrNull(columnLabel: String): String? = retrieveValueOrNull(this.getNString(columnLabel))

// ==================== Blob/Clob 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Blob 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Blob 값 또는 null
 */
fun ResultSet.getBlobOrNull(columnIndex: Int): Blob? = retrieveValueOrNull(this.getBlob(columnIndex))

/**
 * ResultSet으로부터 Blob 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Blob 값 또는 null
 */
fun ResultSet.getBlobOrNull(columnLabel: String): Blob? = retrieveValueOrNull(this.getBlob(columnLabel))

/**
 * ResultSet으로부터 Clob 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Clob 값 또는 null
 */
fun ResultSet.getClobOrNull(columnIndex: Int): Clob? = retrieveValueOrNull(this.getClob(columnIndex))

/**
 * ResultSet으로부터 Clob 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Clob 값 또는 null
 */
fun ResultSet.getClobOrNull(columnLabel: String): Clob? = retrieveValueOrNull(this.getClob(columnLabel))

/**
 * ResultSet으로부터 NClob 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return NClob 값 또는 null
 */
fun ResultSet.getNClobOrNull(columnIndex: Int): NClob? = retrieveValueOrNull(this.getNClob(columnIndex))

/**
 * ResultSet으로부터 NClob 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return NClob 값 또는 null
 */
fun ResultSet.getNClobOrNull(columnLabel: String): NClob? = retrieveValueOrNull(this.getNClob(columnLabel))

// ==================== XML 타입 확장 함수 ====================

/**
 * ResultSet으로부터 SQLXML 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return SQLXML 값 또는 null
 */
fun ResultSet.getSQLXMLOrNull(columnIndex: Int): SQLXML? = retrieveValueOrNull(this.getSQLXML(columnIndex))

/**
 * ResultSet으로부터 SQLXML 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return SQLXML 값 또는 null
 */
fun ResultSet.getSQLXMLOrNull(columnLabel: String): SQLXML? = retrieveValueOrNull(this.getSQLXML(columnLabel))

// ==================== Ref/RowId/URL 타입 확장 함수 ====================

/**
 * ResultSet으로부터 Ref 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return Ref 값 또는 null
 */
fun ResultSet.getRefOrNull(columnIndex: Int): Ref? = retrieveValueOrNull(this.getRef(columnIndex))

/**
 * ResultSet으로부터 Ref 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return Ref 값 또는 null
 */
fun ResultSet.getRefOrNull(columnLabel: String): Ref? = retrieveValueOrNull(this.getRef(columnLabel))

/**
 * ResultSet으로부터 RowId 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return RowId 값 또는 null
 */
fun ResultSet.getRowIdOrNull(columnIndex: Int): RowId? = retrieveValueOrNull(this.getRowId(columnIndex))

/**
 * ResultSet으로부터 RowId 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return RowId 값 또는 null
 */
fun ResultSet.getRowIdOrNull(columnLabel: String): RowId? = retrieveValueOrNull(this.getRowId(columnLabel))

/**
 * ResultSet으로부터 URL 값을 가져옵니다.
 * 해당 columnIndex가 SQL NULL이면 null을 반환합니다.
 *
 * @param columnIndex 컬럼 인덱스 (1부터 시작)
 * @return URL 값 또는 null
 */
fun ResultSet.getURLOrNull(columnIndex: Int): URL? = retrieveValueOrNull(this.getURL(columnIndex))

/**
 * ResultSet으로부터 URL 값을 가져옵니다.
 * 해당 columnLabel이 SQL NULL이면 null을 반환합니다.
 *
 * @param columnLabel 컬럼 레이블 (이름)
 * @return URL 값 또는 null
 */
fun ResultSet.getURLOrNull(columnLabel: String): URL? = retrieveValueOrNull(this.getURL(columnLabel))
