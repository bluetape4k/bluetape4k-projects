# 이슈 1896 - Kafka Streams producer acknowledgement 순서

## 배경

Issue #1896은 `KafkaStreamsTests`가 hosted CI에서 `TimeoutException`,
`NotCoordinatorException`, `NotLeaderOrFollowerException`, KRaft 메타데이터 파일의
`NoSuchFileException`, `KafkaStorageException`과 함께 실패한 사건을 다룬다.
문제가 보고된 원본 head는 `bdecb8183cdb77356831e4a42f8fcacc986a2197`이고, 실패한
workflow run은 `43053814741`이다. 현재 `origin/develop` head는
`619eb71f89259f943f64fb995d7f16b7d2608be6`이다.

테스트는 `KafkaStreams.State.RUNNING` 상태를 기다린 뒤 두 개의 레코드를 보내고
`flush()`를 호출했다. 그러나 `sendDefault`가 반환한 future를 관찰하지 않았기 때문에
producer acknowledgement와 Streams 결과 전달의 순서가 테스트에 드러나지 않았다.

## 재현과 원인

원본 head 기반의 Kafka3 targeted test는 로컬에서 통과했지만, 세 번의 강제 재실행 모두
테스트 종료 시 `KafkaStorageException` 57건과 `NoSuchFileException` 2건을 남겼다. 이는
단순한 green 결과만으로 broker lifecycle 경계를 증명할 수 없음을 보여준다. `runningLatch`는
Streams topology가 실행 중임을 증명할 뿐, producer가 두 레코드를 broker에 승인받았거나
결과 토픽으로 전달했다는 뜻은 아니다.

## 결정

Kafka3와 Kafka4의 동일한 테스트 순서를 함께 고정한다.

1. `sendDefault(0, payload1).get(60.seconds)`로 첫 producer acknowledgement를 기다린다.
2. `sendDefault(0, payload2).get(60.seconds)`로 두 번째 acknowledgement를 기다린다.
3. 그 다음 `resultFuture.get(60.seconds)`로 Streams 결과를 기다린다.

명시적인 bounded wait가 `flush()`보다 이 테스트의 계약을 정확하게 표현하므로 `flush()`는
제거했다. 라이브러리 공개 API나 production code는 변경하지 않았다.

## 검증

- RED: 수정 전 계약 테스트가 Kafka3와 Kafka4 모두 `sendDefault(...).get(60.seconds)` 누락으로 실패했다(`FF`).
- GREEN: 수정 후 동일 계약 테스트가 통과했다.
- Kafka3 targeted test: `1 passing`, `BUILD SUCCESSFUL`, `rc=0`. 종료 경로에는
  `KafkaStorageException` 57건, `NoSuchFileException` 2건, `TimeoutException` 3건이 남았다.
- Kafka4 targeted test: `1 passing`, `BUILD SUCCESSFUL`, `rc=0`; 확인한 종료 경고 패턴은 0건이었다.
- Catalog contract suite: `148 tests`, `OK`.
- `git diff --check`: 통과.

Kafka3 종료 경고는 이번 순서 수정만으로 해결됐다고 주장하지 않는다. 이는 broker/test-kit
cleanup lifecycle의 별도 위험으로 남기며, hosted exact-head Kafka3/Kafka4 matrix와 Coverage
Report read-back은 아직 수행하지 않았다.

## 향후 가드

Kafka3와 Kafka4 Streams 테스트는 broker readiness, producer acknowledgement, result delivery를
각각 bounded wait로 검증해야 한다. Retry-green 결과는 첫 실패 시도의 lifecycle·ordering
증거를 지우지 않으므로, 향후 CI 판정에서도 최초 시도와 재시도를 분리해 기록한다.
