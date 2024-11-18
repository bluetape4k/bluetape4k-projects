package io.bluetape4k.junit5.params.provider

import org.junit.jupiter.params.provider.Arguments

/**
 * 테스트 메소드에 인자를 제공합니다.
 *
 * ```
 * val arguments: List<Arguments> = listOf(
 *        argumentOf(null, true),
 *        argumentOf("", true),
 *        argumentOf("  ", true),
 *        argumentOf("not blank", false)
 * )
 *
 * @ParameterizedTest
 * @FieldSource("arguments")
 * fun `isBlank should return true for null or blank string variable`(input:String?, expected:Boolean) {
 *    input.isBlank() shouldBeEqualTo expected
 * }
 * ```
 *
 */
fun argumentOf(vararg arguments: Any?): Arguments = Arguments.of(*arguments)
