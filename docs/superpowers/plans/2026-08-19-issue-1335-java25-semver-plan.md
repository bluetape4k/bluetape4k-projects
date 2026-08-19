# Java 25 기본 classfile과 2.0.0 SemVer 호환성 계약 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 일반 published artifact의 Java 25 runtime 바닥선과 다섯 모듈의 Java 21 호환성 섬을 `2.0.0` release contract로 고정하고, 문서·CI·release workflow가 동일한 계약을 검증하게 한다.

**Architecture:** `gradle.properties`를 version source of truth로 유지하고 기존 중앙 JVM target 계산은 변경하지 않는다. 정적 Python contract test는 version/module-set/document/workflow drift를 검증하고, 독립 shell check는 대표 classfile의 실제 major version을 검증한다. CI와 release workflow는 publish 전에 두 검사를 호출한다.

**Tech Stack:** Kotlin 2.4, Gradle 9.7.0, Java 25 toolchain, Python 3.12 `unittest`, POSIX Bash, GitHub Actions.

---

## 파일 책임 지도

| 파일 | 책임 |
| --- | --- |
| `gradle.properties` | `baseVersion=2.0.0` 및 빈 `snapshotVersion`의 source of truth |
| `README.md`, `README.ko.md` | Java 25 runtime floor, Java 21 compatibility island, 소비자 migration 안내 |
| `CHANGELOG.md` | 배포 전 `Unreleased` 호환성 변경 기록 |
| `scripts/test_jvm_release_contract.py` | version, module set, 문서 marker, workflow hook의 정적 계약 |
| `scripts/check-jvm-release-contract.sh` | 대표 산출물 compile 및 `javap` classfile major 계약 |
| `.github/workflows/ci.yml` | pull request/push에서 정적·classfile 계약 실행 |
| `.github/workflows/release.yml` | Maven Central publish 전에 동일 계약 재실행 |
| `docs/superpowers/specs/2026-08-19-issue-1335-java25-semver-design.md` | 승인된 설계 결정의 기록(이미 커밋됨) |

### Task 1: 정적 JVM release contract test를 먼저 작성한다

**Files:**
- Create: `scripts/test_jvm_release_contract.py`
- Test: `scripts/test_jvm_release_contract.py`

- [x] **Step 1: 현재 상태에서 실패하는 계약 테스트를 작성한다**

`scripts/test_jvm_release_contract.py`는 아래 구현을 사용한다. Marker 추출은 README/CHANGELOG의 계약 범위만 읽고, module set은 `build.gradle.kts`의 중앙 선언만 읽는다.

```python
#!/usr/bin/env python3

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MARKER = "issue-1335-java25-semver"
EXPECTED_ISLAND = {
    "bluetape4k-assertions",
    "bluetape4k-junit5",
    "bluetape4k-logging",
    "bluetape4k-virtualthread-api",
    "bluetape4k-virtualthread-jdk21",
}
COMMON_TOKENS = ("2.0.0", "1.13.x", "Java 25", "Java 21")

def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")

def block(text: str, marker: str = MARKER) -> str:
    pattern = rf"<!-- {re.escape(marker)}:start -->(.*?)<!-- {re.escape(marker)}:end -->"
    match = re.search(pattern, text, re.DOTALL)
    if match is None:
        raise AssertionError(f"missing marker block: {marker}")
    return match.group(1)

class JvmReleaseContractTest(unittest.TestCase):
    def test_version_source_is_2_0_0_release_ready(self) -> None:
        properties = read("gradle.properties")
        self.assertRegex(properties, r"(?m)^baseVersion=2\.0\.0$")
        self.assertRegex(properties, r"(?m)^snapshotVersion=$")

    def test_java_21_island_and_default_target_are_explicit(self) -> None:
        build = read("build.gradle.kts")
        match = re.search(
            r"private val java21CompatibilityProjects = setOf\((.*?)\n\)",
            build,
            re.DOTALL,
        )
        self.assertIsNotNone(match)
        modules = set(re.findall(r'"([^"]+)"', match.group(1)))
        self.assertEqual(EXPECTED_ISLAND, modules)
        self.assertIn(
            "val javaCompatibilityVersion = if (project.name in java21CompatibilityProjects) 21 else 25",
            build,
        )
        self.assertIn(
            "val kotlinJvmTarget = if (javaCompatibilityVersion == 21) JvmTarget.JVM_21 else JvmTarget.JVM_25",
            build,
        )
        self.assertIn("options.release.set(javaCompatibilityVersion)", build)

    def test_readme_locales_share_the_migration_contract(self) -> None:
        english = block(read("README.md"))
        korean = block(read("README.ko.md"))
        for token in COMMON_TOKENS:
            self.assertIn(token, english)
            self.assertIn(token, korean)
        self.assertIn("baseVersion=2.0.0", read("README.md"))
        self.assertIn("baseVersion=2.0.0", read("README.ko.md"))
        self.assertNotIn("baseVersion=1.11.0", read("README.md"))
        self.assertNotIn("baseVersion=1.11.0", read("README.ko.md"))

    def test_changelog_records_unreleased_compatibility_change(self) -> None:
        changelog = block(read("CHANGELOG.md"))
        self.assertIn("## [Unreleased]", changelog)
        self.assertIn("#1335", changelog)
        for token in COMMON_TOKENS:
            self.assertIn(token, changelog)

    def test_ci_and_release_workflows_run_both_contract_checks(self) -> None:
        for workflow in (read(".github/workflows/ci.yml"), read(".github/workflows/release.yml")):
            self.assertIn("scripts/test_jvm_release_contract.py", workflow)
            self.assertIn("scripts/check-jvm-release-contract.sh", workflow)

if __name__ == "__main__":
    unittest.main()
```

- [x] **Step 2: 테스트가 현재 base에서 실패하는지 확인한다**

Run: `python3 -m unittest scripts/test_jvm_release_contract.py -v`

Expected: 현재 `baseVersion=1.13.0`과 marker/workflow 부재 때문에 실패한다. 이 실패는 구현 전 red 상태를 증명한다.

- [x] **Step 3: 정적 테스트 파일 자체의 문법을 확인한다**

Run: `python3 -m py_compile scripts/test_jvm_release_contract.py`

Expected: exit code `0`; 생성된 `__pycache__`는 커밋하지 않는다.

### Task 2: version source와 EN/KO reader-facing contract를 구현한다

**Files:**
- Modify: `gradle.properties:81`
- Modify: `README.md` (Tech Stack 뒤, Publishing 예제)
- Modify: `README.ko.md` (기술 스택 뒤, 배포 예제)
- Modify: `CHANGELOG.md` (최상단 release entry 앞)

- [x] **Step 1: version source를 2.0.0으로 변경한다**

`gradle.properties`에서 다음 두 줄을 정확히 유지한다.

```properties
baseVersion=2.0.0
snapshotVersion=
```

`build.gradle.kts`의 publication 계산과 Java target 중앙 설정은 수정하지 않는다.

- [x] **Step 2: 영어 README에 계약 marker와 migration 문단을 추가한다**

Tech Stack 섹션 뒤에 다음 marker block을 추가한다.

```markdown
<!-- issue-1335-java25-semver:start -->
### JVM Compatibility and 2.0.0

Starting with `2.0.0`, general Bluetape4k artifacts require a Java 25 runtime.
The five-module Java 21 compatibility island is limited to
`bluetape4k-assertions`, `bluetape4k-junit5`, `bluetape4k-logging`,
`bluetape4k-virtualthread-api`, and `bluetape4k-virtualthread-jdk21`; it does
not make every artifact Java 21 compatible.

Consumers on Java 21–24 should move to Java 25 for `2.0.0` or remain on
`1.13.x`. Consumers that must stay on Java 21 should select only the
compatibility-island modules and must not mix Java 25-targeted artifacts into
the same classpath.
<!-- issue-1335-java25-semver:end -->
```

Publishing 예제의 `baseVersion=1.11.0`을 `baseVersion=2.0.0`으로 바꾼다.

- [x] **Step 3: 한국어 README에 의미가 같은 marker와 migration 문단을 추가한다**

동일한 marker를 사용하고 한국어로 Java 25 일반 artifact, 다섯 모듈 Java 21 섬, `2.0.0`/`1.13.x` 선택지를 설명한다. Publishing 예제의 `baseVersion=1.11.0`은 `baseVersion=2.0.0`으로 바꾼다.

- [x] **Step 4: CHANGELOG에 배포 전 호환성 변경을 기록한다**

파일 최상단의 최신 release heading 앞에 실제 날짜가 없는 다음 block을 추가한다.

```markdown
<!-- issue-1335-java25-semver:start -->
## [Unreleased]

### 호환성 변경

- 일반 published artifact의 Java runtime 바닥선을 Java 25로 올릴 준비를
  하고 `2.0.0` 호환성 계약을 고정했다. Java 21 호환성 섬은
  `bluetape4k-assertions`, `bluetape4k-junit5`, `bluetape4k-logging`,
  `bluetape4k-virtualthread-api`, `bluetape4k-virtualthread-jdk21`로
  유지한다. Java 21–24 소비자는 Java 25로 이동하거나 `1.13.x`를 유지해야
  한다 ([#1335](https://github.com/bluetape4k/bluetape4k-projects/issues/1335)).
<!-- issue-1335-java25-semver:end -->
```

- [x] **Step 5: 정적 contract test를 다시 실행한다**

Run: `python3 -m unittest scripts/test_jvm_release_contract.py -v`

Expected: version/module/document 검사는 통과하고, classfile/workflow 검사는 Task 3–4 완료 전까지 실패한다.

### Task 3: 실제 classfile major 검증 스크립트를 구현한다

**Files:**
- Create: `scripts/check-jvm-release-contract.sh`
- Test: `scripts/check-jvm-release-contract.sh`

- [x] **Step 1: shell checker를 작성한다**

아래 구현은 compile task와 representative classfile path를 한 곳에서 고정한다.

```bash
#!/usr/bin/env bash

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"

readonly GENERAL_CLASS="$ROOT/testing/testcontainers/build/classes/kotlin/main/io/bluetape4k/testcontainers/PropertyExportingServer.class"
readonly JAVA21_CLASS="$ROOT/virtualthread/jdk21/build/classes/kotlin/main/io/bluetape4k/concurrent/virtualthread/jdk21/Jdk21StructuredTaskScopeProvider.class"
readonly JAVA25_CLASS="$ROOT/virtualthread/jdk25/build/classes/kotlin/main/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProvider.class"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

assert_major() {
  local class_file="$1"
  local expected="$2"
  [[ -f "$class_file" ]] || fail "missing classfile: $class_file"
  local actual
  actual="$(javap -verbose "$class_file" | awk '/major version:/{print $3; exit}')"
  [[ "$actual" == "$expected" ]] ||
    fail "classfile major mismatch: file=$class_file expected=$expected actual=$actual"
  printf 'OK: %s major=%s\n' "${class_file#"$ROOT"/}" "$actual"
}

"$ROOT/gradlew" \
  :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-virtualthread-jdk21:compileKotlin \
  :bluetape4k-virtualthread-jdk25:compileKotlin \
  --no-daemon --no-configuration-cache

assert_major "$GENERAL_CLASS" 69
assert_major "$JAVA21_CLASS" 65
assert_major "$JAVA25_CLASS" 69
```

- [x] **Step 2: 실행 권한과 shell 문법을 검증한다**

Run:

```bash
chmod +x scripts/check-jvm-release-contract.sh
bash -n scripts/check-jvm-release-contract.sh
```

Expected: `bash -n` exit code `0`; 실행 권한 변경은 같은 commit에 포함한다.

- [x] **Step 3: classfile checker를 실행한다**

Run: `./scripts/check-jvm-release-contract.sh`

Expected: 세 대표 classfile이 각각 `major=69`, `major=65`, `major=69`로 출력되고 exit code `0`이다.

### Task 4: CI와 release workflow에 계약 검증을 연결한다

**Files:**
- Modify: `.github/workflows/ci.yml` (jobs 아래 `jvm-release-contract` 추가)
- Modify: `.github/workflows/release.yml` (Java setup/Gradle setup 뒤, publication metadata 앞)

- [x] **Step 1: CI에 독립 JVM release contract job을 추가한다**

`release-policy` job과 같은 checkout 정책을 사용하고 Java 25와 Gradle wrapper를 준비한 뒤 두 검사를 순서대로 실행한다.

```yaml
  jvm-release-contract:
    name: JVM Release Contract
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5.7.0
        with:
          java-version: '25'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v6.3.0
        with:
          gradle-version: wrapper
      - name: Verify static JVM release contract
        run: python3 -m unittest scripts/test_jvm_release_contract.py -v
      - name: Verify compiled classfile contract
        run: ./scripts/check-jvm-release-contract.sh
```

- [x] **Step 2: release workflow publish 전에 같은 검증을 추가한다**

기존 `actions/setup-java`와 `gradle/actions/setup-gradle` 직후에 다음 단계를 둔다.

```yaml
      - name: Verify JVM release contract
        run: |
          python3 -m unittest scripts/test_jvm_release_contract.py -v
          ./scripts/check-jvm-release-contract.sh
```

이 단계는 기존 `Verify gradle.properties matches tag`, publication metadata validation, `nmcpPublishAggregationToCentralPortal` 순서를 유지하면서 publish 직전에 추가된다. signing credential이나 GitHub release 생성은 호출하지 않는다.

- [x] **Step 3: workflow hook 정적 테스트를 실행한다**

Run: `python3 -m unittest scripts/test_jvm_release_contract.py -v`

Expected: 두 workflow가 두 checker를 모두 호출하므로 전체 정적 테스트가 통과한다.

### Task 5: 직접 검증을 수행하고 작업 단위를 커밋한다

**Files:**
- Test: `scripts/test_jvm_release_contract.py`
- Test: `scripts/check-jvm-release-contract.sh`
- Test: `.github/workflows/ci.yml`, `.github/workflows/release.yml`
- Test: `README.md`, `README.ko.md`, `CHANGELOG.md`

- [x] **Step 1: 정적·정책 검증을 실행한다**

Run:

```bash
python3 -m unittest scripts/test_jvm_release_contract.py -v
python3 -m unittest scripts/test_release_workflow_policy.py -v
```

Expected: 두 unittest suite 모두 실패 없이 종료한다.

- [x] **Step 2: classfile 및 targeted compile을 실행한다**

Run: `./scripts/check-jvm-release-contract.sh`

Expected: Gradle compile이 성공하고 representative classfile major가 `69/65/69`로 출력된다.

- [x] **Step 3: 문서·diff 품질 검사를 실행한다**

Run:

```bash
git diff --check
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  README.md README.ko.md CHANGELOG.md \
  docs/superpowers/specs/2026-08-19-issue-1335-java25-semver-design.md \
  docs/superpowers/plans/2026-08-19-issue-1335-java25-semver-plan.md
```

Expected: whitespace 오류와 한국어 용어 감사 finding이 없다.

- [x] **Step 4: 첫 번째 구현 커밋을 만든다**

```bash
git add scripts/test_jvm_release_contract.py gradle.properties README.md README.ko.md CHANGELOG.md
git commit -m "2.0.0 Java 호환성 계약을 문서와 테스트에 반영한다" -m "#1335의 runtime floor 상승을 version source와 reader-facing migration contract로 고정한다.\n\nConstraint: 일반 artifact는 Java 25이고 Java 21 호환성은 다섯 모듈로 제한된다\nRejected: published target을 분리해 1.13.0을 유지하는 안은 이번 Slot 범위를 넘어선다\nConfidence: high\nScope-risk: narrow\nDirective: 후속 #1339는 이 커밋의 선행 merge head에서만 시작한다\nTested: scripts/test_jvm_release_contract.py, git diff --check\nNot-tested: classfile checker와 hosted CI는 다음 커밋/검증 단계에서 실행한다"
```

- [x] **Step 5: 두 번째 구현 커밋을 만든다**

```bash
git add scripts/check-jvm-release-contract.sh .github/workflows/ci.yml .github/workflows/release.yml
git commit -m "Java 25 classfile 계약을 CI와 release workflow에 연결한다" -m "publish 전에 version/document contract와 실제 classfile major를 함께 검증한다.\n\nConstraint: release workflow는 기존 tag, credential, Maven-only policy를 보존해야 한다\nRejected: 문서-only 검증은 잘못된 classfile target을 탐지하지 못하므로 제외했다\nConfidence: high\nScope-risk: narrow\nDirective: publish side effect보다 앞에서 계약 검증을 실패시켜야 한다\nTested: JVM release contract, release workflow policy, targeted Gradle compile\nNot-tested: hosted GitHub Actions 실행 결과는 PR CI에서 확인한다"
```

- [x] **Step 6: final local state를 확인한다**

Run: `git status --short --branch && git log -2 --oneline`

Expected: worktree가 clean이고 두 구현 커밋이 설계·plan 커밋 위에 있으며 branch가 `origin/develop`보다 최소 네 commit 앞선다(설계 1 + plan 1 + 구현 2; writer audit 보정이 있으면 추가 commit을 허용한다).

### Task 6: PR train handoff 증거를 준비한다

**Files:**
- Modify: PR body only after PR creation, with `## DoD Status` as the final heading.

- [x] **Step 1: exact base/head와 issue metadata를 재조회한다**

Run:

```bash
git rev-parse HEAD
git merge-base --is-ancestor origin/develop HEAD
gh issue view 1335 --repo bluetape4k/bluetape4k-projects --json number,state,milestone,assignees,labels
```

Expected: branch head가 설계·구현 커밋을 포함하고, #1335가 `OPEN`, milestone `1.13.0`, assignee `debop`인 현재 train metadata와 일치한다.

- [ ] **Step 2: PR #1을 #1335에만 연결한다**

PR base는 `develop`, head는 `chore/1418-01-java25-semver`로 고정한다. PR body의 마지막 섹션은 다음 형식을 사용한다.

```markdown
## DoD Status

Epic #1418 progress: 1/4
Required checks: 0/0; N/A: 0; Blocked: 0
Final status: PENDING

| Item | Evidence |
| --- | --- |
| Version | `gradle.properties` `baseVersion=2.0.0`, empty `snapshotVersion` |
| JVM contract | static test and `major=69/65/69` output |
| Docs | EN/KO README markers and `CHANGELOG.md` #1335 entry |
| CI/release hook | both workflows invoke both contract checks |

Unchecked items: hosted CI, fresh review approval, merge, canonical branch sync, and worktree cleanup.
```

- [ ] **Step 3: successor slot을 hold한다**

PR #1의 exact head, required checks, reviews/threads, mergeability, linked issue, fresh merge approval을 다시 읽기 전에는 #1339 branch/PR을 만들지 않는다. PR #1 merge 후 merge commit SHA를 base로 successor branch를 생성한다.

## Plan self-review checklist

- Spec coverage: version/JVM target은 Task 2, 문서는 Task 2, 정적 contract는 Task 1, classfile contract는 Task 3, CI/release hook은 Task 4, migration/DoD/handoff는 Task 5–6에서 다룬다.
- Placeholder scan: 미해결 placeholder나 모호한 구현 지시를 사용하지 않고 정확한 경로·marker·명령·기대 결과를 고정했다.
- Type consistency: README/CHANGELOG marker는 `issue-1335-java25-semver`, 대표 classfile은 `PropertyExportingServer`/`Jdk21StructuredTaskScopeProvider`/`Jdk25StructuredTaskScopeProvider`, module set은 설계 문서와 동일하다.
