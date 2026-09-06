# Issue #1643 구현 계획 독립 검토 기록

## 검토 범위

- 대상 계획: `docs/superpowers/plans/2026-09-06-bounded-http-body-plan.md`
- 승인 설계: `docs/superpowers/specs/2026-09-06-bounded-http-body-design.md`
- 기준 ref: `origin/develop@6a198fa8461637d02da34fee2d2c4df28a6b7b6e`
- 대상 단계: Type A Full Feature Step 3-R
- 관점: 성능, 안정성, 보안, Operator/Ops, 개발자/API, 사용자/caller
- 방식: 관점별 독립 읽기 전용 검토, 주 세션 통합, P1 관점 재검토
- 구현 상태: production source, test, README, CHANGELOG는 아직 변경하지 않았다.

## 첫 wave 결과와 처분

| 우선순위 | 관점 | finding | 처분 |
|---|---|---|---|
| P1 | 개발자/API | primitive 예시의 존재하지 않는 `segments.flatten(total)` 때문에 계획을 그대로 실행할 수 없음 | `flattenToByteArray(totalSize)`의 exact-size allocation과 `copyInto` 구현을 추가했다. |
| P1 | Operator/Ops | 알려진 JDK Testcontainers image 404를 기준 SHA에서 기록하지 않아 module check 실패의 baseline/회귀 판정 분기가 없음 | Task 0에 두 번으로 제한한 baseline 재현·log/XML 보존을, Task 8에 base/head 비교와 CI exact-head PENDING 분기를 추가했다. |
| P1 | 개발자/API·Operator/Ops | blocking read-ahead test가 latch/assertion/timeout 실패에서도 stream과 executor를 종료한다는 `finally` 순서를 보장하지 않음 | supervisor close, bounded future 회수/cancel, `shutdownNow`, bounded termination 순서를 추가했다. |
| P2 | 사용자/caller | README/KDoc의 “컴파일 가능한 예제”를 실제 compilation으로 확인하는 fixture가 없음 | IO/HC5/JDK RED test에 `README 공개 예제` compile fixture를 각각 추가하고 Task 7에서 locale·KDoc와 대조한다. |
| P2 | 사용자/caller | #939/#451 댓글 등록·read-back 명령과 기대 상태가 없음 | `gh issue comment`, `gh issue view --json state,comments,url`, exact head/publish 전 상태의 기대 결과를 Task 10에 추가했다. |
| P2 | 개발자/API | `maxBytes < Int.MAX_VALUE` 경계 표현이 0과 겹칠 수 있음 | Task 1은 양수 사례를 상한 4로 고정하고 0과 음수를 별도 test로 유지한다. |
| P2 | Operator/Ops | 실제 transport timeout·supervisor close 및 heap startup validation의 소비자 후속 책임이 댓글 증거에 직접 연결되지 않음 | #939/#451 댓글 필수 내용과 read-back 기대 결과에 두 소비자 smoke 검증 의무를 추가했다. |

## 주 세션 추가 교정

첫 wave의 flatten 수정안을 그대로 적용하면 bulk read가 반복해서 0을 반환할 때 1-byte
segment가 본문 크기만큼 생성될 수 있었다. 이는 byte capacity의 점근 상한은 지키지만
설계의 `2 * actualBytes + O(segmentSize)` peak 의도와 맞지 않는다. current segment를
끝까지 채우고 0 반환의 single byte도 같은 segment에 기록하도록 계획 예시를 다시
교정했다. 저장 segment와 current segment capacity 합은 `maxBytes` 이하이고 segment object
수는 `ceil(maxBytes / DEFAULT_BUFFER_SIZE)` 이하다.

## 두 번째 wave 결과

| 우선순위 | 관점 | finding | 처분 |
|---|---|---|---|
| P2 | 성능 | 구조적 memory bound와 달리 throughput/latency/GC 개선을 입증할 benchmark가 없음 | 성능 개선은 비주장으로 명시했다. 검증 대상은 선할당 금지와 구조적 memory bound뿐이며 향후 성능 주장은 별도 benchmark 이슈를 요구한다. |
| P2 | 성능 | 동시성 heap budget이 library와 소비자 책임으로 충분히 분리되지 않음 | library에 mutable shared state가 없어 synchronization test는 N/A로 두고 workload별 동시 read·startup validation을 #939/#451 smoke 필수 항목으로 고정했다. |
| P2 | 성능 | blocking test가 `StructuredTaskScopeTester` 대신 ad hoc executor를 쓰는 이유가 없음 | task-scope semantics가 아니라 caller-owned stream close와 executor teardown 순서를 검사하므로 bounded latch/future harness가 필요한 이유를 기록했다. |
| P3 | 성능 | partial copy와 `ArrayList` reference overhead가 peak 설명에서 생략됨 | transient current/partial segment와 reference storage까지 peak 식에 포함했다. |
| P2 | 보안 | known oversize의 accessor/close 자체가 block하는 경로를 검증하지 않음 | HC5/JDK 모두 latch/abort fake와 bounded cleanup으로 accessor-block·close-block을 검증하도록 추가했다. |
| P3 | 보안 | `Long.MAX_VALUE`, whitespace, plus, comma-joined `Content-Length` 경계가 없음 | JDK parameterized header matrix에 네 사례와 known/unknown 기대 결과를 추가했다. |
| P1 | 안정성 | worker의 원본 실패가 `ExecutionException`으로 감싸진 뒤 teardown이 중단되어 executor를 누수할 수 있고, close-block fake에서 close가 abort보다 먼저 실행될 수 있음 | non-blocking supervisor abort를 첫 단계로 옮기고, cleanup 단계를 독립 처리하며, `ExecutionException.cause` identity 검증 뒤에도 `shutdownNow`와 bounded termination이 항상 실행되도록 고정했다. |
| P1 | 안정성 | shared cleanup helper의 accessor·known oversize·read·close 전이와 primary/suppressed 우선순위가 bullet만으로 열려 있음 | `primary`와 `ownedStream`을 사용하는 구현 가능한 `try/catch/finally` 의사코드와 각 전이의 예외·close 계약을 계획에 추가했다. |

성능·보안 관점은 최신 계획에서 P0=0, P1=0이었다. 첫 안정성 lane은 liveness
기한을 넘겨 취소했고, 같은 범위를 축소한 replacement lane에서 위 P1 두 건을 찾았다.
개발자/API와 Operator/Ops의 첫 P1 수정안 재검토는 각각 P0=0, P1=0으로 통과했지만,
안정성 수정이 두 영역의 계획을 다시 바꾸었으므로 최신 문서에 대해 영향 관점 재검토를
한 번 더 수행한다.

## 최종 Step 3-R 판정

| 관점 | 최초 P0 | 최초 P1 | 최종 P0 | 최종 P1 | 판정 |
|---|---:|---:|---:|---:|---|
| 성능 | 0 | 0 | 0 | 0 | PASS |
| 안정성 | 0 | 2 | 0 | 0 | PASS |
| 보안 | 0 | 0 | 0 | 0 | PASS |
| Operator/Ops | 0 | 2 | 0 | 0 | PASS |
| 개발자/API | 0 | 1 | 0 | 0 | PASS |
| 사용자/caller | 0 | 0 | 0 | 0 | PASS |

안정성, Operator/Ops, 개발자/API 관점은 최종 수정본을 다시 읽어 P0=0, P1=0을
확인했다. 성능·보안·사용자/caller의 P2/P3 finding은 계획에 반영하거나 명시적인 N/A
검증 경계로 처분했으며 처리되지 않은 finding은 없다. 원래 안정성 lane의 liveness 취소는
replacement lane과 최종 재검토로 보완했다.

최종 판정: `PASS` — 구현 계획은 승인된 설계, TDD 순서, API/ABI, 자원 수명주기,
failure precedence, 운영 실패 분기, 문서·소비자 후속 증거를 실행 가능한 수준으로 고정했다.
구현·module test·CI·publish는 이 검토의 성공으로 완료된 것이 아니며 계획 승인 후의 별도
게이트로 남는다.

### DoD

- [x] 승인 설계의 모든 수용 기준이 Task 0~10 추적표에 연결됐다.
- [x] TDD RED→GREEN과 dependent task 순서가 명시됐다.
- [x] exact file, command, expected result, failure branch가 명시됐다.
- [x] KDoc, 영문·국문 module README, CHANGELOG가 범위에 포함됐다.
- [x] allocation, blocking, cleanup, 재사용, 호환성, rollback 경계가 명시됐다.
- [x] 6개 관점 최종 P0/P1 0건 확인
- [x] P1 영향 관점 재검토
- [x] 계획·검토 문서 writer/terminology/diff 검증

현재 판정: `PASS` — Step 3-R은 닫혔다. 구현은 별도 계획 승인을 받기 전까지 시작하지 않는다.
