---
title: Prepared statement와 batch
description: SQL parameter binding, 생성 key 처리, batch row 크기와 실행 단위를 설명합니다.
manualId: bluetape4k-jdbc
chapterId: statements-batches
---

# Prepared statement와 batch

## 값은 SQL 구조와 분리한다

외부에서 받은 값을 SQL 문자열에 보간하지 않습니다. `Connection.executeQuery`와 `executeUpdate`의 parameter overload는 prepared statement를 만들고, 각 값을 1부터 시작하는 JDBC parameter index에 `setObject`로 바인딩합니다. statement와 결과는 block이 끝나기 전에 닫힙니다.

```kotlin
import io.bluetape4k.jdbc.sql.executeQuery
import io.bluetape4k.jdbc.sql.mapFirst
import javax.sql.DataSource

fun findDisplayName(dataSource: DataSource, accountId: Long): String? =
    dataSource.executeQuery(
        "SELECT display_name FROM accounts WHERE id = ?",
        accountId,
    ) { rs ->
        rs.mapFirst { it.getString("display_name") }
    }
```

SQL identifier나 `ORDER BY` 방향은 parameter로 바인딩할 수 없습니다. 허용 목록으로 선택한 SQL 조각만 조립하고, 사용자 입력을 그대로 identifier 위치에 넣지 않습니다.

## 생성 key

INSERT 뒤 생성된 key가 필요하면 `executeUpdateWithGeneratedKeys`를 사용합니다. mapper는 generated-keys `ResultSet`이 열린 동안 값을 꺼내야 합니다.

```kotlin
val accountId: Long? = dataSource.executeUpdateWithGeneratedKeys(
    "INSERT INTO accounts(display_name) VALUES (?)",
    "Ada",
) { keys ->
    if (keys.next()) keys.getLong(1) else null
}
```

driver마다 generated-key 지원과 반환 column이 다를 수 있습니다. production database driver로 통합 테스트하고, key가 반드시 있어야 한다면 nullable 결과를 service 경계에서 명시적으로 검증합니다.

## 직접 binding이 필요할 때

driver 전용 setter나 stream·LOB처럼 `setObject`보다 구체적인 binding이 필요하면 `preparedStatement` block을 사용합니다.

```kotlin
dataSource.withConnect { connection ->
    connection.preparedStatement(
        "SELECT id FROM accounts WHERE status = ? AND created_at >= ?",
    ) { statement ->
        statement.setString(1, "ACTIVE")
        statement.setTimestamp(2, cutoff)

        statement.executeQuery().use { rs ->
            rs.toList { it.getLong("id") }
        }
    }
}
```

이 block은 prepared statement를 닫지만 connection은 바깥 `withConnect`가 닫습니다.

## Batch row 계약

`executeBatch(sql, paramsList, batchSize)`는 parameter row를 여러 JDBC batch로 나눕니다. 1.12.1은 statement를 만들기 전에 모든 row의 parameter 수가 같은지 검사합니다. 짧은 row가 이전 row의 binding을 재사용하는 일을 막기 위한 계약입니다.

```kotlin
val rows = listOf(
    listOf("Ada", "ACTIVE"),
    listOf("Grace", "ACTIVE"),
    listOf("Linus", "INACTIVE"),
)

val results: List<IntArray> = dataSource.executeBatch(
    "INSERT INTO accounts(display_name, status) VALUES (?, ?)",
    rows,
    batchSize = 500,
)
```

두 번째 row에 값이 하나뿐이면 statement 실행 전에 `IllegalArgumentException`이 발생합니다. 이 검증은 SQL placeholder 수와 row 크기가 일치하는지까지 알아내지는 못합니다. 그 부분은 driver가 실행 시 검증합니다.

1.12.1은 `batchSize`가 양수인지 먼저 검사하지 않습니다. `0`이나 음수를 넘겼을 때 안정된 validation exception을 기대하지 말고, 애플리케이션 설정을 읽는 경계에서 양수로 제한합니다.

`executeLargeBatch`는 각 실행 결과를 합친 `LongArray`를 반환합니다. `executeBatch`는 실제로 실행한 batch마다 `IntArray`를 담은 목록을 반환하므로, 부분 실패 분석이나 chunk 단위 관찰이 필요할 때 구분해서 사용합니다.

## 트랜잭션과 함께 쓰기

여러 batch가 하나의 업무 단위라면 `withTransaction` 안에서 같은 connection으로 실행합니다. `DataSource.executeBatch`를 여러 번 부르면 호출마다 새 connection을 빌리므로 하나의 transaction이라고 볼 수 없습니다.

```kotlin
dataSource.withTransaction { connection ->
    connection.executeBatch(accountSql, accountRows, batchSize = 500)
    connection.executeBatch(auditSql, auditRows, batchSize = 500)
}
```

batch 크기는 메모리, driver buffer, network packet 제한과 lock 시간을 함께 고려합니다. 더 크다고 항상 빠르지 않으므로 production과 비슷한 payload로 측정합니다.

## Source와 tests

- [`PreparedStatementExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensions.kt)
- [`PrepareStatementSupport.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt)
- [`PreparedStatementArgumentSetter.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementArgumentSetter.kt)
- [`DataSourceTransactionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt)
- [`PreparedStatementExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensionsTest.kt)
- [`DataSourceTransactionExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensionsTest.kt)

## 다음 읽을 장

쿼리를 안전하게 실행했다면 [ResultSet 읽기와 mapping](./resultset-mapping.md)에서 cursor 소비 규칙과 반환 타입을 정합니다.
