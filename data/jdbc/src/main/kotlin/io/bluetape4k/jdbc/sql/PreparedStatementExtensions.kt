package io.bluetape4k.jdbc.sql

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * SQL 쿼리를 PreparedStatement로 실행하고 결과를 매핑합니다.
 *
 * 이 함수는 PreparedStatement를 생성하고, 파라미터를 바인딩한 후 쿼리를 실행합니다.
 * 결과는 제공된 매퍼 함수를 통해 변환됩니다.
 *
 * ```kotlin
 * val users = connection.executeQuery(
 *     "SELECT * FROM users WHERE age > ? AND status = ?",
 *     18, "active"
 * ) { rs ->
 *     val users = mutableListOf<User>()
 *     while (rs.next()) {
 *         users.add(User(rs.getInt("id"), rs.getString("name")))
 *     }
 *     users
 * }
 * ```
 *
 * @param T 결과 타입
 * @param sql 실행할 SQL 쿼리
 * @param params 쿼리 파라미터들
 * @param mapper ResultSet을 결과 타입으로 매핑하는 함수
 * @return 매핑된 결과
 */
inline fun <T> java.sql.Connection.executeQuery(
    sql: String,
    vararg params: Any?,
    crossinline mapper: (ResultSet) -> T,
): T =
    prepareStatement(sql).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject(index + 1, param)
        }
        stmt.executeQuery().use(mapper)
    }

/**
 * SQL 업데이트를 PreparedStatement로 실행합니다.
 *
 * ```kotlin
 * val affectedRows = connection.executeUpdate(
 *     "UPDATE users SET name = ? WHERE id = ?",
 *     "John Doe", 123
 * )
 * ```
 *
 * @param sql 실행할 SQL 쿼리
 * @param params 쿼리 파라미터들
 * @return 영향받은 행 수
 */
fun java.sql.Connection.executeUpdate(
    sql: String,
    vararg params: Any?,
): Int =
    prepareStatement(sql).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject(index + 1, param)
        }
        stmt.executeUpdate()
    }

/**
 * SQL 업데이트를 실행하고 생성된 키를 반환합니다.
 *
 * ```kotlin
 * val generatedId = connection.executeUpdateWithGeneratedKeys(
 *     "INSERT INTO users (name, email) VALUES (?, ?)",
 *     "John Doe", "john@example.com"
 * ) { rs ->
 *     if (rs.next()) rs.getLong(1) else null
 * }
 * ```
 *
 * @param T 생성된 키의 타입
 * @param sql 실행할 SQL 쿼리
 * @param params 쿼리 파라미터들
 * @param keyMapper 생성된 키 ResultSet을 매핑하는 함수
 * @return 생성된 키 또는 null
 */
inline fun <T> java.sql.Connection.executeUpdateWithGeneratedKeys(
    sql: String,
    vararg params: Any?,
    crossinline keyMapper: (ResultSet) -> T,
): T? =
    prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
        params.forEachIndexed { index, param ->
            stmt.setObject(index + 1, param)
        }
        stmt.executeUpdate()
        stmt.generatedKeys.use(keyMapper)
    }

/**
 * SQL 업데이트를 PreparedStatement로 배치 실행합니다.
 *
 * ```kotlin
 * val batchSize = 1000
 * val paramsList = (1..10000).map { i -> listOf("User $i", "user$i@example.com") }
 *
 * val results = connection.executeBatch(
 *     "INSERT INTO users (name, email) VALUES (?, ?)",
 *     paramsList,
 *     batchSize
 * )
 * ```
 *
 * @param sql 실행할 SQL 쿼리
 * @param paramsList 각 배치 항목의 파라미터 리스트들
 * @param batchSize 배치 크기 (기본값: 1000)
 * @return 각 배치 실행의 결과 배열들의 리스트
 */
fun java.sql.Connection.executeBatch(
    sql: String,
    paramsList: List<List<Any?>>,
    batchSize: Int = 1000,
): List<IntArray> {
    if (paramsList.isEmpty()) return emptyList()

    return prepareStatement(sql).use { stmt ->
        val results = mutableListOf<IntArray>()

        paramsList.forEachIndexed { index, params ->
            params.forEachIndexed { paramIndex, param ->
                stmt.setObject(paramIndex + 1, param)
            }
            stmt.addBatch()

            // 배치 크기에 도달하면 실행
            if ((index + 1) % batchSize == 0) {
                results.add(stmt.executeBatch())
                stmt.clearBatch()
            }
        }

        // 남은 배치 실행
        if (paramsList.size % batchSize != 0) {
            results.add(stmt.executeBatch())
        }

        results
    }
}

/**
 * SQL 업데이트를 PreparedStatement로 대량 실행합니다.
 *
 * 이 함수는 executeBatch를 래핑하여 단일 결과 배열을 반환합니다.
 *
 * ```kotlin
 * val paramsList = listOf(
 *     listOf("Alice", "alice@example.com"),
 *     listOf("Bob", "bob@example.com"),
 *     listOf("Charlie", "charlie@example.com")
 * )
 *
 * val results = connection.executeLargeBatch(
 *     "INSERT INTO users (name, email) VALUES (?, ?)",
 *     paramsList
 * )
 * ```
 *
 * @param sql 실행할 SQL 쿼리
 * @param paramsList 각 배치 항목의 파라미터 리스트들
 * @param batchSize 배치 크기 (기본값: 1000)
 * @return 모든 배치 실행의 결과를 합친 배열
 */
fun java.sql.Connection.executeLargeBatch(
    sql: String,
    paramsList: List<List<Any?>>,
    batchSize: Int = 1000,
): LongArray {
    if (paramsList.isEmpty()) return LongArray(0)

    return prepareStatement(sql).use { stmt ->
        val results = mutableListOf<Long>()

        paramsList.forEachIndexed { index, params ->
            params.forEachIndexed { paramIndex, param ->
                stmt.setObject(paramIndex + 1, param)
            }
            stmt.addBatch()

            if ((index + 1) % batchSize == 0) {
                stmt.executeLargeBatch().forEach { results.add(it) }
                stmt.clearBatch()
            }
        }

        if (paramsList.size % batchSize != 0) {
            stmt.executeLargeBatch().forEach { results.add(it) }
        }

        results.toLongArray()
    }
}

/**
 * PreparedStatement를 생성하고 파라미터를 설정한 후 실행하는 DSL 스타일 함수입니다.
 *
 * ```kotlin
 * connection.preparedStatement("SELECT * FROM users WHERE id = ?") { stmt ->
 *     stmt.setLong(1, userId)
 *     stmt.executeQuery().use { rs ->
 *         // ResultSet 처리
 *     }
 * }
 * ```
 *
 * @param T 결과 타입
 * @param sql 실행할 SQL 쿼리
 * @param block PreparedStatement를 사용한 작업 블록
 * @return 작업 결과
 */
inline fun <T> java.sql.Connection.preparedStatement(
    sql: String,
    block: (PreparedStatement) -> T,
): T = prepareStatement(sql).use(block)

/**
 * PreparedStatement를 생성하고 파라미터를 설정한 후 실행하는 DSL 스타일 함수입니다.
 * 생성된 키를 반환하는 쿼리에 사용됩니다.
 *
 * ```kotlin
 * connection.preparedStatement(
 *     "INSERT INTO users (name) VALUES (?)",
 *     Statement.RETURN_GENERATED_KEYS
 * ) { stmt ->
 *     stmt.setString(1, "John")
 *     stmt.executeUpdate()
 *     stmt.generatedKeys.use { rs ->
 *         if (rs.next()) rs.getLong(1) else null
 *     }
 * }
 * ```
 *
 * @param T 결과 타입
 * @param sql 실행할 SQL 쿼리
 * @param autoGeneratedKeys 생성된 키 반환 옵션
 * @param block PreparedStatement를 사용한 작업 블록
 * @return 작업 결과
 */
inline fun <T> java.sql.Connection.preparedStatement(
    sql: String,
    autoGeneratedKeys: Int,
    block: (PreparedStatement) -> T,
): T = prepareStatement(sql, autoGeneratedKeys).use(block)

/**
 * PreparedStatement를 생성하고 파라미터를 설정한 후 실행하는 DSL 스타일 함수입니다.
 * 컬럼 인덱스를 지정하여 생성된 키를 반환하는 쿼리에 사용됩니다.
 *
 * @param T 결과 타입
 * @param sql 실행할 SQL 쿼리
 * @param columnIndexes 생성된 키를 가져올 컬럼 인덱스들
 * @param block PreparedStatement를 사용한 작업 블록
 * @return 작업 결과
 */
inline fun <T> java.sql.Connection.preparedStatement(
    sql: String,
    columnIndexes: IntArray,
    block: (PreparedStatement) -> T,
): T = prepareStatement(sql, columnIndexes).use(block)

/**
 * PreparedStatement를 생성하고 파라미터를 설정한 후 실행하는 DSL 스타일 함수입니다.
 * 컬럼 레이블을 지정하여 생성된 키를 반환하는 쿼리에 사용됩니다.
 *
 * @param T 결과 타입
 * @param sql 실행할 SQL 쿼리
 * @param columnNames 생성된 키를 가져올 컬럼 이름들
 * @param block PreparedStatement를 사용한 작업 블록
 * @return 작업 결과
 */
inline fun <T> java.sql.Connection.preparedStatement(
    sql: String,
    columnNames: Array<String>,
    block: (PreparedStatement) -> T,
): T = prepareStatement(sql, columnNames).use(block)
