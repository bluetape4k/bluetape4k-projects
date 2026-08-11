# #1362 NearJCache write-through 실패 관찰성

## 배경

`NearJCache`는 front cache mutation을 먼저 수행한 뒤 back cache에
write-through를 적용합니다. 기존 경로는 동기 반영의 실패 결과를 버리거나 비동기
작업의 completion을 호출자에게 노출하지 않아, front cache만 변경된 상태가
성공처럼 보일 수 있었습니다. `removeAll`처럼 여러 key를 처리하는 작업은 일부
실패도 단일 성공 결과로 축약될 수 있었습니다.

## 결정 또는 발견

- 동기 write-through는 back cache 예외를 원래 호출자에게 전파해 front 반영과
  back 반영 완료를 같은 상태로 오인하지 않도록 합니다.
- 비동기 write-through는 operation별 `CompletionStage`와 listener를 제공하고,
  설정된 횟수 안에서만 재시도합니다. timeout, cancellation, `Error` 및 재시도
  소진은 terminal failure로 completion에 기록합니다.
- 최근 completion은 불변 snapshot으로 노출해 관찰자가 내부 mutable 상태를
  변경할 수 없게 합니다.
- bulk mutation은 실패 key와 원인 정보를 집계해 partial failure를 보존합니다.
  특히 `removeAll`의 일부 실패를 전체 성공으로 축약하지 않습니다.
- 기존 JCache mutation 메서드 시그니처는 유지하고, 별도의 completion/listener
  경로로 비동기 결과를 관찰하게 합니다. 비동기 retry 상한은
  `NearJCacheConfig.syncRemoteRetryCount`로 조정하며 최대 3회로 제한합니다.

## 결과

호출자는 동기 실패를 즉시 확인하고, 비동기 작업도 operation 식별자와 함께 성공,
실패, timeout을 관찰할 수 있습니다. bulk 작업의 partial failure가 보존되어
재시도·경보·복구 정책을 적용할 수 있고, front cache의 선행 변경이 back cache
반영 성공으로 잘못 해석되지 않습니다.

## 검증

- write-through 실패·partial failure·retry·timeout·listener·불변 snapshot
  회귀 테스트 18개와 설정 builder 테스트 2개를 통과했습니다.
- `:bluetape4k-cache-core:test` 전체 525개 테스트 통과.
- `:bluetape4k-cache-core:detekt` 성공. 기존 baseline warning 외 변경 파일의
  신규 finding은 없습니다.
- `git diff --check` 통과.
- exact-model 독립 reviewer가 P0/P1/P2/P3 finding 없이 `APPROVE`했습니다.

## 향후 지침

새 NearJCache mutation 경로를 추가할 때는 동기·비동기 양쪽의 terminal completion,
timeout과 retry 상한, bulk partial failure 집계를 함께 정의합니다. 로그만 남기고
실패를 버리지 말며, front cache 선행 변경과 back cache 반영 완료를 서로 다른
상태로 문서화하고 회귀 테스트로 고정합니다.
