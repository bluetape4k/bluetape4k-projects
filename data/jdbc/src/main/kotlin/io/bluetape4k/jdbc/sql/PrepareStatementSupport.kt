package io.bluetape4k.jdbc.sql

import java.sql.PreparedStatement

/**
 * [PreparedStatement]에 인자를 타입 안전 DSL로 바인딩합니다.
 *
 * ## 동작/계약
 * - [PreparedStatementArgumentSetter]를 생성해 [body]를 즉시 실행합니다.
 * - 수신 [PreparedStatement]를 직접 mutate 하며 반환값은 동일 인스턴스입니다.
 * - 인덱스 오류/타입 불일치 등 JDBC 예외는 setter 호출 시 그대로 전파됩니다.
 *
 * ```kotlin
 * val updated = conn.prepareStatement("update actors set lastname=? where id=?").arguments {
 *   string[1] = "BAE"
 *   long[2] = 1L
 * }.executeUpdate()
 * // updated == 1
 * ```
 */
inline fun PreparedStatement.arguments(body: PreparedStatementArgumentSetter.() -> Unit): PreparedStatement =
    apply { PreparedStatementArgumentSetter(this).body() }
