# Review - Cassandra queryForMap varargs (#836, 2026-06-23)

Scope:
- `spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/ReactiveCqlOperationsSupport.kt`
- `spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/ReactiveCqlOperationsSupportUnitTest.kt`
- `spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/ReactiveCqlOperationsSupportTest.kt`

## 발견 사항

P0: 0
P1: 0
P2: 0
P3: 0 after follow-up

## Review Notes

- The implementation now documents and calls `queryForMap(cql, *args)`.
- The unit fixture no longer stubs the misleading `Array<*>` shape; it uses `*anyVararg()`.
- The focused unit contract verifies two concrete positional arguments reach `queryForMap`.
- The Cassandra integration test was strengthened after review feedback to use two real bind markers:
  `id = ? AND firstname = ? ALLOW FILTERING`, with `user.id` and `user.firstname`.
- RED reproduction note: the added unit and integration tests passed before the production change, so
  the suspected runtime failure was not reproduced locally. The change is still useful because it
  makes the public vararg contract explicit and removes the misleading test/KDoc shape.

## 검증

- `./gradlew :bluetape4k-spring-boot-cassandra:cleanTest :bluetape4k-spring-boot-cassandra:compileKotlin :bluetape4k-spring-boot-cassandra:compileTestKotlin :bluetape4k-spring-boot-cassandra:test --no-build-cache`
  - `GRADLE_STATUS=0`
  - `257 passing`
  - `BUILD SUCCESSFUL in 23s`
- `git diff --check`: passed.
- Stale pattern guard:
  `rg -n "queryForMap\\(cql, args\\)|queryForMap\\(any<String>\\(\\), any<Array|any<Array<\\*>>\\(\\)" ...`
  returned no matches.

## Gate Verdict

PASS for PR creation.
Merge is explicitly out of scope for this branch per user request.
