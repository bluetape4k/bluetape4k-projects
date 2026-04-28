# Module bluetape4k-hibernate-reactive

English | [한국어](./README.ko.md)

A Kotlin extension library that eliminates boilerplate when working with Hibernate Reactive (Mutiny/Stage).

## Key Features

- **EntityManagerFactory Conversion**: JPA `EntityManagerFactory` → `Mutiny/Stage SessionFactory`
- **Coroutine-Friendly SessionFactory API**: `withSessionSuspending`, `withTransactionSuspending`
- **Mutiny Session Extensions**: Reified functions such as `findAs`, `getAs`, `create*QueryAs`, `createEntityGraphAs`
- **Stage Session Extensions**: Reified functions following the same patterns as the Mutiny API
- **StatelessSession Support**: Transaction, lookup, and query helper APIs

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-hibernate-reactive:${version}")
}
```

## Feature Details

### 1. SessionFactory Conversion

- `mutiny/EntityManagerFactorySupport.kt`
- `stage/EntityManagerFactorySupport.kt`

```kotlin
val mutinySf = emf.asMutinySessionFactory()
val stageSf = emf.asStageSessionFactory()
```

### 2. Coroutine SessionFactory API

- `mutiny/SessionFactorySupport.kt`
- `stage/SessionFactorySupport.kt`

```kotlin
val count = sf.withTransactionSuspending { session, _ ->
    session.createSelectionQueryAs<Long>("select count(a) from Author a")
        .singleResult
        .await()
        .toLong()
}
```

### 3. Mutiny Session / StatelessSession Extensions

- `mutiny/SessionSupport.kt`
- `mutiny/StatelessSessionSupport.kt`

```kotlin
sf.withSessionSuspending { session ->
    val book = session.findAs<Book>(bookId).awaitSuspending()
}

sf.withStatelessSessionSuspending { session ->
    val author = session.getAs<Author>(authorId).awaitSuspending()
}
```

### 4. Stage Session / StatelessSession Extensions

- `stage/SessionSupport.kt`
- `stage/StatelessSessionSupport.kt`

```kotlin
sf.withSessionSuspending { session ->
    session.findAs<Author>(authorId).await()
}
```

### 5. Example Tests

- `src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/*`
- `src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/*`

Key test classes covering uncovered extension functions:

| Test Class | API | Covered Functions |
|---|---|---|
| `MutinySessionSupportTest` | Mutiny | `findAs(LockMode)`, `getReferenceAs`, `createQueryAs` (Session), `createNativeQueryAs` (Session/StatelessSession), `getAs(LockMode)` |
| `StageSessionSupportTest` | Stage | `findAs(LockMode)`, `getReferenceAs`, `createQueryAs` (Session), `createNativeQueryAs` (Session/StatelessSession), `getAs(LockMode)` |

```kotlin
// findAs with Hibernate LockMode (not JPA LockModeType)
sf.withSessionSuspending { session ->
    session.findAs<Book>(book.id, LockMode.NONE).awaitSuspending()
}

// getReferenceAs — proxy reference, no DB access for id
sf.withSessionSuspending { session ->
    val ref = session.getReferenceAs<Book>(book.id)
    ref.id  // accessible without DB roundtrip
}

// createNativeQueryAs on Session
sf.withSessionSuspending { session ->
    session.createNativeQueryAs<Long>("SELECT COUNT(*) FROM books")
        .singleResult.awaitSuspending()
}
```

## Architecture Diagrams

### Reactive Repository Class Structure

```mermaid
classDiagram
    direction TB
    class ReactiveHibernateRepository~ID_E~ {
        <<abstractSuspend>>
        +findByIdOrNull(id): E?
        +findAll(): Flow~E~
        +save(entity): E
    }

    style ReactiveHibernateRepository fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

### Hibernate Reactive API Structure

```mermaid
flowchart TD
    A[EntityManagerFactory<br/>JPA Standard] -->|asMutinySessionFactory| B[Mutiny.SessionFactory]
    A -->|asStageSessionFactory| C[Stage.SessionFactory]

    subgraph Mutiny_API["Mutiny API (SmallRye)"]
        B --> D[withSessionSuspending]
        B --> E[withTransactionSuspending]
        B --> F[withStatelessSessionSuspending]
        D --> G[Mutiny.Session extensions<br/>findAs / getAs<br/>createSelectionQueryAs]
        F --> H[Mutiny.StatelessSession extensions<br/>getAs / createQueryAs]
    end

    subgraph Stage_API["Stage API (CompletionStage)"]
        C --> I[withSessionSuspending]
        C --> J[withTransactionSuspending]
        I --> K[Stage.Session extensions<br/>findAs / getAs]
    end

    subgraph Coroutines["Coroutines Bridge"]
        L[awaitSuspending] --> M[Converts to suspend function]
        N[await] --> M
    end

    G --> L
    H --> L
    K --> N

    classDef jpaStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef mutinyStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef stageStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef coroutineStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

    class A jpaStyle
    class Mutiny_API mutinyStyle
    class Stage_API stageStyle
    class Coroutines coroutineStyle
```

### Session Type Comparison

```mermaid
classDiagram
    class MutinySessionFactory {
        +withSession(block): Uni~T~
        +withTransaction(block): Uni~T~
        +withStatelessSession(block): Uni~T~
        +withSessionSuspending(block): T  ← extension
        +withTransactionSuspending(block): T  ← extension
    }
    class StageSessionFactory {
        +withSession(block): CompletionStage~T~
        +withTransaction(block): CompletionStage~T~
        +withSessionSuspending(block): T  ← extension
    }
    class MutinySession {
        +find(cls, id): Uni~T~
        +findAs(id): Uni~T~  ← reified extension
        +persist(entity): Uni~Void~
    }
    class StageSession {
        +find(cls, id): CompletionStage~T~
        +findAs(id): CompletionStage~T~  ← reified extension
    }

    MutinySessionFactory --> MutinySession : creates
    StageSessionFactory --> StageSession : creates

    style MutinySessionFactory fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style StageSessionFactory fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style MutinySession fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style StageSession fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
```

## Version Requirements

**Hibernate Reactive 4.3.3.Final** requires:
- Hibernate ORM 7.3.2.Final (updated from 7.2.x)
- Jakarta Persistence 3.0 namespace in `persistence.xml`
- Java 11+ / Kotlin 1.5+

### JPA Persistence Configuration Update

When upgrading from Hibernate Reactive 2.x to 3.x:

1. **Update namespace** in `src/main/resources/META-INF/persistence.xml`:
   ```xml
   <!-- Old (JPA 2.0) -->
   <persistence xmlns="http://java.sun.com/xml/ns/persistence" version="2.0">

   <!-- New (Jakarta Persistence 3.0) -->
   <persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
   ```

2. **Explicit entity registration** replaces jar-file scanning:
   ```xml
   <!-- Old approach (deprecated) -->
   <jar-file>jar:file:///path/to/entities.jar!/</jar-file>

   <!-- New approach (required) -->
   <class>io.bluetape4k.example.Author</class>
   <class>io.bluetape4k.example.Book</class>
   ```

3. **Validator configuration** requires GlassFish Expressly 6.0.0 for Jakarta EL

## References

- [Hibernate Reactive](https://hibernate.org/reactive/)
- [Hibernate ORM 7.2 Release Notes](https://hibernate.org/orm/)
- [Jakarta Persistence 3.0](https://jakarta.ee/specifications/persistence/3.0/)
- [Mutiny](https://smallrye.io/smallrye-mutiny/)
