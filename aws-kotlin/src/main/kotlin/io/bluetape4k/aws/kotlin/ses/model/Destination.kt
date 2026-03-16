package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination
import io.bluetape4k.support.requireNotEmpty

/**
 * 수신자 주소 목록으로 [Destination]을 생성합니다.
 *
 * ```kotlin
 * val dest = destinationOf("user1@example.com", "user2@example.com")
 * ```
 *
 * @param toAddresses 수신자(TO) 이메일 주소 목록 (최소 1개 이상 필요)
 * @return [Destination] 인스턴스
 */
inline fun destinationOf(
    vararg toAddresses: String,
    crossinline builder: Destination.Builder.() -> Unit = {},
): Destination {
    toAddresses.requireNotEmpty("toAddresses")

    return Destination {
        this.toAddresses = toAddresses.toList()

        builder()
    }
}

/**
 * TO/CC/BCC 수신자 주소 목록으로 [Destination]을 생성합니다.
 *
 * ```kotlin
 * val dest = destinationOf(
 *     toAddresses = listOf("user1@example.com"),
 *     ccAddresses = listOf("cc@example.com"),
 * )
 * ```
 *
 * @param toAddresses 수신자(TO) 이메일 주소 목록
 * @param ccAddresses 참조(CC) 이메일 주소 목록
 * @param bccAddresses 숨은 참조(BCC) 이메일 주소 목록
 * @return [Destination] 인스턴스
 */
inline fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
    crossinline builder: Destination.Builder.() -> Unit = {},
): Destination {
    val hasAddress = !toAddresses.isNullOrEmpty() || !ccAddresses.isNullOrEmpty() || !bccAddresses.isNullOrEmpty()
    require(hasAddress) { "At least one address must be provided." }

    return Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }

        builder()
    }
}
