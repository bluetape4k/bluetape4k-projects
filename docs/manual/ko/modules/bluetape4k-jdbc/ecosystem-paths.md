---
title: JDBC 다음의 기술 선택
description: 직접 JDBC, JetBrains Exposed, Hibernate와 R2DBC를 문제와 추상화 수준에 따라 선택하는 기준을 설명합니다.
manualId: bluetape4k-jdbc
chapterId: ecosystem-paths
---

# JDBC 다음의 기술 선택

## JDBC가 출발점인 이유

Hibernate와 Exposed의 JDBC 경로도 결국 `DataSource`, connection, transaction과 database driver 위에서 동작합니다. `bluetape4k-jdbc`로 이 경계를 이해하면 상위 도구에서 문제가 생겼을 때 pool, SQL, mapping, ORM lifecycle 가운데 어디를 봐야 하는지 구분하기 쉽습니다.

상위 기술로 옮기는 목적은 JDBC를 숨기는 데 있지 않습니다. 반복되는 SQL 구조, schema mapping, repository 규칙이나 entity lifecycle을 한 단계 높은 모델로 관리할 가치가 있을 때 이동합니다.

## 선택 지도

| 현재 필요한 것 | 먼저 볼 선택 | 이유 |
| --- | --- | --- |
| vendor SQL을 직접 제어하는 작은 adapter | `bluetape4k-jdbc` | 추상화가 얇고 SQL·cursor·transaction 경계가 그대로 보입니다. |
| Kotlin 타입으로 table·column·query를 구성 | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | JetBrains Exposed DSL과 JDBC/R2DBC repository 확장을 제공합니다. |
| aggregate 중심 entity lifecycle, dirty checking, JPA 생태계 | [bluetape4k-hibernate](../bluetape4k-hibernate.md) | Hibernate Session과 entity mapping을 중심으로 persistence를 구성합니다. |
| coroutine 호출 경로에서 blocking JDBC를 피함 | [bluetape4k-r2dbc](../bluetape4k-r2dbc.md) 또는 Exposed R2DBC | non-blocking driver와 suspend transaction 경계를 선택합니다. |
| JPA query 예제를 실제 애플리케이션 형태로 확인 | [JPA QueryDSL demo](../bluetape4k-examples-jpa-querydsl-demo.md), [Blaze-Persistence demo](../bluetape4k-examples-jpa-blazepersistence-demo.md) | Spring Boot, repository와 query 도구의 결합을 확인할 수 있습니다. |

이 선택은 프로젝트 전체에서 하나만 고르는 투표가 아닙니다. 업무 쓰기 모델은 Hibernate, 통계나 vendor-specific query adapter는 직접 JDBC처럼 경계가 분명하면 함께 사용할 수 있습니다. 다만 같은 transaction 안에서 서로 다른 기술이 각자 connection을 얻지 않도록 transaction owner를 통일해야 합니다.

## Exposed로 발전하는 경우

다음 신호가 반복되면 Exposed를 검토합니다.

- column 이름과 타입을 여러 query에서 문자열로 반복한다.
- 동적 조건을 문자열 조립 없이 타입 안전하게 구성하고 싶다.
- Kotlin DSL을 유지하면서 JDBC와 R2DBC 구현을 선택하고 싶다.
- repository, cache decorator, JSON·암호화 column, database별 helper가 필요하다.

`bluetape4k-exposed`는 `exposed-core`, `exposed-jdbc`, `exposed-r2dbc`, cache backend, column codec와 Spring Boot/Ktor 연동을 별도 모듈로 제공합니다. 처음부터 모두 넣지 말고 사용하는 runtime과 기능에 필요한 모듈만 고릅니다. 의존성 버전은 개별 저장소 버전을 직접 맞추기보다 `bluetape4k-dependencies` BOM을 기준으로 정렬합니다.

Exposed JDBC도 blocking JDBC입니다. coroutine 함수 안에서 호출한다고 자동으로 non-blocking이 되지는 않습니다. non-blocking I/O가 요구사항이면 Exposed R2DBC나 `bluetape4k-r2dbc` 경로를 별도로 선택합니다.

## Hibernate로 발전하는 경우

다음 요구가 중심이면 Hibernate가 더 자연스럽습니다.

- entity relation과 aggregate 변경을 object lifecycle로 관리한다.
- dirty checking, persistence context, lazy/eager loading 정책이 필요하다.
- JPA 표준 annotation과 Spring Data JPA 생태계를 활용한다.
- query보다 domain entity 상태 변화가 persistence 설계의 중심이다.

대신 실행 SQL과 fetch 계획이 코드 표면에서 덜 직접적으로 보일 수 있습니다. N+1 query, session 범위, lazy loading, flush 시점을 운영·테스트 기준에 포함해야 합니다.

## Hibernate와 Exposed를 비교하는 질문

“무엇이 더 좋은가”보다 아래 질문에 답하면 선택이 빨라집니다.

| 질문 | Exposed 쪽 신호 | Hibernate 쪽 신호 |
| --- | --- | --- |
| 주 모델은 무엇인가? | table, column, query DSL | entity, relation, persistence context |
| 변경을 언제 감지하는가? | 명시적 insert/update | managed entity dirty checking |
| query가 설계의 중심인가? | Kotlin DSL과 SQL 구조를 가까이 둠 | entity graph와 repository operation 중심 |
| 숨은 I/O를 얼마나 허용하는가? | 호출한 query가 비교적 직접적 | lazy loading과 flush 경계를 별도 관리 |
| R2DBC가 필요한가? | Exposed JDBC/R2DBC 경로를 선택 가능 | Hibernate Reactive는 일반 ORM과 별도 실행 모델 |
| 팀의 기존 자산은 무엇인가? | Kotlin/SQL 중심 경험 | JPA mapping, tooling, 운영 경험 |

기술 선택 문서는 특정 도구의 승리를 선언하는 글이 아니라, 실패 비용을 어디에서 감당할지 정하는 문서여야 합니다.

## 함께 사용할 때의 경계

- schema migration 도구는 하나를 source of truth로 둡니다.
- 같은 table을 여러 기술이 갱신한다면 locking, version column, cache invalidation 규칙을 문서화합니다.
- transaction owner와 connection binding을 하나로 통일하거나, 서로 다른 transaction임을 명시합니다.
- entity와 Exposed row, JDBC DTO를 persistence boundary 밖에서 섞지 않습니다.
- 통합 테스트는 실제 driver로 commit, rollback, generated key와 isolation을 확인합니다.

## 앞으로 연결될 문서

이 장은 교차 저장소 학습 경로의 첫 적용입니다. 이후 중앙 생태계 가이드에 `Hibernate vs Exposed`, JDBC→R2DBC 전환, transaction manager 조합 같은 비교 문서를 두고, 각 모듈 매뉴얼은 해당 가이드로 연결할 수 있습니다. 중앙 가이드가 생기기 전까지는 이 장이 선택 기준과 현재 저장소 링크를 함께 제공합니다.

## Source와 관련 저장소

- [`bluetape4k-jdbc` build](../../../../../data/jdbc/build.gradle.kts)
- [`bluetape4k-hibernate` source](../../../../../data/hibernate/src/main/kotlin)
- [bluetape4k-exposed 저장소](https://github.com/bluetape4k/bluetape4k-exposed)
- [bluetape4k 생태계 지도](https://bluetape4k.github.io/ko/ecosystem/atlas/)
