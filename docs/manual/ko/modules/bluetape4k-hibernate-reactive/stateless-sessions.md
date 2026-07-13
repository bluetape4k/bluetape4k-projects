---
title: StatelessSession 사용법
description: 1차 캐시 없는 reactive 작업에 StatelessSession을 적용할 때의 이점과 제약을 설명합니다.
manualId: bluetape4k-hibernate-reactive
chapterId: stateless-sessions
---

# StatelessSession 사용법

## 일반 Session과 다른 점

StatelessSession은 1차 캐시와 일반적인 persistence context 상태 추적을 사용하지 않습니다. 대량 단건 조회나 명시적인 bulk 작업처럼 managed entity의 dirty checking이 필요 없는 흐름에 적합합니다. relation 변경을 object graph에 반영하고 flush에 맡기는 use case라면 일반 Session을 사용합니다.

```kotlin
val count = sessionFactory.withStatelessTransactionSuspending { session, tx ->
    check(!tx.isMarkedForRollback)
    session.createSelectionQueryAs<Long>("select count(a) from Author a")
        .singleResult.awaitSuspending()
}
```

## 제공되는 작업

- ID 기반 `getAs<T>`
- typed HQL/JPQL, named query, native query
- result-set mapping과 EntityGraph metadata
- session-only와 transaction block
- tenant ID overload

Mutiny는 `LockModeType`, EntityGraph와 graph-name 기반 `getAs`를 제공하지만 Stage는 ID와 Hibernate `LockMode` overload까지만 제공합니다.

## Fetch와 일관성

테스트는 stateless session에서 fetch join, EntityGraph와 명시적 `fetch()`를 사용합니다. `enableFetchProfile`은 stateless session에서 지원하지 않는다고 예제에 명시되어 있습니다. 여러 query를 같은 stateless transaction에서 순차 실행하는 예제는 count 결과의 일관성을 확인합니다.

StatelessSession을 선택했다고 자동으로 batch 크기나 backpressure가 정해지지는 않습니다. 대량 작업은 한 transaction의 행 수, pool 점유 시간, database timeout을 별도로 제한합니다.

## 실행 근거

- [Mutiny stateless extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/StatelessSessionSupport.kt)
- [Stage stateless extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/StatelessSessionSupport.kt)
- [`MutinyStatelessSessionExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyStatelessSessionExamples.kt)
- [`StageStatelessSessionExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageStatelessSessionExamples.kt)
