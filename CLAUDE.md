# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

Bluetape4k is a shared Kotlin/JVM backend library collection. Maximizes Kotlin idioms, improves Java libraries, supports Coroutines-based async/non-blocking development. Multi-module Gradle project; `settings.gradle.kts` auto-registers subdirectories as `bluetape4k-{dirname}`.

## Development Guidelines

- **README**: Mermaid UML diagrams in every module README; bilingual `README.md` (English) + `README.ko.md` (Korean)
- **KDoc**: Required on all public classes, interfaces, and extension functions
- **Commits**: Korean + prefix (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`)
- **Kotlin**: 2.3+, maximize extensions and DSL; **no ktlint** (IntelliJ IDEA formatter + `.editorconfig`)
- **Tests**: JUnit 5 + MockK + Kluent; production-quality runnable examples
- **Worktree**: All feature work in `.worktrees/<branch>` — never on `develop` directly

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

## Kotlin Edit Workflow (MANDATORY)

Before modifying a class: use `ide_find_references` or `get_impact_radius_tool` to identify affected files.

After every `.kt` edit:

1. `ide_diagnostics` — check import errors and `@Deprecated` warnings
2. Import errors → fix with `ide_optimize_imports`
3. `@Deprecated` → apply Quick Fix via `lsp_code_actions` — never leave unresolved
4. Build/compile only after passing the above steps

## Key Design Patterns

> Full patterns reference: `.claude/references/design-patterns.md`

**Assert vs Require (CRITICAL — do NOT change exception types)**

- `assertXxx()` → `AssertionError` (internal invariants, `@Deprecated`)
- `requireXxx()` → `IllegalArgumentException` (parameter validation — always use this)

**Coroutines-First**: All async work uses Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`.

**Auditable**: Always use `auditedUpdate*` for UPDATE operations.

**Virtual Threads**: Never use `@Synchronized`/`synchronized {}` — use `reentrantLock()`.

## Module Groups

> Full list: `.claude/references/module-groups.md`

| Group             | Description                                                         |
|-------------------|---------------------------------------------------------------------|
| `bluetape4k/`     | `core`, `coroutines`, `logging`, `bom`                              |
| `data/`           | `exposed-*`, `hibernate`, `mongodb`, `jdbc`, `r2dbc`, `cassandra`   |
| `infra/`          | `lettuce`, `redisson`, `kafka`, `pulsar`, `resilience4j`, `cache-*` |
| `spring-boot3/4/` | WebFlux+Coroutines, Exposed repos, Spring Batch                     |
| `virtualthread/`  | `api`, `jdk21`, `jdk25` — always update both together               |

## Build Configuration

- **JVM Toolchain**: Java 21 · **Kotlin**: 2.3 · **Gradle**: ZGC daemon, 4–8 GB heap, parallel build
- Key flags: `-Xjsr305=strict -jvm-default=enable -Xinline-classes -Xcontext-parameters`
- `buildSrc/Libs.kt` — dependency versions · `gradle.properties` — `baseVersion=1.7.0`

## Important Notes

- **jar source extraction**: Use `.claude/lib-sources/<library-name>/` — never `/tmp/` or project source tree
- **atomicfu**: Class property level only — never method-local variables
- **Detekt**: Disabled in `exposed-jdbc-tests`
- **virtualthread-api**: Changes to `virtualthread/api` → always update both `jdk21` and `jdk25`
- **Publishing**: GitHub Packages Maven; exclude `workshop/` and `examples/`

## After Code Changes

- [ ] Run compile + tests for changed module
- [ ] Update both `README.md` and `README.ko.md` for every changed module
- [ ] When creating a new spec/plan: run `/wiki-update`

## Before Creating a PR (MANDATORY)

- [ ] All module tests pass: `./gradlew :<module>:test` (report passing count + duration)
- [ ] Code review: run `oh-my-claudecode:code-reviewer` — resolve all HIGH/CRITICAL issues before push
- [ ] PR description includes test results, fix rationale, and verification commands
- [ ] `README.md` and `README.ko.md` updated for every changed module
- [ ] KDoc added/updated for all new or modified public APIs
- [ ] Work was done inside a git worktree (`.worktrees/<branch>/`)
- [ ] `virtualthread/api` change → both `jdk21` and `jdk25` updated
- [ ] `mock-web-server` changed → `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`
- [ ] `mock-webflux-server` changed →
  `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache`

## Git Workflow

- Base branch: `develop`
- Commits: Korean + prefix (`feat: ...`, `fix: ...`)
- Worktree: `git worktree add .worktrees/<branch> -b <branch>`
- After merging PR: `./bin/clean-branches`
