package io.bluetape4k.aws.ses.model

import software.amazon.awssdk.services.ses.model.Destination

/**
 * [Destination.Builder]를 이용하여 [Destination] 인스턴스를 생성합니다.
 *
 * ```
 * val destination = Destination {
 *    toAddresses("user1@example.com")
 *    ccAddresses("user2@example.com")
 *    bccAddresses("all@example.com")
 * }
 * ```
 *
 * @param initializer [Destination.Builder] 초기화 람다
 * @return [Destination] 인스턴스
 */
inline fun Destination(initializer: Destination.Builder.() -> Unit): Destination {
    return Destination.builder().apply(initializer).build()
}

/**
 * [Destination] 인스턴스를 생성합니다.
 *
 * ```
 * val destination = destinationOf(listOf("debop@example.com", "user1@example.com"))
 * ```
 *
 * @param toAddresses 수신자 주소 목록
 * @param ccAddresses 참조 주소 목록
 * @param bccAddresses 숨은 참조 주소 목록
 * @return [Destination] 인스턴스
 */
fun destinationOf(
    toAddresses: Collection<String>,
    ccAddresses: Collection<String>? = null,
    bccAddresses: Collection<String>? = null,
) = Destination {
    toAddresses(toAddresses)
    ccAddresses(ccAddresses)
    bccAddresses(bccAddresses)
}

/**
 * [Destination] 인스턴스를 생성합니다.
 *
 * ```
 * val destination = destinationOf("debop@example.com", "user1@example.com")
 * ```
 *
 * @param address 수신자 주소 목록
 * @return [Destination] 인스턴스
 */
fun destinationOf(vararg address: String): Destination = Destination {
    toAddresses(*address)
}
