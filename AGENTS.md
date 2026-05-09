# AGENTS.md - bluetape4k-projects

Core bluetape4k Kotlin/JVM backend libraries. This repo improves Java library
ergonomics and provides coroutine-first, non-blocking infrastructure modules.
`settings.gradle.kts` auto-registers subdirectories as `bluetape4k-{dirname}`.

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
./gradlew publishBluetape4kPublicationToBluetape4kRepository
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=
```

## Module Groups

| Group | Purpose |
|---|---|
| `bluetape4k/` | `core`, `coroutines`, `logging`, `bom` |
| `data/` | Exposed, Hibernate, MongoDB, JDBC, R2DBC, Cassandra |
| `infra/` | Lettuce, Redisson, Kafka, Pulsar, Resilience4j, cache modules |
| `spring-boot3/4/` | WebFlux + coroutines, Exposed repositories, Spring Batch |
| `virtualthread/` | `api`, `jdk21`, `jdk25`; update all related modules together |

Full reference may live under `.codex/references/module-groups.md`.

## Build Configuration

- Java 21 toolchain.
- Kotlin 2.3.
- Gradle daemon tuned for ZGC, 4-8 GB heap, parallel build.
- Versions live in `buildSrc/Libs.kt`.
- `gradle.properties` owns `baseVersion`.

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
