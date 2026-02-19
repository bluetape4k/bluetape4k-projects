package io.bluetape4k.opentelemetry.trace

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder

/**
 * 아무 일도 하지 않는 [Span] 만을 생성하는 [TracerProvider] 입니다.
 */
@JvmField
val noopTraceProvider: TracerProvider = TracerProvider.noop()

/**
 * [SdkTracerProvider]를 생성합니다.
 *
 * @param builder [SdkTracerProviderBuilder]를 설정하는 람다
 * @return [SdkTracerProvider] 인스턴스
 */
inline fun sdkTracerProvider(
    @BuilderInference builder: SdkTracerProviderBuilder.() -> Unit,
): SdkTracerProvider {
    return SdkTracerProvider.builder().apply(builder).build()
}


/**
 * 새로운 [Span]을 시작합니다.
 *
 * 사용자는 이 `Span`을 종료하려면 **반드시** 수동으로 [Span.end()]를 호출해야 합니다.
 *
 * 새로 생성된 `Span`을 현재 컨텍스트에 설치하지 않습니다.
 *
 * **중요: 이 메소드는 [SpanBuilder] 인스턴스당 한 번만 호출할 수 있으며 마지막으로 호출되는 메소드여야 합니다.
 * 이 메소드가 호출된 후 다른 메소드를 호출하는 것은 정의되지 않은 동작입니다.**
 *
 * 사용 예:
 *
 * ```
 * class MyClass(otel: OpenTelemetry) {
 *   val tracer: Tracer = otel.getTracer("com.example.rpc")
 *
 *   fun doWork(parent: Span) {
 *     val childSpan = tracer.startSpan("MyChildSpan") {
 *          setParent(Context.current().with(parent))
 *     }
 *     childSpan.addEvent("my event");
 *     try {
 *       doSomeWork(childSpan); // 스택 아래로 새 스팬을 수동으로 전파합니다.
 *     } finally {
 *       // 예외가 발생하더라도 스팬을 확실히 종료합니다.
 *       childSpan.end();  // 수동으로 스팬을 종료합니다.
 *     }
 *   }
 * }
 * ```
 *
 * @return 새로 생성된`Span` 인스턴스
 */
inline fun Tracer.startSpan(
    spanName: String,
    @BuilderInference builder: SpanBuilder.() -> Unit,
): Span {
    return spanBuilder(spanName).apply(builder).startSpan()
}
