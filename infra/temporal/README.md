# Module bluetape4k-temporal

English | [한국어](./README.ko.md)

Coroutine extensions for the Temporal Java SDK. The module keeps Temporal's official workflow,
client, and worker types while removing blocking client-call boilerplate from coroutine-based
applications.

## Features

- `WorkflowStub.startSuspending`, `signalSuspending`, and `querySuspending` run the SDK's blocking
  client calls on `Dispatchers.IO`.
- `WorkflowStub.awaitResult` waits on the SDK `CompletableFuture` without occupying a thread while
  the workflow is running.
- `cancelSuspending` and `terminateSuspending` make remote lifecycle changes explicit.
- Cancelling the coroutine waiting for a result, including `withTimeout`, never sends a remote
  cancel request.
- `WorkerFactory.shutdownSuspending` provides bounded graceful shutdown with optional
  `shutdownNow` escalation and returns the current `isTerminated` state.
- `KLoggingChannel` logs operation status only. Workflow inputs and SDK exception text are not
  included in these logs.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-temporal:$bluetape4kVersion")
}
```

## Client usage

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

The operation names and payloads are passed directly to the official Temporal SDK. The coroutine
wrappers do not add retries, caching, authorization, or client ownership. Configure RPC deadlines
with `WorkflowServiceStubsOptions.Builder#setRpcTimeout`,
`setRpcQueryTimeout`, and `setRpcLongPollTimeout`. `WorkflowOptions` configures workflow execution
and task timeouts rather than RPC deadlines.

`startSuspending`, `signalSuspending`, and `querySuspending` invoke the SDK's blocking methods on
`Dispatchers.IO`, so their SDK RPC deadlines bound the calls. Cancelling the caller coroutine does
not interrupt an SDK call that has already entered its blocking section. `awaitResult` uses a
non-blocking future and follows coroutine cancellation independently of those RPC deadlines.

### Cancellation contract

```kotlin
withTimeout(5.seconds) {
    workflow.awaitResult<String>()
}
```

The timeout above only cancels the local result wait and its pending future. It does not cancel the
remote workflow. Use an explicit call when the remote execution must be cancelled or terminated:

```kotlin
workflow.cancelSuspending("user requested cancellation")
workflow.terminateSuspending("operator termination", "incident-123")
```

Workflow definitions continue to use Temporal's deterministic synchronous APIs. Do not introduce
regular coroutines into a workflow implementation; use Temporal signals, timers, activities, and
the official `temporal-kotlin` DSL.

## Worker shutdown

```kotlin
val terminated = workerFactory.shutdownSuspending(10.seconds, force = true)
```

The extension requests `shutdown()` once, waits up to the supplied duration, and calls
`shutdownNow()` only when the factory is still running and `force` is `true`. It performs no second
unbounded wait. If the caller coroutine is cancelled while waiting, the extension observes that
cancellation before any force escalation. It does not close the `WorkflowClient`, service stubs, or
other caller-owned resources. Repeated calls preserve the SDK's idempotent shutdown behavior.

## Testing

Use Temporal's official `TestWorkflowEnvironment` for in-memory workflow tests. It supports
time-skipping for long timers and lets tests cover signals, queries, result-wait cancellation,
remote cancellation, activity retries, idempotency, Saga compensation, and history replay without
starting an external Temporal service.
