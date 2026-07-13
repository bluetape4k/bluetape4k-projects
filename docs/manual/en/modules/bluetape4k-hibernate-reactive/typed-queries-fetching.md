---
title: Typed queries and fetch plans
description: Use reified lookup and query APIs while fetching lazy associations within a safe session scope.
manualId: bluetape4k-hibernate-reactive
chapterId: typed-queries-fetching
---

# Typed queries and fetch plans

## Reified APIs

`findAs<T>`, `createQueryAs<R>`, `createSelectionQueryAs<R>`, `createNamedQueryAs<R>`, and `createNativeQueryAs<R>` remove repeated `T::class.java` arguments. Hibernate still validates the query text and result mapping.

```kotlin
val books = sessionFactory.withSessionSuspending { session ->
    session.createSelectionQueryAs<Book>(
        "select b from Book b left join fetch b.author"
    ).resultList.awaitSuspending()
}
```

The tests show `findAs` returning `null` for an unknown ID. `getReferenceAs` may return a proxy immediately, and reading its ID does not imply a database lookup. Initializing other properties still requires a live session and row.

## Mutiny and Stage differences

Do not assume that the two packages are identical from their common function names.

- Mutiny Session supports Hibernate `LockMode`, JPA `LockModeType`, multiple IDs, natural IDs, EntityGraphs, and graph-name lookup.
- Stage Session supports Hibernate `LockMode`, multiple IDs, and natural IDs, but has no `LockModeType` or EntityGraph `findAs` overload.
- Mutiny StatelessSession likewise has more `getAs` overloads than Stage.

## Fetching lazy associations

`Book.author` and `Author.books` in the tests are lazy associations. Resolve required relationships inside the session using one of these approaches:

1. an HQL fetch join;
2. an enabled `@FetchProfile`;
3. `createEntityGraphAs` or the named `Book.withAuthor` graph;
4. explicit Hibernate Reactive `fetch()`.

The Criteria examples use generated JPA metamodel types `Author_` and `Book_`, not Querydsl. The module build deliberately selects the metamodel generator.

## Native query and mapping

The type argument to `createNativeQueryAs<R>` does not guarantee that arbitrary SQL results match `R`. State whether the result is a scalar, entity, or registered result-set mapping and verify it against the actual database. The `AffectedEntities` overload supplies synchronization hints for native queries and the persistence context.

## Executable evidence

- [Mutiny query extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt)
- [Stage query extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt)
- [`Book.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/model/Book.kt)
- [`MutinySessionSupportTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinySessionSupportTest.kt)
- [`StageSessionSupportTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageSessionSupportTest.kt)
