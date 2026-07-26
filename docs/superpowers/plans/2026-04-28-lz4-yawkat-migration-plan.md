# LZ4 yawkat 마이그레이션 — Implementation Plan

**Date**: 2026-04-28 **Spec**: `docs/superpowers/specs/2026-04-28-lz4-yawkat-migration-design.md`
**Issue**: #203 **Branch**: `feat/lz4-yawkat-migration`
**Worktree**: `.worktrees/feat-lz4-yawkat-migration`
**Rev**: v2 (Plan Review 반영)

---

## Goal

Replace `org.lz4:lz4-java:1.8.0` with `at.yawk.lz4:lz4-java:1.11.0` to fix:

- **CVE-2025-12183** (CVSS 8.8)
- **CVE-2025-66566** (CVSS 8.2)

The yawkat fork keeps the `net.jpountz.lz4.*` package namespace —
**binary-compatible**, no Kotlin source changes required. Migration is **build-config-only**.

---

## Pre-flight Constraints (Read Before Starting)

1. `org.lz4:lz4-java` upstream repo was archived 2025-12. `org.lz4:lz4-java:1.8.1` is a Sonatype relocation POM (no real JAR) — do
   **not** bump to `1.8.1`.
2. BOM `dependencyManagement` swap **does NOT evict
   different `groupId`**. Eviction must be done via `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` in each affected module's `build.gradle.kts`.
3. Transitive sources of `org.lz4:lz4-java`: `kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`, possibly `pulsar-client`, `avro`, `redisson`. Run dep-tree scans (T3) before assuming exclusion targets.
4. 31 build files reference `Libs.lz4_java`. Most are
   **direct** consumers (no exclude needed); only modules that pull `org.lz4`
   **transitively** via Kafka/Pulsar/Avro/Redisson need `configurations.all { exclude }`.
5. Root `build.gradle.kts:376` uses `dependency(Libs.lz4_java)` — a constant reference, NOT a literal GAV string. After T1 changes the constant, T2 is a verification step only (no edit required in root build file).
6. **Required native binary platforms** (must all be present in the resolved JAR):
    - `linux/amd64` (linux-x86_64)
    - `linux/aarch64`
    - `darwin/aarch64` (Apple Silicon)
    - `darwin/x86_64` (Intel Mac)
    - `win32/amd64` (windows-x86_64)

---

## Rollback Plan

The entire migration is a single commit (T13). Rollback is:

```bash
git revert HEAD
./gradlew :bluetape4k-kafka:build :bluetape4k-core:build
```

No Kotlin source files are changed. LZ4 compression format is standard — persisted data requires no reprocessing.

**Abort criteria** (escalate to spec author, do not proceed):

- T7 native-binary check: any required platform binary is missing
- T8/T9/T10: test failures directly attributable to `net.jpountz.lz4` API changes (not infra/CI flakiness)
- Any module classloader error at runtime linking to `net.jpountz.lz4`

---

## Task List

### T1 — `Libs.lz4_java` GAV 변경 (complexity: low)

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

- [ ] `lz4_java` 상수가 `at.yawk.lz4:lz4-java:1.11.0` 으로 변경됨
- [ ] CVE 두 번호 코멘트가 상수 바로 위에 있음
- [ ] `./gradlew help` 오류 없이 실행됨
- [ ] `rg "org.lz4:lz4-java" buildSrc/` 결과 없음

**Dependencies**: none

---

### T2 — 루트 `dependencyManagement` 검증 (complexity: low)

**File**: root `build.gradle.kts`

> ⛔ **편집 불필요**: 루트 `build.gradle.kts:376` 은 `dependency(Libs.lz4_java)` 상수 참조다.
> T1 에서 상수 값을 바꾸면 이 라인은 자동으로 `at.yawk.lz4:lz4-java:1.11.0` 을 pin 하게 된다.
> 이 task 는 그 결과를 검증하는 단계다 — 파일 편집 없음.

**Verification command**:

```bash
rg "lz4" build.gradle.kts
# Expected: dependency(Libs.lz4_java) — constant reference only, no org.lz4 literal
```

**DoD**:

- [ ] 루트 `build.gradle.kts` 에 `"org.lz4:lz4-java"` 리터럴이 없음
- [ ] `dependency(Libs.lz4_java)` 줄이 그대로 존재함 (상수 참조 유지)
- [ ] `./gradlew help` 성공

**Dependencies**: T1

---

### T3 — Transitive dep-tree 전체 스캔 (complexity: medium)

**Goal**: `org.lz4:lz4-java` 가 어떤 모듈의 `runtimeClasspath` 에 아직 남아 있는지 파악. 직접 `Libs.lz4_java` 를 참조하는 31개 모듈은 T1 이후 자동으로 `at.yawk.lz4` 를 사용하므로 exclude 불필요.
**Kafka/Pulsar/Avro/Redisson 경로로 org.lz4 를 transitive 하게 받는 모듈만 T6 exclude 대상이다.**

**Commands**:

```bash
# Primary transitive sources
./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath | rg "lz4"
./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg "lz4"
./gradlew :bluetape4k-pulsar:dependencies --configuration runtimeClasspath | rg "lz4" 2>/dev/null || true
./gradlew :bluetape4k-avro:dependencies --configuration runtimeClasspath | rg "lz4" 2>/dev/null || true
./gradlew :bluetape4k-redisson:dependencies --configuration runtimeClasspath | rg "lz4" 2>/dev/null || true

# Spring-boot facades
./gradlew :bluetape4k-spring-boot3-kafka:dependencies --configuration runtimeClasspath 2>/dev/null | rg "lz4" || true
./gradlew :bluetape4k-spring-boot4-kafka:dependencies --configuration runtimeClasspath 2>/dev/null | rg "lz4" || true
```

**Decision log** (T3-decision-log): 스캔 후 각 모듈에 대해 다음 표를 작성한다:

| 모듈                      | org.lz4 transitive 유입 여부 | T6 exclude 필요 |
|---------------------------|------------------------------|-----------------|
| bluetape4k-kafka          | (scan result)                | (yes/no)        |
| bluetape4k-testcontainers | (scan result)                | (yes/no)        |
| ...                       | ...                          | ...             |

**DoD**:

- [ ] kafka, testcontainers, pulsar, avro, redisson, spring-boot3-kafka, spring-boot4-kafka 스캔 완료
- [ ] T3-decision-log 표 작성 완료
- [ ] T6 exclude 대상 목록 확정

**Dependencies**: T1, T2

---

### T4 — `infra/kafka/build.gradle.kts` exclude 추가 (complexity: low)

**File**: `infra/kafka/build.gradle.kts`

**Change** (top-level, `dependencies {}` 블록 외부):

```kotlin
configurations.all {
    // CVE-2025-12183 / CVE-2025-66566: evict abandoned org.lz4:lz4-java transitively
    // pulled by kafka-clients/spring-kafka/reactor-kafka. We use at.yawk.lz4:lz4-java instead.
    exclude(group = "org.lz4", module = "lz4-java")
}
```

**DoD**:

- [ ] 블록이 top-level 에 위치 (dependencies {} 내부 아님)
- [ ] CVE 두 번호 코멘트 포함
- [ ] `./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath | rg "org.lz4"` 결과 없음
- [ ] `./gradlew :bluetape4k-kafka:dependencies --configuration runtimeClasspath | rg "at.yawk.lz4"` 에서 `1.11.0` 확인

**Dependencies**: T1, T2, T3

---

### T5 — `testing/testcontainers/build.gradle.kts` exclude 추가 (complexity: low)

**File**: `testing/testcontainers/build.gradle.kts`

T4 와 동일한 `configurations.all { exclude(group = "org.lz4", module = "lz4-java") }` 패턴.

**DoD**:

- [ ] 블록이 top-level 에 위치
- [ ] CVE 코멘트 포함
- [ ] `./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg "org.lz4"` 결과 없음

**Dependencies**: T1, T2, T3

---

### T6 — T3 결과 기반 추가 exclude 적용 (complexity: medium)

**Files**: T3-decision-log 에서 `T6 exclude 필요: yes` 로 표시된 모든 모듈. 후보: `infra/pulsar`, `io/avro`, `infra/redisson`, spring-boot kafka facades 등.

**Change**: T4/T5 와 동일한 `configurations.all { exclude }` 패턴.

**Decision rule**:

- T3 dep-tree 에서 `org.lz4:lz4-java` 가 `runtimeClasspath` 또는 `testRuntimeClasspath` 에 보이면 → exclude 추가
- `at.yawk.lz4:lz4-java:1.11.0` 만 보이거나 lz4 가 없으면 → exclude 불필요 (T3-decision-log 에 `no` 기록)
- skip 한 모듈도 T3-decision-log 에 명시 (audit trail)

**DoD**:

- [ ] T3-decision-log 의 모든 `yes` 모듈에 exclude 블록 추가
- [ ] 각 모듈 `rg "org.lz4"` dep-tree 결과 없음
- [ ] T3-decision-log skip 항목에 사유 기록

**Dependencies**: T3

---

### T7 — Classpath 완전 제거 + native library 검증 (complexity: medium)

#### 7-A. Eviction sweep (결과 없어야 함)

```bash
exit_code=0

for module in bluetape4k-kafka bluetape4k-testcontainers bluetape4k-pulsar bluetape4k-avro bluetape4k-redisson; do
    result=$(./gradlew ":${module}:dependencies" --configuration runtimeClasspath 2>/dev/null | rg "org.lz4" || true)
    if [[ -n "$result" ]]; then
        echo "FAIL ${module}: $result" >&2
        exit_code=1
    fi
done

# T6 에서 추가된 모듈도 포함
for module in $(cat t3-decision-log.txt | grep "yes" | awk '{print $1}'); do
    result=$(./gradlew ":${module}:dependencies" --configuration runtimeClasspath 2>/dev/null | rg "org.lz4" || true)
    if [[ -n "$result" ]]; then
        echo "FAIL ${module}: $result" >&2
        exit_code=1
    fi
done

exit ${exit_code}
```

#### 7-B. Native binary 플랫폼 커버리지 확인

```bash
LZ4_JAR=$(fd -p '.gradle.*at.yawk.lz4.*lz4-java-1.11.0\.jar$' ~/.gradle/caches | head -1)
unzip -l "$LZ4_JAR" | rg "(linux|darwin|windows|aix|freebsd).*(\.so|\.dylib|\.dll)$"
```

**필수 플랫폼** (모두 존재해야 함):

- `linux/amd64` (= linux-x86_64)
- `linux/aarch64`
- `darwin/aarch64` (Apple Silicon)
- `darwin/x86_64` (Intel Mac)
- `win32/amd64` (= windows-x86_64)

플랫폼 누락 시 → **즉시 중단, spec author 에게 에스컬레이션. T8 진행 금지.**

**DoD**:

- [ ] 7-A exit code 0 (모든 모듈에서 org.lz4 없음)
- [ ] 7-B 5개 플랫폼 binary 모두 확인

**Dependencies**: T4, T5, T6

---

### T8 — 테스트: core/io 모듈 (complexity: low)

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-core:test
./bin/repo-test-summary -- ./gradlew :bluetape4k-io:test
```

**DoD**:

- [ ] `:bluetape4k-core:test` BUILD SUCCESSFUL
- [ ] `:bluetape4k-io:test` BUILD SUCCESSFUL

**Dependencies**: T7

---

### T9 — 테스트: infra 모듈 (complexity: low)

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-kafka:test
./bin/repo-test-summary -- ./gradlew :bluetape4k-lettuce:test
```

**DoD**:

- [ ] `:bluetape4k-kafka:test` BUILD SUCCESSFUL
- [ ] `:bluetape4k-lettuce:test` BUILD SUCCESSFUL

**Dependencies**: T7

---

### T10 — 테스트: 기타 영향 모듈 (complexity: low)

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-testcontainers:test
./bin/repo-test-summary -- ./gradlew :bluetape4k-hibernate-cache-lettuce:test

# 모듈 존재 시 조건부 실행
fd -t d "^avro$" io/ && ./bin/repo-test-summary -- ./gradlew :bluetape4k-avro:test
fd -t d "^pulsar$" infra/ && ./bin/repo-test-summary -- ./gradlew :bluetape4k-pulsar:test
fd -t d "^redisson$" infra/ && ./bin/repo-test-summary -- ./gradlew :bluetape4k-redisson:test
```

T6 에서 exclude 추가된 모듈도 포함하여 실행.

**DoD**:

- [ ] 적용 가능한 모든 모듈 BUILD SUCCESSFUL
- [ ] LZ4 관련 테스트 실패 없음

**Dependencies**: T7

---

### T11 — Kotlin 소스 변경 없음 검증 (complexity: low)

```bash
git diff develop...HEAD -- '*.kt' | rg -v '^(\+\+\+|---|\-\-\-)' | rg "^[+-]"
# Expected: 출력 없음 (build.gradle.kts 제외 kt 파일 변경 없어야 함)
```

또는:

```bash
git diff develop...HEAD --name-only -- '*.kt'
# Expected: 빈 출력
```

**DoD**:

- [ ] `.kt` 파일 변경 없음 (build.gradle.kts 는 OK)
- [ ] 변경된 파일이 있다면 즉시 원인 파악 + 스펙 위반 여부 확인

**Dependencies**: T8, T9, T10

---

### T12 — README 업데이트: 변경된 모든 모듈 (complexity: medium)

**Scope**: T4, T5, T6 에서 수정된 모든 모듈의 `README.md` + `README.ko.md`.

CLAUDE.md 규칙: "모듈 변경 시 README.md + README.ko.md 동기화 업데이트".

#### T12-A. `infra/kafka/README.md` (English)

보안 섹션 추가:

```markdown
## Security: LZ4 Migration (CVE-2025-12183, CVE-2025-66566)

`org.lz4:lz4-java` was archived in December 2025 and has two unpatched CVEs:

- **CVE-2025-12183** (CVSS 8.8) — out-of-bounds read
- **CVE-2025-66566** (CVSS 8.2) — uninitialized buffer info leak

This module migrates to the maintained fork **`at.yawk.lz4:lz4-java:1.11.0`**, which keeps the
`net.jpountz.lz4.*` package namespace (binary-compatible — no source changes required).

Because Kafka clients (`kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`) still
declare a transitive dependency on `org.lz4:lz4-java`, this module evicts it via:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

### Downstream consumers

If your application directly depends on `kafka-clients` (or any of its siblings)
**without** going through `bluetape4k-kafka`, add the same `configurations.all { exclude(...) }` block to your build to prevent the vulnerable JAR from appearing on your classpath.

```

#### T12-B. `infra/kafka/README.ko.md` (Korean)

```markdown
## 보안: LZ4 마이그레이션 (CVE-2025-12183, CVE-2025-66566)

`org.lz4:lz4-java` 는 2025년 12월에 아카이브되었으며, 두 개의 미해결 CVE 가 있습니다:

- **CVE-2025-12183** (CVSS 8.8) — 범위 초과 읽기(OOB read)
- **CVE-2025-66566** (CVSS 8.2) — 미초기화 버퍼 정보 유출

본 모듈은 유지보수가 활발한 포크 **`at.yawk.lz4:lz4-java:1.11.0`** 으로 마이그레이션했습니다.
패키지 네임스페이스 `net.jpountz.lz4.*` 가 동일하므로 **바이너리 호환** — 소스 코드 변경 불필요.

Kafka 계열 라이브러리 (`kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`) 가
여전히 `org.lz4:lz4-java` 를 추이적 의존성으로 선언하므로, 다음과 같이 제거합니다:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

### 다운스트림 사용자

`bluetape4k-kafka` 를 거치지 않고 `kafka-clients` 등을 직접 의존하는 경우, 취약 JAR 가 classpath 에 포함되지 않도록 동일한 `configurations.all { exclude(...) }` 블록을 빌드 스크립트에 추가하시기 바랍니다.

```

#### T12-C. T6 에서 exclude 추가된 기타 모듈

T6 에서 수정된 각 모듈의 README.md + README.ko.md 에 다음 간략한 CVE 마이그레이션 안내 추가:

```markdown
## 참고: LZ4 의존성 마이그레이션

`org.lz4:lz4-java` (CVE-2025-12183, CVE-2025-66566) 보안 이슈로 인해
`at.yawk.lz4:lz4-java:1.11.0` 으로 마이그레이션되었습니다.
`net.jpountz.lz4.*` 패키지 네임스페이스가 동일하므로 소스 코드 변경은 불필요합니다.
```

**DoD**:

- [ ] T4/T5/T6 에서 수정된 모든 모듈의 README.md + README.ko.md 업데이트
- [ ] `infra/kafka` README 에 CVE 번호, exclude 코드 블록, downstream 안내 포함
- [ ] 마크다운 코드 펜스 렌더링 오류 없음

**Dependencies**: T4, T5, T6

---

### T13 — Commit + Push (complexity: low)

```bash
git add -A
git status   # 최종 확인

# Kotlin 소스 변경 없음 최종 검증
git diff --name-only HEAD -- '*.kt' | rg -v 'build.gradle' | head

git commit -m "fix: org.lz4 → at.yawk.lz4:1.11.0 으로 마이그레이션 (CVE-2025-12183, CVE-2025-66566)

- buildSrc/Libs.kt: lz4_java GAV 변경 + CVE 코멘트
- infra/kafka, testing/testcontainers (+ T6 추가 모듈): configurations.all exclude org.lz4
- infra/kafka README.md / README.ko.md: CVE 보안 안내 + 다운스트림 마이그레이션 가이드

이슈: #203"

git push -u origin feat/lz4-yawkat-migration
```

**DoD**:

- [ ] 커밋 메시지 한국어 + `fix:` prefix
- [ ] `#203` 이슈 참조 포함
- [ ] `git push` 성공
- [ ] 워킹 트리 clean

**Dependencies**: T1–T12 모두 완료

---

## Dependency Graph

```
T1 ──► T2 ──► T3 ──┬─► T4 ──┐
                   ├─► T5 ──┼──► T7 ──┬─► T8  ──┐
                   └─► T6 ──┘         ├─► T9  ──┼──► T11 ──► T12 ──► T13
                                      └─► T10 ──┘
```

**병렬 실행 가능**:

- T4, T5, T6 — T3 완료 후 병렬
- T8, T9, T10 — T7 완료 후 병렬

---

## Verification Summary (전체 Plan DoD)

- [ ] T1–T13 각 task DoD 충족
- [ ] T7 project-wide eviction sweep exit code 0
- [ ] 최소 통과 테스트 모듈: core, io, kafka, lettuce, testcontainers, hibernate-cache-lettuce
- [ ] T11: `.kt` 파일 변경 없음 확인
- [ ] `infra/kafka/README.md` + `README.ko.md` 업데이트 완료
- [ ] T6 변경 모듈 README 동기화 완료
- [ ] `feat/lz4-yawkat-migration` 브랜치 push 완료
- [ ] PR 생성은 MANDATORY pre-PR 체크리스트 충족 후 별도 진행 (code-reviewer 에이전트 포함)

---

## Out of Scope

- PR 생성 (CLAUDE.md MANDATORY pre-PR 체크리스트 별도)
- `org.lz4` 가 classpath 에 없는 모듈에 불필요한 exclude 추가
- `at.yawk.lz4:lz4-java` 를 `1.11.0` 초과로 업그레이드
- Kotlin 소스 레벨 LZ4 API 변경 — namespace 유지로 불필요
- `workshop/`, `examples/`, `x-obsoleted/` 디렉토리 수정
