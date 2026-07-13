---
title: Persistence 기술 선택
description: 일반 Hibernate/JPA, Hibernate Reactive와 R2DBC를 실행 모델과 데이터 모델 기준으로 비교합니다.
manualId: bluetape4k-hibernate-reactive
chapterId: persistence-choice
---

# Persistence 기술 선택

## 선택 지도

| 중심 요구 | 먼저 볼 기술 | 이유 |
| --- | --- | --- |
| Spring JPA, blocking JDBC, 전통적 EntityManager | [bluetape4k-hibernate](../bluetape4k-hibernate.md) | 일반 Hibernate Session과 JPA 생태계를 사용합니다. |
| entity relation·dirty checking을 유지한 non-blocking I/O | `bluetape4k-hibernate-reactive` | Vert.x SQL Client 위에서 Reactive ORM을 사용합니다. |
| SQL·row mapping·pool·Flow를 직접 제어 | [bluetape4k-r2dbc](../bluetape4k-r2dbc.md) | ORM persistence context 없이 R2DBC 경계를 직접 구성합니다. |

## 일반 Hibernate와 비교

두 Hibernate 모듈은 entity mapping이라는 모델을 공유하지만 실행 방식이 다릅니다. 일반 Hibernate는 JDBC 기반 blocking 호출과 `EntityManager`·`Session` helper, converter, Querydsl 연동에 적합합니다. Hibernate Reactive는 Vert.x context, `Uni`·`CompletionStage`, Reactive Session API를 전제로 합니다.

`bluetape4k-hibernate-reactive`가 `bluetape4k-hibernate`를 API dependency로 포함하더라도 blocking helper를 reactive callback 안에서 호출하면 안 됩니다. 공유할 수 있는 것은 entity model과 일부 공통 타입이지 실행 모델이 아닙니다.

## R2DBC와 비교

R2DBC는 query와 row mapping이 설계의 중심입니다. 어떤 SQL을 실행하고 어떤 DTO로 바꾸는지가 코드에 직접 나타나며, entity graph·cascade·dirty checking은 제공하지 않습니다. Hibernate Reactive는 SQL보다 entity relation과 persistence context가 중심일 때 유리합니다.

다음 질문으로 선택합니다.

- relation 변경을 managed entity lifecycle로 표현해야 하는가?
- lazy association과 fetch plan을 운영할 준비가 되어 있는가?
- vendor SQL과 row mapping을 직접 제어하는 편이 더 단순한가?
- 기존 팀 자산이 JPA mapping인가, SQL/R2DBC pipeline인가?

## 함께 사용할 때

서로 다른 bounded context에서 다른 기술을 선택할 수 있습니다. 다만 같은 원자적 작업 안에서 각 기술이 별도 connection과 transaction을 열지 않도록 owner를 분명히 합니다. 같은 table을 여러 기술이 갱신한다면 version column, cache invalidation, schema migration 책임도 문서화합니다.

## 관련 source

- [일반 Hibernate source](../../../../../data/hibernate/src/main/kotlin)
- [Hibernate Reactive source](../../../../../data/hibernate-reactive/src/main/kotlin)
- [R2DBC source](../../../../../data/r2dbc/src/main/kotlin)
- [JDBC 이후 기술 선택](../bluetape4k-jdbc/ecosystem-paths.md)
