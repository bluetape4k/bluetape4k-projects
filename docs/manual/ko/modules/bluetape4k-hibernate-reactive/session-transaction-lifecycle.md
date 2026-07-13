---
title: 세션과 트랜잭션 수명주기
description: 일반·tenant·stateless 세션 경계와 factory 소유권을 코루틴 호출 흐름에 맞춰 설명합니다.
manualId: bluetape4k-hibernate-reactive
chapterId: session-transaction-lifecycle
---

# 세션과 트랜잭션 수명주기

## Factory, session, transaction의 소유자

factory는 애플리케이션 수명과 함께 유지하고 종료 시 닫습니다. 반면 `withSessionSuspending`과 `withTransactionSuspending`이 연 session과 transaction은 Hibernate Reactive가 block 완료에 맞춰 정리합니다. block 밖으로 session을 반환하거나 보관하지 않습니다.

```kotlin
val result = sessionFactory.withTransactionSuspending { session, transaction ->
    check(!transaction.isMarkedForRollback)
    session.persist(entity).awaitSuspending()
    entity.id
}
```

정상 완료 시 commit하고 실패 시 rollback하는 의미는 Hibernate Reactive의 `withTransaction` 계약입니다. extension은 별도의 transaction manager나 retry를 추가하지 않습니다.

## 제공되는 경계

일반 Session과 StatelessSession 모두 다음 조합을 제공합니다.

- session만 여는 block
- tenant ID를 전달하는 session block
- transaction을 여는 block
- session과 transaction 객체를 함께 받는 block
- tenant ID와 transaction 객체를 함께 받는 block

tenant ID는 upstream overload에 그대로 전달됩니다. tenant 해석, connection 선택, schema 분리는 애플리케이션의 Hibernate Reactive multi-tenancy 설정이 책임집니다.

## Vert.x dispatcher

각 wrapper는 Hibernate Reactive callback 안에서 `async(currentVertxDispatcher())`로 suspend block을 실행합니다. 이는 thread를 자유롭게 바꿔도 된다는 의미가 아니라 Vert.x context를 지키기 위한 bridge입니다.

```kotlin
sessionFactory.withSessionSuspending { session ->
    val book = session.findAs<Book>(id).awaitSuspending()
    // blockingJdbcCall()  // 호출하지 않습니다.
    book
}
```

blocking library가 꼭 필요하면 reactive transaction 밖의 별도 dispatcher와 명확한 데이터 경계로 분리합니다. 같은 transaction 안에서 JDBC와 Reactive session이 각자 connection을 얻도록 섞지 않습니다.

## 실패 시 경계

`MutinyExtrasTest`와 `StageExtrasTest`는 session block의 `IllegalStateException`과 transaction block의 `RuntimeException`이 호출자에게 전달되는지 확인합니다. rollback 결과를 별도의 DB assertion으로 검증하지는 않으므로, 문서에서는 extension 고유 보장이 아니라 upstream transaction semantics로 설명합니다.

## 실행 근거

- [Mutiny lifecycle source](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [Stage lifecycle source](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [`AbstractMutinyTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/AbstractMutinyTest.kt)
- [`AbstractStageTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/AbstractStageTest.kt)
- [`MutinySessionFactoryExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinySessionFactoryExamples.kt)
- [`StageSessionFactoryExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageSessionFactoryExamples.kt)
