# 이슈 #1297 단계 2-R 명세 리뷰

## 리뷰 범위와 방법

- Issue: #1297, `bluetape4k-coroutines` Flow 연산자 동등성
- 대상 문서: `docs/superpowers/specs/2026-08-03-flow-operator-parity-design.md`
- 저장소 근거: 기존 `windowed.kt`, `bufferingDebounce.kt`,
  `concatMapEager.kt`, `onBackpressureDrop.kt`, coroutine 테스트 dependency,
  benchmark 등록
- 리뷰 일자: 2026-08-03
- 실행 방식: 사용자가 inline 실행을 요청하여 필수 관점 6개를 main session에서
  순차 검토했다. Workflow receipt에는 `main-review` fallback lane으로 기록했고
  독립 구현 lane에는 위임하지 않았다.

최초 리뷰에서는 같은 시각의 timer 우선순위, 반환된 window의 수명주기,
benchmark가 증명할 수 있는 범위라는 계약 모호성 3건이 발견되었다. 통합 전에
해당 지적을 설계에 반영했다.

## 관점별 지적과 재검토

| 우선순위 | 관점 | 근거 | 필수 수정 | 재검토 결과 |
|---|---|---|---|---|
| P1 | 성능 | 최초 계획 문구는 하루짜리 benchmark timeout이 timer 발생까지 증명하는 것으로 해석될 수 있었다. | Timer 등록/list 할당 근거와 가상 시간 timer 발생 테스트를 분리한다. | PASS. 명세의 수용 기준과 계획 작업 5의 benchmark 설명에 경계를 명시했다. |
| P1 | 안정성 | Kotlin `select`에서는 같은 가상 시각에 수신 절과 timeout 절이 모두 준비될 수 있다. | 수신 절을 먼저 등록하고 편향된 select의 우선순위를 문서화하며 결정적인 경합 테스트를 추가한다. | PASS. 명세의 현재 근거와 실패 형태, 계획 작업 2의 같은 시각 경합 테스트에 반영했다. |
| P1 | 개발자/API | `windowTimeout`이 반환하는 `Flow`가 실시간인지 재생 가능한지 명시하지 않았다. | 완료된 cold 스냅숏으로 정의하고 실시간 window와의 이전 차이를 문서화한다. | PASS. 명세의 선택한 설계와 호환성, 계획 작업 2의 반복 수집 테스트에 반영했다. |
| P1 | 사용자/호출자 | 잘못된 인자와 upstream cancellation을 설명만 하고 정확한 테스트로 제시하지 않았다. | 잘못된 크기/기간과 `take(1)`/`finally` 테스트를 추가한다. | PASS. 계획 작업 2의 validation과 cancellation 테스트에 반영했다. |
| P2 | 운영 | 이 모듈만 변경해서는 외부 Rx/Reactor runtime 상호 운용성을 검증할 수 없다. | Slice 범위 밖임을 명시하고 `Not-tested` release 위험으로 유지한다. | DEFERRED. 명세의 완료 정의와 계획 작업 6에 범위를 명시했다. |
| P3 | 보안 | 설계에 인증, secret, 외부 입력 신뢰 경계, persistence, network side effect가 추가되지 않는다. | 범위 근거와 함께 N/A로 기록하며 불필요한 보안 계층을 추가하지 않는다. | N/A. 열린 지적이 없다. |

## 통합 계약 검사

| 검사 | 근거 | 결과 |
|---|---|---|
| 경계와 validation | `bufferTimeout`/`windowTimeout`은 양수인 크기와 기간을 요구하고 timeout은 양수인 기간을 요구한다. 빈 batch/window는 없다. | PASS |
| 완료와 실패 | 완료 시 비어 있지 않은 부분 스냅숏 하나를 방출한다. Upstream 실패 시 처리 중인 스냅숏을 폐기하고 원인을 보존한다. | PASS |
| Cancellation | Producer와 timer를 함께 취소하고 `CancellationException`을 데이터 오류로 변환하지 않는다. Fallback은 upstream 정리 후 실행된다. | PASS |
| Timer 결정성 | 모든 새 timer 계약은 coroutine suspension과 `runTest`를 사용하며 `bufferingDebounce.kt`의 wall-clock `System.nanoTime`은 재사용하지 않는다. | PASS |
| 제한된 순서 보장 mapping | 기존 overload는 소스 호환성을 유지한다. 제한된 overload는 명시적 동시성과 내부 항목별 용량 및 `finally` 정리를 사용한다. | PASS |
| 범위와 호환성 | 추가형 API만 제공한다. 표준 Flow 연산자는 대응 항목 또는 비목표로 유지하고 보류한 계열은 연결된 후속 issue 하나로 관리한다. | 작업 1 live 중복 검사 전제 PASS |
| 공개 문서 | 한국어 우선 KDoc과 두 README locale이 필요하며 GitHub 산출물은 영어를 유지한다. | 계획 gate로 PASS. 구현 문서는 아직 PENDING |

## 판정

- P0: **0**
- P1: 위 수정 후 **0**
- P2: **1건 보류**(외부 runtime 상호 운용성, 이 module-local slice의 필수 범위가 아님)
- P3: 열린 항목 **0**
- 단계 2-R 상태: 설계 문서 기준 **PASS**
- 구현 상태: **PENDING**. 이 리뷰는 코드나 runtime 검증 완료를 주장하지 않는다.

다음 gate는 단계 3-R 계획 리뷰다. 계획 리뷰가 P0=0, P1=0으로 종료되고
workflow receipt에 일치하는 근거를 기록한 뒤에만 구현을 시작할 수 있다.
