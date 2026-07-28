# 교훈 - 이슈 #856 Workflow ALL fail-fast

## 배경

issue #856은 blocking/suspend parallel workflow에서 branch 하나가 throw하거나 non-success
`WorkReport`를 반환할 때 `ParallelPolicy.ALL`이 남은 sibling을 중지하도록 고쳤다.

## 교훈

- fail-fast는 thrown exception뿐 아니라 returned report도 cover해야 한다. child가 자기
  exception을 catch하고 `WorkReport.Failure`를 반환하면, 그렇지 않을 경우 structured
  concurrency에는 정상 완료처럼 보인다.
- `WorkReport.Aborted`와 `WorkReport.Cancelled`도 ALL에서는 non-success outcome이다.
  모든 branch를 기다린 뒤 priority table을 적용하지 말고 sibling을 cancel해야 한다.
- cancellation test에는 workflow-owned sibling이 필요하다. stress helper는 반복 독립
  실행에는 유용하지만, 특정 sibling이 workflow scope에서 `InterruptedException` 또는
  coroutine cancellation을 받았다는 증명은 아니다.
- suspend implementation은 `CancellationException`을 삼키면 안 된다. parent cancellation과
  child cancellation contract가 관찰 가능하도록 rethrow한다.
- README policy text와 `ParallelPolicy` KDoc은 implementation semantic과 맞춰 유지한다.
  ALL은 모든 branch가 성공한다는 뜻이고, non-success branch 하나라도 있으면 fail fast다.

## 가드

parallel workflow policy를 바꿀 때는 blocking/suspend 구현 모두에서 throw, failure report,
aborted, cancelled branch를 다루는 parity test를 먼저 추가한다.
