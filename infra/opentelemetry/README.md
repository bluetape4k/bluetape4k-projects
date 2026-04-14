# Module bluetape4k-opentelemetry

English | [한국어](./README.ko.md)

[OpenTelemetry](https://opentelemetry.io/) is an observability framework for cloud-native software. This module provides Kotlin extension functions and utilities that make it easier and more idiomatic to use OpenTelemetry on the JVM.

## Features

- **Kotlin extension functions**: Use the OpenTelemetry Java SDK in a Kotlin-idiomatic way
- **Coroutines support**: Propagate `suspend` function context and coroutine context
- **Span management**: `use` pattern for automatic resource cleanup
- **DSL support**: DSLs for configuring Attributes, TracerProvider, and MeterProvider
- **Spring Boot integration**: Spring Boot Starter support

## Dependency

```kotlin
dependencies {
  implementation("io.github.bluetape4k:bluetape4k-opentelemetry:${bluetape4kVersion}")
}
```

## Key Features

### 1. OpenTelemetry SDK Setup

```kotlin
import io.bluetape4k.opentelemetry.*
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider

// Build an OpenTelemetry SDK instance
val openTelemetry = openTelemetrySdk {
  setTracerProvider(tracerProvider)
  setMeterProvider(meterProvider)
  setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
}

// Register as the global OpenTelemetry instance
val globalOtel = openTelemetrySdkGlobal {
  setTracerProvider(tracerProvider)
  setMeterProvider(meterProvider)
}

// Access the global instance
val otel = globalOpenTelemetry
```

### 2. Creating Tracers and Managing Spans

```kotlin
import io.bluetape4k.opentelemetry.trace.*
import io.opentelemetry.api.trace.SpanKind
import java.time.Duration

// Create a Tracer
val tracer = openTelemetry.tracer("my-service") {
  setInstrumentationVersion("1.0.0")
}

// Manually create and manage a Span
val span = tracer.startSpan("my-operation") {
  setSpanKind(SpanKind.INTERNAL)
  setAttribute("custom.attribute", "value")
}

// Automatic lifecycle management via the use pattern (try-finally handled internally)
span.use { currentSpan ->
  // Work inside the Span context
  currentSpan.addEvent("Processing started")
  doWork()
}  // Span ends automatically

// Start and use a Span directly from a SpanBuilder
tracer.spanBuilder("my-operation").useSpan { span ->
  doWork()
}

// Exceptions are recorded on the span; the original exception type is rethrown as-is
tracer.spanBuilder("failing-operation").useSpan { span ->
  runCatching { doWork() }
    .onFailure {
      span.recordException(it)
      throw it
    }
}

// These timeout arguments are kept for backwards compatibility;
// the current implementation does not artificially delay the span end time
span.use(waitTimeout = 5000) { /* work */ }
span.use(Duration.ofSeconds(5)) { /* work */ }
```

### 3. Coroutines Support

```kotlin
import io.bluetape4k.opentelemetry.coroutines.*
import kotlinx.coroutines.delay

suspend fun coroutineExample() {
  val tracer = openTelemetry.getTracer("my-service")

  // Use a Span inside a coroutine
  tracer.spanBuilder("async-operation").useSpanSuspending { span ->
    span.addEvent("Before delay")
    delay(1000)
    span.addEvent("After delay")
  }  // Span ends automatically

  // Use an existing Span in a coroutine context
  val span = tracer.spanBuilder("parent").startSpan()
  span.useSuspending { currentSpan ->
    withContext(Dispatchers.IO) {
      // Span context is propagated
      doAsyncWork()
    }
  }
}

// Explicit Span context propagation
suspend fun withExplicitContext() {
  val span = tracer.spanBuilder("operation").startSpan()
  withSpanContext(span) { currentSpan ->
    // Runs with the Span context active
    doWork()
  }
}

// Prefer useSpanSuspending over the deprecated useSuspendSpan
tracer.spanBuilder("recommended").useSpanSuspending(Dispatchers.IO) { span ->
  doAsyncWork()
}
```

### 4. Attributes Management

```kotlin
import io.bluetape4k.opentelemetry.common.*

// Create AttributeKeys
val userIdKey = "user.id".toAttributeKey()
val countKey = longAttributeKeyOf("request.count")
val tagsKey = "tags".toStringArrayAttributeKey()

// Build Attributes using the DSL
val attributes = attributes {
  put("service.name", "my-service")
  put("service.version", "1.0.0")
  put("request.count", 100L)
  put("is.active", true)
  put("tags", listOf("tag1", "tag2"))
}

// Concise Attributes creation
val attrs1 = attributesOf("key", "value")
val attrs2 = attributesOf(userIdKey, "user123", countKey, 10L)

// Convert a Map to Attributes
val map = mapOf(
  "key1" to "value1",
  "count" to 42L,
  "enabled" to true
)
val fromMap = map.toAttributes()
```

### 5. Context Management

```kotlin
import io.bluetape4k.opentelemetry.*

// Get the current context
val currentContext = currentOtelContext()

// Get the root context
val rootContext = rootOtelContext()

// Run work within a context
val result = currentContext.withCurrent {
  // Runs with the context active
  doWork()
}

// Retrieve a Span from a context
val span = currentContext.getSpan()
val spanOrNull = currentContext.getSpanOrNull()
```

### 6. TracerProvider Configuration

```kotlin
import io.bluetape4k.opentelemetry.trace.*
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.exporter.logging.LoggingSpanExporter

// Create an SdkTracerProvider
val tracerProvider = sdkTracerProvider {
  addSpanProcessor(simpleSpanProcessorOf(LoggingSpanExporter.create()))
  setResource(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, "my-service")))
}

// Create SpanProcessors
val simpleProcessor = simpleSpanProcessorOf(LoggingSpanExporter.create())
val batchProcessor = batchSpanProcessorOf(LoggingSpanExporter.create()) {
  setScheduleDelay(java.time.Duration.ofMillis(250))
}
```

### 7. Metrics Support

```kotlin
import io.bluetape4k.opentelemetry.*
import io.bluetape4k.opentelemetry.metrics.*
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader

// Create a Meter
val meter = openTelemetry.meter("my-service") {
  setInstrumentationVersion("1.0.0")
}

// Create an SdkMeterProvider
val meterProvider = sdkMeterProvider {
  registerMetricReader(InMemoryMetricReader.create())
}

// MetricReader / Exporter
val inMemoryReader = inMemoryMetricReaderOf()
val loggingReader = periodicMetricReader(loggingMetricExporterOf()) {
  setInterval(java.time.Duration.ofSeconds(5))
}
```

### 8. SpanExporter Configuration

```kotlin
import io.bluetape4k.opentelemetry.trace.*
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter

// Logging SpanExporter
val loggingExporter = loggingSpanExporterOf()

// Combine multiple Exporters
val compositeExporter = spanExporterOf(
  LoggingSpanExporter.create(),
  OtlpGrpcSpanExporter.builder().build()
)
```

## Architecture Diagrams

### Core Class Structure

```mermaid
classDiagram
    class OpenTelemetry {
        <<interface>>
        +getTracer(name) Tracer
        +getMeter(name) Meter
        +getPropagators() ContextPropagators
    }

    class Tracer {
        <<interface>>
        +spanBuilder(spanName) SpanBuilder
    }

    class SpanBuilder {
        <<interface>>
        +setSpanKind(kind) SpanBuilder
        +setAttribute(key, value) SpanBuilder
        +startSpan() Span
        +useSpan(block) T
        +useSpanSuspending(block) T
    }

    class Span {
        <<interface>>
        +addEvent(name) Span
        +setAttribute(key, value) Span
        +recordException(t) Span
        +end()
        +use(block) T
        +useSuspending(block) T
    }

    class SdkTracerProvider {
        +addSpanProcessor(sp) SdkTracerProviderBuilder
        +setResource(resource) SdkTracerProviderBuilder
        +get(instrumentationName) Tracer
    }

    class SdkMeterProvider {
        +registerMetricReader(mr) SdkMeterProviderBuilder
    }

    class Meter {
        <<interface>>
        +counterBuilder(name) LongCounterBuilder
        +timerBuilder(name) DoubleHistogramBuilder
        +gaugeBuilder(name) ObservableDoubleGaugeBuilder
    }

    OpenTelemetry --> Tracer: provides
    OpenTelemetry --> Meter: provides
    Tracer --> SpanBuilder: creates
    SpanBuilder --> Span: starts
    SdkTracerProvider ..|> Tracer: implements
    SdkMeterProvider ..|> Meter: implements

    style OpenTelemetry fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style Tracer fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style SpanBuilder fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style Span fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style Meter fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style SdkTracerProvider fill:#00897B,stroke:#00695C,color:#FFFFFF
    style SdkMeterProvider fill:#00897B,stroke:#00695C,color:#FFFFFF

```

### Component Overview

```mermaid
flowchart TD
    App[Application] --> Tracer[Tracer<br/>Span creation/management]
    App --> Meter[Meter<br/>Metrics collection]
    App --> Logger[Logger<br/>Log collection]

    Tracer --> TP[TracerProvider<br/>SdkTracerProvider]
    Meter --> MP[MeterProvider<br/>SdkMeterProvider]

    TP --> SP[SpanProcessor]
    SP --> SE[SpanExporter]

    MP --> MR[MetricReader]
    MR --> ME[MetricExporter]

    SE --> OTLP[OTLP gRPC/HTTP]
    SE --> LogExp[Logging Exporter]
    ME --> OTLP
    ME --> InMem[InMemory Exporter<br/>for testing]

    OTLP --> Jaeger[Jaeger]
    OTLP --> Zipkin[Zipkin]
    OTLP --> OtelCol[OpenTelemetry Collector]

    classDef coreStyle fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF,font-weight:bold
    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef utilStyle fill:#E65100,stroke:#E65100,color:#FFFFFF
    classDef asyncStyle fill:#6A1B9A,stroke:#6A1B9A,color:#FFFFFF
    classDef extStyle fill:#37474F,stroke:#37474F,color:#FFFFFF
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000
    classDef cacheStyle fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF

    style App fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    style TP fill:#F57F17,stroke:#E65100,color:#000000
    style MP fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style OtelCol fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF
    style SP fill:#37474F,stroke:#263238,color:#FFFFFF
    style SE fill:#37474F,stroke:#263238,color:#FFFFFF
    style MR fill:#37474F,stroke:#263238,color:#FFFFFF
    style ME fill:#37474F,stroke:#263238,color:#FFFFFF
```

### Span Lifecycle in a Coroutine Context

```mermaid
sequenceDiagram
    box rgb(187,222,251) Application
    participant App as Application
    end
    box rgb(197,202,233) Tracing
    participant Builder as SpanBuilder
    participant Span as Span
    end
    box rgb(225,190,231) Coroutines
    participant Context as CoroutineContext
    participant Child as Child Work
    end

    App->>+Builder: tracer.spanBuilder("operation")
    App->>Builder: useSpanSuspending { ... }
    Builder->>+Span: startSpan()
    Span->>+Context: makeCurrent() / withContext
    Note over Context: Span context propagated to coroutine
    Context->>+Child: Launch child coroutine
    Note over Child: Context remains active inside<br/>withContext(Dispatchers.IO)
    Child-->>-Context: Return result
    Context-->>-Span: Block exits
    Span->>Span: end()
    Span-->>-Builder: Span finished
    Builder-->>-App: Return result
```

### Distributed Trace Propagation

```mermaid
flowchart LR
    ServiceA[Service A<br/>Parent Span] -->|HTTP Header<br/>traceparent: 00-traceId-spanId-01| ServiceB[Service B<br/>Child Span]
    ServiceB -->|propagate| ServiceC[Service C<br/>Child Span]

    ServiceA -->|export| Collector[OTel Collector]
    ServiceB -->|export| Collector
    ServiceC -->|export| Collector

    Collector -->|store| Backend[Jaeger / Zipkin<br/>Distributed tracing backend]

    classDef coreStyle fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF,font-weight:bold
    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef utilStyle fill:#E65100,stroke:#E65100,color:#FFFFFF
    classDef asyncStyle fill:#6A1B9A,stroke:#6A1B9A,color:#FFFFFF
    classDef extStyle fill:#37474F,stroke:#37474F,color:#FFFFFF
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000
    classDef cacheStyle fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF

    style ServiceA fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    style ServiceB fill:#F57F17,stroke:#E65100,color:#000000
    style ServiceC fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style Backend fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF
    style Collector fill:#37474F,stroke:#263238,color:#FFFFFF
```

## Testing Strategy

### Unit Tests

```kotlin
import io.bluetape4k.opentelemetry.trace.*
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

class MyServiceTest {
  private val spanExporter = InMemorySpanExporter.create()
  private val tracerProvider = sdkTracerProvider {
    addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
  }
  private val tracer = tracerProvider.get("test")

  @AfterEach
  fun tearDown() {
    spanExporter.reset()
  }

  @Test
  fun `verify that a span is created correctly`() {
    // given
    val service = MyService(tracer)

    // when
    service.doWork()

    // then
    val spans = spanExporter.finishedSpanItems
    spans shouldHaveSize 1
    spans.first().name shouldBe "do-work"
  }
}
```

### Recommended Setup by Environment

| Environment            | Agent | Exporter             | Verification Level                   |
|------------------------|-------|----------------------|--------------------------------------|
| Production/Integration | ON    | GlobalOpenTelemetry  | Verify trace linkage                 |
| Unit tests             | OFF   | InMemorySpanExporter | Detailed checks (parentSpanId, etc.) |
| Integration tests      | ON    | Logging/OTLP         | Verify trace creation                |

## OpenTelemetry Java Agent

You can instrument your application automatically using the Java Agent:

```bash
# Download the agent
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Run the application
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=my-service \
  -Dotel.traces.exporter=otlp \
  -jar my-application.jar
```

Download the agent via a Gradle task:

```kotlin
tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadAgent") {
  src("https://github.com/open-telemetry/.../opentelemetry-javaagent.jar")
  dest("${project.layout.buildDirectory.asFile.get()}/opentelemetry-javaagent.jar")
  onlyIfModified(true)
}
```

## Examples

More examples are available in the `src/test/kotlin/io/bluetape4k/opentelemetry/examples` package:

- `logging/`: Logging Exporter examples
- `metrics/`: Metrics collection examples
- `javaagent/`: Java Agent integration examples (Spring Boot)

## References

- [OpenTelemetry Official Documentation](https://opentelemetry.io/docs/)
- [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java)
- [OpenTelemetry Kotlin Extension](https://github.com/open-telemetry/opentelemetry-java/tree/main/extensions/kotlin)
- [OpenTelemetry Spring Boot Starter](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-boot-autoconfigure)

## License

Apache License 2.0
