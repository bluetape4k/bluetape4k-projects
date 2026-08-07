---
title: Choosing and bootstrapping Mutiny or Stage
description: Configure a reactive persistence unit and select the SessionFactory API that matches the application boundary.
manualId: bluetape4k-hibernate-reactive
chapterId: mutiny-stage-bootstrap
---

# Choosing and bootstrapping Mutiny or Stage

## Choose the API first

Hibernate Reactive exposes the same ORM model through Mutiny and Stage APIs. `bluetape4k-hibernate-reactive` provides coroutine bridges for both, but a single use case rarely benefits from switching between them.

| Application boundary | Recommended API |
| --- | --- |
| Mutiny pipelines and `Uni` operators are already central | Mutiny |
| Integration with Java libraries uses `CompletionStage` | Stage |
| Kotlin services expose only suspending functions | Pick the API the team can operate consistently |

Mutiny is not a complete alias for Stage. In 1.12.1, Mutiny `findAs` and `getAs` provide additional JPA `LockModeType` and EntityGraph lookup overloads that Stage does not have.

## Configure the reactive provider

A reactive persistence unit declares its provider and entities.

```xml
<persistence-unit name="default">
    <provider>org.hibernate.reactive.provider.ReactivePersistenceProvider</provider>
    <class>com.example.Author</class>
    <class>com.example.Book</class>
    <exclude-unlisted-classes>true</exclude-unlisted-classes>
</persistence-unit>
```

The tests use the Jakarta Persistence 3.0 XML schema. That is separate from the Jakarta Persistence API version supplied by the BOM. The Hibernate Reactive and ORM versions printed in the README also differ from the 1.12.1 version catalog, so do not copy that version table into application configuration.

## Unwrap the factory

```kotlin
val mutiny = entityManagerFactory.asMutinySessionFactory()
val stage = entityManagerFactory.asStageSessionFactory()
```

Both functions directly call `EntityManagerFactory.unwrap(...)`. They do not create another factory and refer to the provider resource behind the original JPA factory. A non-reactive provider causes the unwrap exception to reach the caller.

The tests create the factory lazily and close it with `closeSafe()` after the suite. The component that creates a production factory must likewise own shutdown.

## First lookup

```kotlin
val author = mutiny.withSessionSuspending { session ->
    session.findAs<Author>(authorId).awaitSuspending()
}
```

Use `await()` in the same structure when choosing Stage. The tests confirm that an unknown ID returns `null`. State the nullable contract at the caller and decide at the service boundary whether absence becomes a domain exception.

## Executable evidence

- [`persistence.xml`](../../../../../data/hibernate-reactive/src/test/resources/META-INF/persistence.xml)
- [Mutiny factory conversion](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/EntityManagerFactorySupport.kt)
- [Stage factory conversion](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/EntityManagerFactorySupport.kt)
- [`MutinyExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)
- [`StageExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageExtrasTest.kt)
