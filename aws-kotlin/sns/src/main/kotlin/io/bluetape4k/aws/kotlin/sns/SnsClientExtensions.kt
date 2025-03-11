package io.bluetape4k.aws.kotlin.sns

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.checkIfPhoneNumberIsOptedOut
import aws.sdk.kotlin.services.sns.createPlatformEndpoint
import aws.sdk.kotlin.services.sns.createTopic
import aws.sdk.kotlin.services.sns.deleteTopic
import aws.sdk.kotlin.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest
import aws.sdk.kotlin.services.sns.model.CheckIfPhoneNumberIsOptedOutResponse
import aws.sdk.kotlin.services.sns.model.CreatePlatformEndpointRequest
import aws.sdk.kotlin.services.sns.model.CreatePlatformEndpointResponse
import aws.sdk.kotlin.services.sns.model.CreateTopicRequest
import aws.sdk.kotlin.services.sns.model.CreateTopicResponse
import aws.sdk.kotlin.services.sns.model.DeleteTopicRequest
import aws.sdk.kotlin.services.sns.model.DeleteTopicResponse
import aws.sdk.kotlin.services.sns.model.PublishBatchRequest
import aws.sdk.kotlin.services.sns.model.PublishBatchRequestEntry
import aws.sdk.kotlin.services.sns.model.PublishBatchResponse
import aws.sdk.kotlin.services.sns.model.PublishRequest
import aws.sdk.kotlin.services.sns.model.PublishResponse
import aws.sdk.kotlin.services.sns.model.SubscribeRequest
import aws.sdk.kotlin.services.sns.model.SubscribeResponse
import aws.sdk.kotlin.services.sns.model.UnsubscribeRequest
import aws.sdk.kotlin.services.sns.model.UnsubscribeResponse
import aws.sdk.kotlin.services.sns.publish
import aws.sdk.kotlin.services.sns.publishBatch
import aws.sdk.kotlin.services.sns.subscribe
import aws.sdk.kotlin.services.sns.unsubscribe
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.apache.endsWithIgnoreCase
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue

/**
 * [SnsClient] 인스턴스를 생성합니다.
 *
 * ```
 * val snsClient = snsClientOf(
 *  endpoint = "http://localhost:4566",
 *  region = "us-east-1",
 *  credentialsProvider = credentialsProvider
 * )
 * ````
 *
 * @param endpoint SNS endpoint URL
 * @param region AWS region
 * @param credentialsProvider AWS credentials provider
 * @param httpClientEngine [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param configurer SNS client 설정 빌더
 * @return [SnsClient] 인스턴스
 */
inline fun snsClientOf(
    endpoint: String? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClientEngine: HttpClientEngine = defaultCrtHttpEngineOf(),
    crossinline configurer: SnsClient.Config.Builder.() -> Unit = {},
): SnsClient = SnsClient {
    endpoint?.let { this.endpointUrl = Url.parse(it) }
    region?.let { this.region = it }
    credentialsProvider?.let { this.credentialsProvider = it }
    httpClient = httpClientEngine

    configurer()
}.apply {
    ShutdownQueue.register(this)
}

/**
 * 플랫폼 엔드포인트를 생성합니다.
 *
 * ```
 * val response = snsClient.createPlatformEndpoint(token, platformApplicationArn) {
 *   customUserData = "customUserData"
 * }
 * ```
 *
 * @param token 토큰
 * @param platformApplicationArn 플랫폼 애플리케이션 ARN
 * @param configurer 플랫폼 엔드포인트 생성 설정 빌더
 * @return [CreatePlatformEndpointResponse] 인스턴스
 */
suspend inline fun SnsClient.createPlatformEndpoint(
    token: String,
    platformApplicationArn: String,
    crossinline configurer: CreatePlatformEndpointRequest.Builder.() -> Unit = {},
): CreatePlatformEndpointResponse {
    token.requireNotBlank("token")
    platformApplicationArn.requireNotBlank("platformApplicationArn")

    return createPlatformEndpoint {
        this.token = token
        this.platformApplicationArn = platformApplicationArn

        configurer()
    }
}

/**
 * SNS용 Topic을 생성합니다.
 *
 * ```
 * val response = snsClient.createTopic("topicName", mapOf("key" to "value")) {
 *  displayName = "displayName"
 * }
 * ```
 *
 * @param topicName topic 이름
 * @param attributes topic 속성
 * @param configurer [CreateTopicRequest]를 빌드하는 람다 함수
 */
suspend inline fun SnsClient.createTopic(
    topicName: String,
    attributes: Map<String, String>? = null,
    crossinline configurer: CreateTopicRequest.Builder.() -> Unit = {},
): CreateTopicResponse {
    topicName.requireNotBlank("topicName")

    return createTopic {
        this.name = topicName
        this.attributes = attributes

        configurer()
    }
}

/**
 * FIFO topic을 생성합니다. FIFO topic은 .fifo로 끝나야 합니다.
 *
 * ```
 * val response = snsClient.createFifoTopic("topicName.fifo", mapOf("key" to "value")) {
 *     displayName = "displayName"
 * }
 * ```
 *
 * @param topicName topic 이름
 * @param attributes topic 속성
 * @param configurer [CreateTopicRequest]를 빌드하는 람다 함수
 */
suspend inline fun SnsClient.createFifoTopic(
    topicName: String,
    attributes: MutableMap<String, String> = mutableMapOf(),
    crossinline configurer: CreateTopicRequest.Builder.() -> Unit = {},
): CreateTopicResponse {
    topicName.requireNotBlank("topicName")
    require(topicName.endsWithIgnoreCase(".fifo")) { "FIFO topic name must end with .fifo" }

    attributes["FifoTopic"] = "true"
    attributes["ContentBasedDeduplication"] = "true"

    return createTopic {
        this.name = topicName
        this.attributes = attributes

        configurer()
    }
}

/**
 * Topic에 구독합니다.
 *
 * ```
 * val response = snsClient.subscribe(topicArn, endpoint, "sms") {
 *  returnSubscriptionArn = true
 * }
 * ```
 *
 * @param topicArn topic ARN
 * @param endpoint endpoint
 * @param protocol 프로토콜
 * @param returnSubscriptionArn 구독 ARN 반환 여부
 * @param configurer [SubscribeRequest]를 빌드하는 람다 함수
 */
suspend inline fun SnsClient.subscribe(
    topicArn: String,
    endpoint: String,
    protocol: String = "sms",
    returnSubscriptionArn: Boolean = true,
    crossinline configurer: SubscribeRequest.Builder.() -> Unit = {},
): SubscribeResponse {
    return subscribe {
        this.topicArn = topicArn
        this.endpoint = endpoint
        this.protocol = protocol
        this.returnSubscriptionArn = returnSubscriptionArn

        configurer()
    }
}

/**
 * [phoneNumber]가 opt out 되었는지 확인합니다.
 *
 * ```
 * val response = snsClient.checkIfPhoneNumberIsOptedOut(phoneNumber)
 * ```
 *
 * @param phoneNumber 전화번호
 * @param configurer [CheckIfPhoneNumberIsOptedOutRequest]를 빌드하는 람다 함수
 * @return [CheckIfPhoneNumberIsOptedOutResponse] 인스턴스
 */
suspend inline fun SnsClient.checkIfPhoneNumberIsOptedOut(
    phoneNumber: String,
    crossinline configurer: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit = {},
): CheckIfPhoneNumberIsOptedOutResponse {
    phoneNumber.requireNotBlank("phoneNumber")

    return checkIfPhoneNumberIsOptedOut {
        this.phoneNumber = phoneNumber
        configurer()
    }
}

/**
 * Topic에 메시지를 발행합니다.
 *
 * ```
 * val response = snsClient.publish(topicArn, message, "subject") {
 *      messageAttributes = mapOf("key" to "value")
 * }
 * ```
 *
 * @param topicArn topic ARN
 * @param message 메시지
 * @param subject 제목
 * @param configurer [PublishRequest]를 빌드하는 람다 함수
 *
 * @return [PublishResponse] 인스턴스
 */
suspend inline fun SnsClient.publish(
    topicArn: String,
    message: String,
    subject: String? = null,
    crossinline configurer: PublishRequest.Builder.() -> Unit = {},
): PublishResponse {
    topicArn.requireNotBlank("topicArn")
    message.requireNotBlank("message")

    return publish {
        this.topicArn = topicArn
        this.message = message
        subject?.let { this.subject = it }

        configurer()
    }
}

/**
 * Topic에 복수의 메시지를 배치로 발행합니다.
 *
 * ```
 * val messageSize = 10
 *
 * // 발행할 메시지 목록 생성
 * val entries = List(messageSize) {
 *     publishBatchRequestEntryOf(
 *         id = Base58.randomString(6).lowercase(),
 *         message = "Hello, AWS SNS! ${Base58.randomString(6).lowercase()}",
 *         messageDeduplicationId = hashOf(testTopicArn, "Hello, AWS SNS!", testPhoneNumber).toString(),
 *         messageGroupId = "partitionKey"
 *     )
 * }
 *
 * val response = snsClient.publishBatch(testTopicArn, entries) {
 *   messageGroupId = "partitionKey"
 *   messageDeduplicationId = hashOf(topicArn, message, phoneNumber).toString()
 * }
 * ```
 *
 * @param topicArn topic ARN
 * @param entries 발행할 메시지 목록
 * @param configurer [PublishBatchRequest]를 빌드하는 람다 함수
 *
 * @return [PublishBatchResponse] 인스턴스
 */
suspend inline fun SnsClient.publishBatch(
    topicArn: String,
    entries: List<PublishBatchRequestEntry>,
    crossinline configurer: PublishBatchRequest.Builder.() -> Unit = {},
): PublishBatchResponse {
    topicArn.requireNotBlank("topicArn")

    return publishBatch {
        this.topicArn = topicArn
        this.publishBatchRequestEntries = entries
        configurer()
    }
}


/**
 * Topic 구독을 해지합니다.
 *
 * ```
 * val response = snsClient.unsubscribe(subscriptionArn)
 * ```
 *
 * @param subscriptionArn 구독 ARN
 * @param configurer [UnsubscribeRequest]를 빌드하는 람다 함수
 * @return [UnsubscribeResponse] 인스턴스
 */
suspend inline fun SnsClient.unsubscribe(
    subscriptionArn: String,
    crossinline configurer: UnsubscribeRequest.Builder.() -> Unit = {},
): UnsubscribeResponse {
    subscriptionArn.requireNotBlank("subscriptionArn")

    return unsubscribe {
        this.subscriptionArn = subscriptionArn
        configurer()
    }
}

/**
 * Topic을 삭제합니다.
 *
 * ```
 * val response = snsClient.deleteTopic(topicArn)
 * ```
 *
 * @param topicArn topic ARN
 * @param configurer [DeleteTopicRequest]를 빌드하는 람다 함수
 * @return [DeleteTopicResponse] 인스턴스
 */
suspend inline fun SnsClient.deleteTopic(
    topicArn: String,
    crossinline configurer: DeleteTopicRequest.Builder.() -> Unit = {},
): DeleteTopicResponse {
    topicArn.requireNotBlank("topicArn")

    return deleteTopic {
        this.topicArn = topicArn
        configurer()
    }
}
