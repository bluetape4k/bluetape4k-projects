package io.bluetape4k.aws.ses.model

import software.amazon.awssdk.services.ses.model.Destination

/**
 * [Destination.Builder]를 이용하여 [destination] 인스턴스를 생성합니다.
 *
 * ```
 * val destination = destination {
 *    toAddresses("user1@example.com")
 *    ccAddresses("user2@example.com")
 *    bccAddresses("all@example.com")
 * }
 * ```
 *
 * @param builder [Destination.Builder] 초기화 람다
 * @return [destination] 인스턴스
 */
inline fun destination(
    @BuilderInference builder: Destination.Builder.() -> Unit,
): Destination =
    Destination.builder().apply(builder).build()

/**
 * [Destination] 인스턴스를 생성합니다.
 *
 * ```
 * val destination = destinationOf(listOf("debop@example.com", "user1@example.com"))
 * ```
 *
 * @param toAddrs 수신자 주소 목록
 * @param ccAddrs 참조 주소 목록
 * @param bccAddrs 숨은 참조 주소 목록
 * @return [Destination] 인스턴스
 */
fun destinationOf(
    toAddrs: Collection<String>,
    ccAddrs: Collection<String>? = null,
    bccAddrs: Collection<String>? = null,
) = destination {
    toAddresses(toAddrs)
    ccAddrs?.let { ccAddresses(it) }
    bccAddrs?.let { bccAddresses(it) }
}

/**
 * [Destination] 인스턴스를 생성합니다.
 *
 * ```
 * val destination = destinationOf("debop@example.com", "user1@example.com")
 * ```
 *
 * @param toAddrs 수신자 주소 목록
 * @return [Destination] 인스턴스
 */
fun destinationOf(vararg toAddrs: String): Destination = destination {
    toAddresses(*toAddrs)
}
