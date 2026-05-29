# Module Groups Reference

Imported from `.claude/references/module-groups.md` for Codex use.

`settings.gradle.kts` auto-registers subdirectories as `bluetape4k-{dirname}`.
For module additions, moves, removals, or repository splits, follow
[`docs/process/module-documentation-checklist.md`](../../docs/process/module-documentation-checklist.md).

| Group            | Key Modules                                                                                                                     |
|------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `bluetape4k/`    | `core`, `coroutines`, `logging`, `bom`                                                                                          |
| `io/`            | `io`, `okio`, `jackson`/`jackson3`, `feign`, `retrofit2`, `protobuf`, `grpc`, `tink`, `csv`, `vertx`                            |
| `aws/`           | Java SDK v2, 3-tier API (sync -> async -> coroutines)                                                                           |
| `aws-kotlin/`    | Kotlin SDK, native suspend                                                                                                      |
| `data/`          | `exposed-*` (core/dao/jdbc/r2dbc/cache/db-specific), `hibernate`, `mongodb`, `jdbc`, `r2dbc`, `cassandra`                       |
| `infra/`         | `lettuce`, `redisson`, `kafka`, `pulsar`, `resilience4j`, `bucket4j`, `micrometer`, `opentelemetry`, `cache-*`, `elasticsearch` |
| `spring-boot/`   | Spring Boot 4.x modules and demos; use `implementation(platform(libs.spring.boot.dependencies))`                                |
| `texts/`         | `tokenizer-core`, `tokenizer-korean`, `tokenizer-japanese`, `lingua`, `text-search`                                             |
| `images/`        | `images` (scrimage), `images-vips-api`, `images-vips-java21` (JVips/JNI), `images-vips-java25` (vips-ffm/FFM)                   |
| `utils/`         | `geo`, `idgenerators`, `javatimes`, `jwt`, `batch`, `states`, `workflow`, `measured`, `money`                                   |
| `testing/`       | `junit5`, `testcontainers`, `mock-web-server` (SB3 MVC), `mock-webflux-server` (SB4 WebFlux, port 9999)                         |
| `virtualthread/` | `api`, `jdk21`, `jdk25` - **always update both jdk21 AND jdk25 together**                                                       |

Historical docs may still mention retired `spring-boot3/*` and `spring-boot4/*`
paths. Current-facing modules use the `spring-boot/` group.

## Exposed Sub-modules

- `exposed-core` / `exposed-dao` / `exposed-jdbc` / `exposed` (umbrella)
- `exposed-cache` - common cache interface + LocalCacheConfig + testFixtures
- `exposed-jdbc-caffeine` / `exposed-r2dbc-caffeine` - Caffeine local cache strategy
- `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` pattern
