package io.bluetape4k.jdbc.sql

import java.sql.PreparedStatement

/**
 * [PreparedStatement]의 인자값을 설정합니다.
 *
 * ```
 * val ps = connection.preparedStatement("update test_bean set createdAt=?")
 * ps.arguments {
 *      date[1] = Date()  // 첫번째 인자를 Date 형식으로 설정합니다. (setDate(value))
 *      string[2] = "Hello"  // 두번째 인자를 String 형식으로 설정합니다. (setString(value))
 *      long[3] = 100L  // 세번째 인자를 Long 형식으로 설정합니다. (setLong(value))
 *      timestamp[4] = Timestamp()  // 네번째 인자를 Timestamp 형식으로 설정합니다. (setTimestamp(value))
 * }
 * ps.executeUpdate()
 * ```
 *
 * @param body [PreparedStatementArgumentSetter]를 이용한 인자 값을 설정을 수행하는 코드 블럭
 * @return [PreparedStatement] instance
 */
inline fun PreparedStatement.arguments(body: PreparedStatementArgumentSetter.() -> Unit): PreparedStatement =
    apply { PreparedStatementArgumentSetter(this).body() }
