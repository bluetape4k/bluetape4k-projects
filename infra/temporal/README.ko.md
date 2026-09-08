# Module bluetape4k-temporal

[English](./README.md) | 한국어

Temporal Java SDK의 공식 타입을 유지하면서 코루틴 애플리케이션에서 반복되는 blocking client
호출을 줄이는 확장 모듈입니다.

## 주요 기능

- `WorkflowStub.startSuspending`, `signalSuspending`, `querySuspending`는 SDK의 blocking client
  호출을 `Dispatchers.IO`에서 실행합니다.
- `WorkflowStub.awaitResult`는 workflow가 실행되는 동안 스레드를 점유하지 않고 SDK의
  `CompletableFuture`를 기다립니다.
- `cancelSuspending`, `terminateSuspending`으로 원격 lifecycle 변경을 명시적으로 요청합니다.
- 결과를 기다리는 코루틴을 취소하거나 `withTimeout`을 사용해도 원격 cancel 요청은 보내지
  않습니다.
- `WorkerFactory.shutdownSuspending`은 제한된 graceful shutdown과 선택적 `shutdownNow` 승격을
  제공하고 현재 `isTerminated` 상태를 반환합니다.
- `KLoggingChannel`은 operation 상태만 기록합니다. workflow 인자와 SDK 예외 원문은 이 로그에
  포함하지 않습니다.

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-temporal:$bluetape4kVersion")
}
```

## Client 사용

```kotlin
import io.bluetape4k.temporal.awaitResult
import io.bluetape4k.temporal.querySuspending
import io.bluetape4k.temporal.signalSuspending
import io.bluetape4k.temporal.startSuspending
import io.temporal.client.WorkflowStub
import io.temporal.client.newWorkflowStub

val typed = workflowClient.newWorkflowStub<ApprovalWorkflow> {
    setWorkflowId("approval-42")
    setTaskQueue("approval-task-queue")
}
val workflow = WorkflowStub.fromTyped(typed)

workflow.startSuspending("order-42")
workflow.querySuspending<String>("status")
workflow.signalSuspending("approve")
val result = workflow.awaitResult<String>()
```

operation 이름과 payload는 공식 Temporal SDK에 그대로 전달됩니다. 코루틴 wrapper는 retry,
cache, authorization, client 소유권을 추가하지 않습니다. RPC deadline은
`WorkflowServiceStubsOptions.Builder#setRpcTimeout`, `setRpcQueryTimeout`,
`setRpcLongPollTimeout`으로 설정하세요. `WorkflowOptions`는 RPC deadline이 아니라 workflow 실행
및 task timeout을 설정합니다.

`startSuspending`, `signalSuspending`, `querySuspending`은 `Dispatchers.IO`에서 SDK의 blocking
메서드를 호출하므로 SDK RPC deadline이 호출을 제한합니다. 호출 코루틴이 취소되어도 이미 blocking
구간에 진입한 SDK 호출을 중단하지 않습니다. `awaitResult`는 non-blocking future를 사용하므로 이
RPC deadline과 독립적으로 코루틴 취소를 따릅니다.

### 취소 계약

```kotlin
withTimeout(5.seconds) {
    workflow.awaitResult<String>()
}
```

위 timeout은 로컬 결과 대기와 pending future만 취소합니다. 원격 workflow는 취소하지 않습니다.
원격 실행을 취소하거나 종료해야 한다면 명시적으로 호출하세요.

```kotlin
workflow.cancelSuspending("user requested cancellation")
workflow.terminateSuspending("operator termination", "incident-123")
```

Workflow 정의는 Temporal의 deterministic synchronous API를 계속 사용합니다. workflow 구현에
일반 코루틴을 넣지 말고 Temporal signal, timer, activity와 공식 `temporal-kotlin` DSL을
사용하세요.

## Worker 종료

```kotlin
val terminated = workerFactory.shutdownSuspending(10.seconds, force = true)
```

이 확장은 `shutdown()`을 한 번 요청하고 지정한 시간만큼 기다립니다. factory가 계속 실행 중이고
`force`가 `true`일 때만 `shutdownNow()`를 요청합니다. 대기 중 호출 코루틴이 취소되면 강제 종료로
승격하기 전에 취소를 관찰합니다. 두 번째 무제한 대기는 하지 않으며 `WorkflowClient`, service
stubs와 호출자가 소유한 다른 자원을 닫지 않습니다. 반복 호출은 SDK의 멱등적인 shutdown 동작을
유지합니다.

## 테스트

공식 `TestWorkflowEnvironment`를 사용해 인메모리 workflow 테스트를 작성하세요. 긴 timer를 위한
time-skipping을 지원하므로 외부 Temporal service 없이 signal, query, 결과 대기 취소, 원격 취소,
activity retry, 멱등성, Saga 보상, history replay를 검증할 수 있습니다.
