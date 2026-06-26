# Issue 809 - Cassandra Session Cache Review

## Scope

- `data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt`
- `data/cassandra/README.md`
- `data/cassandra/README.ko.md`

## Findings

No P0/P1 findings found in the local review pass.

## Evidence

- TDD red: `CqlSessionProviderTest` failed when the same keyspace with a different builder context reused the same cached session.
- `compileTestKotlin --warning-mode all` passed. Remaining warnings are existing Gradle Kotlin DSL deprecations outside this change.
- `:bluetape4k-cassandra:test --rerun-tasks` passed: 178 tests.

## Residual Risk

The compatibility overload cannot inspect arbitrary `CqlSessionBuilder` internals. It prevents keyspace-only collisions by using builder supplier/lambda identity, while the explicit `CqlSessionIdentity` overload is the documented path for stable reuse across call sites.
