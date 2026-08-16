# #1426 NearJCache write-through 관찰 스냅숏

## 배경

`NearJCache`는 마지막 Back Cache write-through의 operation ID와 completion을
하나의 `AtomicReference`에 저장하지만, 기존 public property는 값을 서로 다른
시점에 읽을 수 있었습니다. concurrent write가 끼어들면 모니터링·재시도·감사
코드가 서로 다른 operation을 하나의 결과로 결합할 위험이 있었습니다.

## 결정

- `lastBackCacheWrite`가 operation ID·operation 이름·completion을 한 번에 반환하는
  `BackCacheWriteCompletion` observation snapshot을 제공합니다.
- snapshot의 completion은 `minimalCompletionStage()`로 노출해 호출자가 내부 future를
  완료하거나 실패 상태를 바꿀 수 없게 합니다.
- 기존 `lastBackCacheWriteOperationId`와 `lastBackCacheWriteCompletion` property는
  source compatibility를 위해 유지하고, 상관관계가 필요한 호출자는 snapshot을
  사용하도록 KDoc과 EN/KO README에 명시합니다.

## 검증

- concurrent write 회귀 테스트가 snapshot의 operation ID를 listener completion과
  대조해 operation과 completion이 같은 state에서 왔음을 확인합니다.
- snapshot과 기존 completion copy를 호출자가 변경해도 실제 write 결과가 보존되는
  불변성 계약을 확인합니다.

## Stacked train

- 선행: #1410 / PR #1431 / `fix/1410-nearcache-listener-reentrancy`
- 현재: #1426 / `fix/1426-nearcache-observation`
- 후속: #1348은 #1426 exact head를 base로 삼는 child PR로만 쌓습니다.
