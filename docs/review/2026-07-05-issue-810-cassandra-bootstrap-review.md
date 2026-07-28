# Issue 810 - Cassandra Bootstrap Builder 검토

## Scope

- `data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt`
- `data/cassandra/README.md`
- `data/cassandra/README.ko.md`

## 7-Tier 검토

| Tier | Result | Evidence |
|------|--------|----------|
| API contract | PASS | Shared builder settings now apply to bootstrap and final sessions; explicit bootstrap/session builder overloads cover keyspace-specific settings. |
| Correctness | PASS | The final session binds `identity.keyspace` after bootstrap, preserving keyspace creation order. |
| Regression coverage | PASS | `CqlSessionProviderTest` proves a bare supplier only succeeds when caller builder contact point and datacenter are applied during bootstrap. |
| Kotlin style | PASS | New identity creation uses the package function `cqlSessionIdentityOf`; existing `of()` is retained only as deprecated compatibility. |
| Resource lifecycle | PASS | Admin session still closes with `use`; final session remains registered in `ShutdownQueue`. |
| Documentation | PASS | README and README.ko document bootstrap side effects and keyspace binding rules. |
| Blast radius | PASS | CodeGraph review context and impact radius report low risk and no impacted nodes. |

## 발견 사항

- P0: none.
- P1: none.

## Verification

- PASS: `./gradlew :bluetape4k-cassandra:compileKotlin :bluetape4k-cassandra:compileTestKotlin --no-build-cache --no-configuration-cache`
- PASS: `./gradlew :bluetape4k-cassandra:test --tests 'io.bluetape4k.cassandra.CqlSessionProviderTest' --no-build-cache --no-configuration-cache`
- PASS: `./gradlew :bluetape4k-cassandra:compileKotlin :bluetape4k-cassandra:compileTestKotlin :bluetape4k-cassandra:test :bluetape4k-cassandra:consumerRuntimeTest :bluetape4k-cassandra:koverXmlReport --no-build-cache --no-configuration-cache`
  - 180 main tests passing
  - 1 consumer runtime test passing
  - Kover XML generated
- PASS: `git diff --check`
