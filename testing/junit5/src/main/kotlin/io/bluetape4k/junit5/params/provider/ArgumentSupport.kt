package io.bluetape4k.junit5.params.provider

import org.junit.jupiter.params.provider.Arguments

/**
 * 가변 인자를 JUnit [Arguments]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 인자 배열을 그대로 [Arguments.of]에 전달합니다.
 * - 입력 값 자체를 검증하거나 변환하지 않습니다.
 * - 새 [Arguments] 인스턴스를 매 호출마다 생성합니다.
 *
 * ```kotlin
 * val args = argumentOf("A", 1, true)
 * // args == Arguments.of("A", 1, true)
 * ```
 */
fun argumentOf(vararg arguments: Any?): Arguments = Arguments.of(*arguments)
