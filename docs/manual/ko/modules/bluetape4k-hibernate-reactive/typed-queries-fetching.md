---
title: 타입 안전 쿼리와 fetch 계획
description: reified 조회·쿼리 API와 lazy association을 안전하게 가져오는 방법을 설명합니다.
manualId: bluetape4k-hibernate-reactive
chapterId: typed-queries-fetching
---

# 타입 안전 쿼리와 fetch 계획

## Reified API

`findAs<T>`, `createQueryAs<R>`, `createSelectionQueryAs<R>`, `createNamedQueryAs<R>`, `createNativeQueryAs<R>`는 반복되는 `T::class.java` 인자를 감춥니다. query 문자열과 결과 mapping의 정확성은 여전히 Hibernate가 검사합니다.

```kotlin
val books = sessionFactory.withSessionSuspending { session ->
    session.createSelectionQueryAs<Book>(
        "select b from Book b left join fetch b.author"
    ).resultList.awaitSuspending()
}
```

존재하지 않는 ID의 `findAs`는 테스트에서 `null`을 반환합니다. `getReferenceAs`는 proxy reference를 즉시 반환할 수 있으며 ID 접근이 DB 조회를 뜻하지는 않습니다. 다른 property를 초기화하는 시점에는 세션과 실제 row가 필요합니다.

## Mutiny와 Stage의 차이

공통 API만 보고 두 package를 완전히 같다고 가정하지 않습니다.

- Mutiny Session: Hibernate `LockMode`, JPA `LockModeType`, 여러 ID, natural ID, EntityGraph와 graph name 조회를 지원합니다.
- Stage Session: Hibernate `LockMode`, 여러 ID, natural ID를 지원하지만 `LockModeType`·EntityGraph 기반 `findAs`는 없습니다.
- Mutiny StatelessSession도 Stage보다 `getAs` overload가 많습니다.

## Lazy association을 가져오는 방법

테스트의 `Book.author`와 `Author.books`는 lazy association입니다. 필요한 관계를 세션 안에서 다음 중 하나로 가져옵니다.

1. HQL fetch join
2. `@FetchProfile` 활성화
3. `createEntityGraphAs` 또는 이름 있는 `Book.withAuthor` graph
4. Hibernate Reactive `fetch()`

Criteria 예제는 Querydsl이 아니라 생성된 JPA metamodel `Author_`, `Book_`를 사용합니다. 이 모듈의 build도 Querydsl 대신 metamodel generator를 선택합니다.

## Native query와 mapping

`createNativeQueryAs<R>`의 타입은 SQL 결과와 자동으로 맞춰지는 보장이 아닙니다. scalar, entity, 등록된 result-set mapping 가운데 무엇을 반환하는지 명시하고 실제 database로 검증합니다. `AffectedEntities` overload는 native query와 persistence context 동기화 힌트를 전달합니다.

## 실행 근거

- [Mutiny query extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt)
- [Stage query extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt)
- [`Book.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/model/Book.kt)
- [`MutinySessionSupportTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinySessionSupportTest.kt)
- [`StageSessionSupportTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageSessionSupportTest.kt)
