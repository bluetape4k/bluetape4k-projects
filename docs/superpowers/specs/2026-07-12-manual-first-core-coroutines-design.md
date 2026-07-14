# Core·Coroutines Manual First 설계

**날짜:** 2026-07-12
**상태:** 승인됨
**대상 저장소:** `bluetape4k-projects`, `bluetape4k.github.io`
**기준 브랜치:** `feature/all-module-manuals`, `feature/ecosystem-atlas-manual`

## 배경

현재 `bluetape4k-projects/docs/manual`에는 모든 모듈의 영문·한글 문서가 있지만, `bluetape4k-core`와 `bluetape4k-coroutines`는 아직 한 페이지 안에서 API 선택과 몇 가지 레시피를 설명하는 수준이다. 반면 `bluetape4k.github.io`의 기존 blog와 workshop 글에는 Flow, Subject, observability, core validation을 설명하는 코드와 diagram이 이미 있다.

이 자료를 그대로 매뉴얼에 복사하는 것은 목표가 아니다. Blog는 일부 주제를 설명하는 파생 콘텐츠이며 완전성이나 최신성을 보장하지 않는다. 앞으로는 기존 blog가 없는 모듈도 매뉴얼을 먼저 작성하고, 필요할 때 매뉴얼을 바탕으로 blog를 작성할 수 있어야 한다.

따라서 기술 문서의 유일한 source of truth를 각 repository의 `docs/manual`로 정한다. Site는 이를 deterministic snapshot으로 게시하고, blog는 매뉴얼의 기술 계약을 독자 친화적인 이야기로 재구성한다.

## 목표

1. `bluetape4k-core`와 `bluetape4k-coroutines`를 README보다 상세한 교과서형 다중 챕터 매뉴얼로 만든다.
2. 문서, diagram source SVG, rendered PNG를 `bluetape4k-projects/docs/manual`에서 함께 소유한다.
3. 매뉴얼 하나만으로 API 선택, 구현, 실패 처리, 운영, 테스트 결정을 내릴 수 있게 한다.
4. `bluetape4k.github.io` sync가 chapter와 asset을 함께 검증·게시하게 한다.
5. 기존 blog와 diagram은 현재 source와 test로 다시 검증한 뒤 초기 재료로만 사용한다.
6. 이후 blog가 매뉴얼에만 존재하는 기술 설명과 asset을 참조해 작성될 수 있게 한다.

## 비목표

- 모든 `bluetape4k-projects` 모듈을 이번 작업에서 같은 깊이로 확장하지 않는다.
- Blog를 매뉴얼의 필수 선행 자료로 만들지 않는다.
- Site repository에 manual 전용 기술 원본이나 diagram 원본을 새로 두지 않는다.
- Production Kotlin API, dependency, runtime 동작은 변경하지 않는다.
- 이번 설계만으로 push, PR, merge, GitHub Pages 배포를 승인하지 않는다.
- 장문의 단일 Markdown 파일 하나에 모든 내용을 넣지 않는다.

## 핵심 원칙

### Manual First

기술 정보 흐름은 단방향이다.

```text
current source + tests
        ↓ 검증
repo/docs/manual
        ↓ deterministic snapshot
bluetape4k.github.io/manual
        ↓ 선택적 재구성
blog
```

새로운 기술 사실은 source/test 또는 manual에 먼저 기록한다. Blog에만 존재하는 API 계약, 실패 정책, 운영 규칙은 허용하지 않는다.

### 독립적인 완결성

각 매뉴얼 chapter는 blog 링크가 없어도 독자가 해당 주제의 선택과 구현을 끝낼 수 있어야 한다. Blog와 workshop 링크는 배경 이야기와 실행 가능한 확장 경로이지, 누락된 설명을 대신하는 외부 의존성이 아니다.

### 소스 기반 설명

모든 public type, 함수, ordering, capacity, lifecycle, failure, cancellation 설명은 현재 implementation 또는 representative test와 연결한다. 기존 blog 문장이 source와 다르면 source/test가 우선하며 blog도 나중에 정렬한다.

### 다중 챕터

Module landing page는 학습 경로와 선택 지도를 제공한다. 세부 개념과 예제는 하위 chapter로 분리해 한 파일의 길이와 변경 충돌을 제한한다.

### Asset 일원화

Manual diagram은 SVG와 PNG를 한 쌍으로 관리한다. SVG는 편집 가능한 canonical source이고, PNG는 실제 독자가 보는 rendered truth다. 두 파일 모두 `docs/manual/assets`가 소유한다.

## 저장소 구조

```text
docs/manual/
├── manifest.yaml
├── ko/
│   └── modules/
│       ├── bluetape4k-coroutines.md
│       ├── bluetape4k-coroutines/
│       │   ├── lifecycle.md
│       │   ├── deferred.md
│       │   ├── flow.md
│       │   ├── subjects.md
│       │   ├── structured-concurrency.md
│       │   ├── operations.md
│       │   └── recipes.md
│       ├── bluetape4k-core.md
│       └── bluetape4k-core/
│           ├── validation.md
│           ├── bounded-collections.md
│           ├── encoding-data.md
│           ├── time-ranges.md
│           ├── concurrency-lifecycle.md
│           └── recipes.md
├── en/
│   └── modules/
│       └── <Korean과 동일한 chapter inventory>
└── assets/
    ├── coroutines/
    │   ├── <asset>.svg
    │   └── <asset>.png
    └── core/
        ├── <asset>.svg
        └── <asset>.png
```

기존 module landing 경로는 유지한다. 하위 chapter만 추가해 현재 route와 inbound link를 보존한다.

## Manifest 계약

현재 module inventory에 optional `chapters`와 `assets`를 추가한다. 기존 모듈은 두 필드가 없어도 유효하게 유지해 전체 90개 module을 한 번에 마이그레이션하지 않는다.

개념 구조는 다음과 같다.

```yaml
- id: bluetape4k-coroutines
  en: en/modules/bluetape4k-coroutines.md
  ko: ko/modules/bluetape4k-coroutines.md
  chapters:
    - id: lifecycle
      en: en/modules/bluetape4k-coroutines/lifecycle.md
      ko: ko/modules/bluetape4k-coroutines/lifecycle.md
    - id: deferred
      en: en/modules/bluetape4k-coroutines/deferred.md
      ko: ko/modules/bluetape4k-coroutines/deferred.md
  assets:
    - assets/coroutines/scope-lifecycle.svg
    - assets/coroutines/scope-lifecycle.png
```

Validator는 다음을 보장한다.

- chapter ID는 module 안에서 유일하다.
- 영문·한글 chapter path가 모두 존재한다.
- 영문·한글 chapter inventory와 순서가 동일하다.
- chapter frontmatter의 `manualId`, `chapterId`가 manifest와 일치한다.
- manifest에 등록한 asset이 존재한다.
- SVG와 PNG 쌍이 필요한 diagram에는 두 파일이 모두 존재한다.
- Markdown의 상대 image/link가 repository boundary 밖으로 나가지 않는다.
- 존재하지 않는 chapter와 asset을 참조하면 validation이 실패한다.
- 등록되지 않은 manual asset은 orphan으로 보고 실패한다.

## Chapter 작성 계약

모든 chapter는 주제에 맞게 다음 순서를 사용한다.

1. 독자가 해결하려는 문제
2. Mental model과 필요하면 diagram
3. 가장 작은 API surface
4. 실행 가능한 완전한 예제
5. 대안과 선택 기준
6. Failure, cancellation, lifecycle, capacity 계약
7. 운영과 문제 진단
8. Representative test와 source 근거
9. 이어 읽을 chapter와 runnable workshop

모든 항목을 기계적으로 같은 길이로 채우지는 않는다. 다만 실패·운영 계약이나 source 근거가 필요한 주제에서 이를 생략할 수 없다.

## Coroutines 챕터

### Landing — Overview와 학습 경로

- Coroutine helper가 필요한 이유
- 표준 Kotlin coroutine API를 먼저 선택해야 하는 경우
- `DeferredValue`, Deferred coordination, ordered Flow, parallel Flow, Subject, structured policy 선택 지도
- 초급, HTTP service, stream processing, operations 학습 경로

### 1. Lifecycle & Cancellation

- Caller scope와 component-owned scope 구분
- `CloseableCoroutineScope`, `DefaultCoroutineScope`, `ThreadPoolCoroutineScope`
- Dispatcher ownership과 idempotent close
- `CancellationException` 재전파
- Timeout이 local wait와 remote I/O에 미치는 차이
- Scope lifecycle diagram

### 2. Deferred Coordination

- `DeferredValue` eager start와 owned scope
- Deprecated blocking `value`와 `await()` 선택
- `awaitAny`, `awaitAnyAndCancelOthers`, `zip`
- First completion과 first success 차이
- Winner/loser cancellation sequence diagram

### 3. Ordered & Parallel Flow

- `flow.async`의 ordered emission
- `mapParallel`의 bounded concurrency와 unordered result
- `parallelism <= 1` sequential path
- Downstream capacity를 기준으로 concurrency를 정하는 법
- Race/fallback, chunk/window, parallel enrichment diagram 중 source-backed subset

### 4. Subjects & Event Contracts

- `PublishSubject`, `BehaviorSubject`, `ReplaySubject`, `MulticastSubject`, `UnicastWorkSubject`
- Event, latest state, bounded history, coordinated fan-out, work queue 차이
- `awaitCollector()`가 필요한 시작 순서
- Complete/error terminal contract
- Existing Subject marble diagram을 current implementation과 다시 대조해 승격

### 5. Structured Concurrency Policies

- `taskScope`, `firstSuccessTaskScope`, `supervisedTaskScope`
- Fail-fast, partial result, first-success 선택 기준
- JDK structured task/virtual-thread bridge 경계
- Failure ordering과 loser cancellation

### 6. Operations & Observability

- Active job, queue/buffer growth, latency, cancellation, timeout 관찰
- Cancellation을 error span으로 기록하지 않는 규칙
- Readiness와 request acceptance 구분
- Shutdown에서 owned scope와 channel 정리
- Existing observability diagram의 재사용 범위

### 7. Recipes & Workshops

- Request 안에서 여러 suspend call 조합
- Fastest replica와 first-success replica
- Ordered transform과 throughput-first transform
- Callback SDK를 Subject contract로 변환
- Aggregation/windowing과 메모리 상한
- `bluetape4k-workshop`의 runnable example과 representative test 연결

## Core 챕터

### Landing — Overview와 학습 경로

- Core를 helper dump가 아니라 boundary toolkit으로 설명
- Validation, bounded collection, data encoding, time/range, concurrency/lifecycle 선택 지도
- 어떤 기능을 표준 Kotlin/JDK API로 유지해야 하는지 명시

### 1. Validation Contracts

- Nullable, String, Number, Collection validation
- `require*` receiver 반환과 `IllegalArgumentException`
- Internal invariant와 caller input 구분
- Kotlin contract가 downstream type에 미치는 영향
- API boundary recipe

### 2. Bounded Collections

- `BoundedStack`과 `RingBuffer` ordering 차이
- Capacity 초과 시 oldest eviction
- `push/pop`, `add/drop`, iteration 방향
- Process-local structure와 durable queue/backpressure의 경계
- Ordering/eviction diagram

### 3. Encoding & Data Helpers

- Codec, bytes, hash, text boundary를 task 중심으로 분류
- Transport/storage 경계에서 변환을 제한하는 법
- Malformed input과 underlying decoder failure
- Hot path allocation과 측정 필요성

### 4. Time & Ranges

- Duration, period, temporal, quarter, range helper 선택
- Timezone과 boundary semantics
- 표준 `java.time`을 대체하지 않고 보완하는 범위
- Representative source/test recipe

### 5. Concurrency & Lifecycle

- `ConcurrentReducer`의 max concurrency와 queue capacity
- Queue full과 closed submission의 failed future 계약
- Queued job cancellation과 이미 실행 중인 external stage 차이
- `ShutdownQueue`의 reverse registration order
- Capacity flow와 LIFO shutdown diagram

### 6. Recipes & Diagnostics

- Validation에서 domain type 생성
- Undo와 chronological history 선택
- External async API 앞의 overload boundary
- Shutdown registration order
- Failure cause 분류와 troubleshooting table

## 기존 Blog·Asset 이관

초기 참고 대상은 다음과 같다.

- `bluetape4k-projects-part1-shared-foundation.mdx`
- `bluetape4k-projects-part2-core-coroutines-tests.mdx`
- `bluetape4k-flow-extensions-workshop.mdx`
- `coroutine-observability-micrometer-readiness.mdx`
- `bluetape4k-projects-part2-flow.svg/png`
- `bluetape4k-flow-extensions-*-marble-01.svg/png`
- `coroutine-observability-trace-flow-01.svg/png`
- `coroutine-observability-readiness-sequence-01.svg/png`

이관 절차는 asset마다 순차적으로 진행한다.

1. Diagram이 답하는 독자 질문을 정한다.
2. 현재 source, test, workshop 동작과 label/relationship을 대조한다.
3. Manual chapter에 필요한 정보만 유지하되 source-backed 개념을 layout 편의를 위해 삭제하지 않는다.
4. 필요하면 label, scope, geometry를 수정한다.
5. SVG XML validation을 실행한다.
6. CairoSVG CLI로 scale 2 PNG를 렌더링한다.
7. Full-size PNG에서 label, endpoint, connector, overlap, spacing, font를 확인한다.
8. SVG/PNG를 `docs/manual/assets/<module>`에 등록한다.
9. Site와 blog가 새 manual asset route를 사용하도록 정렬한다.
10. 기존 site-only asset은 모든 reference와 compatibility 요구를 확인한 뒤 별도 작업에서 제거한다.

단순 파일 복사는 완료로 인정하지 않는다.

## Site Sync 설계

`bluetape4k.github.io/scripts/manual/sync-manual.mjs`는 다음 순서로 확장한다.

1. Manifest와 module/chapter/asset inventory를 읽는다.
2. Source repository의 Markdown와 asset을 모두 수집한다.
3. Chapter frontmatter와 relative link를 검증한다.
4. Landing과 chapter Markdown에 immutable source metadata를 추가한다.
5. Repository-relative asset reference를 `/manual-assets/bluetape4k-projects/...`로 변환한다.
6. SVG/PNG를 site `public/manual-assets/bluetape4k-projects/...`로 복사한다.
7. Stale Markdown와 stale asset output을 제거한다.
8. Source Markdown, output Markdown, source asset, output asset digest를 계산한다.
9. Snapshot metadata에 `sourceFiles`, `contentFiles`, `sourceAssets`, `contentAssets`를 기록한다.

Sync는 항상 한 source commit을 기준으로 한다. Site snapshot 안의 source link와 asset은 같은 repository state를 나타내야 한다.

## Blog 파생 계약

- Blog는 manual의 기술 계약을 바꾸지 않는다.
- Blog는 문제 배경, 경험, 선택 과정, 비교, 사례에 집중한다.
- 상세 API table, 완전한 failure matrix, exhaustive troubleshooting은 manual route로 연결한다.
- Blog가 manual asset을 사용할 때 site의 `/manual-assets/...` 경로를 참조한다.
- Blog에서 새로운 기술 사실이 발견되면 manual/source를 먼저 수정한 뒤 blog를 작성한다.
- Historical story가 특정 버전을 설명하면 현재 manual 계약과 명시적으로 구분한다.

## 오류 처리와 Fail-Closed 규칙

다음 조건에서는 sync, phase progression, 완료 보고를 중단한다.

- Source 또는 test로 입증할 수 없는 기술 설명
- 영문·한글 chapter inventory drift
- 깨진 relative link 또는 image reference
- Manifest 밖의 orphan manual asset
- PNG가 SVG 의도와 다르게 보이거나 visual defect가 남은 경우
- Site repository에만 존재하는 manual 기술 원본
- 삭제된 source chapter/asset이 site output에 남은 경우
- Site snapshot source commit이 공개되지 않아 immutable GitHub source link가 깨지는 경우

실패한 validation은 경고로 내리지 않는다. 원본 또는 sync 계약을 수정하고 영향을 받는 downstream 검증을 다시 실행한다.

## 구현 단계

### Phase 1 — Manual Platform

- Manifest chapter/asset schema
- Repository validator와 fixture test
- Site Markdown/asset sync
- Snapshot digest와 stale cleanup
- 기존 90개 module backward compatibility

### Phase 2 — Coroutines Textbook

- Landing과 7개 상세 chapter
- 기존 Coroutines/Flow/observability diagram의 순차 재검증과 승격
- 부족한 lifecycle, Deferred policy, structured policy diagram 제작
- Workshop recipe와 representative source/test link
- 영문·한글 parity와 rendered route QA

### Phase 3 — Core Textbook

- Landing과 6개 상세 chapter
- Ordering/eviction, concurrency capacity, shutdown order diagram 제작
- Validation부터 lifecycle까지 source-backed recipe
- 영문·한글 parity와 rendered route QA

### Phase 4 — Blog Realignment

- 기존 blog의 상세 기술 설명을 canonical manual route와 연결
- Manual asset route 재사용
- 유용한 서사는 유지하고 중복된 exhaustive reference는 축소
- 기존 blog route와 필요한 asset compatibility 유지

## 테스트 전략

### Projects Repository

- Manifest parser/validator unit test
- Missing English/Korean chapter fixture
- Mismatched chapter ID/order fixture
- Missing SVG/PNG fixture
- Orphan asset fixture
- Unsafe path와 broken relative link fixture
- Existing module without chapters/assets backward-compatibility fixture
- `git diff --check`

### Site Repository

- Real source sync가 chapter와 asset을 deterministic하게 생성하는 test
- Relative image path rewrite test
- Markdown/asset digest sensitivity test
- Stale chapter/asset cleanup test
- English/Korean route mapping test
- Snapshot validator count/digest test
- `npm test`, `npm run check:manual`, `npm run build`

### Content와 Visual

- Chapter별 source/test claim ledger
- 영문·한글 chapter/title/code/diagram/source-link parity
- SVG XML validation
- CairoSVG scale 2 PNG render
- Asset별 full-size PNG visual inspection
- Desktop/mobile manual route 확인
- TOC, code block, table, diagram width와 accessibility text 확인

## 배포 순서

1. `bluetape4k-projects`의 manual source commit을 공개한다.
2. 해당 commit을 기준으로 site snapshot을 생성한다.
3. Site test/build/route/asset 검증을 완료한다.
4. 그 후에만 site snapshot을 배포할 수 있다.

이 순서를 지켜 site의 immutable GitHub source link가 처음부터 유효하게 한다.

## 완료 기준

- `docs/manual`이 Core·Coroutines 문서와 diagram의 유일한 기술 원본이다.
- Coroutines landing과 7개 chapter, Core landing과 6개 chapter가 영문·한글로 존재한다.
- 각 chapter가 blog 없이 독립적으로 완결된다.
- 모든 기술 claim이 current source/test에 연결된다.
- 필요한 SVG/PNG가 asset별 QA를 통과한다.
- Manifest와 validator가 chapter/asset/locale/link/orphan 계약을 fail closed로 검사한다.
- Site sync가 Markdown와 asset을 deterministic snapshot으로 게시한다.
- Site tests, snapshot validation, build, desktop/mobile route QA가 통과한다.
- 기존 blog는 manual의 파생 콘텐츠로 정렬되고 깨진 route/asset reference가 없다.
- Production Kotlin behavior와 dependency는 변경되지 않는다.
