---
manualId: bluetape4k-hibernate-reactive
title: "Hibernate Reactive 확장"
description: "Hibernate Reactive의 Mutiny·Stage API를 Kotlin 코루틴과 reified 타입으로 사용하는 방법을 설명합니다."
kind: library
group: data
learningOrder: 650
---

# Hibernate Reactive 확장

## 제공하는 기능 {#problem}

`bluetape4k-hibernate-reactive`는 Hibernate Reactive의 Mutiny·Stage API를 Kotlin에서 다루기 쉽게 만드는 확장 라이브러리입니다. JPA `EntityManagerFactory`를 Reactive `SessionFactory`로 변환하고, 세션과 트랜잭션을 `suspend` 블록으로 감싸며, 조회·쿼리·EntityGraph API에서 반복되는 Java `Class` 인자를 reified 타입으로 바꿉니다.

새 ORM이나 독자적인 트랜잭션 관리자를 제공하지는 않습니다. 세션 생성과 종료, commit·rollback, query 실행은 Hibernate Reactive에 위임합니다. 따라서 Hibernate의 entity mapping을 유지하면서 Vert.x SQL Client 기반의 non-blocking I/O가 필요할 때 선택합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 애플리케이션의 주 API를 Mutiny `Uni`로 둘지, Java `CompletionStage`로 둘지 먼저 정합니다.
- Reactive `SessionFactory`를 만든 계층과 애플리케이션 종료 시 닫을 계층을 일치시킵니다.
- ORM의 persistence context와 dirty checking이 필요한지, SQL과 row mapping을 직접 제어하는 R2DBC가 더 적합한지 판단합니다.
- 세션 콜백은 Vert.x dispatcher에서 실행되므로 blocking JDBC, 파일 I/O, 긴 CPU 작업을 넣지 않습니다.
- lazy association을 세션 밖에서 읽지 않도록 fetch join, EntityGraph, fetch profile 또는 명시적 `fetch()` 전략을 정합니다.

## 의존성 추가 {#coordinates}

사용자는 Hibernate Reactive나 ORM의 개별 버전을 직접 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate-reactive")
}
```

Gradle project path는 `:bluetape4k-hibernate-reactive`, source directory는 `data/hibernate-reactive`입니다.

## 첫 트랜잭션 {#quick-start}

JPA factory를 Mutiny factory로 unwrap한 다음, `withTransactionSuspending` 안에서 Reactive API를 기다립니다.

```kotlin
import io.bluetape4k.hibernate.reactive.mutiny.asMutinySessionFactory
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.smallrye.mutiny.coroutines.awaitSuspending

val sessionFactory = entityManagerFactory.asMutinySessionFactory()

val saved = sessionFactory.withTransactionSuspending { session ->
    session.persist(author).awaitSuspending()
    author
}
```

`withTransactionSuspending`은 factory를 닫지 않습니다. factory 소유자는 애플리케이션 종료 시 이를 닫아야 합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | Mutiny | Stage | 기억할 경계 |
| --- | --- | --- | --- |
| JPA factory 변환 | `asMutinySessionFactory` | `asStageSessionFactory` | 기존 provider factory를 unwrap합니다. |
| 세션에서 suspend 작업 | `withSessionSuspending` | `withSessionSuspending` | 세션 수명주기는 Hibernate Reactive가 관리합니다. |
| commit·rollback 경계 | `withTransactionSuspending` | `withTransactionSuspending` | 정상 완료와 실패 의미는 upstream `withTransaction`을 따릅니다. |
| 1차 캐시 없는 작업 | `withStatelessSessionSuspending` | 같은 이름 | entity 상태 추적이 필요하면 일반 Session을 사용합니다. |
| 타입 기반 조회 | `findAs<T>` | `findAs<T>` | Mutiny 쪽이 `LockModeType`·EntityGraph 오버로드를 더 제공합니다. |
| typed query | `createSelectionQueryAs<R>` | 같은 이름 | 실행 실패는 `Uni` 또는 `CompletionStage` 실패로 전달됩니다. |
| graph·native mapping | `createEntityGraphAs`, `getResultSetMappingAs` | 같은 이름 | 등록 이름과 결과 타입은 provider mapping과 일치해야 합니다. |

## 학습 경로 {#concepts}

아래 장은 API 이름만 나열하지 않습니다. 각 기능이 필요한 이유, 실제 코드 예제, `1.12.1` 배포 소스와 MySQL Testcontainers 기반 테스트를 함께 연결합니다. 설명을 읽은 뒤 곧바로 실행 예제와 실패 근거까지 확인할 수 있습니다.

1. [Mutiny·Stage 선택과 시작](./bluetape4k-hibernate-reactive/mutiny-stage-bootstrap.md) — provider 설정, factory unwrap, 두 API의 차이를 정리합니다.
2. [세션과 트랜잭션 수명주기](./bluetape4k-hibernate-reactive/session-transaction-lifecycle.md) — 일반·tenant·stateless 경계와 factory 소유권을 설명합니다.
3. [타입 안전 쿼리와 fetch 계획](./bluetape4k-hibernate-reactive/typed-queries-fetching.md) — reified query, lazy association, fetch join과 EntityGraph를 다룹니다.
4. [StatelessSession 사용법](./bluetape4k-hibernate-reactive/stateless-sessions.md) — 1차 캐시 없는 작업의 이점과 제약을 구분합니다.
5. [실패·취소·운영](./bluetape4k-hibernate-reactive/failure-cancellation-operations.md) — 예외 전파, 취소 보존의 한계, event-loop와 pool 관측 지점을 설명합니다.
6. [Persistence 기술 선택](./bluetape4k-hibernate-reactive/persistence-choice.md) — 일반 Hibernate/JPA, Hibernate Reactive, R2DBC의 선택 경계를 비교합니다.

처음 도입한다면 1→2→3→5 순서로 읽습니다. 대량 단순 작업이 있다면 4장을, persistence 계층을 결정하는 단계라면 6장을 먼저 읽어도 됩니다.

## 권장 패턴 {#patterns}

한 use case 안에서는 Mutiny와 Stage 중 하나를 일관되게 사용합니다. transaction block을 application service의 가장 작은 원자적 작업에 두고, 모든 lazy association은 세션 범위 안에서 필요한 fetch 계획을 명시합니다. stateless session은 대량 단순 작업처럼 persistence context가 필요 없는 경우에만 선택합니다.

## 연동 {#integrations}

이 모듈은 `bluetape4k-hibernate`, `bluetape4k-mutiny`, `bluetape4k-vertx`, Hibernate Reactive, Mutiny Kotlin과 coroutines bridge를 API로 노출합니다. `bluetape4k-hibernate`가 전이 dependency라고 해서 blocking `EntityManager`나 JDBC helper를 Vert.x 세션 콜백 안에서 호출해도 된다는 뜻은 아닙니다.

Hibernate Reactive에서는 Querydsl 대신 JPA metamodel generator를 사용합니다. 테스트의 `Author_`, `Book_`와 Criteria 예제에서 typed metamodel 사용법을 확인할 수 있습니다.

## 설정 {#configuration}

Reactive persistence unit은 `org.hibernate.reactive.provider.ReactivePersistenceProvider`를 사용하고 entity를 명시적으로 등록할 수 있습니다. `1.12.1` 테스트의 XML 문서는 Jakarta Persistence XML schema 3.0이지만, dependency API는 BOM이 정한 Jakarta Persistence 3.2 계열입니다. XML schema 버전과 library API 버전을 같은 값으로 설명하지 않습니다.

README의 하위 dependency 버전 표는 `1.12.1` 실제 version catalog와 맞지 않으므로 복사하지 않습니다. 애플리케이션 설정과 호환성은 중앙 BOM을 기준으로 확인합니다.

## 실패 동작 {#failures}

session block의 예외는 호출자에게 전달됩니다. transaction block은 Hibernate Reactive의 `withTransaction`에 경계를 위임하므로, extension이 별도의 retry나 보상 로직을 추가하지 않습니다. typed query의 문법·mapping·lock 오류도 provider 실패로 전달됩니다.

코루틴 bridge는 `CancellationException`을 명시적으로 다시 던집니다. 다만 이 소스에는 실제 driver query가 즉시 중단되는지를 검증하는 테스트가 없습니다. “코루틴을 취소하면 실행 중인 SQL도 즉시 취소된다”고 보장하면 안 됩니다.

## 운영 {#operations}

Vert.x event-loop 지연, connection pool 대기와 사용량, query latency, transaction rollback, lock timeout, lazy fetch 횟수를 함께 관찰합니다. blocking 호출 탐지와 slow query를 같은 요청 context에서 추적하고, factory shutdown이 완료되는지도 확인합니다.

## 테스트 {#testing}

대표 suite는 Mutiny와 Stage의 factory unwrap, session·transaction 예외 전파, typed query, EntityGraph, stateless 작업을 MySQL Testcontainers에서 검증합니다. Docker가 필요하며 테스트 설정은 병렬 실행을 끕니다.

```bash
./gradlew :bluetape4k-hibernate-reactive:test --no-build-cache --no-configuration-cache
```

## 워크숍 {#workshops}

별도 workshop은 아직 등록되지 않았습니다. 대신 `MutinySessionFactoryExamples`, `StageSessionFactoryExamples`, 두 `SessionSupportTest`, 두 `StatelessSessionExamples`가 실행 가능한 학습 자료입니다. 각 장에서 관련 테스트를 순서대로 안내합니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 태그의 production source와 tests만 설명합니다. 이 모듈은 schema migration, driver 수준 SQL 취소 보장, Querydsl, process-wide retry 정책을 제공하지 않습니다. `1.12.1`과 현재 production source 사이에는 추가 API가 없지만, 향후 버전 문서에서는 다시 비교해야 합니다.

## Source와 tests {#sources}

- [Mutiny `SessionFactorySupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [Mutiny `SessionSupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt)
- [Mutiny `StatelessSessionSupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/StatelessSessionSupport.kt)
- [Stage `SessionFactorySupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [Stage `SessionSupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt)
- [`MutinyExtrasTest.kt`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)
- [`StageExtrasTest.kt`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageExtrasTest.kt)
- [`persistence.xml`](../../../../data/hibernate-reactive/src/test/resources/META-INF/persistence.xml)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Reactive 확장 구조 다이어그램

[![Reactive 확장 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-01.svg)

_배포본 README: [`data/hibernate-reactive/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate-reactive/README.ko.md)_

### Hibernate Reactive API 구조 다이어그램

[![Hibernate Reactive API 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-02.svg)

_배포본 README: [`data/hibernate-reactive/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate-reactive/README.ko.md)_

### 세션 유형 비교 다이어그램

[![세션 유형 비교 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-03.svg)

_배포본 README: [`data/hibernate-reactive/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate-reactive/README.ko.md)_

<!-- release-readme-diagrams:end -->
