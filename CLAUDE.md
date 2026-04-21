# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

Bluetape4k is a shared Kotlin/JVM backend library collection. Maximizes Kotlin idioms, improves Java libraries, supports Coroutines-based async/non-blocking development. Multi-module Gradle project; `settings.gradle.kts` auto-registers subdirectories as `bluetape4k-{dirname}`.

## Development Guidelines

- [ ] **README Diagrams**: Include Mermaid UML diagrams in every module README
- [ ] **KDoc**: Required on all public classes, interfaces, and extension functions (Korean KDoc acceptable)
- [ ] **Commit Messages**: Korean + prefix (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`)
- [ ] **Kotlin**: 2.3+, maximize extensions and DSL
- [ ] **Tests**: JUnit 5 + MockK + Kluent; examples must be runnable, production-quality
- [ ] **Format**: IntelliJ IDEA formatter + `.editorconfig` — **no ktlint**

## Build Commands

```bash
./bin/repo-status          # git status summary (prefer over raw git)
./bin/repo-diff            # per-file change count
./bin/repo-test-summary -- ./gradlew :module:test   # condensed test output

./gradlew clean build
./gradlew build -x test
./gradlew :bluetape4k-coroutines:build
./gradlew test --tests "io.bluetape4k.io.CompressorTest"
./gradlew detekt
./gradlew publishBluetape4kPublicationToBluetape4kRepository           # SNAPSHOT
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=  # RELEASE
```

## After Code Changes

- [ ] Run compile + tests
- [ ] Record result at top of `docs/testlogs/YYYY-MM.md` (skip for doc-only changes)
- [ ] When changing a module: sync-update both `README.md` **and** `README.ko.md`
- [ ] After superpowers work: add entry to `docs/superpowers/index/YYYY-MM.md` → update count in `docs/superpowers/INDEX.md`
- [ ] When creating a new spec/plan: run `/wiki-update`

## Module Groups

| Group | Key modules |
|-------|-------------|
| `bluetape4k/` | `core`, `coroutines`, `logging`, `bom` |
| `io/` | `io`, `okio`, `jackson`/`jackson3`, `feign`, `retrofit2`, `protobuf`, `grpc`, `tink`, `csv`, `vertx` |
| `aws/` | Java SDK v2, 3-tier API (sync→async→coroutines) |
| `aws-kotlin/` | Kotlin SDK, native suspend |
| `data/` | `exposed-*` (core/dao/jdbc/r2dbc/cache/db-specific), `hibernate`, `mongodb`, `jdbc`, `r2dbc`, `cassandra` |
| `infra/` | `lettuce`, `redisson`, `kafka`, `resilience4j`, `bucket4j`, `micrometer`, `opentelemetry`, `cache-*` |
| `spring-boot3/` | WebFlux+Coroutines, Exposed JDBC/R2DBC repos, Hibernate Lettuce cache, Spring Batch |
| `spring-boot4/` | Same as boot3 — use `implementation(platform(Libs.spring_boot4_dependencies))` (not `dependencyManagement`) |
| `utils/` | `geo`, `idgenerators`, `javatimes`, `jwt`, `batch`, `states`, `workflow`, `measured`, `money` |
| `testing/` | `junit5`, `testcontainers`, `mock-web-server` (Spring Boot 3 MVC), `mock-webflux-server` (Spring Boot 4 WebFlux, port 9999) |
| `virtualthread/` | `api`, `jdk21`, `jdk25` — always update both jdk21 AND jdk25 together |

## Kotlin Edit Workflow (MANDATORY)

### Before Modifying a Class
- [ ] Use `ide_find_references` or `get_impact_radius_tool` to identify affected files

### After Every `.kt` Edit
- [ ] `ide_diagnostics` — check import errors and `@Deprecated` warnings immediately
- [ ] Import errors → fix with `ide_optimize_imports`
- [ ] `@Deprecated` → apply Quick Fix via `lsp_code_actions` — never leave unresolved
- [ ] Only run build/compile after passing the above steps

## Key Design Patterns

**Coroutines-First**: All async work uses Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`.

**Record/Model data class**: Must implement `Serializable` + `companion object : KLogging()` + `serialVersionUID = 1L`. Place in `exposed.model` package.

**Repository generic**: `<ID: Any, E: Any>` — no table type generic. `SoftDeleted*` repos retain `T` for `table.isDeleted`.

**NearCache**: `NearCacheOperations<V>` (blocking), `SuspendNearCacheOperations<V>` (suspend). Use `lettuceNearCacheOf<V>()` + `.withResilience {}`.

**Auditable pattern** (3 layers): `exposed-core` → `AuditableIdTable` + `UserContext`; `exposed-dao` → `AuditableEntity` auto-sets createdBy/updatedBy; `exposed-jdbc` → `auditedUpdateById()` / `auditedUpdateAll()` auto-sets updatedAt/updatedBy. **Always use `auditedUpdate*` for UPDATE operations.**

**High-perf**: LZ4/Zstd compression · Kryo/Fory serialization · Custom Redis codecs.

## Build Configuration

- **JVM Toolchain**: Java 21 · **Kotlin**: 2.3 · **Gradle**: ZGC daemon, 4–8 GB heap, parallel build
- Key flags: `-Xjsr305=strict -jvm-default=enable -Xinline-classes -Xcontext-parameters`
- `buildSrc/Libs.kt` — dependency versions · `gradle.properties` — `baseVersion=1.7.0`

## Important Notes

- [ ] **jar source extraction**: Use `.claude/lib-sources/<library-name>/` — never `/tmp/` or project source tree
- [ ] **Publishing**: GitHub Packages Maven; exclude `workshop/` and `examples/`
- [ ] **atomicfu**: Class property level only — never method-local variables
- [ ] **Detekt**: Disabled in `exposed-jdbc-tests`
- [ ] **virtualthread-api**: Changes to `virtualthread/api` → always update both `jdk21` and `jdk25`

## Before Creating a PR (MANDATORY)

Verify every item before running `gh pr create`:

- [ ] All tests pass for changed modules
- [ ] `docs/testlogs/YYYY-MM.md` entry recorded (skip for doc-only changes)
- [ ] `README.md` **and** `README.ko.md` updated for every changed module
- [ ] KDoc added/updated for all new or modified public APIs
- [ ] Work was done inside a `git worktree` (`.worktrees/<branch>/`)
- [ ] `testing/mock-web-server` changed → rebuild Docker image: `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`
- [ ] `testing/mock-webflux-server` changed → rebuild Docker image: `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache`
- [ ] superpowers work → `docs/superpowers/index/YYYY-MM.md` updated
- [ ] `virtualthread/api` change → both `jdk21` and `jdk25` updated
- [ ] Run CodeRabbit review via `/coderabbit:review` skill before merging

## Git Workflow

- Branch: `develop`
- Commits: Korean + prefix (`feat: ...`, `fix: ...`)
- All feature work in a worktree: `git worktree add .worktrees/<branch> -b <branch>`
- After merging PR: run `./bin/clean-branches` to delete gone local branches
