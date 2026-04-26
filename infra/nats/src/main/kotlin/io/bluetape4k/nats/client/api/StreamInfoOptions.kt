package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.StreamInfoOptions

/**
 * DSL 빌더로 [StreamInfoOptions]를 생성합니다.
 *
 * @param builder [StreamInfoOptions.Builder] 설정 블록
 */
inline fun streamInfoOptions(
    builder: StreamInfoOptions.Builder.() -> Unit,
): StreamInfoOptions =
    StreamInfoOptions.builder().apply(builder).build()

/**
 * subject 필터가 적용된 [StreamInfoOptions]를 반환합니다.
 *
 * @param subjectsFilter 필터링할 subject 패턴
 */
fun streamInfoOptionsOfFilterSubject(subjectsFilter: String): StreamInfoOptions {
    subjectsFilter.requireNotBlank("subjectsFilter")
    return StreamInfoOptions.filterSubjects(subjectsFilter)
}

/**
 * 모든 subject 정보를 포함하는 [StreamInfoOptions]를 반환합니다.
 */
fun streamInfoOptionsOfAllSubjects(): StreamInfoOptions =
    StreamInfoOptions.allSubjects()
