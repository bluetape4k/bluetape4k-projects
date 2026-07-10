# Testcontainers Server Reuse Implementation Plan

## Goal

Make non-reuse the default for every Testcontainers Server wrapper while
preserving explicit local reuse and singleton-per-test-JVM Launchers.

## Tasks

- [x] Add a source-policy regression test and prove RED against the existing
  111 `reuse: Boolean = true` defaults.
- [x] Change every production Server `reuse` default to `false`.
- [x] Update KDoc that documents `true` as the default.
- [x] Keep the explicit Floci Launcher guard and verify no implicit
  `withReuse(true)` remains.
- [x] Run policy, compile, focused representative, and proportional module
  validation sequentially.
- [ ] Update PR #1010 scope and DoD, wait for CI, then publish the replacement
  `1.11.1-SNAPSHOT` before downstream mutex work.

## Verification

```shell
./gradlew :bluetape4k-testcontainers:test \
  --tests 'io.bluetape4k.testcontainers.ContainerReusePolicyTest' \
  --no-daemon --no-configuration-cache

./gradlew :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-testcontainers:compileTestKotlin \
  --no-daemon --no-configuration-cache

rg -n 'reuse\s*:\s*Boolean\s*=\s*true|withReuse\(true\)' \
  testing/testcontainers/src/main/kotlin
```

The final search must return no matches.

## Evidence

- RED: the policy test found 52 affected files before implementation.
- GREEN: the policy test passed after all 111 defaults changed to `false`.
- Representative integration tests passed for Floci, PostgreSQL, Redis,
  Consul, NATS, and Neo4j (27 tests total).
- Full module: 449 tests, 0 failures, 25 skipped in 7m 3s.
