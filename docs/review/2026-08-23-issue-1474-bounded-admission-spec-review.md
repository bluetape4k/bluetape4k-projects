# #1474 bounded admission 설계 spec review

## 검토 범위와 근거

- 대상 artifact: `docs/superpowers/specs/2026-08-23-issue-1474-bounded-admission-design.md`
- 기준 commit: `6e993fd74`
- live authority: [bluetape4k-projects #1474](https://github.com/bluetape4k/bluetape4k-projects/issues/1474)
- local evidence: `SuspendJCacheEntryEventListener.kt`,
  `SuspendJCacheEntryEventListenerTest.kt`, `SuspendNearJCache.kt`
- baseline: `SuspendJCacheEntryEventListenerTest` 12 passing
- review method: performance, stability, security, operator/Ops, developer/API,
  user/caller six lenses와 main-session integration

## Six-lens 결과

| Priority | Lens | Evidence | Required edit / disposition | Rerun |
| --- | --- | --- | --- | --- |
| P2 | performance | 선택 설계의 `Semaphore.tryAcquire()`는 callback job 수를 cap으로 제한하지만 callback iterable 사본 생성과 batch 내부 entry 수는 제한하지 않는다. | spec의 “batch 내부 entry 수에는 새 상한을 추가하지 않는다”와 중지 조건으로 범위를 명시했다. payload-size bound는 별도 후속 설계로 남긴다. | main integration에서 범위 문구 재확인 |
| P2 | stability | `close()`와 permit 획득/`scope.launch`가 경쟁할 수 있고 close 시 accepted job 완료를 보장하지 않는다. | #1360 cooperative cancellation 계약과 `finally` permit 반환을 명시했다. 구현 plan에서 launch 전후 permit 누수와 close race 테스트를 구체화한다. | stability plan lane |
| P3 | security | overflow/기존 callback log에 raw key/value/source를 넣지 않아 payload 노출면을 줄인다. | 추가 수정 없음. log appender redaction 검증을 plan에 유지한다. | security plan lane |
| P2 | operator/Ops | public metric API를 추가하지 않아 overflow rate를 metric으로 수집하지 않는다. | 이번 issue는 admission 계약과 결정적 검증 범위다. sanitized debug log를 운영 신호로 문서화하고 metric backend는 비범위로 유지한다. | operator plan lane |
| P2 | developer/API | public constructor와 provider registration은 유지되지만 `forTest` cap 주입은 internal API다. | `@JvmSynthetic internal` test seam과 positive-cap validation을 plan에 고정한다. public API/ABI gate를 추가한다. | developer/API plan lane |
| P2 | user/caller | cap 초과 callback은 거부되므로 listener는 durable delivery가 아니다. default `64`는 public tuning API 없이 고정된다. | best-effort/overflow 경계와 별도 durable delivery 설계를 spec에 명시했다. KDoc에 caller-facing caveat를 추가한다. | user/caller plan lane |

## Main-session integration

### Contradiction check

- `accepted`는 `tryAcquire()` 성공으로 정의하고, close race에서는 완료가 아닌
  cooperative cancellation을 우선한다. 따라서 “accepted event 무손실”은 close가
  개입하지 않은 정상 drain 구간의 정확한 한 번의 apply 시도로 해석한다.
- 대안 A는 queue를 만들지 않으므로 queueing proof는 queue depth `0`과 in-flight
  child bound로 검증한다. 대안 C의 worker cancellation 문제를 다시 도입하지
  않는다.
- `targetCache.isClosed()` rejection과 semaphore overflow rejection은 서로 다른
  경계이며, 두 경우 모두 JCache callback에는 예외를 반환하지 않는다.

### Coverage check

- 범위/비범위: PASS
- alternatives와 선택 근거: PASS
- linearization/overflow/event semantics: PASS
- cancellation/close/ordinary exception: PASS
- failure modes: PASS, 6개
- compatibility/ABI/provider registration: PASS
- deterministic tests와 validation commands: PASS
- docs/KDoc/PR/merge side-effect boundary: PASS

### Severity verdict

P0=0, P1=0. P2 5건은 모두 선택 범위와 후속 검증 작업으로 disposition했으며,
P3 1건은 추가 수정 없이 pass다. 구현은 spec 의미를 바꾸지 않는 범위에서
계속할 수 있다.

## Writer gate

- SPW-01: PASS — cache-core maintainer/reviewer audience, #1474 outcome, live issue,
  source paths, baseline, exclusions와 unknowns를 고정했다.
- SPW-02: PASS — spec contract와 review contract가 각각 문제·대안·경계·실패
  모드·호환성·수용 기준·DoD와 finding disposition을 포함한다.
- SPW-03: PASS — Korean technical register와 identifier/code token 보존을
  적용했고 `audit-korean-terms.mjs` 결과 findings=0이다.
- SPW-04: PASS — source path, issue URL, baseline command, constructor,
  registration, exception/불변 사본 근거를 현재 artifact와 대조했다.
- SPW-05: PASS — 최종 Markdown read-back, heading/table/list/code formatting,
  P0/P1 verdict와 remaining P2 disposition을 확인했다.

## Step DoD

- [x] six-lens review 수행
- [x] main integration으로 중복·모순·근거·범위 확인
- [x] P0=0, P1=0
- [x] P2/P3 disposition 기록
- [x] writer SPW-01..05 기록
- [x] 구현 전 승인된 설계와 exact stop condition 확인
