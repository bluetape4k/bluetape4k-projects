package io.bluetape4k.r2dbc.core

/**
 * SQL 식별자(테이블명, 컬럼명 등)의 유효성을 검증하기 위한 정규식.
 *
 * 영문자, 숫자, 언더스코어, 점(스키마.테이블 표기)만 허용합니다.
 */
private val SQL_IDENTIFIER_PATTERN = Regex("^[A-Za-z_][A-Za-z0-9_.]*$")

/**
 * SQL 식별자가 유효한지 검사하고, 유효하면 그대로 반환합니다.
 *
 * 알파벳, 숫자, 언더스코어(`_`), 점(`.`) 만 허용합니다.
 * SQL injection을 방지하기 위해 table/column/where 식별자를 SQL 문에 보간하기 전에 반드시 호출해야 합니다.
 *
 * @param name 검사할 SQL 식별자 (테이블명, 컬럼명 등)
 * @return 유효한 식별자 문자열
 * @throws IllegalArgumentException 허용되지 않는 문자가 포함된 경우
 */
internal fun requireValidIdentifier(name: String): String {
    require(SQL_IDENTIFIER_PATTERN.matches(name)) {
        "Invalid SQL identifier: '$name'. Only alphanumeric characters, underscores, and dots are allowed."
    }
    return name
}
