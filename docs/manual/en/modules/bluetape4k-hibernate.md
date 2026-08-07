---
manualId: bluetape4k-hibernate
title: "Hibernate and Querydsl Extensions"
description: "Use Hibernate ORM and JPA from Kotlin while preserving entity, query, converter, transaction, and StatelessSession boundaries."
kind: library
group: data
learningOrder: 640
---

# Hibernate and Querydsl Extensions

## Capabilities {#problem}

`bluetape4k-hibernate` adds Kotlin-oriented entity base classes and extensions for Hibernate ORM and Jakarta Persistence. It covers `EntityManager` and `Session` operations, Criteria and Querydsl helpers, attribute converters, and bounded StatelessSession work.

The module does not remove Hibernate's persistence-context and transaction rules. Persist versus merge, lazy loading, flush timing, and bulk-query behavior still follow Hibernate. Learn the lifecycle owned by each helper before relying on its shorter syntax.

## Decisions before adoption {#when-to-use}

- Decide whether ORM-managed aggregates and entity lifecycle fit the domain.
- Choose between a Spring-owned transaction and an independent `withNewEntityManager` transaction.
- Define the business signature used by entity equality.
- Choose JPQL, Criteria, or Querydsl as the primary query style.
- Define the trust boundary and keyset storage and rotation policy for converters.
- Decide whether bulk work needs a stateful Session or can give up cascading and listeners for StatelessSession.

Use [bluetape4k-jdbc](./bluetape4k-jdbc.md) for a smaller adapter that needs explicit SQL control. Compare Hibernate with Exposed in [Choosing the surrounding persistence stack](./bluetape4k-hibernate/ecosystem-paths.md).

## Coordinates {#coordinates}

Consumers manage the central BOM version instead of aligning Hibernate, Querydsl, and subordinate bluetape4k libraries separately.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate")

    runtimeOnly("org.postgresql:postgresql") // replace with the selected driver
}
```

The application separately selects Spring Data JPA, a cache provider, and the database driver.

## First transaction {#quick-start}

In standalone code, `withNewEntityManager` owns an `EntityManager`, transaction, and close boundary.

```kotlin
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.hibernate.withNewEntityManager
import jakarta.persistence.EntityManagerFactory

fun renameAccount(
    emf: EntityManagerFactory,
    id: Long,
    newName: String,
) = emf.withNewEntityManager { em ->
    val account = checkNotNull(em.findAs<Account>(id))
    account.name = newName
    account
}
```

The helper commits dirty-checking changes and closes the `EntityManager`. On failure it attempts rollback and rethrows the original exception. In 1.12.1, rollback failure is only logged as a warning and is not attached as a suppressed exception. A Spring application normally lets `@Transactional` and its injected `EntityManager` own this boundary instead of nesting another transaction.

## API by task {#api-by-task}

| Task | Start with | Boundary to preserve |
| --- | --- | --- |
| Run an independent EntityManager transaction | `EntityManagerFactory.withNewEntityManager` | The helper owns commit, rollback, and close. |
| Save or delete entities | `save`, `delete`, `deleteById` | A merge result may be a different managed instance. |
| Remove repeated type arguments | `findAs`, `findOne`, `createQueryAs` | Proxy initialization and SQL timing still follow Hibernate. |
| Query natural IDs | `findBySimpleNaturalId`, `findByNaturalId` | A composite natural-id map must not be empty. |
| Build Criteria queries | `createQueryAs`, `attribute`, `eq`, `ne`, `inValues` | Property names must match JPA mappings. |
| Build Querydsl expressions | `querydsl.core` and `querydsl.jpa` extensions | Q-type annotation processing is still required. |
| Temporarily change JDBC batch size | `Session.withBatchSize` | The size must be positive and is restored afterward. |
| Perform lower-level bulk work | `SessionFactory.withStateless` | There is no first-level cache, dirty checking, cascading, or JPA listener. |
| Convert column values | `converters` package | JSON, encryption, and serialization converters have different failure contracts. |

## Learning path {#concepts}

Each chapter combines detailed explanation, working examples, common failure modes, and links to the 1.12.1 release source and representative tests.

1. [Entity model and lifecycle](./bluetape4k-hibernate/entity-model-lifecycle.md) — identifiers, transient and persisted equality, tree entities, and proxies.
2. [EntityManager and transactions](./bluetape4k-hibernate/entitymanager-transactions.md) — transaction ownership, save and delete behavior, flush, and bulk queries.
3. [JPQL, Criteria, and Querydsl](./bluetape4k-hibernate/queries-criteria-querydsl.md) — move from basic queries to dynamic composition and projections.
4. [Converters and security boundaries](./bluetape4k-hibernate/converters-security.md) — JSON, compression, encryption keysets, and typed serialization.
5. [StatelessSession, batches, and events](./bluetape4k-hibernate/stateless-batch-events.md) — understand which ORM features bulk processing gives up.
6. [Choosing the surrounding persistence stack](./bluetape4k-hibernate/ecosystem-paths.md) — compare JDBC, Exposed, JPA examples, and reactive paths.

New users should normally read chapters 1 through 3 in order. Review chapter 4 before deploying any converter, and start with chapter 5 when designing a bulk path.

## Recommended pattern {#patterns}

Place the transaction around the smallest service operation whose changes must commit together. Continue with the managed entity returned by `merge`, load required associations or map to a DTO inside the transaction, paginate large queries, and clear the persistence context after bulk updates or deletes.

Define business equality from values stable at entity creation. The 1.12.1 transient hash contract has a known limitation, so do not use identifier-less entities as deduplication keys in hash-based collections.

## Integrations {#integrations}

The module exposes Hibernate ORM, Jakarta Persistence, Transaction, Validation, and Querydsl JPA APIs. Spring Boot JPA integration is `compileOnly`; the application configures its starter and transaction manager. Converter runtime support includes Tink, Jackson, compression, and serialization implementations.

Evaluate `bluetape4k-hibernate-cache-lettuce` for second-level caching. When the execution model must be non-blocking, choose `bluetape4k-hibernate-reactive` or R2DBC instead of wrapping a normal Session in a coroutine.

## Configuration {#configuration}

The application owns datasource, dialect, schema migration, connection pool, statement timeout, batch size, SQL logging, and cache configuration. `HibernateConsts.DefaultJpaProperties` disables schema generation but enables `SHOW_SQL` and formatted SQL and sets pool size 30. Treat it as a sample, not a production default.

Applications using encrypted converters must load key material from a protected external store and configure `EncryptedStringConverterKeysets` before reading or writing encrypted fields. Do not embed cleartext keyset JSON in source or plain configuration.

## Failure behavior {#failures}

Hibernate and database-provider failures normally propagate. SQL and constraint failures may appear at flush or commit instead of the helper call. `findOneOrNull` converts only `NoResultException` to null; non-unique and database failures still propagate.

`AbstractObjectAsJsonConverter` logs Jackson conversion failures and returns null. Validate required data around the converter when silent null is unacceptable. Encrypted converters fail fast without a configured keyset and cannot decrypt ciphertext produced by a different keyset.

## Operations {#operations}

Observe query latency, flush counts, transaction rollbacks, pool usage, batch size, first- and second-level cache behavior, and lazy-loading query counts together. `findAll` defaults to `Int.MAX_VALUE`, so production queries need explicit pagination. Entity-listener trace logs can contain complete entities, including personal or pre-encryption values.

## Testing {#testing}

The module test suite covers EntityManager and Session helpers, mappings, converters, Querydsl, StatelessSession, and Spring integration. Some paths use Testcontainers.

```bash
./gradlew :bluetape4k-hibernate:test --no-build-cache --no-configuration-cache
```

The source tree contains a `TestEntityManager` helper under `src/test`, but it is not public API in the normal main artifact.

## Workshops {#workshops}

No dedicated workshop is registered. Tests under `mapping`, `SimpleQuerydslExamples`, and `StatelessSessionStandaloneTest` provide executable learning material for associations, inheritance, natural IDs, trees, queries, and bulk work.

Continue to the [JPA Querydsl demo](./bluetape4k-examples-jpa-querydsl-demo.md) and [Blaze-Persistence demo](./bluetape4k-examples-jpa-blazepersistence-demo.md) for application-shaped examples.

## 1.12.1 scope {#limitations}

This manual targets source published by the `bluetape4k-projects` 1.12.1 tag. Later `develop` fixes for the transient entity hash contract and the Spring StatelessSession transaction resource key are not described as 1.12.1 behavior.

In 1.12.1, `StatelessSessionFactoryBean` can collide with an existing JPA resource key in a Spring transaction. Prefer explicit `SessionFactory.withStateless` to the injected Spring proxy for this release. StatelessSession itself omits cascading, dirty checking, the first-level cache, and JPA listeners.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Persistence Extension Structure diagram

[![Persistence Extension Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-01.svg)

_Release README: [`data/hibernate/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate/README.md)_

### JPA Entity Class Hierarchy diagram

[![JPA Entity Class Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-02.svg)

_Release README: [`data/hibernate/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate/README.md)_

### AttributeConverter Types diagram

[![AttributeConverter Types diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-03.svg)

_Release README: [`data/hibernate/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`EntityManagerFactorySupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupport.kt)
- [`EntityManagerSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt)
- [`SessionSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt)
- [`AbstractJpaEntity.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractJpaEntity.kt)
- [`CriteriaSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/criteria/CriteriaSupport.kt)
- [`EncryptedStringConverters.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/EncryptedStringConverters.kt)
- [`StatelessSesisonSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/stateless/StatelessSesisonSupport.kt)
- [`EntityManagerSupportTest.kt`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerSupportTest.kt)
- [`SimpleQuerydslExamples.kt`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/SimpleQuerydslExamples.kt)
- [`StatelessSessionStandaloneTest.kt`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/standalone/StatelessSessionStandaloneTest.kt)
