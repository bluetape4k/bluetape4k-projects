# Module Examples - Kotlin Coroutines

English | [한국어](./README.ko.md)

A collection of examples for learning the features and usage patterns of Kotlin Coroutines.

## Examples

### Basics (guide/)

| Example File                  | Description                                      |
|-------------------------------|--------------------------------------------------|
| `CoroutineExamples.kt`        | Coroutine basics: coroutineScope, launch         |
| `CoroutineBuilderExamples.kt` | Coroutine builders: launch, async, produce       |
| `CoroutineContextExamples.kt` | Understanding and using CoroutineContext         |
| `SuspendExamples.kt`          | Basics of suspend functions                      |
| `MDCContextExamples.kt`       | Integration with MDC (Mapped Diagnostic Context) |

### Flow Examples (flow/)

| Example File               | Description                                    |
|----------------------------|------------------------------------------------|
| `FlowBasicExamples.kt`     | Basic Flow creation and collection             |
| `FlowBuilderExamples.kt`   | Builders: flowOf, asFlow, channelFlow, etc.    |
| `FlowOperatorExamples.kt`  | Operators: map, filter, transform, etc.        |
| `FlowLifecycleExamples.kt` | Lifecycle hooks: onStart, onCompletion, onEach |
| `SharedFlowExamples.kt`    | Implementing an event bus with SharedFlow      |
| `StateFlowExamples.kt`     | State management with StateFlow                |
| `ChannelFlowExamples.kt`   | channelFlow and cold/hot flows                 |
| `CallbackFlowExamples.kt`  | Converting callback-based APIs to Flow         |

### Channel Examples (channels/)

| Example File         | Description                             |
|----------------------|-----------------------------------------|
| `ChannelExamples.kt` | Channel basics: produce, consume        |
| `ActorExamples.kt`   | State management with the Actor pattern |

### Cancellation (cancellation/)

| Example File              | Description                                     |
|---------------------------|-------------------------------------------------|
| `CancellationExamples.kt` | Cooperative cancellation and exception handling |

### Coroutine Context (context/)

| Example File                      | Description                            |
|-----------------------------------|----------------------------------------|
| `CoroutineContextExamples.kt`     | Implementing a custom CoroutineContext |
| `CounterCoroutineContext.kt`      | Counter context example                |
| `UuidProviderCoroutineContext.kt` | UUID-providing context                 |

### Builders (builders/)

| Example File                         | Description               |
|--------------------------------------|---------------------------|
| `CoroutineBuilderExamples.kt`        | Custom coroutine builders |
| `CoroutineContextBuilderExamples.kt` | Context builder patterns  |

### Dispatchers (dispatchers/)

| Example File            | Description                                   |
|-------------------------|-----------------------------------------------|
| `DispatcherExamples.kt` | Default, IO, Unconfined, and Main dispatchers |

### Exception Handling (exceptions/)

| Example File                   | Description                                   |
|--------------------------------|-----------------------------------------------|
| `ExceptionHandlingExamples.kt` | CoroutineExceptionHandler and try-catch usage |

### Scope (scope/)

| Example File                | Description                                       |
|-----------------------------|---------------------------------------------------|
| `CoroutineScopeExamples.kt` | lifecycleScope, viewModelScope, and custom scopes |

### Testing (tests/)

| Example File         | Description               |
|----------------------|---------------------------|
| `TurbineExamples.kt` | Testing Flow with Turbine |

## How to Run

```bash
# Run all example tests
./gradlew :examples:coroutines:test

# Run specific examples
./gradlew :examples:coroutines:test --tests "io.bluetape4k.examples.coroutines.guide.*"
./gradlew :examples:coroutines:test --tests "io.bluetape4k.examples.coroutines.flow.*"
```

## Key Learning Points

1. **CoroutineScope**: The foundation of structured concurrency
2. **Suspend functions**: Writing async code that reads like synchronous code
3. **Flow**: Kotlin's implementation of reactive streams
4. **Channel**: Communication between coroutines
5. **Exception handling**: SupervisorJob and CoroutineExceptionHandler

## References

- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)
