# bluetape4k-coroutines / bluetape4k-io 리뷰·테스트·문서 보강 설계

**날짜**: 2026-05-11
**상태**: DRAFT
**대상 모듈**: `bluetape4k-coroutines`, `bluetape4k-io`

## 목적

두 모듈을 순차적으로 리뷰해 P0/P1 결함이 남지 않도록 보정하고, 누락된 edge test, public API KDoc 예제, README/README.ko 동기화를 보강한다.

## 범위

- `bluetape4k-coroutines`: coroutine cancellation 전파, subject 종료 신호, bounded buffer edge, public API 문서.
- `bluetape4k-io`: file/path/zip/compressor/serializer API의 입력 검증, edge test, public API 문서.
- 각 모듈별 targeted test와 module test를 통과시킨다.

## 리뷰 기준

1. Correctness: 정상/오류/취소 경로가 public contract와 일치한다.
2. Concurrency: coroutine cancellation과 structured concurrency를 삼키지 않는다.
3. Validation: public caller 입력은 bluetape4k validation helper 또는 명확한 Kotlin 표준 검증을 사용한다.
4. API/KDoc: public API에는 한국어 KDoc, 계약, 예제가 있다.
5. Tests: 성공 경로 외 실패, 취소, 경계값, 빈 입력을 포함한다.
6. Docs: README와 README.ko가 같은 기능 표면을 설명한다.

## 초기 위험 지점

- `awaitAnyAndCancelOthers`는 winner 탐지 내부에서 `runCatching { deferred.await() }`를 사용하므로 취소를 값처럼 담을 위험이 있다.
- `BehaviorSubject.complete`, `PublishSubject.emitError/complete`는 suspend 종료 알림을 `runCatching`으로 감싸 cancellation 전파가 불명확하다.
- `bluetape4k-io`는 `runCatching` 기반 `Result` API가 많아 실패를 의도적으로 값화하는 곳과 숨기면 안 되는 곳을 구분해야 한다.

## 완료 조건

- 각 모듈 6-Tier review에서 P0/P1이 0개다.
- 추가/수정된 tests가 실패 재현 또는 edge contract를 증명한다.
- module-level Gradle verification이 통과한다.
- 변경 사항은 Lore commit으로 기록하고 draft PR을 생성한다.
