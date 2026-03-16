package io.bluetape4k.nats.client.api

import io.nats.client.api.KeyValuePurgeOptions

/**
 * DSL 블록으로 [KeyValuePurgeOptions]를 생성합니다.
 *
 * @param builder [KeyValuePurgeOptions.Builder]에 적용할 설정 블록
 * @return [KeyValuePurgeOptions] 인스턴스
 */
inline fun keyValuePurgeOptions(
    builder: KeyValuePurgeOptions.Builder.() -> Unit,
): KeyValuePurgeOptions = KeyValuePurgeOptions.builder().apply(builder).build()
