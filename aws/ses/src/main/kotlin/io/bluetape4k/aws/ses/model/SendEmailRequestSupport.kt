package io.bluetape4k.aws.ses.model

import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.MessageTag
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest

/**
 * [SendEmailRequest.Builder]를 사용하여 [SendEmailRequest] 인스턴스를 생성합니다.
 *
 * ```
 * val request = SendEmailRequest {
 *    source("xxx")
 *    destination(destinationOf("yyy"))
 *    ...
 * ```
 *
 * @param initializer [SendEmailRequest.Builder] 초기화 람다
 * @return [SendEmailRequest] 인스턴스
 */
inline fun SendEmailRequest(initializer: SendEmailRequest.Builder.() -> Unit): SendEmailRequest {
    return SendEmailRequest.builder().apply(initializer).build()
}

/**
 * [SendEmailRequest] 인스턴스를 생성합니다.
 *
 * ```
 * val request = sendEmailRequestOf(
 *     source = "xxx",
 *     destination = destinationOf("yyy"),
 *     ...
 * )
 * ```
 *
 * @param source 발신자
 * @param destination 수신자
 * @param sourceArn 발신자 ARN
 * @param replyToAddresses 회신 주소
 * @param returnPath 반환 주소
 * @param returnPathArn 반환 주소 ARN
 * @param tags 메시지 태그
 * @return [SendEmailRequest] 인스턴스
 */
fun sendEmailRequestOf(
    source: String,
    destination: Destination,
    sourceArn: String? = null,
    replyToAddresses: Collection<String>? = null,
    returnPath: String? = null,
    returnPathArn: String? = null,
    tags: Collection<MessageTag>? = null,
): SendEmailRequest = SendEmailRequest {
    source(source)
    destination(destination)
    sourceArn?.run { sourceArn(this) }
    replyToAddresses?.run { replyToAddresses(this) }
    returnPath?.run { returnPath(this) }
    returnPathArn?.run { returnPathArn(this) }
    tags?.run { tags(this) }
}

/**
 * [SendTemplatedEmailRequest.Builder]를 사용하여 [SendTemplatedEmailRequest] 인스턴스를 생성합니다.
 *
 * ```
 * val request = SendTemplatedEmailRequest {
 *    source("xxx")
 *    destination(destinationOf("yyy"))
 *    template("template-1")
 *    ...
 * ```
 *
 * @param initializer [SendTemplatedEmailRequest.Builder] 초기화 람다
 * @return [SendTemplatedEmailRequest] 인스턴스
 */
inline fun SendTemplatedEmailRequest(
    initializer: SendTemplatedEmailRequest.Builder.() -> Unit,
): SendTemplatedEmailRequest {
    return SendTemplatedEmailRequest.builder().apply(initializer).build()
}

/**
 * [SendTemplatedEmailRequest] 인스턴스를 생성합니다.
 *
 * ```
 * val request = sendTemplatedEmailRequestOf(
 *     source = "xxx",
 *     destination = destinationOf("yyy"),
 *     template = "template-1",
 *     ...
 * )
 * ```
 *
 * @param source 발신자
 * @param destination 수신자
 * @param template 템플릿
 * @param templateArn 템플릿 ARN
 * @param templateData 템플릿 데이터
 * @param sourceArn 발신자 ARN
 * @param replyToAddresses 회신 주소
 * @param returnPath 반환 주소
 * @param returnPathArn 반환 주소 ARN
 * @param tags 메시지 태그
 * @param configurationSetName 설정 집합 이름
 *
 * @return [SendTemplatedEmailRequest] 인스턴스
 */
fun sendTemplatedEmailRequestOf(
    source: String,
    destination: Destination,
    template: String,
    templateArn: String? = null,
    templateData: String? = null,
    sourceArn: String? = null,
    replyToAddresses: Collection<String>? = null,
    returnPath: String? = null,
    returnPathArn: String? = null,
    tags: Collection<MessageTag>? = null,
    configurationSetName: String? = null,
): SendTemplatedEmailRequest = SendTemplatedEmailRequest {
    source(source)
    destination(destination)
    template(template)
    templateArn?.run { templateArn(this) }
    templateData?.run { templateData(this) }
    sourceArn?.run { sourceArn(this) }
    replyToAddresses?.run { replyToAddresses(this) }
    returnPath?.run { returnPath(this) }
    returnPathArn?.run { returnPathArn(this) }
    tags?.run { tags(this) }
    configurationSetName?.run { configurationSetName(this) }
}
