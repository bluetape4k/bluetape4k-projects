package io.bluetape4k.rule.exception

/**
 * 원하는 Fact 요소가 없을 때 발생하는 예외입니다.
 *
 * @property missingFact 찾을 수 없는 Fact 이름
 */
class NoSuchFactException(
    message: String,
    val missingFact: String,
): RuleException(message)
