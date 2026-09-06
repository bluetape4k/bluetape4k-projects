# Issue #1643 bounded HTTP body 구현 검토

## 검토 범위와 기준

- 저장소: `bluetape4k/bluetape4k-projects`
- 기준: `origin/develop@31ee966cf339f7b895284329c8fbd53638ab837c`
- 검토 대상 구현 commit: `d50377db38` (review·lesson artifact commit 제외)
- 모듈 slice: `io/io` primitive, `io/http` HC5·JDK adapter
- 설계: `docs/superpowers/specs/2026-09-06-bounded-http-body-design.md`
- 계획: `docs/superpowers/plans/2026-09-06-bounded-http-body-plan.md`
- 관점: 성능, 안정성, 보안, Operator/Ops, 개발자/API, 사용자/caller

## 검증 증거

| 증거 | 결과 |
| --- | --- |
| `BoundedInputStreamSupportTest` 강제 실행 | 9개 통과 |
| HC5·JDK adapter 테스트 강제 동시 실행 | 33개 통과, `BUILD SUCCESSFUL in 30s` |
| `:bluetape4k-io:check` | 1,274개 통과 |
| `:bluetape4k-http:check` | 491개 중 392개 통과, 3개 건너뜀, 96개 실패 |
| HTTP check 실패 분류 | 94개 `ContainerFetchException`과 이를 감싼 2개 `MultiException`; 모두 `bluetape4k/mock-web-server:2.1.0` 404 |
| 공개 JVM API | `ByteLimitExceededException`, `InputStream.readAllBytes(maxBytes)`, HC5·JDK bytes/string 함수와 default bridge를 `javap`로 확인 |
| source hygiene | `git diff --check` 통과, 금지 선할당·`maxBytes + 1` 산술·무제한 `ByteArrayOutputStream` 없음 |
| 문서 정합성 | README locale API signature·숫자 일치, 변경 줄 한국어 용어 감사 finding 0 |

로컬 HTTP 전체 check는 외부 테스트 이미지 404 때문에 완료되지 않았다. 변경 대상 HC5·JDK
테스트는 같은 실행 환경에서 모두 통과했으며, PR의 정확한 head CI를 최종 판정 근거로 사용한다.

## 여섯 관점 통합 결과

| 관점 | P0 | P1 | P2 | P3 | 판정 근거 |
| --- | ---: | ---: | ---: | ---: | --- |
| 성능 | 0 | 0 | 0 | 0 | segment 점진 할당, 합산 용량 검사, 1-byte read-ahead로 큰 선할당과 정수 overflow를 피한다. 성능 수치 주장은 하지 않는다. |
| 안정성 | 0 | 0 | 0 | 0 | zero-read 진행, EOF와 strict overflow, stream ownership, blocking supervisor 종료 경계를 테스트했다. |
| 보안 | 0 | 0 | 0 | 0 | 명시적 byte 상한과 payload 비노출 예외를 제공하며 decoded/decompressed body에는 별도 상한이 필요함을 문서화했다. |
| Operator/Ops | 0 | 0 | 0 | 0 | payload 없는 low-cardinality 분류, timeout·abort·heap 예산 책임, publish·rollback 순서를 문서화했다. |
| 개발자/API | 0 | 0 | 0 | 0 | strict API와 기존 truncation API를 분리하고, raw stream과 adapter의 close 책임 및 stable `maxBytes`를 명시했다. |
| 사용자/caller | 0 | 0 | 0 | 0 | null HC5 entity 정규화, JDK `BodyHandlers.ofInputStream()`, central catalog 또는 소비자별 override 전환 예제를 제공했다. |

## 검토 중 발견한 P1과 처분

초기 구현은 cleanup에서 발생한 `Error` 또는 `CancellationException`을 기존 overflow/read
실패보다 우선했다. 이는 승인 설계의 Kotlin `use`식 primary/suppressed 계약과 달랐다.

- RED: HC5 회귀 테스트 18개 중 2개 실패로 계약 위반을 재현했다.
- 수정: 기존 primary를 유지하고 identity가 다른 후속 실패만 suppressed로 추가했다.
- GREEN: HC5 18개와 공용 helper를 쓰는 JDK를 포함한 33개를 강제 재실행해 모두 통과했다.
- 재검토: blocking `InputStream` API는 coroutine cancellation 경계가 아니므로 후속
  `java.util.concurrent.CancellationException`을 특별 승격하지 않는다. 향후 suspend adapter는
  별도 cancellation 계약을 설계해야 한다.

## 요구와 구현 대응

| 요구 | 구현·문서 증거 | 검증 |
| --- | --- | --- |
| 최대 `maxBytes`까지만 성공 | bounded segment read와 overflow 예외 | 경계·초과·`Int.MAX_VALUE` 테스트 |
| partial body를 성공으로 반환하지 않음 | 초과 판정용 1 byte read-ahead | strict 실패와 기존 truncation API 비교 |
| caller stream을 닫지 않음 | primitive는 읽기만 수행 | tracking stream 상태 검증 |
| adapter-owned stream은 항상 정리 | 공용 `readOwnedBodyBytes`의 `finally` close | accessor/read/close 조합과 identity 검증 |
| 알려진 oversize는 body를 읽지 않음 | metadata 판정 후 accessor·close cleanup | HC5 content length와 JDK header matrix |
| 공개 사용법과 운영 경계 | `io/io`, `io/http` README와 CHANGELOG | locale 정합성과 공개 예제 컴파일 테스트 |

## 범위와 잔여 위험

- dependency, module registration, catalog, workflow, CI 구성은 변경하지 않았다.
- helper는 blocking이며 event-loop에서 호출하면 안 된다. transport timeout과 외부 supervisor
  abort는 caller 책임이다.
- 제한은 helper에 전달된 stream byte에 적용된다. 압축 해제 후 본문에는 별도 제한이 필요하다.
- 로컬 HTTP 전체 check의 이미지 404는 PR CI에서 다시 판정한다. CI가 같은 실패를 보이면
  이미지 공급 상태와 workflow 환경을 별도로 조사하며 성공으로 간주하지 않는다.

## 최종 판정

- P0: 0
- P1: 0
- 미처분 P2/P3: 0
- 구현 검토 게이트: `PASS`

### DoD

- [x] 두 모듈 slice를 여섯 관점으로 검토
- [x] 초기 P1을 RED→GREEN 회귀 테스트로 해결
- [x] 공개 API, 문서, ownership, failure composition 증거 확인
- [x] 로컬 외부 이미지 실패를 성공과 분리해 기록
- [ ] PR 정확한 head CI와 post-PR 검토
