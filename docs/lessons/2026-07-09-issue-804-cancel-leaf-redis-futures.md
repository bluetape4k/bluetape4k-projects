# 이슈 804: leaf Redis future 취소

## 배경

Redis bulk await helper는 `CompletionStage.sequence`가 만든 하나의 aggregate
`CompletableFuture`를 기다렸다. Caller coroutine을 취소하면 aggregate await는
취소되었지만, 원본 Lettuce `RedisFuture` 또는 Redisson `RFuture` leaf는 취소되지
않았다.

## 결정

Leaf cancellation을 shared `CompletionStage.sequence` boundary에 둔다. 반환된 aggregate
future가 취소되면 각 source future가 `cancel(true)`를 받는다. Redis-specific helper는
기존 API를 유지하고 shared all-or-nothing cancellation behavior를 상속한다.

## 결과

Core, Lettuce, Redisson test는 aggregate/coroutine cancellation이 pending source
future를 취소하면서 input-order success result와 기존 failure propagation을 보존함을
증명한다.

## 향후 지침

많은 external future를 하나의 coroutine await 뒤에 감쌀 때 cancellation은 aggregate
future뿐 아니라 source future에도 전파되어야 한다. 이 동작이 여러 module에서
재사용되면 shared aggregate helper와 대표 adapter boundary를 모두 다룬다.
