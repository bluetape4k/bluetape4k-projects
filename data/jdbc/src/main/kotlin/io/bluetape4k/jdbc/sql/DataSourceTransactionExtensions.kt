package io.bluetape4k.jdbc.sql

import java.sql.Connection

/**
 * DataSource에서 Connection을 얻어 트랜잭션을 실행합니다.
 *
 * 이 함수는 자동으로 Connection을 획득하고 트랜잭션을 시작합니다.
 * 블록 실행이 성공하면 커밋하고, 예외가 발생하면 롤백한 후 Connection을 닫습니다.
 *
 * ```kotlin
 * val result = dataSource.withTransaction { conn ->
 *     conn.executeUpdate("INSERT INTO users (name) VALUES ('Alice')")
 *     conn.executeUpdate("INSERT INTO logs (message) VALUES ('User created')")
 *     // 모든 작업이 성공하면 자동으로 커밋됩니다.
 *     42  // 결과 반환
 * }
 * ```
 *
 * @param T 결과 타입
 * @param isolationLevel 트랜잭션 격리 수준 (기본값: [Connection.TRANSACTION_READ_COMMITTED])
 * @param block 트랜잭션 내에서 실행할 코드 블록
 * @return 블록의 실행 결과
 * @throws Exception 트랜잭션 실행 중 오류 발생 시
 */
inline fun <T> javax.sql.DataSource.withTransaction(
    isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
    crossinline block: (Connection) -> T,
): T =
    withConnect { conn ->
        conn.withTransaction(isolationLevel) { block(it) }
    }

/**
 * DataSource에서 Connection을 얻어 읽기 전용 트랜잭션을 실행합니다.
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
inline fun <T> javax.sql.DataSource.withReadOnlyTransaction(
    isolationLevel: Int = Connection.TRANSACTION_READ_COMMITTED,
    crossinline block: (Connection) -> T,
): T =
    withConnect { conn ->
        conn.withReadOnlyTransaction(isolationLevel) { block(it) }
    }

/**
 * DataSource에서 Connection을 얻어 쿼리를 실행하고 결과를 매핑합니다.
 *
 * ```kotlin
 * val users = dataSource.executeQuery(
 *     "SELECT * FROM users WHERE age > ?",
 *     18
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
inline fun <T> javax.sql.DataSource.executeQuery(
    sql: String,
    vararg params: Any?,
    crossinline mapper: (java.sql.ResultSet) -> T,
): T =
    withConnect { conn ->
        conn.executeQuery(sql, *params, mapper = mapper)
    }

/**
 * DataSource에서 Connection을 얻어 업데이트를 실행합니다.
 *
 * ```kotlin
 * val affectedRows = dataSource.executeUpdate(
 *     "UPDATE users SET name = ? WHERE id = ?",
 *     "John Doe", 123
 * )
 * ```
 *
 * @param sql 실행할 SQL 쿼리
 * @param params 쿼리 파라미터들
 * @return 영향받은 행 수
 */
fun javax.sql.DataSource.executeUpdate(
    sql: String,
    vararg params: Any?,
): Int =
    withConnect { conn ->
        conn.executeUpdate(sql, *params)
    }

/**
 * DataSource에서 Connection을 얻어 업데이트를 실행하고 생성된 키를 반환합니다.
 *
 * ```kotlin
 * val generatedId = dataSource.executeUpdateWithGeneratedKeys(
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
inline fun <T> javax.sql.DataSource.executeUpdateWithGeneratedKeys(
    sql: String,
    vararg params: Any?,
    crossinline keyMapper: (java.sql.ResultSet) -> T,
): T? =
    withConnect { conn ->
        conn.executeUpdateWithGeneratedKeys(sql, *params, keyMapper = keyMapper)
    }

/**
 * DataSource에서 Connection을 얻어 배치 업데이트를 실행합니다.
 *
 * ```kotlin
 * val paramsList = listOf(
 *     listOf("Alice", "alice@example.com"),
 *     listOf("Bob", "bob@example.com"),
 *     listOf("Charlie", "charlie@example.com")
 * )
 *
 * val results = dataSource.executeBatch(
 *     "INSERT INTO users (name, email) VALUES (?, ?)",
 *     paramsList,
 *     batchSize = 100
 * )
 * ```
 *
 * @param sql 실행할 SQL 쿼리
 * @param paramsList 각 배치 항목의 파라미터 리스트들
 * @param batchSize 배치 크기 (기본값: 1000)
 * @return 각 배치 실행의 결과 배열들의 리스트
 */
fun javax.sql.DataSource.executeBatch(
    sql: String,
    paramsList: List<List<Any?>>,
    batchSize: Int = 1000,
): List<IntArray> =
    withConnect { conn ->
        conn.executeBatch(sql, paramsList, batchSize)
    }

/**
 * DataSource에서 Connection을 얻어 대량 배치 업데이트를 실행합니다.
 *
 * ```kotlin
 * val paramsList = (1..10000).map { i ->
 *     listOf("User $i", "user$i@example.com")
 * }
 *
 * val results = dataSource.executeLargeBatch(
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
fun javax.sql.DataSource.executeLargeBatch(
    sql: String,
    paramsList: List<List<Any?>>,
    batchSize: Int = 1000,
): LongArray =
    withConnect { conn ->
        conn.executeLargeBatch(sql, paramsList, batchSize)
    }
