package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.CreateTopicRequest
import aws.sdk.kotlin.services.sns.model.DeleteTopicRequest
import aws.sdk.kotlin.services.sns.model.Tag
import io.bluetape4k.support.requireNotBlank

/**
 * [name]의 Topic 을 생성합니다.
 *
 * ```
 * val request = createTopicRequestOf("MyTopic")
 * client.createTopic(request)
 * ```
 *
 * @param name 생성할 Topic 의 이름
 * @param tags Topic 에 추가할 Tag 목록
 * @param attributes Topic 에 추가할 속성 목록
 * @param builder [CreateTopicRequest.Builder] 를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [CreateTopicRequest] 인스턴스
 */
inline fun createTopicRequestOf(
    name: String,
    tags: List<Tag>? = null,
    attributes: Map<String, String>? = null,
    crossinline builder: CreateTopicRequest.Builder.() -> Unit = {},
): CreateTopicRequest {
    name.requireNotBlank("name")

    return CreateTopicRequest {
        this.name = name
        tags?.let { this.tags = it }
        attributes?.let { this.attributes = it }

        builder()
    }
}

/**
 * [topicArn]의 Topic 을 삭제합니다.
 *
 * ```
 * val request = deleteTopicRequestOf("arn:aws:sns:us-east-1:123456789012:MyTopic")
 * client.deleteTopic(request)
 * ```
 *
 * @param topicArn 삭제할 Topic 의 ARN
 * @param builder [DeleteTopicRequest.Builder] 를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [DeleteTopicRequest] 인스턴스
 */
inline fun deleteTopicRequestOf(
    topicArn: String,
    crossinline builder: DeleteTopicRequest.Builder.() -> Unit = {},
): DeleteTopicRequest {
    topicArn.requireNotBlank("topicArn")

    return DeleteTopicRequest {
        this.topicArn = topicArn

        builder()
    }
}
