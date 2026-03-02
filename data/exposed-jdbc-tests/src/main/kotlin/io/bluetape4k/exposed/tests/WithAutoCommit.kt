package io.bluetape4k.exposed.tests

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction


/**
 * Postgres 는 `CREATE DATABASE`, `DROP DATABASE` 같은 작업 시 `autoCommit` 이 `true` 여야 합니다.
 *
 * ## 동작/계약
 * - 현재 커넥션의 `autoCommit` 값을 [autoCommit]으로 바꿔 [statement]를 실행합니다.
 * - 블록에서 예외가 발생해도 `finally`에서 원래 `autoCommit` 값을 복원합니다.
 *
 * ```kotlin
 * withAutoCommit(true) {
 *     // DDL 실행
 * }
 * // connection.autoCommit 은 원래 값으로 복원됨
 * ```
 */
fun JdbcTransaction.withAutoCommit(
    autoCommit: Boolean = true,
    statement: JdbcTransaction.() -> Unit,
) {
    val originalAutoCommit = connection.autoCommit
    connection.autoCommit = autoCommit
    try {
        statement()
    } finally {
        connection.autoCommit = originalAutoCommit
    }
}
