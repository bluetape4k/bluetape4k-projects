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
