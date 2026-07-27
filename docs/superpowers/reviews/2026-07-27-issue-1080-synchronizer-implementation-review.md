# Issue #1080 Lettuce Synchronizer 구현 검토

## 범위

- Delivery 2만 검토했다: `LettuceDistributedSemaphore`, `LettucePermitExpirableSemaphore`,
  `LettuceCountDownLatch`의 blocking/async/suspend API, Redis Lua protocol, 테스트, KDoc,
  영어/한국어 문서와 다이어그램.
- Lock 구현 변경, 기존 `LettuceSemaphore` 제거, Delivery 3 API convergence는 제외했다.

## 독립 검토 수렴

| 관점 | 최초 결과 | 보완 | 최종 결과 |
|---|---:|---|---:|
| Security | P0=0, P1=1 | waiter TTL/cap과 generation-bound unregister, 이전 세대 cleanup 회귀 테스트 | P0=0, P1=0 |
| Performance / Stability | P0=0, P1=3 | connection-owned polling runtime, durable async cleanup, cancellation 재전파, 최소 1ms polling | P0=0, P1=0 |
| API / User / Test | P0=0, P1=5, 이후 P0=1/P1=1 | result matrix/KDoc, Java·Kotlin 계약, 직접 ambiguity 분기, 테스트 제네릭 타입 | P0=0, P1=0 |

최종 API 재검토에서는 `LatchAwaitResult.Ambiguous` 직접 검증이 한 차례 더 요구됐다.
`SynchronizerAmbiguityTest`에 blocking `await`의 post-dispatch timeout 결과를 추가하고
대상 테스트 1개가 통과한 뒤 `P0=0, P1=0`으로 닫았다.

## 검증 증거

- Synchronizer package: 28 passing, 실제 Redis Cluster same-slot script 포함.
- Module check: 869 passing, 별도 lock/lease stability·performance test와 Kover 포함.
- Dokka: 성공. 출력된 unresolved link 경고는 기존 모듈 문서 기준선이며 새 synchronizer KDoc 오류는 없다.
- 정적 검사: `git diff --check` 통과. Production에 `GlobalScope`, `runBlocking`, `!!` 없음.
  broad catch는 Redis 예외를 typed backend/ambiguous 결과로 변환하며 suspend cancellation은 먼저 재전파한다.
- 문서 parity: README locale pair는 H2/table/fence 수가 각각 `11/139/48`,
  synchronizer guide pair는 `4/5/6`; locale 링크와 diagram route가 모두 유효하다.

## Diagram evidence ledger

| Locale | SVG / PNG | Render | Audits | Visual review |
|---|---|---|---|---|
| English | `infra-lettuce-diagram-04.svg` / `.png` | XML valid, 1900×900 | geometry 0, connector PASS, endpoint PASS, mixed-corner 0 | full-size label, spacing, clipping, endpoint inspection passed |
| 한국어 | `infra-lettuce-diagram-04-ko.svg` / `.png` | XML valid, 1900×900 | geometry 0, connector PASS, endpoint PASS, mixed-corner 0 | full-size 한글 글꼴, label, spacing, clipping, endpoint inspection passed |

## 결론

구현·문서 사전 PR gate는 `P0=0 / P1=0`이다. 게시 후에는 exact-head PR metadata,
CI, review/thread 상태를 다시 검증하고 merge 승인 gate에서 멈춘다.
