# Module Examples - Kotlin Coroutines

English | [한국어](./README.ko.md)

A collection of examples for learning the features and usage patterns of Kotlin Coroutines.

![Coroutines demo learning map](../../docs/images/readme-diagrams/examples-coroutines-demo-diagram-01.png)

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
| `CallbackFlowExamples.kt`  | Converting Kafka producer callbacks to `Flow<RecordMetadata>` with bounded backpressure, cancellation, and cleanup |

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
./gradlew :bluetape4k-examples-coroutines-demo:test

# Run specific examples
./gradlew :bluetape4k-examples-coroutines-demo:test --tests "io.bluetape4k.examples.coroutines.guide.*"
./gradlew :bluetape4k-examples-coroutines-demo:test --tests "io.bluetape4k.examples.coroutines.flow.*"
```

### Kafka callbackFlow contract

Run the executable Kafka callback example with:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples' \
  --no-configuration-cache --max-workers=1
```

This test requires a Docker daemon because Testcontainers starts a Kafka broker
on a dynamic port. Each test uses a unique topic, and consumer polling and
producer closing have bounded timeouts. The adapter does not retry sends and
does not guarantee metadata order. A failed or cancelled collection may expose
partial results; the first producer/callback failure remains the terminal cause,
and cancellation of in-flight send futures is requested on a best-effort basis;
late callbacks are ignored before bounded cleanup closes the producer.

## Key Learning Points

1. **CoroutineScope**: The foundation of structured concurrency
2. **Suspend functions**: Writing async code that reads like synchronous code
3. **Flow**: Kotlin's implementation of reactive streams
4. **Channel**: Communication between coroutines
5. **Exception handling**: SupervisorJob and CoroutineExceptionHandler

## References

- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)
