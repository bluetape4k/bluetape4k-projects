package io.bluetape4k.jdbc.sql

import java.sql.Connection
import java.sql.SQLException

/**
 * Executes [block] inside a JDBC transaction.
 *
 * The connection's `autoCommit`, isolation level, and read-only flag are restored
 * independently to their original values. Any [Throwable] from the block or
 * commit path triggers rollback and is rethrown unchanged. Rollback or restore
 * failures are attached as suppressed exceptions to the primary failure.
 *
 * If the block and commit succeed but state restoration fails, the restore
 * failure is thrown instead of returning a successful result.
 *
 * ```kotlin
 * dataSource.withTransaction { conn ->
 *     conn.executeUpdate("INSERT INTO users (name) VALUES ('Alice')")
 *     conn.executeUpdate("INSERT INTO logs (message) VALUES ('User created')")
 *     // Commits automatically when all operations succeed.
 * }
 * ```
 *
 * @param T result type.
 * @param isolationLevel transaction isolation level to use while running [block].
 * @param block code to execute in the transaction.
 * @return result returned by [block].
 * @throws SQLException when JDBC transaction, rollback, or restore operations fail.
 */
inline fun <T> Connection.withTransaction(
    isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
    block: (Connection) -> T,
): T {
    val originalAutoCommit = this.autoCommit
    val originalIsolationLevel = this.transactionIsolation
    val originalReadOnly = this.isReadOnly
    var primaryFailure: Throwable? = null

    return try {
        this.autoCommit = false
        this.transactionIsolation = isolationLevel

        val result = block(this)
        this.commit()
        result
    } catch (e: Throwable) {
        primaryFailure = e
        try {
            this.rollback()
        } catch (rollbackEx: Throwable) {
            e.addSuppressed(rollbackEx)
        }
        throw e
    } finally {
        val restoreFailure =
            restoreTransactionState(
                originalAutoCommit = originalAutoCommit,
                originalIsolationLevel = originalIsolationLevel,
                originalReadOnly = originalReadOnly,
            )

        if (restoreFailure != null) {
            primaryFailure?.addSuppressed(restoreFailure) ?: throw restoreFailure
        }
    }
}

/**
 * Executes [block] inside a read-only JDBC transaction.
 *
 * The connection's original read-only flag is restored by [withTransaction], so
 * callers that provide an already read-only connection keep that state after the
 * transaction finishes.
 *
 * ```kotlin
 * val users = dataSource.withReadOnlyTransaction { conn ->
 *     conn.runQuery("SELECT * FROM users") { rs ->
 *         // ResultSet 처리
 *     }
 * }
 * ```
 *
 * @param T result type.
 * @param isolationLevel transaction isolation level to use while running [block].
 * @param block code to execute in the read-only transaction.
 * @return result returned by [block].
 */
inline fun <T> Connection.withReadOnlyTransaction(
    isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
    block: (Connection) -> T,
): T =
    withTransaction(isolationLevel) { conn ->
        conn.isReadOnly = true
        block(conn)
    }

@PublishedApi
internal fun Connection.restoreTransactionState(
    originalAutoCommit: Boolean,
    originalIsolationLevel: Int,
    originalReadOnly: Boolean,
): Throwable? {
    var failure: Throwable? = null

    fun recordFailure(restoreFailure: Throwable) {
        val current = failure
        if (current == null) {
            failure = restoreFailure
        } else {
            current.addSuppressed(restoreFailure)
        }
    }

    try {
        this.autoCommit = originalAutoCommit
    } catch (e: Throwable) {
        recordFailure(e)
    }

    try {
        this.transactionIsolation = originalIsolationLevel
    } catch (e: Throwable) {
        recordFailure(e)
    }

    try {
        this.isReadOnly = originalReadOnly
    } catch (e: Throwable) {
        recordFailure(e)
    }

    return failure
}

/**
 * 지정된 격리 수준으로 트랜잭션을 실행합니다.
 *
 * ```kotlin
 * // SERIALIZABLE 격리 수준으로 실행
 * val result = conn.withIsolationLevel(Connection.TRANSACTION_SERIALIZABLE) { connection ->
 *     connection.runQuery("SELECT * FROM accounts WHERE id = 1") { rs ->
 *         // ResultSet 처리
 *     }
 * }
 * ```
 *
 * @param T 결과 타입
 * @param level 트랜잭션 격리 수준
 * @param block 실행할 코드 블록
 * @return 블록의 실행 결과
 */
inline fun <T> Connection.withIsolationLevel(
    level: Int,
    block: (Connection) -> T,
): T {
    val originalLevel = this.transactionIsolation
    var primaryFailure: Throwable? = null
    return try {
        this.transactionIsolation = level
        block(this)
    } catch (e: Throwable) {
        primaryFailure = e
        throw e
    } finally {
        restoreConnectionState(primaryFailure) {
            this.transactionIsolation = originalLevel
        }
    }
}

/**
 * Connection의 auto-commit 상태를 임시로 변경하여 작업을 실행합니다.
 *
 * ```kotlin
 * conn.withAutoCommit(false) { connection ->
 *     // auto-commit이 비활성화된 상태에서 작업 수행
 *     connection.executeUpdate("INSERT INTO ...")
 * }
 * ```
 *
 * @param T 결과 타입
 * @param autoCommit auto-commit 설정값
 * @param block 실행할 코드 블록
 * @return 블록의 실행 결과
 */
inline fun <T> Connection.withAutoCommit(
    autoCommit: Boolean,
    block: (Connection) -> T,
): T {
    val originalAutoCommit = this.autoCommit
    var primaryFailure: Throwable? = null
    return try {
        this.autoCommit = autoCommit
        block(this)
    } catch (e: Throwable) {
        primaryFailure = e
        throw e
    } finally {
        restoreConnectionState(primaryFailure) {
            this.autoCommit = originalAutoCommit
        }
    }
}

/**
 * Connection을 읽기 전용 모드로 설정하여 작업을 실행합니다.
 *
 * ```kotlin
 * conn.withReadOnly { connection ->
 *     // 읽기 전용 모드에서 작업 수행
 *     connection.runQuery("SELECT * FROM users") { rs ->
 *         // ResultSet 처리
 *     }
 * }
 * ```
 *
 * @param T 결과 타입
 * @param block 실행할 코드 블록
 * @return 블록의 실행 결과
 */
inline fun <T> Connection.withReadOnly(block: (Connection) -> T): T {
    val originalReadOnly = this.isReadOnly
    var primaryFailure: Throwable? = null
    return try {
        this.isReadOnly = true
        block(this)
    } catch (e: Throwable) {
        primaryFailure = e
        throw e
    } finally {
        restoreConnectionState(primaryFailure) {
            this.isReadOnly = originalReadOnly
        }
    }
}

/**
 * Connection의 holdability를 임시로 변경하여 작업을 실행합니다.
 *
 * ```kotlin
 * conn.withHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT) { connection ->
 *     // 커서가 커밋 시 닫히는 모드에서 작업 수행
 * }
 * ```
 *
 * @param T 결과 타입
 * @param holdability 커서 홀더빌리티 설정값 ([ResultSet.HOLD_CURSORS_OVER_COMMIT] 또는 [ResultSet.CLOSE_CURSORS_AT_COMMIT])
 * @param block 실행할 코드 블록
 * @return 블록의 실행 결과
 */
inline fun <T> Connection.withHoldability(
    holdability: Int,
    block: (Connection) -> T,
): T {
    val originalHoldability = this.holdability
    var primaryFailure: Throwable? = null
    return try {
        this.holdability = holdability
        block(this)
    } catch (e: Throwable) {
        primaryFailure = e
        throw e
    } finally {
        restoreConnectionState(primaryFailure) {
            this.holdability = originalHoldability
        }
    }
}

@PublishedApi
internal inline fun restoreConnectionState(
    primaryFailure: Throwable?,
    restore: () -> Unit,
) {
    try {
        restore()
    } catch (restoreFailure: Throwable) {
        primaryFailure?.addSuppressed(restoreFailure) ?: throw restoreFailure
    }
}
