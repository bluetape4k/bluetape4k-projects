package io.bluetape4k.jdbc.sql

/**
 * [java.sql.ResultSet]으로부터 Column 값을 얻기 위한 코드를 담는 클래스입니다.
 */
class GetColumnToken<out T: Any>(
    val withColumnLabel: (String) -> T?,
    val withColumnIndex: (Int) -> T?,
) {
    /**
     * 해당 컬럼명의 정보를 가져옵니다.
     * @param columnLabel 데이터를 가져올 컬럼 명
     */
    operator fun get(columnLabel: String): T? = withColumnLabel(columnLabel)

    /**
     * 해당 컬럼 인덱스의 정보를 가져옵니다.
     * @param columnIndex 데이터를 가져올 컬럼 인덱스
     */
    operator fun get(columnIndex: Int): T? = withColumnIndex(columnIndex)
}
