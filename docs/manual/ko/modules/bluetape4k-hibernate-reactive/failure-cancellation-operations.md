---
title: 실패·취소·운영
description: Reactive session의 예외와 취소 의미를 과장하지 않고 운영 관측 지점까지 연결합니다.
manualId: bluetape4k-hibernate-reactive
chapterId: failure-cancellation-operations
---

# 실패·취소·운영

## 예외 전파

session block에서 던진 예외는 `Uni` 또는 `CompletionStage` 실패로 연결된 뒤 suspend 호출자에게 전달됩니다. 테스트는 `IllegalStateException`과 `RuntimeException`의 타입이 보존되는지 확인합니다. query 문법, mapping, named graph, lock 실패도 provider 예외로 전달되며 extension은 이를 domain exception으로 바꾸지 않습니다.

transaction block의 실패 처리는 Hibernate Reactive `withTransaction`에 위임됩니다. retry는 transaction 밖의 application policy로 두고, 멱등성과 재실행 비용을 확인한 경우에만 적용합니다.

## 취소가 보장하는 범위

Mutiny와 Stage wrapper는 모든 callback에서 `CancellationException`을 잡아 그대로 다시 던집니다. 이는 coroutine→`Uni` 또는 coroutine→`CompletableFuture` 경계에서 취소를 일반 오류로 바꾸지 않기 위한 처리입니다.

그러나 `1.12.1` 테스트에는 실행 중인 driver query가 즉시 취소되는지 검증하는 사례가 없습니다. 취소는 coroutine 협력 계약이며, 이미 DB 서버로 전달된 SQL의 중단 시점까지 보장하지 않습니다. query timeout과 database-side timeout을 별도로 둡니다.

## Event loop를 막지 않기

session block은 `currentVertxDispatcher()`에서 실행됩니다. 다음 작업을 block 안에서 직접 실행하지 않습니다.

- JDBC 또는 blocking ORM 호출
- 동기 파일·네트워크 I/O
- 긴 압축·암호화·대량 변환
- `Thread.sleep`과 blocking lock

필요하다면 transaction 밖으로 데이터를 꺼낸 뒤 별도 dispatcher에서 처리합니다.

## 운영 관측표

| 신호 | 함께 볼 원인 |
| --- | --- |
| event-loop delay | blocking 호출, 긴 callback, 과도한 mapping |
| pool acquire 지연 | pool 크기, 긴 transaction, database 부하 |
| query latency | 실행 계획, lazy fetch, lock wait, network |
| rollback 증가 | validation 실패, 충돌, timeout, 취소 |
| shutdown 지연 | 열린 factory·session, 완료되지 않은 request |

테스트의 `drop-and-create`, pool size `30`은 fixture 설정일 뿐 운영 권장값이 아닙니다. 서비스의 instance 수, DB connection 한도와 latency budget으로 값을 정합니다.

## 실행 근거

- [Mutiny coroutine bridge](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [Stage coroutine bridge](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [`MutinyExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)
- [`StageExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageExtrasTest.kt)
- [`MySQLLauncher.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/MySQLLauncher.kt)
