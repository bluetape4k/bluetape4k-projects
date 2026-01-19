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

inline fun sdkTracerProvider(
    @BuilderInference builder: SdkTracerProviderBuilder.() -> Unit,
): SdkTracerProvider {
    return SdkTracerProvider.builder().apply(builder).build()
}


/**
 * Starts a new [Span].
 *
 * Users **must** manually call [Span.end()] to end this `Span`.
 *
 * Does not install the newly created `Span` to the current Context.
 *
 * **IMPORTANT: This method can be called only once per [SpanBuilder] instance and as the
 * last method called. After this method is called calling any method is undefined behavior.**
 *
 * Example of usage:
 *
 * ```
 * class MyClass(otel: OpenTelemetry) {
 *   val tracer: Tracer = otel.getTracer("com.example.rpc")
 *
 *   fun doWork(parent: Span) {
 *     Span childSpan = tracer.startSpan("MyChildSpan") {
 *          setParent(Context.current().with(parent))
 *     }
 *     childSpan.addEvent("my event");
 *     try {
 *       doSomeWork(childSpan); // Manually propagate the new span down the stack.
 *     } finally {
 *       // To make sure we end the span even in case of an exception.
 *       childSpan.end();  // Manually end the span.
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
