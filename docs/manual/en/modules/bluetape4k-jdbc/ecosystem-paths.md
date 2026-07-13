---
title: Choosing what comes after JDBC
description: Choose direct JDBC, JetBrains Exposed, Hibernate, or R2DBC by problem shape and abstraction level.
manualId: bluetape4k-jdbc
chapterId: ecosystem-paths
---

# Choosing what comes after JDBC

## Why JDBC is the starting point

The JDBC paths in Hibernate and Exposed still run on a `DataSource`, connections, transactions, and a database driver. Understanding those boundaries through `bluetape4k-jdbc` makes it easier to locate a failure in the pool, SQL, mapping, or ORM lifecycle.

Moving upward is not about hiding JDBC. Move when repeated SQL structure, schema mapping, repository conventions, or entity lifecycle deserve a higher-level model.

## Decision map

| Current need | Start with | Why |
| --- | --- | --- |
| A small adapter with direct control of vendor SQL | `bluetape4k-jdbc` | The abstraction is thin and keeps SQL, cursor, and transaction boundaries visible. |
| Kotlin types for tables, columns, and queries | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | It extends the JetBrains Exposed DSL with JDBC and R2DBC repositories. |
| Aggregate-oriented entity lifecycle, dirty checking, and the JPA ecosystem | [bluetape4k-hibernate](../bluetape4k-hibernate.md) | Persistence is organized around Hibernate sessions and entity mappings. |
| Avoid blocking JDBC on coroutine call paths | [bluetape4k-r2dbc](../bluetape4k-r2dbc.md) or Exposed R2DBC | Choose a non-blocking driver and suspend transaction boundary. |
| Study JPA queries in a complete application | [JPA QueryDSL demo](../bluetape4k-examples-jpa-querydsl-demo.md), [Blaze-Persistence demo](../bluetape4k-examples-jpa-blazepersistence-demo.md) | See how Spring Boot, repositories, and query tools fit together. |

This is not a vote for one technology across the whole project. A service can use Hibernate for its business write model and direct JDBC for statistics or vendor-specific queries when the boundaries are explicit. Within one transaction, however, use a single transaction owner so that different tools do not borrow unrelated connections.

## When to move to Exposed

Consider Exposed when these signals repeat:

- Column names and types recur as strings across many queries.
- Dynamic predicates should be composed with type safety instead of string concatenation.
- The team wants a Kotlin DSL while choosing between JDBC and R2DBC runtimes.
- Repository conventions, cache decorators, JSON or encrypted columns, or database-specific helpers are needed.

`bluetape4k-exposed` provides separate modules for `exposed-core`, `exposed-jdbc`, `exposed-r2dbc`, cache backends, column codecs, and Spring Boot or Ktor integration. Add only the modules required by the selected runtime and features. Align versions through the `bluetape4k-dependencies` BOM instead of manually matching each repository version.

Exposed JDBC is still blocking JDBC. Calling it from a coroutine does not make it non-blocking. When non-blocking I/O is a requirement, choose Exposed R2DBC or the `bluetape4k-r2dbc` path explicitly.

## When to move to Hibernate

Hibernate is often the more natural choice when these needs dominate:

- Entity relations and aggregate changes are managed as an object lifecycle.
- Dirty checking, a persistence context, and lazy or eager loading policies are required.
- The application relies on JPA annotations and the Spring Data JPA ecosystem.
- Domain-entity state changes matter more than query construction in the persistence design.

The executed SQL and fetch plan can become less visible at the call site. Include N+1 queries, session scope, lazy loading, and flush timing in operational and test criteria.

## Questions for comparing Hibernate and Exposed

Ask these questions instead of asking which technology is universally better.

| Question | Exposed signal | Hibernate signal |
| --- | --- | --- |
| What is the primary model? | Tables, columns, and a query DSL | Entities, relations, and a persistence context |
| How are changes detected? | Explicit insert and update operations | Managed-entity dirty checking |
| Is query construction central to the design? | Keep Kotlin DSL close to SQL structure | Center the design on entity graphs and repository operations |
| How much implicit I/O is acceptable? | Queries are relatively explicit at the call site | Lazy loading and flush boundaries require separate control |
| Is R2DBC required? | Choose an Exposed JDBC or R2DBC path | Hibernate Reactive uses a separate execution model from conventional ORM |
| What experience does the team already have? | Kotlin and SQL | JPA mapping, tooling, and operations |

A technology-choice guide should not declare a winner. It should make explicit where the team is willing to absorb failure cost and complexity.

## Boundaries when combining technologies

- Keep one schema-migration tool as the source of truth.
- If several technologies update the same table, document locking, version-column, and cache-invalidation rules.
- Use one transaction owner and connection-binding model, or state clearly that the operations use separate transactions.
- Do not mix entities, Exposed rows, and JDBC DTOs beyond the persistence boundary.
- Verify commit, rollback, generated keys, and isolation with the production driver in integration tests.

## Future cross-repository guides

This chapter is the first cross-repository learning path in the module manuals. A future central ecosystem guide can host deeper comparisons such as Hibernate versus Exposed, JDBC-to-R2DBC migration, and transaction-manager combinations, while each module manual links to the relevant guide. Until then, this chapter keeps the decision criteria and current repository links together.

## Sources and related repositories

- [`bluetape4k-jdbc` build](../../../../../data/jdbc/build.gradle.kts)
- [`bluetape4k-hibernate` source](../../../../../data/hibernate/src/main/kotlin)
- [bluetape4k-exposed repository](https://github.com/bluetape4k/bluetape4k-exposed)
- [bluetape4k ecosystem atlas](https://bluetape4k.github.io/en/ecosystem/atlas/)
