---
title: Hibernate 다음의 기술 선택
description: JDBC, Hibernate, Exposed와 reactive persistence를 데이터 모델과 실행 방식에 따라 선택하는 기준을 설명합니다.
manualId: bluetape4k-hibernate
chapterId: ecosystem-paths
---

# Hibernate 다음의 기술 선택

## ORM이 필요한 문제인지 먼저 본다

Hibernate는 aggregate와 entity lifecycle, dirty checking, lazy association, JPA annotation과 Spring Data JPA가 필요한 애플리케이션에 맞습니다. 단순 SQL adapter나 통계 query까지 모두 entity로 표현하면 mapping과 flush 규칙이 오히려 복잡해집니다.

| 필요한 것 | 시작할 경로 | 선택 기준 |
| --- | --- | --- |
| SQL·connection·vendor 기능 직접 제어 | [bluetape4k-jdbc](../bluetape4k-jdbc.md) | 작은 adapter, 명시적 mapping과 transaction |
| JPA entity lifecycle과 Spring Data | `bluetape4k-hibernate` | aggregate 쓰기 모델과 ORM mapping |
| Kotlin table·column·SQL DSL | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | SQL 중심 모델, JDBC 또는 R2DBC runtime |
| non-blocking persistence | [bluetape4k-hibernate-reactive](../bluetape4k-hibernate-reactive.md), [bluetape4k-r2dbc](../bluetape4k-r2dbc.md) | blocking Session을 coroutine으로 감싸지 않음 |

한 애플리케이션 안에서 Hibernate와 JDBC를 함께 쓸 수 있습니다. 다만 같은 transaction에서 각 기술이 별도 connection을 얻지 않도록 transaction owner를 통일합니다. entity, Exposed row와 JDBC DTO는 persistence boundary 밖에서 섞지 않습니다.

## Hibernate와 Exposed

Hibernate는 entity 상태 전이와 연관 mapping이 중심입니다. Exposed는 Kotlin DSL로 SQL 구조를 드러내며 JDBC와 R2DBC 구현을 선택할 수 있습니다.

- aggregate를 수정한 뒤 dirty checking으로 저장하는 모델이면 Hibernate가 자연스럽습니다.
- 복잡한 SQL을 코드에 명시하고 row를 DTO로 바로 바꾸면 Exposed가 단순합니다.
- JPA annotation, Spring Data repository와 기존 운영 도구가 중요한 팀은 Hibernate의 이점이 큽니다.
- coroutine API가 있다는 사실만으로 JDBC가 non-blocking이 되지는 않습니다. runtime driver까지 확인합니다.

버전은 각 저장소의 library 번호를 직접 맞추지 않고 `bluetape4k-dependencies` BOM으로 정렬합니다.

## 실행 가능한 다음 단계

- [JPA Querydsl demo](../bluetape4k-examples-jpa-querydsl-demo.md): Spring Data repository, 동적 query와 DTO projection
- [Blaze-Persistence demo](../bluetape4k-examples-jpa-blazepersistence-demo.md): Entity View, offset·keyset pagination과 count metadata
- [`SimpleQuerydslExamples`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/SimpleQuerydslExamples.kt): 모듈 안에서 바로 실행하는 Querydsl 예제
- [`mapping` tests](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping): association, inheritance, composite·natural id, tree와 localized entity

중앙 생태계 가이드가 생기면 `Hibernate vs Exposed`, JDBC→R2DBC 전환과 transaction manager 조합 같은 비교 주제는 그곳에서 확장할 수 있습니다. 이 장은 현재 매뉴얼에서 선택 기준과 실제 학습 자료를 잇는 출발점입니다.

## Source와 links

- [bluetape4k-exposed 저장소](https://github.com/bluetape4k/bluetape4k-exposed)
- [`bluetape4k-jdbc` 학습 경로](../bluetape4k-jdbc/ecosystem-paths.md)
- [`bluetape4k-hibernate-reactive`](../bluetape4k-hibernate-reactive.md)
- [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md)
