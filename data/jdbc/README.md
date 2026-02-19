# Module bluetape4k-jdbc

JDBC(Java Database Connectivity) 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다.
Kotlin의 힘을 활용하여 타입 안전하고 간결한 데이터베이스 코드를 작성할 수 있습니다.

## 특징

- **타입 안전한 ResultSet 처리**: Nullable 확장 함수 제공
- **간결한 Connection 관리**: `use` 패턴과 DSL 지원
- **트랜잭션 지원**: 선언적 트랜잭션 관리
- **배치 처리**: 대량 데이터 삽입 지원
- **객체 매핑**: ResultSet을 객체로 쉽게 변환

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-jdbc:${version}")
    // 사용하는 데이터베이스 드라이버 추가 (예: H2, MySQL, PostgreSQL 등)
    implementation("com.h2database:h2:${h2Version}")
}
```

## 주요 기능

### 1. DataSource/Connection 관리

DataSource에서 Connection을 획득하고 작업을 수행합니다.

```kotlin
import io.bluetape4k.jdbc.sql.*
import javax.sql.DataSource

// DataSource 생성 (예: HikariCP, Apache DBCP 등)
val dataSource: DataSource = createDataSource()

// Connection 획득 및 사용
dataSource.withConnect { conn ->
    // Connection 사용
    val result = conn.runQuery("SELECT * FROM users") { rs ->
        // ResultSet 처리
    }
}
```

### 2. Statement 실행

간편한 Statement 생성과 실행:

```kotlin
// Statement 생성 및 사용
dataSource.withStatement { stmt ->
    val rs = stmt.executeQuery("SELECT * FROM users")
    // ResultSet 처리
}

// Connection에서 직접 사용
dataSource.connection.use { conn ->
    conn.withStatement { stmt ->
        stmt.executeUpdate("INSERT INTO users (name) VALUES ('Alice')")
    }
}
```

### 3. ResultSet 처리

타입 안전한 ResultSet 조회:

```kotlin
dataSource.runQuery("SELECT * FROM users") { rs ->
    val users = mutableListOf<User>()
    while (rs.next()) {
        users.add(
            User(
                id = rs.getLongOrNull("id"),
                name = rs.getStringOrNull("name"),
                age = rs.getIntOrNull("age")
            )
        )
    }
    users
}

// 컬럼명으로 접근
val name: String? = rs.getStringOrNull("name")
val age: Int? = rs.getIntOrNull("age")

// 인덱스로 접근
val firstColumn: String? = rs.getStringOrNull(1)
```

### 4. 인덱스/레이블 기반 조회 연산자

편리한 연산자 오버로딩:

```kotlin
dataSource.runQuery("SELECT id, name FROM users") { rs ->
    while (rs.next()) {
        val id = rs["id"] as? Long    // 레이블로 접근
        val name = rs[2] as? String   // 인덱스로 접근 (1부터 시작)
        println("User: $id - $name")
    }
}
```

### 5. 객체 매핑

ResultSet을 객체로 쉽게 변환:

```kotlin
data class User(val id: Int, val name: String, val email: String)

// 첫 번째 행 매핑
val user = dataSource.runQuery("SELECT * FROM users WHERE id = 1") { rs ->
    rs.mapFirst { row ->
        User(row.getInt("id"), row.getString("name"), row.getString("email"))
    }
}

// 단일 행 매핑 (결과가 0개 또는 2개 이상이면 예외)
val singleUser = dataSource.runQuery("SELECT * FROM users WHERE id = 1") { rs ->
    rs.mapSingle { row ->
        User(row.getInt("id"), row.getString("name"), row.getString("email"))
    }
}

// 리스트로 변환
val users = dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.toList { row ->
        User(row.getInt("id"), row.getString("name"), row.getString("email"))
    }
}

// Set으로 변환
val uniqueNames = dataSource.runQuery("SELECT name FROM users") { rs ->
    rs.toSet { it.getString("name") }
}

// Map으로 변환
val userMap = dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.toMap(
        keyMapper = { it.getInt("id") },
        valueMapper = { User(it.getInt("id"), it.getString("name"), it.getString("email")) }
    )
}

// 그룹화
val usersByStatus = dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.groupBy(
        keyMapper = { it.getString("status") },
        valueMapper = { User(it.getInt("id"), it.getString("name"), it.getString("email")) }
    )
}
```

### 6. ResultSet 순회

Iterator 및 Sequence 지원:

```kotlin
// Iterator 사용
val rs = statement.executeQuery("SELECT * FROM users")
for (row in rs) {
    println(row.getString("name"))
}

// Sequence 사용
val users = dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.sequence { row ->
        User(row.getInt("id"), row.getString("name"), row.getString("email"))
    }.toList()
}

// forEach 사용
dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.forEach { row ->
        println("User: ${row.getString("name")}")
    }
}

// forEachIndexed 사용
dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.forEachIndexed { index, row ->
        println("$index: ${row.getString("name")}")
    }
}
```

### 7. PreparedStatement 지원

PreparedStatement 생성 및 파라미터 바인딩:

```kotlin
// 파라미터가 있는 쿼리 실행
dataSource.withConnect { conn ->
    conn.executeQuery(
        "SELECT * FROM users WHERE age > ? AND status = ?",
        18, "active"
    ) { rs ->
        // ResultSet 처리
    }
}

// 업데이트 실행
val affectedRows = dataSource.withConnect { conn ->
    conn.executeUpdate(
        "UPDATE users SET name = ? WHERE id = ?",
        "New Name", 123
    )
}

// 생성된 키 반환
val generatedId = dataSource.withConnect { conn ->
    conn.executeUpdateWithGeneratedKeys(
        "INSERT INTO users (name, email) VALUES (?, ?)",
        "John Doe", "john@example.com"
    ) { rs ->
        if (rs.next()) rs.getLong(1) else null
    }
}

// DSL 스타일
dataSource.withConnect { conn ->
    conn.preparedStatement("SELECT * FROM users WHERE id = ?") { stmt ->
        stmt.setLong(1, userId)
        stmt.executeQuery().use { rs ->
            // ResultSet 처리
        }
    }
}
```

### 8. 배치 처리

대량 데이터 삽입:

```kotlin
// 배치 INSERT
val paramsList = listOf(
    listOf("User1", "user1@example.com"),
    listOf("User2", "user2@example.com"),
    listOf("User3", "user3@example.com")
)

val results = dataSource.withConnect { conn ->
    conn.executeBatch(
        "INSERT INTO users (name, email) VALUES (?, ?)",
        paramsList,
        batchSize = 100
    )
}

// 대량 배치 (Long 반환)
val largeResults = dataSource.withConnect { conn ->
    conn.executeLargeBatch(
        "INSERT INTO users (name, email) VALUES (?, ?)",
        paramsList,
        batchSize = 1000
    )
}

// DataSource에서 직접 실행
val batchResults = dataSource.executeBatch(
    "INSERT INTO users (name, email) VALUES (?, ?)",
    paramsList
)
```

### 9. 트랜잭션 관리

선언적 트랜잭션 관리:

```kotlin
// 기본 트랜잭션
dataSource.withTransaction { conn ->
    conn.executeUpdate("INSERT INTO accounts (user_id, balance) VALUES (?, ?)", 1, 1000)
    conn.executeUpdate("INSERT INTO logs (message) VALUES (?)", "Account created")
    // 자동으로 커밋됨
}

// 읽기 전용 트랜잭션
dataSource.withReadOnlyTransaction { conn ->
    conn.runQuery("SELECT * FROM users") { rs ->
        // 읽기 작업만 수행
    }
}

// 격리 수준 지정
dataSource.withTransaction(Connection.TRANSACTION_SERIALIZABLE) { conn ->
    conn.runQuery("SELECT * FROM accounts WHERE id = 1 FOR UPDATE") { rs ->
        // 직렬화 가능한 격리 수준으로 조회
    }
}

// Connection에서 직접 사용
dataSource.connection.use { conn ->
    conn.withTransaction { connection ->
        connection.executeUpdate("INSERT INTO users (name) VALUES (?)", "Alice")
        connection.executeUpdate("INSERT INTO users (name) VALUES (?)", "Bob")
        // 자동으로 커밋
    }
}

// 롤백 예시
try {
    dataSource.withTransaction { conn ->
        conn.executeUpdate("INSERT INTO users (name) VALUES (?)", "Temp User")
        throw RuntimeException("Something went wrong")
        // 예외 발생 시 자동 롤백
    }
} catch (e: Exception) {
    // 롤백됨
}
```

### 10. Connection 속성 임시 변경

Connection의 속성을 임시로 변경하여 작업:

```kotlin
dataSource.connection.use { conn ->
    // Auto-commit 임시 변경
    conn.withAutoCommit(false) { connection ->
        // auto-commit이 비활성화된 상태에서 작업
    }

    // 읽기 전용 모드
    conn.withReadOnly { connection ->
        // 읽기 전용 모드에서 작업
    }

    // 격리 수준 임시 변경
    conn.withIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED) { connection ->
        // 지정된 격리 수준에서 작업
    }

    // Holdability 임시 변경
    conn.withHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT) { connection ->
        // 지정된 holdability로 작업
    }
}
```

### 11. ResultSetGetColumnTokens

token 기반 타입 안전한 값 조회:

```kotlin
dataSource.runQuery("SELECT * FROM users") { rs ->
    rs.extract {
        User(
            id = long["id"]!!,
            name = string["name"]!!,
            age = int["age"],
            createdAt = timestamp["created_at"],
            active = boolean["is_active"] ?: false
        )
    }
}
```

### 12. 단일 값 조회

집계 쿼리 등에서 단일 값 조회:

```kotlin
// Int 조회
val count = dataSource.runQuery("SELECT COUNT(*) FROM users") { rs ->
    rs.singleInt()
}

// Long 조회
val maxId = dataSource.runQuery("SELECT MAX(id) FROM users") { rs ->
    rs.singleLong()
}

// Double 조회
val avgAge = dataSource.runQuery("SELECT AVG(age) FROM users") { rs ->
    rs.singleDouble()
}

// String 조회
val name = dataSource.runQuery("SELECT name FROM users WHERE id = 1") { rs ->
    rs.singleString()
}

// BigDecimal 조회
val totalAmount = dataSource.runQuery("SELECT SUM(amount) FROM orders") { rs ->
    rs.singleBigDecimal()
}
```

### 13. ResultSet 메타데이터

```kotlin
dataSource.runQuery("SELECT * FROM users") { rs ->
    // 컬럼명 목록
    val columns = rs.columnNames
    println(columns) // ["id", "name", "email", ...]

    // 컬럼 레이블(별칭) 목록
    val labels = rs.columnLabels

    // 컬럼 수
    val columnCount = rs.columnCount
}
```

### 14. 필터링 및 검색

```kotlin
dataSource.runQuery("SELECT * FROM users") { rs ->
    // all: 모든 행이 조건 만족
    val allAdults = rs.all { it.getInt("age") >= 18 }

    // any: 하나라도 조건 만족
    val hasAdmin = rs.any { it.getString("role") == "admin" }

    // none: 조건 만족하는 행 없음
    val noInactive = rs.none { it.getString("status") == "inactive" }

    // filterMap: 조건에 맞는 행만 매핑
    val adultUsers = rs.filterMap(
        predicate = { it.getInt("age") >= 18 },
        mapper = { User(it.getInt("id"), it.getString("name"), it.getString("email")) }
    )

    // firstOrNull: 조건 만족하는 첫 행
    val firstAdmin = rs.firstOrNull(
        predicate = { it.getString("role") == "admin" },
        mapper = { User(it.getInt("id"), it.getString("name"), it.getString("email")) }
    )
}
```

### 15. 유틸리티 함수

```kotlin
// isEmpty / isNotEmpty
dataSource.runQuery("SELECT * FROM users WHERE 1=0") { rs ->
    rs.isEmpty()    // true
    rs.isNotEmpty() // false
}

// count
dataSource.runQuery("SELECT * FROM users") { rs ->
    val total = rs.count()
    val adults = rs.count { it.getInt("age") >= 18 }
}
```

## 테스트

모듈은 H2 데이터베이스를 사용한 테스트를 포함하고 있습니다.

```kotlin
class MyJdbcTest : AbstractJdbcTest() {
    @Test
    fun `사용자 조회 테스트`() {
        val user = dataSource.executeQuery(
            "SELECT * FROM users WHERE name = ?",
            "Alice"
        ) { rs ->
            rs.mapFirst { row ->
                User(row.getInt("id"), row.getString("name"), row.getString("email"))
            }
        }

        user.shouldNotBeNull()
        user.name shouldBeEqualTo "Alice"
    }
}
```

## 참고 자료

- [JDBC 공식 문서](https://docs.oracle.com/javase/tutorial/jdbc/)
- [Kotlin Use 함수](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html)
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 고성능 JDBC 커넥션 풀

## 라이선스

Apache License 2.0
