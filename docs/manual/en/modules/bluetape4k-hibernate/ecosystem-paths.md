---
title: Choosing the surrounding persistence stack
description: Choose JDBC, Hibernate, Exposed, or a reactive runtime by data model and execution constraints.
manualId: bluetape4k-hibernate
chapterId: ecosystem-paths
---

# Choosing the surrounding persistence stack

## Confirm that the problem needs an ORM

Hibernate fits aggregate lifecycle, dirty checking, lazy associations, JPA annotations, and Spring Data JPA. A small SQL adapter or reporting query can be simpler without entity state and flush rules.

| Need | Start with | Boundary |
| --- | --- | --- |
| Direct SQL, connection, and vendor control | [bluetape4k-jdbc](../bluetape4k-jdbc.md) | Explicit mapping and transactions |
| JPA lifecycle and Spring Data | `bluetape4k-hibernate` | ORM aggregate model |
| Kotlin table and SQL DSL | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | SQL-oriented JDBC or R2DBC runtime |
| Non-blocking persistence | [bluetape4k-hibernate-reactive](../bluetape4k-hibernate-reactive.md), [bluetape4k-r2dbc](../bluetape4k-r2dbc.md) | Do not wrap blocking Session calls in coroutines |

Hibernate and JDBC can coexist, but one transaction owner must provide the connection when they share a transaction. Do not leak Hibernate entities, Exposed rows, and JDBC DTOs across the same persistence boundary.

Hibernate is natural when aggregate changes are persisted by dirty checking. Exposed is often simpler when SQL shape stays explicit and rows map directly to DTOs. Existing JPA mappings, Spring Data repositories, and operational tooling also influence the choice. Align all library versions through the `bluetape4k-dependencies` BOM.

## Executable next steps

- [JPA Querydsl demo](../bluetape4k-examples-jpa-querydsl-demo.md): Spring Data repositories, dynamic queries, and DTO projections.
- [Blaze-Persistence demo](../bluetape4k-examples-jpa-blazepersistence-demo.md): Entity Views, offset and keyset pagination, and count metadata.
- [`SimpleQuerydslExamples`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/SimpleQuerydslExamples.kt): module-local Querydsl examples.
- [`mapping` tests](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping): associations, inheritance, IDs, trees, and localized entities.

This chapter can later link to a central ecosystem guide for Hibernate versus Exposed, JDBC-to-R2DBC migration, and transaction-manager combinations.

## Sources and links

- [bluetape4k-exposed repository](https://github.com/bluetape4k/bluetape4k-exposed)
- [`bluetape4k-jdbc` learning path](../bluetape4k-jdbc/ecosystem-paths.md)
- [`bluetape4k-hibernate-reactive`](../bluetape4k-hibernate-reactive.md)
- [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md)
