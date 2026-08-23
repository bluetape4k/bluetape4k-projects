# #1474 bounded admission 구현 코드 검토

## 검토 범위와 기준

- 저장소: `bluetape4k/bluetape4k-projects`
- 모듈 slice: `cache/cache-core` 단일 모듈
- 기준 SHA: `f7c1cf1bb9da7ea2d6811f2833582ab0f434d15d`
- 현재 구현 head: `ee2e7cd625` + 미커밋 구현 diff
- 구현 소스: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt`
- 회귀 테스트: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt`
- 설계/계획: `docs/superpowers/specs/2026-08-23-issue-1474-bounded-admission-design.md`,
  `docs/superpowers/plans/2026-08-23-issue-1474-bounded-admission-plan.md`
- 검토 방식: Performance, Stability, Security, Operator/Ops, Developer/API,
  User/Caller 여섯 관점과 main-session 통합

## 검증 증거

| 증거 | 결과 |
| --- | --- |
| focused listener test | 17 passing, `BUILD SUCCESSFUL in 27s` |
| listener + near-cache contract | 73 passing, `BUILD SUCCESSFUL in 18s` |
| full `cache-core` test | 704 passing, `BUILD SUCCESSFUL in 1m 1s` |
| `:bluetape4k-cache-core:detekt --rerun-tasks` | exit 0; 변경 listener 파일 finding 없음. 기존 near-cache/memoizer/문서 테스트 finding은 별도 scope로 유지 |
| `:bluetape4k-cache-core:build -x test --rerun-tasks` | exit 0, `BUILD SUCCESSFUL in 6s` |
| public constructor ABI | main jar `bluetape4k-cache-core-2.0.0.jar`에서 one-argument `SuspendJCache` constructor 정확히 1개 매칭; Kotlin synthetic bridge는 필터 제외 |
| source hygiene | `git diff --check` PASS, Korean terminology audit 2 files / findings=0 |
| concurrency scan | listener production source에 `GlobalScope`, `runBlocking(`, `Thread.sleep`, `delay`, monitor, `runCatching` 없음 |

## 여섯 관점 통합 결과

| 우선순위 | 관점 | 근거 | 조치/처분 |
| --- | --- | --- | --- |
| P0 | 전체 | 승인 범위 밖 public API, global ordering, coalescing, dependency, provider registration 변경 없음 | 없음 |
| P1 | 전체 | permit 수명, callback cancellation, close, raw payload redaction, ABI를 현재 테스트와 정적 증거로 닫음 | 없음 |
| P2 | Performance | `tryAcquire()`는 callback thread에서 대기하지 않지만 event batch 사본은 admission 전에 기존처럼 생성됨 | payload 크기 상한은 승인 설계의 비범위로 유지; child/accepted call finite burst 증거를 사용 |
| P2 | Stability | `finally`와 `job.start() == false` 두 경로가 permit을 반환하고 close는 join하지 않음 | cancellation, cancelled scope, bounded close burst 테스트로 처분 |
| P2 | Security | overflow/error/trace log에 raw key·value·source를 넣지 않음 | overflow secret token 및 기존 trace redaction 테스트로 처분 |
| P2 | Operator/Ops | overflow 관측은 low-cardinality debug log이며 public metric은 없음 | 승인된 운영 범위; metric backend와 runtime tuning은 후속 이슈 범위 |
| P2 | Developer/API | `forTest`에만 cap 주입, public one-argument constructor와 registration 경로 유지 | `javap`, near-cache contract, full module test로 처분 |
| P2 | User/Caller | 기본 64와 overflow best-effort 의미가 KDoc에 추가되고 durable delivery로 과장하지 않음 | README/examples는 public API 불변으로 N/A |

## 구현·테스트 대응표

| 요구 | 구현 증거 | 테스트 증거 |
| --- | --- | --- |
| listener별 bounded non-blocking admission | private `Semaphore(64)`, `tryAcquire`, 즉시 debug 거부 | 8 callback / cap 2에서 child 2개와 `putAll` 2회 |
| accepted callback 무손실·중복 없음 | permit 소유 child당 backend operation 한 번 | bounded burst `coVerify(exactly = 2)` 및 기존 event 종류별 테스트 |
| permit 회수 | child `finally`, lazy job 미시작 caller-side release, post-close pre-admission release | CancellationException 후 두 번째 callback, cancelled scope 두 callback, close burst |
| 오류·취소 계약 | `CancellationException` broad catch 선행 재전파, 일반 `Exception` error log 후 sibling 유지 | child cancellation 상태, 일반 예외 sibling·error log, full module |
| close lifecycle | `closed.compareAndSet` 후 `scope.cancel()`만 호출 | started child close 후 join은 test-only, 두 child 모두 cancelled |
| log redaction | operation·sanitized cache id·cap만 기록 | overflow secret token, 기존 callback trace redaction |
| ABI/provider 호환 | public constructor unchanged, `SuspendNearJCache` registration 미수정 | exact `javap`, `NearJCacheContractTest`, `SuspendNearJCacheBackFirstContractTest` |

## 성능·안정성 quick scan

- `scope.launch(start = CoroutineStart.LAZY)` 후 즉시 `Job.start()`하므로 callback
  thread는 permit을 기다리거나 backend를 직접 호출하지 않는다.
- cap 포화 시 내부 queue/coalescing이 없고, overflow callback은 한 번의 debug log 후
  반환한다.
- permit은 정상 완료, 일반 예외, `CancellationException`, close 취소, lazy 미시작의
  모든 경로에서 반환된다.
- source의 `runBlocking`은 KDoc에서 금지 예시로만 언급되며 production 호출이 아니다.
  테스트의 `source.toString()`는 raw payload가 로그로 유출되지 않는지 검증하는 mock
  fixture일 뿐 production path가 아니다.
- Testcontainers나 다른 module source는 변경하지 않았고 full module test는 순차 실행했다.

## 문서·릴리스·운영 경계

- 새 class/API가 아니므로 README/localized README, examples, BOM/catalog, module
  registration, CI/Nightly, Kover, CHANGELOG/release note는 N/A다.
- public KDoc는 Korean prose로 bounded admission, 기본 cap, overflow 거부, close
  무대기 동작을 설명한다.
- PR 생성은 아직 하지 않았으며 CG-11 fresh authority가 필요하다. merge, auto-merge,
  publish, tag, branch 삭제는 실행하지 않았다.
- detekt 출력에 기존 near-cache/memoizer 및 documentation test finding이 존재하지만
  task exit는 0이고 변경 listener 파일은 finding이 없다. 이 baseline은 별도 정리 범위다.

## 최종 판정

- P0: 0
- P1: 0
- P2: 5건 모두 승인된 비범위 또는 fresh test/static evidence로 처분
- P3: 없음
- 코드 검토 게이트: PASS

다음 단계는 구현 source/test와 이 review artifact를 Lore commit으로 고정하고,
exact implementation head에서 verification worktree를 만드는 것이다. PR 생성은
fresh repository/base/head 권한 확인 후 별도 단계로 남긴다.

### DoD

- [x] 여섯 관점 review와 main-session 통합
- [x] P0/P1 수렴
- [x] 요구사항·위험·테스트·정적/API 증거 매핑
- [x] diff/source hygiene 및 Korean terminology audit
- [x] PR/merge/release side-effect 경계 기록
