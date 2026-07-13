---
title: R2DBC 생태계 학습 경로
description: 직접 Spring R2DBC에서 bluetape4k helper, Spring coroutine 확장, Exposed R2DBC와 workshop으로 발전하는 순서를 설명합니다.
manualId: bluetape4k-r2dbc
chapterId: ecosystem-paths
---

# R2DBC 생태계 학습 경로

## 한 단계씩 올라간다

R2DBC를 처음 도입할 때부터 가장 높은 추상화를 선택할 필요는 없습니다. connection과 transaction 경계를 먼저 이해한 다음, 반복되는 문제에 맞춰 helper, coroutine CRUD, table DSL과 repository를 추가합니다.

| 단계 | 선택 | 배우고 해결하는 것 |
| --- | --- | --- |
| 1 | Spring `DatabaseClient` | publisher 실행, parameter binding, driver exception과 transaction context |
| 2 | `bluetape4k-r2dbc` | Kotlin typed mapping, CRUD helper, pool DSL, suspend transaction, query builder |
| 3 | [`bluetape4k-spring-boot-r2dbc`](../bluetape4k-spring-boot-r2dbc.md) | `R2dbcEntityOperations`의 coroutine CRUD와 Flow repository 패턴 |
| 4 | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | Exposed table·column DSL, coroutine-native repository, cache와 Spring/Ktor 연동 |
| 5 | [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop) | DDL/DML, transaction, WebFlux, multi-tenancy, routing과 production integration 실습 |

이 흐름은 이전 단계를 버리는 migration 순서가 아닙니다. 상위 계층도 결국 R2DBC driver, connection factory와 transaction 위에서 실행되므로 문제를 진단할 때 아래 계층의 지식이 필요합니다.

## Spring coroutine 확장이 필요한 경우

Spring Data의 entity operation을 유지하면서 `findOne`, `select`, `insert`, `update`, `delete`, `count`를 일관된 suspend·Flow API로 사용하고 싶다면 `bluetape4k-spring-boot-r2dbc`가 자연스럽습니다. base module은 SQL과 pool을 더 직접적으로 다루고, Spring Boot 모듈은 entity operation coroutine adapter에 집중합니다.

## Exposed R2DBC로 올라가는 경우

다음 문제가 반복되면 Exposed를 검토합니다.

- table과 column 이름을 여러 query에서 문자열로 반복한다.
- 동적 조건과 join을 Kotlin DSL로 구성하고 싶다.
- repository 규칙과 cache decorator를 공유하고 싶다.
- Spring WebFlux와 Ktor에서 같은 coroutine-native persistence model을 사용하고 싶다.

`bluetape4k-exposed`는 R2DBC repository와 suspend transaction, cache backend, column codec와 framework 연동을 모듈별로 제공합니다. 필요한 runtime과 기능만 선택하고 버전은 중앙 `bluetape4k-dependencies` BOM으로 정렬합니다.

## JDBC와 비교하는 기준

[`bluetape4k-jdbc`](../bluetape4k-jdbc.md)는 blocking driver 위에서 동작하며 단순한 adapter와 vendor SQL에는 여전히 적합합니다. R2DBC를 선택하는 이유는 coroutine 문법 자체가 아니라 요청당 thread를 점유하지 않는 driver와 backpressure-aware 실행 모델이 필요하기 때문입니다.

| 질문 | JDBC 신호 | R2DBC 신호 |
| --- | --- | --- |
| 기존 driver·도구 자산 | JDBC 중심 | R2DBC driver와 framework 지원 확인 |
| 호출 모델 | blocking service·batch | WebFlux/Ktor coroutine과 많은 동시 I/O |
| transaction·ORM 선택 | JDBC/JPA 생태계 | Spring R2DBC 또는 Exposed R2DBC |
| 운영 병목 | thread와 pool 모두 관찰 | connection pool, publisher cancellation, pending acquire 관찰 |

## workshop에서 따라갈 순서

Exposed R2DBC Workshop은 SQL DSL 기초→connection/DDL→DML/transaction→coroutine/Flow→Spring repository→multi-tenant와 routing→production integration 순서로 구성되어 있습니다. 각 장의 테스트를 먼저 실행하고, H2에서 확인한 뒤 PostgreSQL·MySQL Testcontainers 경로로 검증 범위를 넓힙니다.

## 관련 모듈과 저장소

- [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- [`bluetape4k-spring-boot-r2dbc`](../bluetape4k-spring-boot-r2dbc.md)
- [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md)
- [`bluetape4k-jdbc`](../bluetape4k-jdbc.md)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop)
- [bluetape4k 생태계 지도](https://bluetape4k.github.io/ko/ecosystem/atlas/)
