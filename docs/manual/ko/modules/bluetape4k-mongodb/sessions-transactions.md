---
title: 세션과 트랜잭션
description: withClientSession과 inTransaction의 session 전달, commit·abort·close 순서, topology와 cancellation 경계를 설명합니다.
manualId: bluetape4k-mongodb
chapterId: sessions-transactions
---

# 세션과 트랜잭션

## 세션만 필요한 경우

`withClientSession`은 `startSession()`으로 session을 만들고 suspend block에 전달한 뒤 `finally`에서 닫습니다. session을 시작한다고 transaction이 자동으로 시작되지는 않습니다.

```kotlin
val result = client.withClientSession { session ->
    orders.find(session, Filters.eq("orderId", orderId)).firstOrNull()
}
```

collection helper인 `findFirst`, `exists`, `upsert`, `findAsFlow`에는 session parameter가 없습니다. causal consistency나 transaction session이 필요하면 driver의 session overload를 직접 호출합니다.

## `inTransaction`의 정상 경로

`inTransaction`은 session을 열고 `startTransaction`을 호출한 뒤 block을 실행합니다. block이 정상 종료하면 `commitTransaction()`을 기다리고 block 결과를 반환합니다. 그 뒤 `withClientSession`의 `finally`가 session을 닫습니다.

```kotlin
val receipt = client.inTransaction { session ->
    orders.insertOne(session, order)
    inventory.updateOne(
        session,
        Filters.eq("sku", order.sku),
        Updates.inc("stock", -1),
    )
    Receipt(order.id)
}
```

모든 operation에 같은 `ClientSession`을 전달해야 같은 transaction에 들어갑니다. session 없는 overload를 호출하면 해당 operation은 transaction 밖에서 실행됩니다.

## `TransactionOptions`

options가 없으면 `session.startTransaction()`을 사용하고, 값이 있으면 `startTransaction(transactionOptions)`를 호출합니다. read concern, write concern과 read preference를 업무 일관성 요구와 cluster 설정에 맞춰 정합니다.

helper는 transient transaction error를 판별해 전체 block을 재시도하지 않습니다. 재시도에는 block의 외부 side effect와 idempotency를 함께 고려해야 하므로 application boundary에서 명시적으로 설계합니다.

## 예외와 abort

block이나 commit이 `Exception`을 던지면 `abortTransaction()`을 시도하고 원래 예외를 다시 던집니다. abort도 실패하면 abort 예외를 원래 예외의 suppressed list에 추가하고 log를 남깁니다. 마지막에는 session을 닫습니다.

취소는 별도의 `CancellationException` catch에서 처리하므로 cancellation signal 자체는 삼키지 않습니다. 다만 1.12.1은 이미 취소된 coroutine에서 `abortTransaction()`을 직접 호출하며 `NonCancellable` context로 감싸지 않습니다. 따라서 abort가 suspend 중 취소될 수 있는 경로까지 cleanup이 완료된다고 단정하지 않습니다.

## topology 조건

MongoDB transaction은 replica set 또는 sharded cluster가 필요합니다. 단순 standalone Testcontainer에서 transaction test를 실행하면 server topology 때문에 실패할 수 있습니다. transaction을 사용하는 서비스는 production과 같은 topology 특성을 가진 fixture에서 commit, abort와 retry policy를 검증합니다.

## 외부 side effect

transaction block 안에서 HTTP 호출, message publish나 file write를 수행해도 MongoDB transaction이 함께 rollback하지는 않습니다. database commit과 외부 publish를 묶어야 한다면 outbox 같은 명시적인 pattern을 사용합니다.

## 관찰할 항목

transaction duration, commit·abort 실패, retry label, write concern timeout과 pool wait를 분리해 봅니다. exception log에는 suppressed abort failure도 포함해 cleanup 실패를 놓치지 않습니다.

## Source와 tests

- [`MongoClientExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt)
- [`MongoClientSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)

1.12.1 test는 `withClientSession`의 block 실행과 예외 재전파를 확인하지만 `inTransaction`의 commit·abort·cancellation을 직접 검증하지 않습니다. 이 영역은 application 통합 테스트가 보완해야 합니다.
