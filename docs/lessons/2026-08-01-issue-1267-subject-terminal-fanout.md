# 이슈 #1267 Subject terminal fanout 취소 경계

## 배경

`PublishSubject`와 `MulticastSubject`는 terminal 상태를 먼저 기록한 뒤 현재
collector 배열을 순서대로 순회한다. 첫 collector가 값 소비 중이면
`ResumableCollector.complete()`/`error()`가 consumer 준비 신호를 기다리는데,
이때 caller가 취소되면 기존 순회가 즉시 중단되어 뒤의 collector가 종료 신호를
받지 못하고 Subject에 남을 수 있었다.

## 결정 또는 발견

- terminal fanout은 호출 시점의 collector snapshot을 끝까지 처리한다.
- caller 또는 개별 collector 취소로 handshake가 중단된 collector에는
  `ResumableCollector.terminate()`로 terminal 상태와 wake-up을 non-suspending 방식으로
  기록한다.
- caller 취소가 감지되면 남은 snapshot collector도 같은 terminal 상태로 정리한 뒤
  최초 `CancellationException`을 다시 던진다.
- 예외 terminal은 coroutine stack-trace recovery로 wrapper가 생길 수 있으므로
  regression test는 wrapper의 cause가 원래 예외 객체인지 검증한다.

## 결과

첫 collector가 값 소비를 멈춘 상태에서 caller timeout을 발생시켜도 두 번째
collector가 정상 완료 또는 동일한 terminal error를 받는다. 종료 후 두
Subject의 `collectorCount`는 0이며, 기존 single-collector timeout 및
개별 collector 취소 계약은 유지된다.

## 검증

- RED: 수정 전 `SubjectCancellationTest` 16개 중 새 terminal fanout 4개가 두 번째
  collector 대기에서 virtual-time timeout으로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-coroutines:test --tests
  'io.bluetape4k.coroutines.flow.extensions.subject.SubjectCancellationTest'
  --rerun-tasks --no-configuration-cache --console=plain` 결과
  `SUCCESS: Executed 20 tests in 2.5s`, `BUILD SUCCESSFUL`.
- GREEN full: `./gradlew :bluetape4k-coroutines:test --rerun-tasks
  --no-configuration-cache --console=plain` 결과 `SUCCESS: Executed 590 tests in
  15.3s`, `BUILD SUCCESSFUL`.
- affected detekt와 Dokka 생성이 `BUILD SUCCESSFUL`로 완료되었고,
  `git diff --check`도 통과했다. Detekt/Dokka의 기존 StructuredConcurrency
  링크 경고는 남아 있지만 이번 변경 파일에는 새 경고가 없다.

## 향후 지침

`ResumableCollector`의 producer-consumer handshake를 바꿀 때는 caller timeout이
첫 terminal signal에서 발생하는 fanout, 개별 collector 취소, 종료 후
`collectorCount == 0`을 함께 검증한다. 취소된 caller의 cleanup을
`NonCancellable` 대기만으로 처리하면 영구적으로 busy한 collector에서 terminal
호출이 반환하지 않을 수 있으므로, bounded non-suspending terminal state와
wake-up 경로를 우선 검토한다.
