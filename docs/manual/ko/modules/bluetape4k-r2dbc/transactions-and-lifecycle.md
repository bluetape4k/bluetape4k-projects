---
title: Transaction과 lifecycle
description: suspend transaction의 commit·rollback, transaction-aware connection, schema 초기화와 auto-configuration 경계를 설명합니다.
manualId: bluetape4k-r2dbc
chapterId: transactions-and-lifecycle
---

# Transaction과 lifecycle

## 가장 작은 원자적 경계

`withTransactionSuspend`는 `DatabaseClient.connectionFactory`에 대응하는 `R2dbcTransactionManager`와 `TransactionalOperator`를 만들고 suspend block을 실행합니다. block이 성공하면 commit하고 예외가 발생하면 rollback합니다.

```kotlin
val result = client.databaseClient.withTransactionSuspend {
    client.databaseClient
        .sql("INSERT INTO accounts (owner, balance) VALUES (:owner, :balance)")
        .bind("owner", "kim")
        .bind("balance", 1000)
        .fetch()
        .awaitRowsUpdated()

    client.databaseClient
        .sql("INSERT INTO audit_log (message) VALUES (:message)")
        .bind("message", "account created")
        .fetch()
        .awaitRowsUpdated()

    "created"
}
```

block parameter로 전달되는 `ReactiveTransaction`은 rollback-only 같은 transaction 상태를 다룰 때 사용합니다. 여러 write가 함께 성공해야 하는 가장 작은 service operation에 경계를 둡니다.

## transaction manager cache

1.12.1은 같은 `ConnectionFactory`에 대해 transaction manager를 반복 생성하지 않도록 lock으로 보호한 `WeakHashMap`에 보관합니다. factory가 더 이상 참조되지 않으면 cache entry도 GC 대상이 됩니다. 이 cache가 pool lifecycle을 대신 관리하는 것은 아닙니다.

`withTransactionSuspending`은 deprecated alias입니다. 새 코드는 `withTransactionSuspend`를 사용합니다.

## connection을 직접 다룰 때

`getConnectionAndAwait`와 `releaseConnectionAndAwait`는 Spring의 transaction-bound connection을 인식합니다. 반면 `fetchConnectionAndAwait`는 `ConnectionFactory.create()`를 직접 호출하므로 호출자가 connection을 닫아야 하며, 현재 transaction connection과 같다고 가정하면 안 됩니다.

저수준 connection 접근이 꼭 필요하지 않다면 transaction 안에서도 같은 `DatabaseClient`를 사용해 Spring binding을 유지합니다.

## schema와 data 초기화

`resourceDatabasePopulatorOf`는 SQL resource를 populator로 만들고, `compositeDatabasePopulatorOf`는 schema와 data 같은 여러 populator를 순서대로 묶습니다. `connectionFactoryInitializer`가 이를 factory lifecycle에 연결합니다.

```kotlin
val initializer = connectionFactoryInitializer(connectionFactory) {
    setDatabasePopulator(
        compositeDatabasePopulatorOf(
            resourceDatabasePopulatorOf(ClassPathResource("schema.sql")),
            resourceDatabasePopulatorOf(ClassPathResource("data.sql")),
        )
    )
}
```

운영 schema migration을 이 helper와 별도 migration 도구가 동시에 소유하지 않게 합니다. 하나를 source of truth로 정하고 initializer 실행 시점과 재실행 안전성을 테스트합니다.

## 1.12.1 auto-configuration

`R2dbcClientAutoConfiguration`은 `DatabaseClient` class가 있을 때 `DatabaseClient`, `R2dbcEntityTemplate`, `MappingR2dbcConverter` bean으로 `R2dbcClient`를 등록합니다. 1.12.1에는 `@ConditionalOnMissingBean(R2dbcClient::class)`가 없습니다. 사용자 bean을 함께 선언하면 자동으로 back-off한다고 가정하지 말고 auto-configuration exclusion이나 bean 구성을 명시적으로 정리합니다.

## 실패 처리

transaction block의 exception과 cancellation을 삼키면 commit/rollback 의미가 달라질 수 있습니다. domain exception으로 바꾸더라도 원래 cause를 보존하고, transient retry는 전체 transaction이 idempotent할 때만 경계 밖에서 수행합니다.

## Source와 tests

- [`TransactionSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/TransactionSupport.kt)
- [`ConnectionFactoryUtils.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/ConnectionFactoryUtils.kt)
- [`R2dbcClientAutoConfiguration.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfiguration.kt)
- [`ConnectionFactoryInitializer.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionFactoryInitializer.kt)
- [`TransactionSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/TransactionSupportTest.kt)
- [`ConnectionInitTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionInitTest.kt)

## 다음 읽을 장

저수준 경계를 익혔다면 [R2DBC 생태계 학습 경로](./ecosystem-paths.md)에서 필요한 추상화 수준을 선택합니다.
