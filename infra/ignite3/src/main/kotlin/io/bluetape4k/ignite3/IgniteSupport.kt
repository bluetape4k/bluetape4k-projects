package io.bluetape4k.ignite3

import org.apache.ignite.client.IgniteClient

/**
 * Apache Ignite 3.x 씬 클라이언트를 생성합니다.
 *
 * Ignite 3.x는 [IgniteClient.builder()]를 통해 클라이언트를 생성합니다.
 *
 * ```kotlin
 * val client = igniteClient("localhost:10800") {
 *     retryPolicy(RetryReadPolicy())
 * }
 * ```
 *
 * @param addresses 연결할 Ignite 3.x 노드 주소 목록 (예: "localhost:10800")
 * @param setup [IgniteClient.Builder] 추가 설정 블록
 * @return [IgniteClient] 인스턴스
 */
fun igniteClient(
    vararg addresses: String = arrayOf("localhost:10800"),
    setup: IgniteClient.Builder.() -> Unit = {},
): IgniteClient {
    return IgniteClient.builder()
        .addresses(*addresses)
        .apply(setup)
        .build()
}

/**
 * [IgniteClient.Builder]를 사용하여 Ignite 3.x 클라이언트를 생성합니다.
 *
 * @param builder Ignite 3.x 클라이언트 빌더
 * @return [IgniteClient] 인스턴스
 */
fun igniteClient(builder: IgniteClient.Builder): IgniteClient = builder.build()
