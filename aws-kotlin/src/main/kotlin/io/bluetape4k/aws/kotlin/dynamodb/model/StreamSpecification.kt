package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.StreamSpecification
import aws.sdk.kotlin.services.dynamodb.model.StreamViewType

/**
 * DSL 블록으로 DynamoDB [StreamSpecification]을 빌드합니다.
 *
 * ## 동작/계약
 * - [streamEnabled]가 true이면 DynamoDB Streams가 활성화된다.
 * - [streamViewType]은 스트림에 기록할 데이터 종류를 지정하며, [streamEnabled]가 true일 때만 유효하다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val spec = streamSpecificationOf(streamEnabled = true, streamViewType = StreamViewType.NewAndOldImages)
 * // spec.streamEnabled == true
 * // spec.streamViewType == StreamViewType.NewAndOldImages
 * ```
 *
 * @param streamEnabled 스트림 활성화 여부
 * @param streamViewType 스트림에 기록할 뷰 타입
 */
inline fun streamSpecificationOf(
    streamEnabled: Boolean? = null,
    streamViewType: StreamViewType? = null,
    crossinline builder: StreamSpecification.Builder.() -> Unit = {},
): StreamSpecification =
    StreamSpecification {
        this.streamEnabled = streamEnabled
        this.streamViewType = streamViewType

        builder()
    }
