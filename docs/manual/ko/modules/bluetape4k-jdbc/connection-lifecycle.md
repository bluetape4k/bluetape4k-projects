---
title: Connection과 DataSource 수명주기
description: DataSource에서 connection을 빌리고 닫는 범위와 HikariCP helper의 책임을 설명합니다.
manualId: bluetape4k-jdbc
chapterId: connection-lifecycle
---

# Connection과 DataSource 수명주기

## 먼저 소유자를 정한다

JDBC helper를 쓰기 전에 `DataSource`, pool, `Connection`의 소유자를 구분해야 합니다. 애플리케이션은 `DataSource`와 pool의 시작·종료를 관리하고, `withConnect`는 한 작업에서 빌린 `Connection`만 닫습니다. `withConnect` 안에서 얻은 connection을 반환하거나 다른 thread에 넘기면 블록이 끝난 뒤 닫힌 자원을 사용하게 됩니다.

```kotlin
import io.bluetape4k.jdbc.sql.withConnect
import javax.sql.DataSource

fun ping(dataSource: DataSource): Boolean =
    dataSource.withConnect { connection ->
        connection.prepareStatement("SELECT 1").use { statement ->
            statement.executeQuery().use { rs ->
                rs.next()
            }
        }
    }
```

`withConnect`는 `DataSource.connection`이 `null`이면 `IllegalStateException`을 던지고, 정상 획득한 connection은 `use`로 닫습니다. statement와 `ResultSet`은 각각 만든 코드가 `use`로 닫습니다.

## helper가 닫아 주는 범위

| API | 생성·대여하는 자원 | 종료 범위 |
| --- | --- | --- |
| `DataSource.withConnect` | `Connection` | block 종료 시 connection close |
| `DataSource.withStatement` | `Connection`, `Statement` | 중첩된 block 종료 순서대로 close |
| `DataSource.runQuery` | `Connection`, `Statement`, `ResultSet` | mapper가 반환하기 전에 모두 소비해야 함 |
| `Connection.preparedStatement` | `PreparedStatement` | block 종료 시 statement close; connection은 호출자가 소유 |
| `hikariDataSourceOf` | `HikariDataSource` | helper는 생성만 함; 애플리케이션이 close |

`Connection` 확장과 `DataSource` 확장을 섞을 때는 누가 connection을 닫는지 이름만 보고 추측하지 않습니다. `DataSource` 확장은 대개 한 번 빌린 connection을 block과 함께 닫고, `Connection` 확장은 이미 받은 connection의 소유권을 바꾸지 않습니다.

## HikariCP 구성

HikariCP는 optional `compileOnly` dependency입니다. helper를 사용하려면 애플리케이션 runtime에 HikariCP를 추가해야 합니다.

```kotlin
import io.bluetape4k.jdbc.hikari.hikariDataSourceOf

val dataSource = hikariDataSourceOf(
    jdbcUrl = "jdbc:postgresql://db.example.com:5432/app",
    username = "app",
    password = databasePassword,
) {
    poolName = "app-pool"
    maximumPoolSize = 16
    connectionTimeout = 3_000
}

try {
    // application work
} finally {
    dataSource.close()
}
```

`hikariConfigOf`와 `hikariDataSourceOf`는 Hikari 설정을 Kotlin lambda로 묶을 뿐입니다. 적절한 pool 크기나 timeout을 계산해 주지 않으며, password 관리나 shutdown hook도 설치하지 않습니다.

## Spring이 connection을 소유할 때

Spring의 transaction manager는 현재 transaction에 묶인 connection을 관리할 수 있습니다. 그런 흐름에서 별도의 `DataSource.withConnect`나 `withTransaction`을 호출하면 같은 service 안에 서로 다른 connection 또는 transaction 경계가 생길 수 있습니다. Spring 관리 transaction 안에서는 Spring JDBC나 framework가 제공한 connection 접근 경계를 우선하고, 이 모듈의 저수준 mapping·statement helper만 필요한 범위에서 사용합니다.

## 운영 기준

- pool은 애플리케이션 시작 시 만들고 종료 시 한 번 닫습니다.
- 요청마다 `HikariDataSource`를 만들지 않습니다.
- `withConnect` 밖으로 `Connection`, `Statement`, `ResultSet`, 지연 `Sequence`를 반환하지 않습니다.
- connection 획득 시간, active/idle connection, timeout과 leak detection을 pool metric으로 관찰합니다.
- driver와 pool dependency는 중앙 BOM과 애플리케이션 dependency graph에서 함께 확인합니다.

## Source와 tests

- [`DataSourceExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt)
- [`ConnectionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt)
- [`HikariSupport.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/hikari/HikariSupport.kt)
- [`DataSourceSupportTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceSupportTest.kt)
- [`HikariSupportTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/hikari/HikariSupportTest.kt)

## 다음 읽을 장

자원 소유권을 정했다면 [Prepared statement와 batch](./statements-batches.md)에서 SQL 값 binding과 대량 쓰기 계약을 확인합니다.
