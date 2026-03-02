package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

/**
 * Postgres 는 `CREATE DATABASE`, `DROP DATABASE` 같은 작업 시 `autoCommit` 이 `true` 여야 합니다.
 *
 * ## 동작/계약
 * - 현재 연결의 `autoCommit` 값을 [autoCommit]으로 바꿔 [statement]를 실행합니다.
 * - 블록에서 예외가 발생해도 `finally`에서 원래 autoCommit 값으로 복원합니다.
 *
 * ```kotlin
 * withAutoCommit(true) {
 *     // DDL 실행
 * }
 * // autoCommit 은 원래 값으로 복원됨
 * ```
 */
suspend fun R2dbcTransaction.withAutoCommit(
    autoCommit: Boolean = true,
    statement: suspend R2dbcTransaction.() -> Unit,
) {
    val conn = this.connection()
    val originalAutoCommit = conn.getAutoCommit()
    conn.setAutoCommit(autoCommit)
    try {
        statement()
    } finally {
        conn.setAutoCommit(originalAutoCommit)
    }
}
