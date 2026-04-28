# LZ4 yawkat 마이그레이션 — Implementation Plan

**Date**: 2026-04-28
**Spec**: `docs/superpowers/specs/2026-04-28-lz4-yawkat-migration-design.md`
**Issue**: #203
**Branch**: `feat/lz4-yawkat-migration`
**Worktree**: `.worktrees/feat-lz4-yawkat-migration`

---

## Goal

Replace `org.lz4:lz4-java:1.8.0` with `at.yawk.lz4:lz4-java:1.11.0` to fix:

- **CVE-2025-12183** (CVSS 8.8)
- **CVE-2025-66566** (CVSS 8.2)

The yawkat fork keeps the `net.jpountz.lz4.*` package namespace, so it is **binary-compatible** — no Kotlin source changes are required. The migration is a **build-config-only** change, plus runtime exclusion of the abandoned `org.lz4` JAR transitively pulled in by Kafka/Pulsar/etc.

---

## Pre-flight Constraints (Read Before Starting)

1. `org.lz4:lz4-java` upstream repo was archived 2025-12. `1.8.1` is a Sonatype relocation POM (no real JAR) — do **not** bump to `1.8.1`.
2. BOM `dependencyManagement` swap **does NOT evict different `groupId`**. Eviction must be done via `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` in each affected module's `build.gradle.kts`.
3. Transitive sources of `org.lz4:lz4-java`: `kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`, possibly `pulsar-client`, `avro`, `redisson`. Run dep-tree scans (T3) before assuming exclusion targets.
4. 31 build files reference `Libs.lz4_java`. Their `compileClasspath` should pull `at.yawk.lz4:lz4-java:1.11.0` via `Libs.lz4_java` direct ref; the `org.lz4:lz4-java` artifact only appears via Kafka-family transitives.
5. Native-library coverage check: confirm the new JAR ships native binaries for `linux-x86_64`, `linux-aarch64`, `darwin-aarch64`, `windows-x86_64` so existing CI and Mac dev workstations continue to work.

---

## Task List

### T1 — Update `Libs.lz4_java` GAV (complexity: low)

**File**: `buildSrc/src/main/kotlin/Libs.kt`

**Change**:
```kotlin
// before
const val lz4_java = "org.lz4:lz4-java:1.8.0"

// after
// CVE-2025-12183 (CVSS 8.8) and CVE-2025-66566 (CVSS 8.2) — migrated to maintained yawkat fork.
// Keeps net.jpountz.lz4.* namespace (binary-compatible).
const val lz4_java = "at.yawk.lz4:lz4-java:1.11.0"
```

**DoD**:
- [ ] String constant updated to `at.yawk.lz4:lz4-java:1.11.0`
- [ ] CVE comment present immediately above the constant
- [ ] `./gradlew help` runs without configuration error
- [ ] `rg "org.lz4:lz4-java" buildSrc/` returns no matches in `Libs.kt`

**Dependencies**: none

---

### T2 — Replace root-level dependencyManagement pin (complexity: low)

**File**: root `build.gradle.kts`

**Change**: Inside the `dependencyManagement { dependencies { ... } }` block, replace any existing `dependency("org.lz4:lz4-java:...")` pin with `dependency("at.yawk.lz4:lz4-java:1.11.0")`.

**Note**: This is for *version alignment* only. It does **not** evict the `org.lz4:lz4-java` artifact when transitives request it — see T4–T6 for actual eviction.

**DoD**:
- [ ] Root `build.gradle.kts` no longer pins `org.lz4:lz4-java`
- [ ] New pin `at.yawk.lz4:lz4-java:1.11.0` present in `dependencyManagement`
- [ ] `./gradlew help` succeeds

**Dependencies**: T1 (so the version constant is consistent)

---

### T3 — Pre-implementation dep-tree scan (complexity: medium)

**Goal**: Identify every module whose `runtimeClasspath` (or `testRuntimeClasspath`) still contains `org.lz4:lz4-java` after T1 + T2, so we know exactly which `build.gradle.kts` files need exclude blocks (T4–T6).

**Commands**:
```bash
# Always run after T1+T2 are committed (locally) so version alignment is in effect.
./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath        | rg "lz4"
./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg "lz4"
./gradlew :bluetape4k-pulsar:dependencies --configuration runtimeClasspath       | rg "lz4" 2>/dev/null || true
./gradlew :bluetape4k-avro:dependencies --configuration runtimeClasspath         | rg "lz4" 2>/dev/null || true
./gradlew :bluetape4k-redisson:dependencies --configuration runtimeClasspath     | rg "lz4" 2>/dev/null || true

# Spring-boot facades that aggregate kafka:
./gradlew :bluetape4k-spring-boot3-kafka:dependencies --configuration runtimeClasspath 2>/dev/null | rg "lz4" || true
./gradlew :bluetape4k-spring-boot4-kafka:dependencies --configuration runtimeClasspath 2>/dev/null | rg "lz4" || true

# Catch-all sweep across every module that references lz4_java in its build.gradle.kts:
rg -l "Libs\.lz4_java" --type kotlin -g "build.gradle.kts"
```

**Record**: For each module, note whether `org.lz4:lz4-java` still resolves on the classpath. Build the exclusion target list for T6.

**DoD**:
- [ ] Dependency reports captured for at least: kafka, testcontainers, pulsar (if module exists), avro (if module exists), redisson, spring-boot3-kafka, spring-boot4-kafka
- [ ] Final list of modules requiring `configurations.all { exclude }` is documented (will be used by T6)
- [ ] All modules in the dep-tree list either show only `at.yawk.lz4:lz4-java:1.11.0`, or are flagged for T6 exclusion

**Dependencies**: T1, T2

---

### T4 — Add exclude block to `infra/kafka/build.gradle.kts` (complexity: low)

**File**: `infra/kafka/build.gradle.kts`

**Change**: At top-level (outside `dependencies { }`), add:
```kotlin
configurations.all {
    // CVE-2025-12183 / CVE-2025-66566: evict abandoned org.lz4:lz4-java transitively
    // pulled by kafka-clients/spring-kafka/reactor-kafka. We use at.yawk.lz4:lz4-java instead.
    exclude(group = "org.lz4", module = "lz4-java")
}
```

**DoD**:
- [ ] Block placed at top-level of the build script (not nested under `dependencies`)
- [ ] Comment references both CVE IDs
- [ ] `./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath | rg "org.lz4"` returns **no matches**
- [ ] `./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath | rg "at.yawk.lz4"` shows `at.yawk.lz4:lz4-java:1.11.0`

**Dependencies**: T1, T2, T3

---

### T5 — Add exclude block to `testing/testcontainers/build.gradle.kts` (complexity: low)

**File**: `testing/testcontainers/build.gradle.kts`

**Change**: Same top-level `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` pattern as T4 with the same CVE comment.

**DoD**:
- [ ] Block placed at top-level
- [ ] CVE comment present
- [ ] `./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg "org.lz4"` returns no matches

**Dependencies**: T1, T2, T3

---

### T6 — Add exclude blocks to other modules flagged by T3 (complexity: medium)

**Files**: Whichever `build.gradle.kts` files were flagged by T3 — candidates include but are not limited to:

- `infra/pulsar/build.gradle.kts` (if module exists)
- `io/avro/build.gradle.kts` (if module exists)
- `infra/redisson/build.gradle.kts`
- `spring-boot3/kafka/build.gradle.kts`
- `spring-boot4/kafka/build.gradle.kts`
- Any other modules from the catch-all `rg -l "Libs\.lz4_java"` sweep that still resolve `org.lz4:lz4-java` on classpath

**Change**: Same top-level `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` pattern with CVE comment.

**Decision rule**: If T3 dep-tree shows `org.lz4:lz4-java` still resolved on `runtimeClasspath` or `testRuntimeClasspath`, add the exclude block. If only `at.yawk.lz4:lz4-java:1.11.0` resolves, skip.

**DoD**:
- [ ] Every module flagged by T3 has the exclude block
- [ ] Per-module re-run of `./gradlew :<module>:dependencies --configuration runtimeClasspath | rg "org.lz4"` returns no matches
- [ ] No exclude blocks added to modules that don't need them (avoid configuration noise)

**Dependencies**: T3

---

### T7 — Verify clean classpath + native-library coverage (complexity: medium)

**Goal**: Confirm `org.lz4:lz4-java` is fully evicted across the whole project, and the new `at.yawk.lz4:lz4-java:1.11.0` JAR ships every native binary the project needs.

**Commands — eviction sweep** (must produce empty results):
```bash
# Project-wide: enumerate every module's runtimeClasspath and testRuntimeClasspath; flag any org.lz4 hit.
./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath | rg "org.lz4"
./gradlew :bluetape4k-kafka:dependencies --configuration testRuntimeClasspath | rg "org.lz4"
./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg "org.lz4"
# Repeat for every module flagged in T3/T6:
for m in $(rg -l "Libs\.lz4_java" --type kotlin -g "build.gradle.kts" | sed 's|/build.gradle.kts$||;s|^|:bluetape4k-|;s|/|-|g'); do
  ./gradlew "${m}:dependencies" --configuration runtimeClasspath 2>/dev/null | rg "org.lz4" && echo "FAIL $m"
done
```

**Commands — native binaries inventory**:
```bash
# Locate the resolved JAR (Gradle cache).
LZ4_JAR=$(fd -p '.gradle.*at.yawk.lz4.*lz4-java-1.11.0\.jar$' ~/.gradle/caches | head -1)
unzip -l "$LZ4_JAR" | rg "(linux|darwin|windows|aix|freebsd).*(\.so|\.dylib|\.dll)$"
```
**Required entries** (must all be present):
- `linux/amd64` (a.k.a. `linux-x86_64`)
- `linux/aarch64`
- `darwin/aarch64` (Apple Silicon — covers dev workstations)
- `darwin/x86_64`
- `win32/amd64`

If any required platform binary is missing, **abort and surface to spec author** — yawkat fork might not cover that platform and we'd need a different remediation.

**DoD**:
- [ ] Eviction sweep returns empty output for every module
- [ ] Native-binary inventory contains all five required platforms
- [ ] If a platform is missing, T7 is marked failed and the issue is escalated (do not proceed to T8)

**Dependencies**: T4, T5, T6

---

### T8 — Test execution: core/io modules (complexity: low)

**Commands**:
```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-core:test
./bin/repo-test-summary -- ./gradlew :bluetape4k-io:test
```

**Rationale**: `bluetape4k-io` is the home of `Compressors`/LZ4 wrappers; `bluetape4k-core` provides primitives many modules depend on. These are the most likely to surface a `net.jpountz.lz4` regression.

**DoD**:
- [ ] `:bluetape4k-core:test` BUILD SUCCESSFUL, all tests pass
- [ ] `:bluetape4k-io:test` BUILD SUCCESSFUL, all tests pass
- [ ] Pass count + duration recorded for each

**Dependencies**: T7

---

### T9 — Test execution: infra modules (complexity: low)

**Commands**:
```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-kafka:test
./bin/repo-test-summary -- ./gradlew :bluetape4k-lettuce:test
```

**Rationale**: Kafka is the primary consumer of `lz4-java` (compression codec). Lettuce uses LZ4 in custom Redis codecs.

**DoD**:
- [ ] `:bluetape4k-kafka:test` BUILD SUCCESSFUL
- [ ] `:bluetape4k-lettuce:test` BUILD SUCCESSFUL
- [ ] Any test invoking LZ4 compression/decompression confirmed green
- [ ] Pass count + duration recorded

**Dependencies**: T7

---

### T10 — Test execution: other affected modules (complexity: low)

**Commands**:
```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-testcontainers:test
./bin/repo-test-summary -- ./gradlew :bluetape4k-hibernate-cache-lettuce:test

# Conditional — run only if the module exists
fd -t d "^avro$" io/ && ./bin/repo-test-summary -- ./gradlew :bluetape4k-avro:test
fd -t d "^pulsar$" infra/ && ./bin/repo-test-summary -- ./gradlew :bluetape4k-pulsar:test
fd -t d "^redisson$" infra/ && ./bin/repo-test-summary -- ./gradlew :bluetape4k-redisson:test
```

Also run any module that T6 added an exclude block to.

**DoD**:
- [ ] All applicable module test runs BUILD SUCCESSFUL
- [ ] Pass count + duration recorded per module
- [ ] No test failures attributable to the LZ4 swap

**Dependencies**: T7

---

### T11 — README update: `infra/kafka/README.md` (English) (complexity: medium)

**File**: `infra/kafka/README.md`

**Add a new section** (placement: near "Dependencies" or at the end, choose what reads best):

```markdown
## Security: LZ4 Migration (CVE-2025-12183, CVE-2025-66566)

`org.lz4:lz4-java` was archived in December 2025 and has two unpatched CVEs:

- **CVE-2025-12183** (CVSS 8.8)
- **CVE-2025-66566** (CVSS 8.2)

This module migrates to the maintained fork **`at.yawk.lz4:lz4-java:1.11.0`**, which keeps the
`net.jpountz.lz4.*` package namespace and is binary-compatible — no source changes are required.

Because Kafka clients (`kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`) still
declare a transitive dependency on the abandoned `org.lz4:lz4-java`, this module evicts it via:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

### Downstream consumers

If your application directly depends on `kafka-clients` (or any of its siblings) **without** going
through `bluetape4k-kafka`, add the same `configurations.all { exclude(...) }` block to your build
to ensure the vulnerable JAR is not on your classpath.
```

**DoD**:
- [ ] Section added with both CVE IDs
- [ ] Migration explanation includes "binary-compatible / same package namespace"
- [ ] Downstream-consumer guidance present
- [ ] Markdown renders cleanly (no broken code fences)

**Dependencies**: T4 (so the snippet matches the actual code)

---

### T12 — README update: `infra/kafka/README.ko.md` (Korean) (complexity: medium)

**File**: `infra/kafka/README.ko.md`

**Add a new section** (Korean translation of the T11 section):

```markdown
## 보안: LZ4 마이그레이션 (CVE-2025-12183, CVE-2025-66566)

`org.lz4:lz4-java` 는 2025년 12월에 아카이브되었으며, 두 개의 미해결 CVE 가 있습니다:

- **CVE-2025-12183** (CVSS 8.8)
- **CVE-2025-66566** (CVSS 8.2)

본 모듈은 유지보수가 지속되는 포크 **`at.yawk.lz4:lz4-java:1.11.0`** 으로 마이그레이션했습니다.
패키지 네임스페이스 `net.jpountz.lz4.*` 가 동일하므로 **바이너리 호환** 이며, 소스 코드 변경이 필요하지 않습니다.

Kafka 계열 라이브러리 (`kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`) 는
여전히 폐기된 `org.lz4:lz4-java` 를 추이적 의존성으로 선언하므로, 본 모듈에서는 다음과 같이 제거합니다:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

### 다운스트림 사용자

`bluetape4k-kafka` 를 거치지 않고 `kafka-clients` 등을 직접 의존하는 애플리케이션이라면,
취약 JAR 가 classpath 에 포함되지 않도록 동일한 `configurations.all { exclude(...) }` 블록을
빌드 스크립트에 추가하시기 바랍니다.
```

**DoD**:
- [ ] Korean section added with both CVE IDs
- [ ] Content matches T11 in meaning
- [ ] Code block identical to README.md
- [ ] Markdown renders cleanly

**Dependencies**: T11 (keep both READMEs in sync)

---

### T13 — Commit and push (complexity: low)

**Commands**:
```bash
git add -A
git status   # final review
git commit -m "fix: org.lz4 → at.yawk.lz4:1.11.0 (CVE-2025-12183, CVE-2025-66566)

- buildSrc/Libs.kt: lz4_java GAV swap with CVE comment
- root build.gradle.kts: dependencyManagement realignment
- infra/kafka, testing/testcontainers (and other flagged modules): configurations.all exclude org.lz4
- infra/kafka README.md / README.ko.md: CVE notice + downstream guidance

Issue: #203"
git push -u origin feat/lz4-yawkat-migration
```

PR creation is **out of scope** for this plan — the user/PR step happens after this plan's tasks are all green and `oh-my-claudecode:code-reviewer` has approved per the project's MANDATORY pre-PR checklist.

**DoD**:
- [ ] Commit message uses Korean+prefix-compatible English `fix:` prefix
- [ ] Commit body lists every modified area
- [ ] Issue ID `#203` in trailer
- [ ] `git push -u origin feat/lz4-yawkat-migration` succeeds
- [ ] Local working tree clean after push

**Dependencies**: T1–T12 all green

---

## Dependency Graph

```
T1 ──► T2 ──► T3 ──┬─► T4 ──┐
                   ├─► T5 ──┼──► T7 ──► T8  ──┐
                   └─► T6 ──┘          ├─► T9 ──┼─► T11 ──► T12 ──► T13
                                       └─► T10 ─┘
```

**Parallel opportunities**:
- T4, T5, T6 are independent — can run in parallel after T3.
- T8, T9, T10 are independent test runs — can run in parallel after T7.
- T11 must precede T12 (Korean is translated from English to ensure consistency).

---

## Verification Summary (Whole-Plan DoD)

Before declaring the plan complete:

- [ ] T1–T13 each individually meet their DoD
- [ ] Project-wide sweep `for module in <flagged>; ./gradlew :$module:dependencies --configuration runtimeClasspath | rg "org.lz4"` produces zero hits
- [ ] At least the following module test suites pass: core, io, kafka, lettuce, testcontainers, hibernate-cache-lettuce
- [ ] `infra/kafka/README.md` and `infra/kafka/README.ko.md` both updated and consistent
- [ ] Branch `feat/lz4-yawkat-migration` pushed
- [ ] PR creation deferred until pre-PR MANDATORY checklist (CLAUDE.md) is satisfied — including `oh-my-claudecode:code-reviewer` review

---

## Out of Scope

- PR creation (handled separately, per project's MANDATORY pre-PR checklist)
- Touching modules where `org.lz4:lz4-java` is **not** present on classpath (avoid configuration noise)
- Bumping `at.yawk.lz4:lz4-java` past `1.11.0` (use latest stable at the time of plan only if `1.11.0` is already superseded — otherwise stay on `1.11.0`)
- Source-level changes to LZ4 usage sites — none required because the package namespace is preserved
