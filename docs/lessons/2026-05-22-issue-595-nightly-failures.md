# Issue 595 Nightly Failures

## Context

Nightly run 26243476594 failed in three slices: IO HTTP, infra search-messaging, and Testcontainers graphdb-memgraph.

## Decision

- Update the MyBatis dynamic-sql join validation test to exercise the current `on`-based Kotlin DSL and expect `InvalidSqlException`.
- Start the shared Elasticsearch test singleton with `reuse = false` so CI cannot attach to a reused secured container initialized with stale credentials.
- Bind Memgraph Bolt explicitly to `0.0.0.0` so Testcontainers host port mapping always reaches the Bolt listener.

## Outcome

The failing slices were reduced to deterministic local checks and passed after the fixes.

## Verification

- `./gradlew :bluetape4k-vertx:test --tests '*join with no on condition*' --no-configuration-cache --max-workers=1`
- `./gradlew :bluetape4k-elasticsearch:test --no-configuration-cache --max-workers=1`
- `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.graphdb.MemgraphServerTest' --no-configuration-cache --max-workers=1`
- `./gradlew :bluetape4k-feign:test :bluetape4k-http:test :bluetape4k-retrofit2:test :bluetape4k-vertx:test --parallel --no-configuration-cache`
- `./gradlew :bluetape4k-elasticsearch:test :bluetape4k-nats:test --max-workers=1 --no-configuration-cache`

## Future Guidance

When Elasticsearch tests fail with 401 in CI, inspect Testcontainers reuse before changing credentials. For Memgraph, keep the Bolt bind address explicit when changing image tags or command-line flags.
