package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

/**
 * Postgres 는 `CREATE DATABASE`, `DROP DATABASE` 같은 작업 시 `autoCommit` 이 `true` 여야 합니다.
 */
suspend fun R2dbcTransaction.withAutoCommit(
    autoCommit: Boolean = true,
    statement: suspend R2dbcTransaction.() -> Unit,
) {
    val originalAutoCommit = this.connection.getAutoCommit()
    this.connection.setAutoCommit(autoCommit)
    try {
        statement()
    } finally {
        this.connection.setAutoCommit(originalAutoCommit)
    }
}
