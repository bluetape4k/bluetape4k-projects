package io.bluetape4k.rule.core

import io.bluetape4k.rule.api.Facts

/**
 * 규칙 실행 로그에 사용할 payload 없는 Facts 요약입니다.
 *
 * [Facts.toString]은 진단과 호환성을 위해 값을 계속 보여주므로, 로그 경계에서는
 * 이 요약만 사용해야 합니다.
 */
internal fun Facts.toLogContext(): String = "factCount=$size"
