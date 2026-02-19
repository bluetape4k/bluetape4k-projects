package io.bluetape4k.jdbc.sql

import java.sql.Connection
import java.sql.SQLException

/**
 * 데이터베이스 트랜잭션을 실행합니다.
 *
 * 이 함수는 자동으로 트랜잭션을 시작하고, 블록 실행이 성공하면 커밋하고,
 * 예외가 발생하면 롤백합니다. 마지막에는 Connection을 자동으로 닫습니다.
 *
 * ```kotlin
 * dataSource.withTransaction { conn ->
 *     conn.executeUpdate("INSERT INTO users (name) VALUES ('Alice')")
 *     conn.executeUpdate("INSERT INTO logs (message) VALUES ('User created')")
 *     // 모든 작업이 성공하면 자동으로 커밋됩니다.
 * }
 * ```
 *
 * @param T 결과 타입
 * @param isolationLevel 트랜잭션 격리 수준 (기본값: [Connection.TRANSACTION_READ_COMMITTED])
 * @param block 트랜잭션 내에서 실행할 코드 블록
 * @return 블록의 실행 결과
 * @throws SQLException 트랜잭션 실행 중 오류 발생 시
 */
inline fun <T> Connection.withTransaction(
    isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
    block: (Connection) -> T,
): T {
    val originalAutoCommit = this.autoCommit
    val originalIsolationLevel = this.transactionIsolation

    return try {
        this.autoCommit = false
        this.transactionIsolation = isolationLevel

        val result = block(this)
        this.commit()
        result
    } catch (e: Exception) {
        try {
            this.rollback()
        } catch (rollbackEx: SQLException) {
            // 롤백 실패 시 원래 예외에 추가 정보로 기록
            e.addSuppressed(rollbackEx)
        }
        throw e
    } finally {
        // 원래 상태로 복원
        try {
            this.autoCommit = originalAutoCommit
            this.transactionIsolation = originalIsolationLevel
        } catch (e: SQLException) {
            // 상태 복원 실패는 무시
        }
    }
}

/**
 * 읽기 전용 트랜잭션을 실행합니다.
 *
 * 이 함수는 읽기 작업에 최적화된 설정으로 트랜잭션을 실행합니다.
 *
 * ```kotlin
 * val users = dataSource.withReadOnlyTransaction { conn ->
 *     conn.runQuery("SELECT * FROM users") { rs ->
 *         // ResultSet 처리
 *     }
 * }
 * ```
 *
 * @param T 결과 타입
 * @param isolationLevel 트랜잭션 격리 수준 (기본값: [Connection.TRANSACTION_READ_COMMITTED])
 * @param block 트랜잭션 내에서 실행할 코드 블록
 * @return 블록의 실행 결과
 */
inline fun <T> Connection.withReadOnlyTransaction(
    isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
    block: (Connection) -> T,
): T =
    withTransaction(isolationLevel) { conn ->
        conn.isReadOnly = true
        try {
            block(conn)
        } finally {
            conn.isReadOnly = false
        }
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
    return try {
        this.transactionIsolation = level
        block(this)
    } finally {
        this.transactionIsolation = originalLevel
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
    return try {
        this.autoCommit = autoCommit
        block(this)
    } finally {
        this.autoCommit = originalAutoCommit
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
    return try {
        this.isReadOnly = true
        block(this)
    } finally {
        this.isReadOnly = originalReadOnly
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
    return try {
        this.holdability = holdability
        block(this)
    } finally {
        this.holdability = originalHoldability
    }
}
