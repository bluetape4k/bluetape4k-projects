# Issue 1565 Publication Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 공통 Maven POM의 developer organization 메타데이터를 복구하고 publication 경로가 configuration cache를 사용하지 않는다는 계약을 CI, 배포 workflow, 문서, 회귀 테스트에 고정한다.

**Architecture:** `applyBluetape4kPomMetadata`를 모든 publication의 단일 메타데이터 원본으로 유지한다. Gradle publication 모델을 configuration-cache 호환 구조로 재설계하지 않고, 이미 CI가 사용하는 `--no-configuration-cache` 정책과 실제 `checkPomFileForBluetape4kPublication` 검증을 배포 경계에 강제한다.

**Tech Stack:** Gradle 9.7.0, Kotlin DSL/buildSrc, Python unittest, GitHub Actions, Maven POM validation

---

### Task 1: 결함 재현과 RED 정책 계약

**Files:**
- Modify: `scripts/test_release_workflow_policy.py`

- [x] `:bluetape4k-core:checkPomFileForBluetape4kPublication`를 `--no-configuration-cache`로 실행해 developer `organization`/`organizationUrl` 누락 실패를 기록한다.
- [x] `:bluetape4k-core:generatePomFileForBluetape4kPublication`를 `--configuration-cache-problems=fail`로 실행해 `Project`, `Configuration`, `DependencyHandler` 직렬화 실패를 기록한다.
- [x] CI, RELEASE, SNAPSHOT publication 검증 명령이 `checkPomFileForBluetape4kPublication`, `--no-configuration-cache`, `--no-build-cache`를 포함해야 한다는 테스트를 추가한다.
- [x] `python3 -m unittest scripts.test_release_workflow_policy`를 실행해 현재 workflow가 `checkPomFileForBluetape4kPublication` 누락으로 실패하는 RED를 확인한다.

### Task 2: 공통 POM 메타데이터 복구

**Files:**
- Modify: `buildSrc/src/main/kotlin/PublishingSigningSupport.kt`

- [x] 공통 developer에 `organization.set("Bluetape4k")`와 `organizationUrl.set("https://github.com/bluetape4k")`을 추가한다.
- [x] `:bluetape4k-core:generatePomFileForBluetape4kPublication`와 `:bluetape4k-core:checkPomFileForBluetape4kPublication`를 명시적인 no-cache 옵션으로 실행해 GREEN을 확인한다.
- [x] 생성된 core POM에서 organization 두 필드를 읽어 실제 XML을 확인한다.

### Task 3: publication workflow와 문서 계약 단일화

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/release.yml`
- Modify: `.github/workflows/publish-snapshot.yml`
- Modify: `README.ko.md`
- Modify: `README.md`

- [x] 세 workflow의 publication metadata 검증 명령에 `checkPomFileForBluetape4kPublication`를 추가한다.
- [x] README의 두 SNAPSHOT 명령에 `--no-configuration-cache`를 추가하고 한·영 예시를 동일하게 유지한다.
- [x] 정책 단위 테스트를 다시 실행해 GREEN을 확인한다.

### Task 4: 전체 publication 검증

**Files:**
- Verify: `scripts/publication/*.rb`

- [x] `./gradlew generatePomFileForBluetape4kPublication checkPomFileForBluetape4kPublication generateMetadataFileForBluetape4kPublication -PsnapshotVersion=-SNAPSHOT --no-daemon --no-configuration-cache --no-build-cache`를 실행한다.
- [x] publication inventory, POM, module metadata 단위 테스트와 실제 생성물 validator를 실행한다.
- [x] `actionlint`, `git diff --check`, Kotlin/buildSrc 검증, README 한국어 용어 감사를 실행한다.
- [x] #1562의 세 TenantContext artifact는 아직 `develop`에 없음을 명시하고 dirty worktree를 수정하지 않는다.
- [x] `docs/testlogs/2026-08.md`에 RED/GREEN과 전체 publication 검증 결과를 기록한다.
- [x] `docs/superpowers/index/`가 `origin/develop`에 없음을 확인하고 legacy index 갱신을 N/A로 기록한다.

### Task 5: 검토와 PR 전달

**Files:**
- Create when reusable: `docs/lessons/2026-08-28-publication-configuration-cache-policy.md`

- [x] 독립 리뷰에서 P0=0/P1=1을 확인하고 job-level·quoted guard 우회를 RED로 재현한 뒤 36개 정책 테스트 GREEN으로 교정해 알려진 P1=0을 확인한다.
- [x] 재사용 가능한 publication 실패 규칙이면 한국어 lesson을 작성한다.
- [ ] lesson을 merge 후 canonical `develop`에서 GNO에 갱신하고 대표 검색 결과를 확인한다. `bluetape4k-docs` collection의 exclude 목록에 `.worktrees`가 있어 현재 `gno update`는 0 added/updated를 반환했다.
- [x] 독립 리뷰가 발견한 validation 문자열·shell parser false green을 RED로 재현하고 허용된 publication step 전체의 exact fail-closed 계약과 중복 executable invocation 검사를 추가한다.
- [x] Lore protocol에 맞는 한국어 commit을 생성한다.
- [ ] `bluetape4k/bluetape4k-projects`, base `develop`, head `fix/issue-1565-publication-gate`로 한국어 PR을 생성한다.
- [ ] PR 본문을 `## DoD Status`로 끝내고 issue metadata를 반영한다.
- [ ] exact-head CI와 review thread를 확인해 merge-ready에서 중단한다. 병합은 별도의 최신 승인을 받는다.
