package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.CreateTopicRequest

/**
 * DSL 블록으로 [CreateTopicRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `name`, `attributes` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = createTopicRequest { name("my-topic") }
 * ```
 */
inline fun createTopicRequest(
    builder: CreateTopicRequest.Builder.() -> Unit,
): CreateTopicRequest =
    CreateTopicRequest.builder().apply(builder).build()

/**
 * 토픽 이름으로 [CreateTopicRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [name]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = createTopicRequestOf("my-topic")
 * // req.name() == "my-topic"
 * ```
 */
inline fun createTopicRequestOf(
    name: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: CreateTopicRequest.Builder.() -> Unit = {},
): CreateTopicRequest {
    name.requireNotBlank("name")

    return createTopicRequest {
        name(name)
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
