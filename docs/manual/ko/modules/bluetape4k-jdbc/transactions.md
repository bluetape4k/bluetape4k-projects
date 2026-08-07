---
title: 트랜잭션과 상태 복원
description: commit, rollback, connection 상태 복원과 suppressed exception 처리 계약을 설명합니다.
manualId: bluetape4k-jdbc
chapterId: transactions
---

# 트랜잭션과 상태 복원

## 트랜잭션은 connection 상태를 빌린다

pool이 반환한 `Connection`은 다음 요청에서 다시 쓰일 수 있습니다. 트랜잭션 helper는 commit과 rollback만 처리해서는 안 되고, 작업 전의 `autoCommit`, isolation level, read-only 상태도 돌려놔야 합니다. `withTransaction`은 세 값을 기록한 뒤 transaction을 실행하고 각각 독립적으로 복원합니다.

```kotlin
import io.bluetape4k.jdbc.sql.executeUpdate
import io.bluetape4k.jdbc.sql.withTransaction
import javax.sql.DataSource

fun renameAccount(
    dataSource: DataSource,
    id: Long,
    newName: String,
) {
    dataSource.withTransaction { connection ->
        connection.executeUpdate(
            "UPDATE accounts SET display_name = ? WHERE id = ?",
            newName,
            id,
        )
        connection.executeUpdate(
            "INSERT INTO account_audit(account_id, action) VALUES (?, ?)",
            id,
            "RENAME",
        )
    }
}
```

block과 commit이 성공해야 결과가 반환됩니다. 둘 중 하나라도 `Throwable`을 던지면 rollback을 시도한 뒤 원래 throwable을 그대로 다시 던집니다.

## 상태 전이

| 시점 | `autoCommit` | isolation | read-only |
| --- | --- | --- | --- |
| 진입 전 | 현재 connection 값 | 현재 connection 값 | 현재 connection 값 |
| transaction block | `false` | 인자로 지정한 값, 기본 `READ_COMMITTED` | 기존 값 또는 read-only helper가 `true`로 변경 |
| 정상 종료 | 원래 값으로 복원 | 원래 값으로 복원 | 원래 값으로 복원 |
| 실패 종료 | rollback 뒤 복원 시도 | 각각 독립적으로 복원 시도 | 각각 독립적으로 복원 시도 |

복원 하나가 실패해도 나머지 복원을 계속 시도합니다. pool에 오염된 connection 상태를 되돌릴 기회를 최대한 보존하기 위해서입니다.

## 예외 우선순위

block 실패가 가장 중요한 원인입니다. rollback이나 복원도 실패했다면 원래 throwable의 `suppressed` 목록에 붙습니다. 원래 작업과 commit은 성공했지만 상태 복원만 실패하면 복원 예외가 호출자에게 전달됩니다. 성공으로 처리하면 pool에 잘못된 상태의 connection이 돌아갈 수 있기 때문입니다.

```kotlin
try {
    dataSource.withTransaction { connection ->
        // work
    }
} catch (failure: Throwable) {
    logger.error("JDBC transaction failed", failure)
    failure.suppressed.forEach { suppressed ->
        logger.warn("Rollback or state restoration also failed", suppressed)
    }
    throw failure
}
```

실제 서비스에서 모든 계층이 같은 예외를 중복 기록하지 않도록 logging 책임은 최상위 operation boundary에 둡니다.

## Read-only와 임시 상태 helper

`withReadOnlyTransaction`은 transaction 안에서 `isReadOnly = true`로 설정하고, 종료 시 진입 전 값을 복원합니다. read-only는 database와 driver가 최적화 힌트나 쓰기 제한으로 해석할 수 있지만, 모든 database에서 동일한 보안 경계는 아닙니다.

`withIsolationLevel`, `withAutoCommit`, `withReadOnly`, `withHoldability`는 한 속성을 block 동안 바꿨다가 `finally`에서 돌려놓습니다. 이 helper들은 commit이나 rollback을 수행하지 않습니다. transaction이 필요하면 `withTransaction`을 사용합니다.

1.12.1에서 이 단일 속성 helper들은 block 실패와 복원 실패가 동시에 발생했을 때 복원 예외가 원래 예외를 가릴 수 있습니다. rollback과 suppressed exception 보존이 필요한 transaction 작업에는 `withTransaction`을 사용하고, 단일 속성 helper를 쓰는 호출 경로에서도 복원 실패를 별도 failure mode로 테스트합니다.

## DataSource wrapper와 같은 connection

`DataSource.withTransaction`은 connection 하나를 빌린 뒤 `Connection.withTransaction`에 넘깁니다. transaction 안에서는 반드시 block parameter인 connection으로 모든 SQL을 실행합니다.

```kotlin
dataSource.withTransaction { connection ->
    connection.executeUpdate(firstSql, firstValue)
    connection.executeUpdate(secondSql, secondValue)
}
```

block 안에서 다시 `dataSource.executeUpdate(...)`를 부르면 새 connection을 얻을 수 있으며 현재 transaction에 참여한다고 보장할 수 없습니다.

## Framework transaction과의 경계

Spring transaction manager나 Exposed transaction이 이미 경계를 소유한다면 그 framework의 connection binding과 propagation 규칙을 따릅니다. 이 helper를 중첩하는 것만으로 두 transaction model이 결합되지는 않습니다. 동일한 database operation에 transaction owner는 하나만 두는 편이 안전합니다.

## Source와 tests

- [`TransactionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensions.kt)
- [`DataSourceTransactionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt)
- [`TransactionExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensionsTest.kt)
- [`DataSourceTransactionExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensionsTest.kt)

## 다음 읽을 장

저수준 JDBC 경계를 이해했다면 [JDBC 다음의 기술 선택](./ecosystem-paths.md)에서 그대로 유지할지 Exposed나 Hibernate로 발전시킬지 결정합니다.
