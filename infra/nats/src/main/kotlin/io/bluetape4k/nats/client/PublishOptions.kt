package io.bluetape4k.nats.client

import io.nats.client.PublishOptions
import java.util.*

/**
 * DSL 블록으로 [PublishOptions]를 생성합니다.
 *
 * @param builder [PublishOptions.Builder]에 적용할 설정 블록
 * @return [PublishOptions] 인스턴스
 */
inline fun publishOptions(
    @BuilderInference builder: PublishOptions.Builder.() -> Unit,
): PublishOptions = PublishOptions.builder().apply(builder).build()

/**
 * [Properties] 기반으로 [PublishOptions]를 생성합니다.
 *
 * @param properties 초기 설정 속성
 * @param builder 추가 설정 블록
 * @return [PublishOptions] 인스턴스
 */
inline fun publishOptionsOf(
    properties: Properties,
    @BuilderInference builder: PublishOptions.Builder.() -> Unit = {},
): PublishOptions = PublishOptions.Builder(properties).apply(builder).build()
