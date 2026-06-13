# AGENTS.md - bluetape4k-projects

This repository inherits the workspace guidance from `../AGENTS.md`.
Read and follow the workspace root guide first. This file only adds
repo-specific layout, commands, domain rules, and local exceptions.


Core bluetape4k Kotlin/JVM backend libraries. This repo improves Java library
ergonomics and provides coroutine-first, non-blocking infrastructure modules.
`settings.gradle.kts` auto-registers module directories. Most groups publish as
`bluetape4k-{name}`; `ktor/*`, `spring-boot/*`, `virtualthread/*`, and
`examples/*` keep their base directory in the Gradle project name, while
`examples/ktor/*` uses the leaf directory name directly.

## Commands

```bash
repo-status
repo-diff
repo-test-summary -- ./gradlew :module:test

./gradlew clean build
./gradlew build -x test
./gradlew :bluetape4k-coroutines:build
./gradlew test --tests "io.bluetape4k.io.CompressorTest"
./gradlew detekt
./gradlew nmcpPublishAggregationToCentralPortalSnapshots               # SNAPSHOT
./gradlew nmcpPublishAggregationToCentralPortal -PsnapshotVersion=     # RELEASE
```

## Module Groups

| Group | Purpose |
|---|---|
| `bluetape4k/` | `core`, `coroutines`, `logging`, `bom` |
| `io/` | I/O, compression, serialization, HTTP clients, Jackson 2/3, Okio, Tink, Vert.x, gRPC, Protobuf |
| `data/` | Cassandra, Hibernate, MongoDB, JDBC, R2DBC; Exposed lives in `bluetape4k-exposed` |
| `infra/` | Redis/Lettuce/Redisson, Kafka 3/4, Elasticsearch, NATS, Pulsar, Bucket4j, Micrometer, OpenTelemetry, Resilience4j |
| `cache/` | Cache umbrella/core/backend modules and Hibernate Lettuce cache bridge |
| `ktor/` | Ktor server foundation modules: core, observability, and testing helpers |
| `spring-boot/` | Spring Boot 4.x modules and demos; no `spring-boot3/*` line remains |
| `testing/` | `assertions`, `junit5`, `testcontainers`, mock web server images |
| `utils/` | Geo, ID generators, date/time, JWT, math, measured, money, mutiny, probabilistic, rule-engine, science, states, workflow |
| `virtualthread/` | `api`, `jdk21`, `jdk25`; update all related modules together |
| `examples/` | Library examples; not published to Maven |

Full reference may live under `.codex/references/module-groups.md`.

## Build Configuration

- Java 21 toolchain.
- Kotlin 2.3.
- Spring Boot 4.x only.
- Gradle daemon tuned for ZGC, 4-8 GB heap, parallel build.
- Versions live in `gradle/libs.versions.toml`.
- `gradle.properties` owns `projectGroup`, `baseVersion`, `snapshotVersion`,
  and external `exposedVersion`.

## Rules

- Extract jar sources into `.codex/lib-sources/<library-name>/`, never `/tmp`
  or the project source tree.
- atomicfu is class-property only; do not use it for method-local variables.
- Detekt is disabled in `exposed-jdbc-tests`; do not re-enable casually.
- Changes in `virtualthread/api` must be reflected in both `jdk21` and `jdk25`.
- Changes in `mock-web-server` require
  `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`.
- Changes in `mock-webflux-server` require
  `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache`.
- Publishing uses GitHub Packages Maven; `workshop/` and `examples/` are
  excluded.
- Auditable update paths must use `auditedUpdate*`.

## Repo-Specific Guards

- For module moves, splits, additions, or removals, verify auto-registration
  and generated catalog/check scripts in addition to the workspace module
  registration chain.
- Keep Testcontainers-backed verification sequential across modules/worktrees.
