package io.bluetape4k.exposed.tests

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Postgres 는 `CREATE DATABASE`, `DROP DATABASE` 같은 작업 시 `autoCommit` 이 `true` 여야 합니다.
 */
@Deprecated(
    message = "Use withAutoCommitSuspending() instead.",
    replaceWith = ReplaceWith(
        "withAutoCommitSuspending(autoCommit, statement)",
        "io.bluetape4k.exposed.tests.withAutoCommitSuspending"
    )
)
suspend fun JdbcTransaction.withSuspendedAutoCommit(
    autoCommit: Boolean = true,
    statement: suspend JdbcTransaction.() -> Unit,
) {
    withAutoCommitSuspending(autoCommit, statement)
}

/**
 * Postgres 는 `CREATE DATABASE`, `DROP DATABASE` 같은 작업 시 `autoCommit` 이 `true` 여야 합니다.
 *
 * ## 동작/계약
 * - 현재 커넥션의 `autoCommit` 값을 [autoCommit]으로 설정하고 suspend [statement]를 실행합니다.
 * - 블록 중 예외가 발생해도 `finally`에서 원래 `autoCommit` 값을 복원합니다.
 *
 * ```kotlin
 * withAutoCommitSuspending(true) {
 *     // suspend DDL/검증 실행
 * }
 * // connection.autoCommit 은 원래 값으로 복원됨
 * ```
 */
suspend fun JdbcTransaction.withAutoCommitSuspending(
    autoCommit: Boolean = true,
    statement: suspend JdbcTransaction.() -> Unit,
) {
    val originalAutoCommit = connection.autoCommit
    connection.autoCommit = autoCommit
    try {
        statement()
    } finally {
        connection.autoCommit = originalAutoCommit
    }
}
