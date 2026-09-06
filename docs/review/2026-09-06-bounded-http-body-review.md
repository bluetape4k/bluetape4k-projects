# Issue #1643 설계 검토 기록

## 검토 범위와 결과

- 대상: `docs/superpowers/specs/2026-09-06-bounded-http-body-design.md`
- 기준: `origin/develop@6a198fa8461637d02da34fee2d2c4df28a6b7b6e`
- 관점: 성능, 안정성, 보안, Operator/Ops, 개발자/API, 사용자/caller
- 방식: 관점별 독립 읽기 전용 검토 후 주 세션 통합, P1 관점 재검토
- 최종 결과: P0 0건, P1 0건, 미처분 P2/P3 0건
- 테스트·빌드: 설계 검토 단계에서는 실행하지 않음

## Finding과 처분

| 초기 우선순위 | 관점 | 근거와 위험 | 통합 처분 | 재검토 |
|---|---|---|---|---|
| P1 | 성능·안정성 | 큰 `maxBytes` 선할당과 `Int.MAX_VALUE` 산술은 작은 body에도 OOM·overflow를 만들 수 있음 | segment 점진 할당, capacity 합 상한, `maxBytes + 1` 산술 금지, 작은 body+`Int.MAX_VALUE` 검증 추가 | PASS |
| P1 | 안정성 | adapter가 음수 상한보다 metadata 조기 거부를 먼저 수행할 수 있음 | 모든 진입점에서 body/metadata 접근 전 음수 검증으로 순서 고정 | PASS |
| P1 | 성능·안정성 | bulk read가 계속 0이면 busy loop 또는 무한 재시도 가능 | 0 반환 시 단일 `read()`로 즉시 전환해 진행 또는 EOF를 보장 | PASS |
| P1 | 안정성·운영 | 상한 직후 read와 close가 무기한 block될 수 있으나 완료 경계가 없음 | helper의 blocking/non-cancellable 계약, client timeout, 외부 supervisor close, event-loop 금지 책임 추가 | PASS |
| P1 | 운영 | known oversize에서 body accessor·close와 transport cleanup의 소유권이 모호함 | accessor 반환 시 소유권 이전, overflow primary, accessor/close suppressed, 상위 cleanup 상태표 추가 | PASS |
| P1 | 개발자/API | 판정용 1 byte 소비 뒤 primitive stream 위치가 모호함 | 판정 byte는 복원하지 않고 `maxBytes + 1` 위치에 열린 채 남는다고 명시 | PASS |
| P1 | 사용자/caller | null HC5 entity와 empty body를 구분할 수 없는 새 계약의 migration 위험 | null→empty 정규화를 명시하고 null 보존용 `entity?.let` 예제·선택표 추가 | PASS |
| P1 | 사용자/caller | raw primitive와 adapter의 close 책임 차이를 보여 주는 예제가 없음 | raw `InputStream.use`, HC5 response `use`, JDK body-close 예제 추가 | PASS |
| P2 | 보안·운영 | caller가 과도한 상한을 설정하면 동시 allocation으로 heap 고갈 가능 | 기본 상한을 제공하지 않고 caller가 유한 상한을 반드시 전달하게 함. 동시성×peak 메모리 예산과 startup 검증 책임 명시 | 해결 |
| P2 | 보안 | 제한이 wire byte인지 decoded byte인지와 decompression bomb 경계가 불명확 | 실제 전달 stream byte를 제한한다고 정의하고 후속 압축 해제에는 별도 상한 요구 | 해결 |
| P2 | 안정성·보안 | malformed·negative·overflow·중복 `Content-Length` 처리 미정 | metadata를 unknown으로 취급하고 실제 body primitive로 판정 | 해결 |
| P2 | 개발자/API | 예외 message 안정 계약과 constructor 불변조건이 모호함 | `maxBytes`만 안정 계약으로 유지하고 non-negative 불변조건, payload 비노출 명시 | 해결 |
| P2 | 개발자/API | `maxBytes + 1` 테스트가 `Int.MAX_VALUE`에서 overflow | `1 <= maxBytes < Int.MAX_VALUE` 경계와 `Int.MAX_VALUE` 작은 body 사례 분리 | 해결 |
| P2 | 개발자/API | tracking stream만으로 배열 allocation을 검증할 수 없음 | read 수·결과 크기는 runtime test, segment capacity는 구현 불변식·코드 검토로 분리 | 해결 |
| P2 | 사용자/caller | 기존 truncation API와 strict 거부 API 선택 기준 부족 | 선택표와 Workshop #939·Clinic #451 전환 형태 추가 | 해결 |
| P2 | 운영 | payload 없이 실패를 진단할 관측 지침 부족 | library side effect는 두지 않고 caller의 low-cardinality 집계·alert 지침 추가 | 해결 |
| P2 | 운영 | publish·소비자 전환·rollback 순서 부재 | library publish→소비자 smoke→독립 PR→수동 strict loop rollback 순서 추가 | 해결 |

## 통합 판단

- 공개 API는 사용자 승인안 그대로 유지했다. 전역 최대 상한을 새로 강제하는 대신 호출자가
  명시적 유한 상한을 제공하고 구현이 작은 body에 큰 배열을 선할당하지 않도록 했다.
- known oversize는 content byte를 읽지 않지만 accessor와 close의 비차단성은 보장하지
  않는다. transport별 timeout·강제 abort는 범용 adapter 밖의 caller 책임이다.
- low-level library에 log·metric side effect를 추가하지 않는다. payload 비노출과
  low-cardinality 분류는 application 관측 계약으로 남긴다.
- 압축 해제 정책은 transport와 caller마다 다르므로 helper가 받은 stream byte만 제한한다.
  후속 decode/decompression에는 별도 상한이 필요하다.
- N/A 관점은 없다. 여섯 관점 모두 API·수명주기·호환성 또는 전달 과정에 직접 영향을
  주므로 독립 검토했다.

## 후속 검증 의무

- 구현 계획에서 모든 수용 기준을 atomic test/implementation task로 매핑한다.
- TDD에서 경계, read-ahead, zero-read, accessor/read/close failure, timeout supervisor close,
  nullable entity와 header matrix를 검증한다.
- 실제 KDoc/README 예제와 ABI/API, 두 모듈 check, CI를 완료하기 전에는 구현 완료로 보지
  않는다.
- 실제 HC5/JDK transport별 timeout·abort 동작과 decoded body 상한은 소비자 #939/#451
  전환에서도 다시 검증한다.

## Gate verdict

`PASS` — 최신 통합 설계에 P0/P1 finding이 없고 모든 P2/P3가 해결되거나 명시적인 후속
검증 의무로 처분됐다. 구현 계획 작성은 사용자 설계 문서 검토 이후에만 시작한다.
