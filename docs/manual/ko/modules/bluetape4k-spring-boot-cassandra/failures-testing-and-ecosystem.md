---
title: "실패, 테스트와 생태계 경로"
description: "오류·cancellation·관측 경계를 정하고 가벼운 단위 검증에서 실제 Cassandra 검증으로 확장합니다."
---

# 실패, 테스트와 생태계 경로

## 실패를 세 층으로 나눈다

운영에서 보이는 Cassandra 실패를 한 예외로 뭉치지 않습니다.

1. **호출 계약 오류**: 잘못된 CQL, bind marker 개수·타입, row mapping과 nullable 계약
2. **분산 시스템 오류**: unavailable, timeout, overload, schema disagreement와 연결 실패
3. **조건 미적용**: LWT 요청은 성공했지만 `wasApplied()`가 false인 상태

Coroutine adapter는 이 구분을 새 예외로 바꾸지 않습니다. 안정된 domain 오류가 필요하면 repository/service 경계에서 원인과 재시도 가능성을 보존해 변환합니다.

## Cancellation과 timeout

Reactive subscription이나 future 대기가 취소돼도 이미 cluster에 제출된 쿼리의 최종 상태는 별도입니다. 특히 write timeout 뒤에는 적용 여부가 불확실할 수 있습니다. driver idempotence와 retry policy, LWT, 조회 확인 절차를 함께 설계합니다.

`CancellationException`을 일반 backend 실패로 기록하거나 retry하지 않습니다. timeout을 늘리기 전에 query shape, partition 크기, pool과 coordinator 상태를 확인합니다.

## 관측 책임

이 artifact에는 health indicator, `ObservationRegistry`, meter bean이나 auto-configuration이 없습니다. 애플리케이션이 Spring Boot Actuator와 driver Micrometer integration을 추가합니다.

관찰할 항목은 다음과 같습니다.

- session 연결과 available node 수
- request latency와 timeout 비율
- unavailable, overloaded, authentication·schema 오류
- connection pool in-flight와 queue
- LWT latency와 미적용 비율
- Flow batch에 들어가는 item 수와 메모리 사용량

query 원문이나 bind 값을 무분별하게 metric label로 넣지 않습니다. 높은 cardinality와 개인정보 노출을 피합니다.

## 가장 작은 테스트부터 시작하기

Mock 기반 unit test는 coroutine adapter가 올바른 Spring Data method를 호출하고 결과·예외를 전달하는지 빠르게 확인합니다. `OptionsSupportTest`와 `AbstractCassandraModelTest`는 Cassandra 없이 option statement와 model 계약을 검증합니다.

```bash
./gradlew :bluetape4k-spring-boot-cassandra:test \
  --tests '*UnitTest' \
  --tests '*OptionsSupportTest' \
  --tests '*AbstractCassandraModelTest'
```

실제 mapping, schema, optimistic locking과 query는 Cassandra가 필요합니다.

```bash
./gradlew :bluetape4k-spring-boot-cassandra:test --no-configuration-cache
```

이 suite는 `CassandraServer.Launcher.cassandra4`를 시작합니다. 다른 Testcontainers·native·emulator 검증과 순차 실행합니다.

## 테스트 fixture에서 배울 것

- `ReactiveSessionCoroutinesExamples`: execute와 prepare의 최소 호출
- `ReactiveCassandraTemplateTest`: reactive CRUD와 slice
- `AsyncCassandraTemplateTest`: async+suspend CRUD와 options
- `AsyncOptimisticLockingTest`: version 증가와 stale entity 실패
- `CassandraTypeMappingTest`: Spring Data converter와 Cassandra type mapping
- `SchemaGeneratorTest`: table 존재 확인과 생성 경계

테스트 configuration의 공유 `CqlSession`은 단순 최적화가 아닙니다. application context마다 session을 새로 만들면 connection이 누적되고 `AllNodesFailedException`으로 이어질 수 있습니다.

## 생태계 학습 경로

다음 단계는 필요한 추상화 수준에 따라 고릅니다.

- driver statement, paging과 CQL helper: [`bluetape4k-cassandra`](../bluetape4k-cassandra.md)
- coroutine cancellation과 Flow 기본 규칙: [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- Spring context와 공통 실행 경계: [`bluetape4k-spring-boot-core`](../bluetape4k-spring-boot-core.md)
- relational 비동기 접근과 비교: [`bluetape4k-spring-boot-r2dbc`](../bluetape4k-spring-boot-r2dbc.md)

Cassandra는 repository 모양이 비슷해도 관계형 DB와 query·transaction 모델이 다릅니다. 데이터 모델을 access pattern에서 시작하고, coroutine은 그 위의 호출 표현으로 사용합니다.

## 근거

- [`AsyncCassandraOperationsCoroutinesUnitTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutinesUnitTest.kt)
- [`AsyncOptimisticLockingTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/async/AsyncOptimisticLockingTest.kt)
- [`CassandraTypeMappingTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/convert/CassandraTypeMappingTest.kt)
- [`AbstractCassandraTestConfiguration.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AbstractCassandraTestConfiguration.kt)
