---
title: R2DBC 생태계 학습 경로
description: Spring Data coroutine 확장에서 raw R2DBC, JDBC, Exposed R2DBC와 workshop으로 이어지는 선택 기준을 정리합니다.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: ecosystem-paths
---

# R2DBC 생태계 학습 경로

## abstraction 수준부터 고른다

같은 database를 사용해도 필요한 제어 수준에 따라 시작점이 달라집니다.

| 필요한 것 | 권장 시작점 |
| --- | --- |
| Spring Data entity mapping과 coroutine CRUD | `bluetape4k-spring-boot-r2dbc` |
| raw SQL, binding, custom row mapping, connection·transaction helper | [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md) |
| Kotlin table DSL, DDL/DML, repository abstraction | `bluetape4k-exposed` R2DBC |
| blocking driver 생태계와 단순한 transaction model | [`bluetape4k-jdbc`](../bluetape4k-jdbc.md) |
| object graph, dirty checking, persistence context | [`bluetape4k-hibernate`](../bluetape4k-hibernate.md) |

R2DBC가 항상 JDBC보다 낫다는 뜻은 아닙니다. 호출 경로가 실제로 non-blocking이어야 하고, 사용하는 database와 driver가 필요한 기능을 지원해야 합니다.

## 1단계: Spring Data entity operation

이 모듈에서 먼저 익힐 것은 `R2dbcEntityOperations`, `Query`, `Criteria`, `Flow`, suspend cardinality입니다. 내부 `coroutines.blog` 예제를 따라 entity→repository→controller 흐름을 실행합니다.

추천 순서:

1. `PostRepository`의 전체·단건 조회
2. `CommentRepository`의 조건 `Flow`와 count
3. `R2dbcEntityOperationsExtensionsTest`의 CRUD cycle
4. `PostControllerTest`의 WebFlux 경계

## 2단계: core R2DBC로 내려가기

Spring Data entity abstraction으로 표현하기 어려운 join, DTO projection, vendor SQL이 생기면 `bluetape4k-r2dbc`로 내려갑니다. connection pool과 transaction lifecycle, typed null binding, raw SQL mapping은 [core R2DBC 매뉴얼](../bluetape4k-r2dbc.md)에 따로 설명되어 있습니다.

한 애플리케이션에서 두 모듈을 함께 사용할 수 있습니다. 단순 entity CRUD는 이 모듈에 두고, 복잡한 조회만 `R2dbcClient`나 `DatabaseClient`로 구현하면 됩니다.

## 3단계: Exposed R2DBC로 올라가기

SQL을 직접 쓰되 table·column을 Kotlin DSL로 표현하고 repository 패턴을 쌓고 싶다면 [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)의 R2DBC 모듈을 검토합니다. Spring Data mapping과 Exposed table mapping은 서로 다른 model이므로 한 aggregate를 두 persistence model로 중복 소유하지 않게 경계를 정합니다.

[Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop)은 다음 흐름을 실행 예제로 제공합니다.

- table과 schema 정의
- coroutine transaction과 CRUD
- repository와 domain mapping
- Spring WebFlux integration
- 실제 database를 사용한 test와 운영 패턴

## JDBC와 비교

기존 JDBC driver와 library가 충분하고 요청 처리량보다 구현 단순성이 중요하면 JDBC가 더 나을 수 있습니다. virtual thread나 명시적인 `Dispatchers.IO` 경계도 선택지입니다. 반대로 WebFlux부터 driver까지 non-blocking 흐름을 유지하고 많은 concurrent I/O를 처리해야 한다면 R2DBC가 맞습니다.

[JDBC 매뉴얼](../bluetape4k-jdbc.md)은 blocking connection, transaction, statement lifecycle을 설명합니다. 기술 이름이 아니라 application 호출 경로와 driver 성숙도를 기준으로 고릅니다.

## 실전 학습 체크리스트

- `one`, `oneOrNull`, `first`, `Flow`의 cardinality를 테스트로 구분했다.
- update/delete의 반환 행 수를 domain 조건과 비교한다.
- transaction owner와 connection/pool owner를 문서에 적었다.
- H2 test와 운영 database test가 증명하는 범위를 구분했다.
- raw SQL이 필요한 query와 entity operation을 분리했다.
- blocking library를 coroutine 함수 안에서 직접 호출하지 않는다.

## Source와 관련 저장소

- [`spring-boot/r2dbc` 테스트 애플리케이션](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog)
- [`bluetape4k-r2dbc` 매뉴얼](../bluetape4k-r2dbc.md)
- [`bluetape4k-jdbc` 매뉴얼](../bluetape4k-jdbc.md)
- [`bluetape4k-hibernate` 매뉴얼](../bluetape4k-hibernate.md)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop)

## 다음 단계

현재 application에 필요한 수준을 골랐다면 해당 매뉴얼의 첫 장에서 작은 repository 하나를 구현합니다. 기능이 커질 때만 다음 abstraction으로 이동합니다.
