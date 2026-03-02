package io.bluetape4k.junit5.params.provider

import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * 파라미터화 테스트 인자를 메서드가 아닌 필드에서 읽도록 지정합니다.
 *
 * ## 동작/계약
 * - [FieldArgumentsProvider]가 `value` 이름의 필드를 찾아 인자 스트림으로 변환합니다.
 * - 대상 필드는 `Stream/Iterable/Array` 타입이어야 하며 null이면 예외가 발생합니다.
 * - 함수 단위 어노테이션이며 MethodSource 대체 용도로 사용합니다.
 *
 * ```kotlin
 * @FieldSource("cases")
 * @org.junit.jupiter.params.ParameterizedTest
 * fun sample(v: String) { /* ... */ }
 * // cases == listOf(argumentOf("a"), argumentOf("b"))
 * ```
 *
 * @property value 테스트 인자를 담은 필드 이름
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@ArgumentsSource(FieldArgumentsProvider::class)
annotation class FieldSource(
    val value: String,
)
