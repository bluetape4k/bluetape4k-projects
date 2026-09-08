package io.bluetape4k.rule.core

/**
 * 규칙 정의 source를 로그에 남길 때 payload를 제외한 요약을 반환합니다.
 *
 * 사용자가 제공한 조건식과 실행 스크립트는 민감한 값을 포함할 수 있으므로
 * 로그에는 원문 대신 길이만 남겨야 합니다.
 */
internal fun String.toRuleSourceLogContext(): String = "sourceLength=$length"
