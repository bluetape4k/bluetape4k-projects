# Logging Manual First 설계

**날짜:** 2026-07-12  
**상태:** 승인됨 — 전체 모듈 확장 방향과 첫 대상 `logging` 실행 승인  
**대상:** `bluetape4k-projects`, `bluetape4k.github.io`

## 목표

`bluetape4k-logging`을 README보다 상세한 독립 매뉴얼로 만든다. 현재 Kotlin source와 test가 기술 사실의 근거이고, `docs/manual`이 설명과 diagram의 source of truth이며, site와 blog는 이를 게시하거나 재구성한다.

## 범위

- 기존 landing을 학습 경로와 선택 지도로 재작성한다.
- 영문·한글 6개 chapter를 동일 inventory로 제공한다.
- logger 선택, lazy 평가, MDC 복원, coroutine 전파, channel lifecycle을 source-backed contract로 설명한다.
- architecture 2종과 sequence 1종을 canonical SVG/PNG pair로 소유한다.
- site deterministic snapshot과 bilingual Part 2 blog link를 갱신한다.
- Core landing 명칭을 `Core Kotlin library` / `Core Kotlin 라이브러리`로 정렬한다.

## 비범위

- Kotlin production source, API, dependency, runtime behavior는 변경하지 않는다.
- README의 측정 근거 없는 성능 숫자를 매뉴얼 계약으로 승격하지 않는다.
- async logging을 기본 권장으로 만들지 않는다.
- logging 전용 workshop이 없는 상태에서 가상의 workshop을 만들지 않는다.
- push, PR, merge, deploy는 수행하지 않는다.

## Chapter inventory

1. `logger-foundation` — `KLogging`, `KotlinLogging`, logger naming과 provider 경계
2. `lazy-messages` — level guard, safe supplier, marker/error overload와 평가 실패
3. `scoped-mdc` — nested scope, null handling, restore/remove와 exception cleanup
4. `coroutine-mdc` — `MDCContext`, suspension/dispatcher 전환, child coroutine 경계
5. `async-channel` — `MutableSharedFlow`, buffer 64, collector, close ownership, post-close drop
6. `operations-recipes` — 선택 기준, Logback pattern, redaction, shutdown/testing 진단

각 chapter는 문제, mental model, 최소 API, 완전한 예제, 선택 기준, 실패/수명주기, 운영 진단, source/test 링크를 포함한다.

## Diagram inventory

- `logging/logger-api-map` (architecture): logger creation과 message extension 책임 지도
- `logging/mdc-scope-lifecycle` (architecture): outer/current/inner 값과 restore/remove 경계
- `logging/async-channel-sequence` (sequence): caller, SharedFlow, collector, SLF4J, close/post-close 흐름

기존 README diagram은 재료와 visual family 참고로만 사용한다. 최종 manual asset은 current source/test와 대조하고 `docs/manual/assets/logging`에서 별도로 소유한다.

## Acceptance criteria

- manifest에 6개 bilingual chapter와 6개 diagram asset이 등록된다.
- 모든 상대 링크와 frontmatter가 validator를 통과한다.
- 각 technical claim이 current source 또는 representative test에 연결된다.
- `:bluetape4k-logging:test`, manual validator/tests, site Node tests, snapshot check, Astro build가 통과한다.
- KO/EN landing, 대표 chapter, PNG, blog link가 browser에서 정상이다.
