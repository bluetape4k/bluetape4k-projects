# Module bluetape4k-opentelemetry

[OpenTelemetry](https://opentelemetry.io/) 를 Kotlin에서 더욱 쉽고 편리하게 사용할 수 있도록 도와주는 유틸리티 함수들을 제공합니다.

## 모듈 구성

### `io.bluetape4k.opentelemetry`

- `ContextExtensions.kt`: OpenTelemetry
  `Context`와 관련된 유틸리티 함수 및 확장 함수를 제공합니다. 현재 컨텍스트를 가져오거나, Root 컨텍스트를 사용하거나, 특정 컨텍스트 내에서 블록을 실행하는 등의 기능을 포함합니다.
- `OpenTelemetrySupport.kt`: `OpenTelemetrySdk` 빌더를 사용하거나, `Tracer`, `Meter` 빌더를 사용하여
  `OpenTelemetry` 인스턴스를 생성하고 설정하는 데 필요한 함수들을 제공합니다.

### `io.bluetape4k.opentelemetry.common`

- `AttributeKeySupport.kt`: 다양한 타입의
  `AttributeKey`를 생성하는 팩토리 함수 및 확장 함수를 제공합니다. 이를 통해 OpenTelemetry 속성을 더욱 쉽게 정의할 수 있습니다.
- `AttributesSupport.kt`: `Attributes` 인스턴스를 빌드하거나, `Map`에서 `Attributes`로 변환하는 유틸리티 함수들을 제공합니다.

### `io.bluetape4k.opentelemetry.coroutines`

- `CompletableResultCodeSupport.kt`: `CompletableResultCode`에 대한
  `await()` 확장 함수를 제공하여 코루틴 환경에서 비동기 결과 코드를 처리할 수 있도록 돕습니다.
- `ContextSupport.kt`: 코루틴 컨텍스트와 OpenTelemetry `Context`를 통합하여 특정 컨텍스트 내에서 코루틴 블록을 실행하는 `withOtelContext` 함수를 제공합니다.
- `SpanSupport.kt`: `SpanBuilder`를 사용하여 새로운 `Span`을 생성하고 특정 코루틴 컨텍스트 내에서 블록을 실행하는 `useSuspendSpan` 함수를 제공합니다.

### `io.bluetape4k.opentelemetry.metrics`

- `MeterProviderSupport.kt`: `SdkMeterProvider`를 빌드하거나 No-op `MeterProvider`를 제공하는 함수를 포함합니다.
- `MetricExporterSupport.kt`: `InMemoryMetricExporter`와 `LoggingMetricExporter` 인스턴스를 생성하는 팩토리 함수를 제공합니다.
- `MetricReaderSupport.kt`: `InMemoryMetricReader` 및 `PeriodicMetricReader`를 생성하여 메트릭 데이터를 읽고 내보낼 수 있도록 돕습니다.

### `io.bluetape4k.opentelemetry.trace`

- `SpanExporterSupport.kt`: `LoggingSpanExporter`와 여러 `SpanExporter`를 조합하는 기능을 제공합니다.
- `SpanProcessorSupport.kt`: `SimpleSpanProcessor` 및 `BatchSpanProcessor`를 생성하는 팩토리 함수를 제공하여 스팬 처리 방식을 설정할 수 있도록 합니다.
- `SpanSupport.kt`: `Span` 및 `SpanBuilder`에 대한 `use` 및 `useSpan` 확장 함수를 제공하여 스팬의 생명주기를 간편하게 관리할 수 있도록 합니다.
- `TracerSupport.kt`: `SdkTracerProvider`를 빌드하거나 새로운 `Span`을 시작하는 `startSpan` 확장 함수를 제공합니다.

## 테스트 시 주의점

추천하는 운영 구성(한 줄)

- 운영/통합환경: agent ON + GlobalOpenTelemetry 사용(앱에서 SDK 만들지 않기)
- 단위테스트: agent OFF + InMemorySpanExporter로 parentSpanId까지 정밀 assert
- 통합테스트: agent ON + logging/otlp exporter로 “트레이스가 붙는지”만 확인(정밀 assert는 보통 어려움)
