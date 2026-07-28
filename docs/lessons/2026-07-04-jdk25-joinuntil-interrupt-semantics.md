# JDK25 `joinUntil` interrupt semantics

## 배경

이슈 #953은 JDK25 structured-scope `joinUntil` fallback이 모든
`InterruptedException`을 `TimeoutException`으로 바꾸고 호출자 thread의 interrupt
status를 지운다는 점을 확인했다.

## 결정

Scheduled timeout interrupt만 timeout evidence로 추적한다. 기존 interrupt와 외부
interrupt는 `InterruptedException`을 다시 던지고 호출자 thread interrupt flag를
복구해서 보존한다.

## 결과

Timeout join은 계속 `TimeoutException`을 던지지만, timeout scheduler 밖에서 온
cancellation 또는 interrupt signal은 진단 가능한 상태로 남는다.

## 검증

- `./gradlew :bluetape4k-virtualthread-jdk25:test --tests 'io.bluetape4k.concurrent.virtualthread.jdk25.Jdk25StructuredTaskScopeProviderTest' --tests 'io.bluetape4k.concurrent.virtualthread.jdk25.Jdk25StructuredTaskScopeProviderExtTest'`

## 향후 지침

Structured-concurrency helper에서는 소비하는 interrupt signal을 해당 코드가 만들었다는
점을 증명할 수 있을 때만 interrupt status를 지운다.
