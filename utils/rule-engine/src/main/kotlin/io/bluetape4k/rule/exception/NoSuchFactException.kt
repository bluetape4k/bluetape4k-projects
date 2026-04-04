package io.bluetape4k.rule.exception

/**
 * 원하는 Fact 요소가 없을 때 발생하는 예외입니다.
 *
 * ```kotlin
 * // @Fact("age") 파라미터가 있는데 facts에 "age"가 없으면 발생합니다.
 * try {
 *     rule.evaluate(Facts.empty())
 * } catch (e: NoSuchFactException) {
 *     println("누락된 Fact: ${e.missingFact}")
 * }
 * ```
 *
 * @property missingFact 찾을 수 없는 Fact 이름
 */
class NoSuchFactException(
    message: String,
    val missingFact: String,
): RuleException(message)
