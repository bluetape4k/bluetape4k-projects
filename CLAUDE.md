# CLAUDE.md - bluetape4k-projects

Core bluetape4k Kotlin/JVM backend libraries. This repository keeps the shared
foundation after AWS, image, text, leader-election, JaVers, and Exposed domains
were split into standalone repositories.

`settings.gradle.kts` auto-registers module directories. Most groups publish as
`bluetape4k-{name}`; `spring-boot/*`, `virtualthread/*`, and `examples/*` keep
their base directory in the Gradle project name, while `examples/ktor/*` uses
the leaf directory name directly.

## Build Commands

```bash
repo-status                      # compact git status
repo-diff                        # compact file-level diff summary
repo-test-summary -- ./gradlew :module:test   # summarized test output

./gradlew clean build
./gradlew build -x test
./gradlew :bluetape4k-coroutines:build
./gradlew test --tests "io.bluetape4k.io.CompressorTest"
./gradlew detekt
./gradlew publishBluetape4kPublicationToBluetape4kRepository           # SNAPSHOT
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=  # RELEASE
```

## Module Groups

| Group | Description |
|---|---|
| `bluetape4k/` | `core`, `coroutines`, `logging`, `bom` |
| `io/` | I/O, compression, serialization, HTTP clients, Jackson 2/3, Okio, Tink, Vert.x, gRPC, Protobuf |
| `data/` | Cassandra, Hibernate, JDBC, MongoDB, R2DBC; Exposed lives in `bluetape4k-exposed` |
| `infra/` | Redis/Lettuce/Redisson, Kafka 3/4, Elasticsearch, NATS, Pulsar, Bucket4j, Micrometer, OpenTelemetry, Resilience4j |
| `cache/` | Cache abstraction/backends and Hibernate Lettuce cache bridge |
| `spring-boot/` | Spring Boot 4.x modules and demos; no `spring-boot3/*` line remains |
| `testing/` | `assertions`, `junit5`, `testcontainers`, mock web server images |
| `utils/` | Geo, ID generators, date/time, JWT, math, measured, money, mutiny, probabilistic, rule-engine, science, states, workflow |
| `virtualthread/` | `api`, `jdk21`, `jdk25`; update all related modules together |
| `examples/` | Library examples; not published to Maven |

## Build Configuration

- JVM toolchain: Java 21.
- Kotlin language/API: 2.3.
- Spring Boot line: 4.x only.
- Version catalog: `gradle/libs.versions.toml`.
- Project coordinates: `gradle.properties` owns `projectGroup`, `baseVersion`, `snapshotVersion`, and external `exposedVersion`.
- Gradle daemon is tuned for ZGC, 4-8 GB heap, and parallel build.

## Important Notes

- Extract jar sources into `.claude/lib-sources/<library-name>/`, never `/tmp`
  or the project source tree.
- atomicfu is class-property only; do not use it for method-local variables.
- Changes in `virtualthread/api` must be reflected in both `jdk21` and `jdk25`.
- Changes in `mock-web-server` require
  `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`.
- Changes in `mock-webflux-server` require
  `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache`.
- Publishing excludes `workshop/`, `examples/`, `*-demo`, and benchmark-style modules.
- Keep top-level `README.md` and `README.ko.md` synchronized with
  `settings.gradle.kts` when modules are added, moved, removed, or split out.

## Design Patterns

- Auditable update paths must use `auditedUpdate*`.
- KDoc full reference: `.claude/references/design-patterns.md`.
