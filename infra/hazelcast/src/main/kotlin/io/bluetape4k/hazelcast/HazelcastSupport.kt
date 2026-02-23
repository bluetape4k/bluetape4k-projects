package io.bluetape4k.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance

/**
 * Hazelcast 클라이언트 인스턴스를 생성합니다.
 *
 * Near Cache는 클라이언트 모드에서만 지원됩니다 (Hazelcast 5.x 오픈소스).
 *
 * ```kotlin
 * val client = hazelcastClient("localhost:5701") {
 *     clusterName = "my-cluster"
 * }
 * ```
 *
 * @param addresses 연결할 Hazelcast 서버 주소 목록 (예: "localhost:5701")
 * @param setup [ClientConfig] 추가 설정 블록
 * @return [HazelcastInstance] 클라이언트 인스턴스
 */
fun hazelcastClient(
    vararg addresses: String = arrayOf("localhost:5701"),
    setup: ClientConfig.() -> Unit = {},
): HazelcastInstance {
    val config = ClientConfig().apply {
        networkConfig.addAddress(*addresses)
        setup()
    }
    return HazelcastClient.newHazelcastClient(config)
}

/**
 * [ClientConfig]를 사용하여 Hazelcast 클라이언트 인스턴스를 생성합니다.
 *
 * @param config Hazelcast 클라이언트 설정
 * @return [HazelcastInstance] 클라이언트 인스턴스
 */
fun hazelcastClient(config: ClientConfig): HazelcastInstance =
    HazelcastClient.newHazelcastClient(config)
