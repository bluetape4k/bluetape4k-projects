# Logging Manual First Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-logging`을 source/test 기반 bilingual chapter와 canonical diagram을 갖춘 manual-first reference로 게시한다.

**Architecture:** Projects의 `docs/manual`이 문서와 diagram 원본을 소유하고 schema v2 manifest가 inventory를 선언한다. Site는 Projects commit을 deterministic snapshot으로 동기화하며 blog는 manual route를 참조하는 파생 글로 유지한다.

**Tech
Stack:** Markdown, YAML, Kotlin source/tests, SVG, CairoSVG, Ruby/Minitest manual validator, Node.js snapshot tests, Astro/Starlight

---

### Task 1: Core 명칭과 Logging landing/chapter inventory

**Files:**

- Modify: `docs/manual/{ko,en}/modules/bluetape4k-core.md`
- Modify: `docs/manual/{ko,en}/modules/bluetape4k-logging.md`
- Create: `docs/manual/{ko,en}/modules/bluetape4k-logging/{logger-foundation,lazy-messages,scoped-mdc,coroutine-mdc,async-channel,operations-recipes}.md`

- [ ] **Step 1:** Core title/H1을 `Core Kotlin library` / `Core Kotlin 라이브러리`로 정렬한다.
- [ ] **Step 2:** Logging landing에 선택 지도와 6개 chapter 학습 경로를 작성한다.
- [ ] **Step 3:** KO chapter 6개를 current source/test 링크와 독립 실행 예제로 작성한다.
- [ ] **Step 4:** EN chapter 6개를 의미·순서 parity로 작성한다.
- [ ] **Step
  5:** `ruby scripts/manual/validate_manuals.rb`를 실행해 아직 manifest 미등록 chapter가 orphan이 아닌지 확인하고 다음 task로 진행한다.

### Task 2: Canonical logging diagrams

**Files:**

- Create: `docs/manual/assets/logging/logger-api-map.{svg,png}`
- Create: `docs/manual/assets/logging/mdc-scope-lifecycle.{svg,png}`
- Create: `docs/manual/assets/logging/async-channel-sequence.{svg,png}`

- [ ] **Step 1:** source-backed reader question과 architecture/sequence reference PNG를 ledger에 고정한다.
- [ ] **Step 2:** `logger-api-map.svg`를 작성하고 XML → CairoSVG scale 2 → audits → full-size PNG 순서로 검증한다.
- [ ] **Step 3:** `mdc-scope-lifecycle.svg`를 같은 one-asset loop로 검증한다.
- [ ] **Step 4:** `async-channel-sequence.svg`를 participant/lifeline/activation/numbered messages/close branch와 함께 검증한다.
- [ ] **Step 5:** chapter image link와 SVG/PNG pair를 확인한다.

### Task 3: Manifest와 Projects verification

**Files:**

- Modify: `docs/manual/manifest.yaml`
- Modify: `docs/superpowers/checklists/2026-07-12-logging-manual-checklist.md`

- [ ] **Step 1:** logging entry에 6개 chapter와 6개 asset path를 같은 순서로 등록한다.
- [ ] **Step 2:** `ruby scripts/manual/validate_manuals_test.rb`와 `ruby scripts/manual/export_manifest_test.rb`를 실행한다.
- [ ] **Step 3:** `ruby scripts/manual/validate_manuals.rb`와 manifest export current check를 실행한다.
- [ ] **Step 4:** `./gradlew :bluetape4k-logging:test --no-configuration-cache`를 실행한다.
- [ ] **Step 5:** diff/checklist를 review하고 Lore commit으로 Projects checkpoint를 만든다.

### Task 4: Site snapshot과 derived blog

**Files:**

- Generated: `bluetape4k.github.io/src/content/docs/{ko/,}manual/bluetape4k-projects/**`
- Generated: `bluetape4k.github.io/public/manual-assets/bluetape4k-projects/logging/**`
- Modify: `bluetape4k.github.io/src/content/docs/{ko/,}blog/bluetape4k-projects-part2-core-coroutines-tests.mdx`

- [ ] **Step 1:** Projects checkpoint revision으로 site manual sync를 실행한다.
- [ ] **Step 2:** KO/EN blog logging section의 상세 reference를 manual landing/chapter route로 바꾼다.
- [ ] **Step 3:** Node manual tests, snapshot `--check`, Astro check/build를 실행한다.
- [ ] **Step 4:** localhost에서 KO/EN landing, MDC와 async chapter, PNG, blog links/console을 검증한다.
- [ ] **Step 5:** diff/checklist를 review하고 Lore commit으로 Site checkpoint를 만든다.

## Self-review

- Spec coverage: Core rename, 6 bilingual chapters, 3 paired diagrams, manifest, site snapshot, blog, verification이 Task 1–4에 모두 연결됨.
- Placeholder scan: `TBD`, `TODO`, `similar`, 미정 구현 없음.
- Type/path consistency: chapter IDs와 asset basenames가 spec/plan/manifest target에서 동일함.
