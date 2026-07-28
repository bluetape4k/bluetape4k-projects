# Cassandra learning path introduction 구현 계획

> **agentic worker용:** 필수 sub-skill: 이 계획은 superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task별 구현한다. 진행 상태는 checkbox(`- [ ]`) syntax로 추적한다.

**목표:** Cassandra 모듈 landing에서 제공 기능과 다섯 개 상세 학습 장의 가치, 예제, 학습 결과를 바로 이해하고 각 장으로 이동하게 만든다.

**아키텍처:** Canonical source인 `bluetape4k-projects/docs/manual`의 한국어와 영문 landing을 같은 정보 구조로 수정한다. Projects 검증과 커밋이 끝난 source commit으로 Site의 1.11 snapshot을 refresh하되 `releaseRef=1.11.0`과 immutable release commit은 유지한다. 제목 변경 전 fragment로 들어오는 링크를 위해 이전 heading slug를 빈 `span` anchor로 보존한다.

**기술 스택:** Markdown, Ruby manual validators, Node.js manual snapshot pipeline, Astro/Starlight, npm test/build

---

## 파일 지도

- 수정: `docs/manual/ko/modules/bluetape4k-cassandra.md` — 자연스러운 한국어 기능 소개와 설명형 학습 경로
- 수정: `docs/manual/en/modules/bluetape4k-cassandra.md` — 한국어와 같은 정보 밀도의 영문 기능 소개와 학습 경로
- site repo에서 refresh: `src/content/docs/{ko/,}manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md` — canonical source에서 생성되는 versioned landing
- site repo에서 refresh: `src/data/manual/bluetape4k-projects*.json`, `.manual-sync-generation.json` — 새 source commit과 deterministic snapshot metadata

### Task 1: 한국어 landing을 설명형 학습 경로로 바꾼다

**파일:**

- 수정: `docs/manual/ko/modules/bluetape4k-cassandra.md`
- 테스트: `scripts/manual/validate_manuals_test.rb`

- [ ] **Step 1: 변경 전 상태가 새 계약을 만족하지 않는지 확인**

실행:

```bash
! rg -q '^## 제공하는 기능 \{#problem\}$' docs/manual/ko/modules/bluetape4k-cassandra.md
! rg -q '각 장은 문제를 이해하는 설명에서 시작해' docs/manual/ko/modules/bluetape4k-cassandra.md
```

예상 결과: 두 명령 모두 exit 0. 아직 새 heading과 학습 안내가 없음을 증명한다.

- [ ] **Step 2: 기능 소개 heading과 본문을 교체**

기존 `## 이 라이브러리가 맡는 일 {#problem}` 앞에 다음 호환 anchor를 두고 heading을 바꾼다.

```markdown
<span id="이-라이브러리가-맡는-일"></span>

## 제공하는 기능 {#problem}

`bluetape4k-cassandra`는 Apache Cassandra Java Driver 위에 Kotlin용 세션 생성 함수, 세션 재사용 경계, 코루틴 쿼리, row mapping과 statement 확장을 제공합니다. 짧은 작업에서 세션을 직접 만들고 닫는 흐름부터 여러 페이지를 비동기로 읽고 드라이버 값을 Kotlin 타입으로 옮기는 작업까지, 애플리케이션 코드에서 반복되는 부분을 줄여 줍니다.

이 모듈이 Cassandra cluster나 schema를 운영하는 것은 아닙니다. 접속 주소, 인증 정보, keyspace, consistency와 세션 종료 시점은 애플리케이션이 결정해야 합니다.
```

- [ ] **Step 3: 링크 목록을 학습 결과가 보이는 경로로 교체**

`## 학습 경로 {#concepts}` 아래를 다음 내용으로 교체한다.

```markdown
아래 다섯 장은 API 이름만 나열하지 않습니다. 각 장은 문제를 이해하는 설명에서 시작해 실행 가능한 예제, API 선택 기준, 실패와 운영 경계, 1.11.0 소스와 테스트 근거까지 연결합니다. 처음 도입한다면 순서대로 읽고, 이미 사용 중이라면 지금 해결하려는 문제에 맞는 장부터 시작해도 됩니다.

1. **[CqlSession 수명주기와 캐시 경계](./bluetape4k-cassandra/session-lifecycle.md)**
   직접 만든 세션을 `use`로 닫는 가장 작은 예제부터 `CqlSessionProvider`와 `CqlSessionIdentity`로 공유 세션을 재사용하는 방법까지 설명합니다. 세션 소유권, 캐시 identity와 1.11.0 bootstrap 설정을 어디에 둘지 판단할 수 있습니다.
2. **[코루틴 쿼리](./bluetape4k-cassandra/coroutine-queries.md)**
   `executeSuspending`, `prepareSuspending`과 `AsyncResultSet.asFlow()`로 단일 결과와 여러 페이지를 읽는 예제를 다룹니다. 취소, mapper 예외와 다음 페이지 조회가 호출자에게 어떻게 전달되는지 확인할 수 있습니다.
3. **[Row와 data mapping](./bluetape4k-cassandra/rows-data-mapping.md)**
   `Row`, collection, tuple, UDT와 `CqlDuration`을 Kotlin 값과 도메인 객체로 옮기는 예제를 제공합니다. null을 기본값으로 바꿀 때와 값이 없다는 사실을 보존할 때를 구분할 수 있습니다.
4. **[Statement와 query builder](./bluetape4k-cassandra/statements-query-builder.md)**
   raw CQL, prepared/bound statement와 QueryBuilder로 같은 작업을 표현하는 방법을 비교합니다. bind marker, consistency, timeout, page size와 keyspace를 어느 경계에서 드러낼지 선택할 수 있습니다.
5. **[운영과 테스트](./bluetape4k-cassandra/operations-testing.md)**
   keyspace 생성·삭제의 side effect, 세션 종료, paging 실패와 Testcontainers 검증을 한 흐름으로 정리합니다. 운영 권한을 애플리케이션과 배포 단계 중 어디에 둘지 결정하고 대표 장애를 진단할 수 있습니다.
```

- [ ] **Step 4: 한국어 문구와 구조 검증**

실행:

```bash
rg -n '^## 제공하는 기능|각 장은 문제를 이해하는 설명에서 시작해|직접 만든 세션을|운영 권한을' docs/manual/ko/modules/bluetape4k-cassandra.md
ruby scripts/manual/validate_manuals_test.rb
```

예상 결과: heading과 소개·첫 장·마지막 장 문구가 검색되고 `14 runs, 41 assertions, 0 failures, 0 errors`.

### Task 2: 영문 landing을 같은 정보 구조로 맞춘다

**파일:**

- 수정: `docs/manual/en/modules/bluetape4k-cassandra.md`
- 테스트: `scripts/manual/validate_manuals_test.rb`

- [ ] **Step 1: 변경 전 영문 상태 확인**

실행:

```bash
! rg -q '^## Features \{#problem\}$' docs/manual/en/modules/bluetape4k-cassandra.md
! rg -q 'Each chapter starts with the problem' docs/manual/en/modules/bluetape4k-cassandra.md
```

예상 결과: 두 명령 모두 exit 0.

- [ ] **Step 2: 영문 기능 소개를 한국어 계약과 맞춤**

기존 `## What this library owns {#problem}` 구역을 다음 내용으로 교체한다.

```markdown
<span id="what-this-library-owns"></span>

## Features {#problem}

`bluetape4k-cassandra` adds Kotlin session factories, session-reuse boundaries, coroutine queries, row mapping, and statement extensions to the Apache Cassandra Java Driver. It removes repetitive application code from short-lived sessions through asynchronous multi-page reads and conversion of driver values into Kotlin types.

The module does not operate the Cassandra cluster or its schema. The application still owns contact points, credentials, keyspaces, consistency, and session shutdown.
```

- [ ] **Step 3: 영문 학습 경로를 설명형 목록으로 교체**

`## Learning path {#concepts}` 아래를 다음 내용으로 교체한다.

```markdown
The five chapters below do more than list API names. Each chapter starts with the problem, then connects runnable examples, API selection rules, failure and operational boundaries, and the supporting 1.11.0 source and tests. Read them in order when adopting the module, or jump directly to the chapter that matches a problem in an existing application.

1. **[CqlSession lifecycle and cache boundaries](./bluetape4k-cassandra/session-lifecycle.md)**
   Start with the smallest `use`-scoped session example, then move to shared-session reuse with `CqlSessionProvider` and `CqlSessionIdentity`. This chapter helps you decide session ownership, cache identity, and where 1.11.0 bootstrap settings belong.
2. **[Coroutine queries](./bluetape4k-cassandra/coroutine-queries.md)**
   Follow single-result and multi-page examples built with `executeSuspending`, `prepareSuspending`, and `AsyncResultSet.asFlow()`. See how cancellation, mapper failures, and next-page fetch failures reach the caller.
3. **[Rows and data mapping](./bluetape4k-cassandra/rows-data-mapping.md)**
   Map `Row`, collections, tuples, UDTs, and `CqlDuration` into Kotlin values and domain objects. Learn when a null may become a domain default and when absence must remain explicit.
4. **[Statements and query builder](./bluetape4k-cassandra/statements-query-builder.md)**
   Compare raw CQL, prepared and bound statements, and QueryBuilder for the same work. Choose where bind markers, consistency, timeout, page size, and keyspace should remain visible.
5. **[Operations and testing](./bluetape4k-cassandra/operations-testing.md)**
   Connect keyspace side effects, session shutdown, paging failures, and Testcontainers verification. Decide whether production DDL authority belongs to the application or deployment and diagnose representative failures.
```

- [ ] **Step 4: locale parity와 canonical manual 검증**

실행:

```bash
ruby scripts/manual/validate_manuals_test.rb
ruby scripts/manual/export_manifest_test.rb
ruby scripts/manual/release_contract_test.rb
ruby scripts/manual/generate_manuals_test.rb
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/export_manifest.rb --check
git diff --check
```

예상 결과: 총 `35 runs, 108 assertions`, failure/error 0, `Manuals are aligned.`, `Manual manifest snapshot is current.`

- [ ] **Step 5: canonical source 커밋**

```bash
git add docs/manual/ko/modules/bluetape4k-cassandra.md docs/manual/en/modules/bluetape4k-cassandra.md
git commit -m "Make Cassandra learning chapters worth entering" -m "Constraint: The landing must explain chapter value without duplicating full chapter content.
Rejected: Link-only learning navigation | readers cannot see the examples or decisions behind each chapter.
Confidence: high
Scope-risk: narrow
Directive: Keep Korean and English chapter outcomes aligned.
Tested: Projects manual validators and release contract checks
Not-tested: Site snapshot and rendered routes"
```

### Task 3: Site 1.11 snapshot을 canonical source로 갱신한다

**파일:**

- 수정: `.manual-sync-generation.json`
- 수정: `src/content/docs/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md`
- 수정: `src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md`
- 수정: `src/data/manual/bluetape4k-projects.1.11.manifest.json`
- 수정: `src/data/manual/bluetape4k-projects.1.11.snapshot.json`
- 수정: `src/data/manual/bluetape4k-projects.manifest.json`
- 수정: `src/data/manual/bluetape4k-projects.snapshot.json`
- 수정: `src/data/manual/bluetape4k-projects.versions.json`

- [ ] **Step 1: 새 source commit으로 1.11 snapshot refresh**

Run in `/Users/debop/work/bluetape4k/bluetape4k.github.io`:

```bash
npm run sync:manual -- --refresh 1.11.0 --source /Users/debop/work/bluetape4k/bluetape4k-projects
```

예상 결과: `release=1.11.0`, release commit `6187173b58e8b4c5c435c145e00e94708f31ef75`, Projects의 새 source commit, documents 244, assets 30.

- [ ] **Step 2: 생성된 landing과 이전 fragment 호환성 확인**

실행:

```bash
rg -n '^## 제공하는 기능|id="이-라이브러리가-맡는-일"|각 장은 문제를 이해하는 설명에서 시작해' src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md
rg -n '^## Features|id="what-this-library-owns"|Each chapter starts with the problem' src/content/docs/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md
npm run sync:manual -- --check
npm run check:manual
```

예상 결과: 새 heading과 설명형 학습 경로, 이전 fragment anchor가 모두 존재하고 snapshot은 `changed=false`, valid.

- [ ] **Step 3: Site 전체 검증**

실행:

```bash
npm test
npm run build
git diff --check
```

예상 결과: 86 tests pass, Astro 47 files에서 error/warning/hint 0, 393 pages build, Pagefind 637 HTML files.

- [ ] **Step 4: 브라우저에서 한국어·영문 landing 확인**

다음 route를 확인한다.

```text
http://127.0.0.1:4323/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/
http://127.0.0.1:4323/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/
```

확인 항목:

- `제공하는 기능` / `Features` heading이 보인다.
- 학습 경로 다섯 항목마다 설명이 있고 chapter link가 동작한다.
- 기존 한국어·영문 heading fragment가 기능 소개 위치로 이동한다.
- desktop과 390px mobile에서 horizontal overflow, broken image, console error가 없다.

- [ ] **Step 5: Site snapshot 커밋**

```bash
git add .manual-sync-generation.json src/content/docs/manual src/content/docs/ko/manual src/data/manual
git commit -m "Show the value behind Cassandra learning chapters" -m "Constraint: The versioned 1.11 landing must come from the canonical Projects manual while preserving release provenance.
Rejected: Editing generated Site Markdown directly | the next sync would erase the change.
Confidence: high
Scope-risk: narrow
Directive: Refresh the snapshot after canonical manual copy changes.
Tested: npm test; npm run build; manual snapshot check; desktop and mobile route review
Not-tested: remote GitHub Pages deployment"
```

### Task 4: 종료 상태를 증명한다

- [ ] **Step 1: 두 저장소 상태와 provenance 확인**

```bash
git -C /Users/debop/work/bluetape4k/bluetape4k-projects status --short
git -C /Users/debop/work/bluetape4k/bluetape4k-projects log -2 --oneline
git -C /Users/debop/work/bluetape4k/bluetape4k.github.io status --short
git -C /Users/debop/work/bluetape4k/bluetape4k.github.io log -2 --oneline
```

예상 결과: 두 main checkout은 clean이고 Site snapshot의 `sourceCommit`은 새 Projects manual commit, `releaseCommit`은 `6187173b58e8b4c5c435c145e00e94708f31ef75`다. Push, PR, deploy는 수행하지 않는다.
