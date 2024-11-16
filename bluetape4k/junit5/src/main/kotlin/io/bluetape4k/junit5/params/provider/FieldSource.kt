package io.bluetape4k.junit5.params.provider

import org.junit.jupiter.params.provider.ArgumentsSource

/**
 * 테스트 메소드에 인자를 제공할 때, 필드 변수로부터 얻을 수 있도록 합니다.
 *
 * ```
 * val arguments: List<Arguments> = listOf(
 *         argumentOf(null, true),
 *         argumentOf("", true),
 *         argumentOf("  ", true),
 *         argumentOf("not blank", false)
 *     )
 *
 * @ParameterizedTest
 * @FieldSource("arguments")
 * fun `isBlank should return true for null or blank string variable`(input:String?, expected:Boolean) {
 *     Strings.isBlank(input) shouldBeEqualTo expected
 * }
 * ```
 * @see [org.junit.jupiter.params.provider.MethodSource]
 * @see [argumentOf]
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@ArgumentsSource(FieldArgumentsProvider::class)
annotation class FieldSource(
    val value: String,
)
