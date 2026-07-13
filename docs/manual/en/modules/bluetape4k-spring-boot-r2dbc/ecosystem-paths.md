---
title: R2DBC ecosystem path
description: Choose among Spring Data coroutine extensions, core R2DBC, JDBC, Exposed R2DBC, and their workshops.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: ecosystem-paths
---

# R2DBC ecosystem path

## Choose the abstraction level first

The same database can have several useful entry points.

| Requirement | Recommended starting point |
| --- | --- |
| Spring Data entity mapping and coroutine CRUD | `bluetape4k-spring-boot-r2dbc` |
| Raw SQL, binding, custom row mapping, connection and transaction helpers | [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md) |
| Kotlin table DSL, DDL/DML, and repository abstractions | `bluetape4k-exposed` R2DBC |
| Blocking-driver ecosystem and a simpler transaction model | [`bluetape4k-jdbc`](../bluetape4k-jdbc.md) |
| Object graphs, dirty checking, and a persistence context | [`bluetape4k-hibernate`](../bluetape4k-hibernate.md) |

R2DBC is not inherently better than JDBC. The full call path must benefit from non-blocking I/O, and the chosen database driver must support the required behavior.

## Stage 1: Spring Data entity operations

Start with `R2dbcEntityOperations`, `Query`, `Criteria`, `Flow`, and suspending cardinality. The in-module `coroutines.blog` application demonstrates the entity→repository→controller path.

Recommended order:

1. Whole-list and one-row reads in `PostRepository`
2. Filtered `Flow` and count in `CommentRepository`
3. The CRUD cycle in `R2dbcEntityOperationsExtensionsTest`
4. The WebFlux boundary in `PostControllerTest`

## Stage 2: Move down to core R2DBC

Move to `bluetape4k-r2dbc` for joins, DTO projections, or vendor-specific SQL that do not fit entity operations. Its [core R2DBC manual](../bluetape4k-r2dbc.md) explains connection pools, transaction lifecycle, typed null binding, and raw SQL mapping.

Both modules can coexist. Keep straightforward entity CRUD here and implement only complex queries with `R2dbcClient` or `DatabaseClient`.

## Stage 3: Move up to Exposed R2DBC

Evaluate the R2DBC modules in [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) when you want Kotlin table and column DSLs with repository patterns. Spring Data entity mapping and Exposed table mapping are separate models; avoid making both own the same aggregate without a clear boundary.

The [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop) provides runnable exercises for:

- Table and schema definitions
- Coroutine transactions and CRUD
- Repository and domain mapping
- Spring WebFlux integration
- Real-database tests and operational patterns

## Compare with JDBC

JDBC may be a better fit when its driver and library ecosystem already covers the application and implementation simplicity matters more than a fully non-blocking request path. Virtual threads or an explicit `Dispatchers.IO` boundary are also valid options. R2DBC fits when the path remains non-blocking from WebFlux through the driver and handles many concurrent I/O operations.

The [JDBC manual](../bluetape4k-jdbc.md) covers blocking connection, transaction, and statement lifecycles. Choose from application call paths and driver maturity rather than technology labels.

## Practical learning checklist

- Tests distinguish `one`, `oneOrNull`, `first`, and `Flow` cardinality.
- Update and delete counts are compared with domain expectations.
- Transaction ownership and connection/pool ownership are documented.
- H2 and production-driver tests have separate evidence scopes.
- Raw SQL queries are separated from entity operations.
- Blocking libraries are not called directly inside a suspending function.

## Sources and related repositories

- [`spring-boot/r2dbc` test application](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog)
- [`bluetape4k-r2dbc` manual](../bluetape4k-r2dbc.md)
- [`bluetape4k-jdbc` manual](../bluetape4k-jdbc.md)
- [`bluetape4k-hibernate` manual](../bluetape4k-hibernate.md)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop)

## Next step

After choosing the appropriate level, implement one small repository from the first chapter of that manual. Move to another abstraction only when the feature needs it.
