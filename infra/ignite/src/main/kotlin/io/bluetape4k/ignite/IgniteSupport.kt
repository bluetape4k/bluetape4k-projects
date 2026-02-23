package io.bluetape4k.ignite

import org.apache.ignite.Ignite
import org.apache.ignite.Ignition
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration
import org.apache.ignite.configuration.IgniteConfiguration

/**
 * Apache Ignite 2.x 씬 클라이언트를 생성합니다.
 *
 * ```kotlin
 * val client = igniteClient("localhost:10800")
 * ```
 *
 * @param addresses 연결할 Ignite 노드 주소 목록 (예: "localhost:10800")
 * @param setup [ClientConfiguration] 추가 설정 블록
 * @return [IgniteClient] 씬 클라이언트 인스턴스
 */
fun igniteClient(
    vararg addresses: String = arrayOf("localhost:10800"),
    setup: ClientConfiguration.() -> Unit = {},
): IgniteClient {
    val cfg = ClientConfiguration().apply {
        setAddresses(*addresses)
        setup()
    }
    return Ignition.startClient(cfg)
}

/**
 * [ClientConfiguration]을 사용하여 Ignite 2.x 씬 클라이언트를 생성합니다.
 *
 * @param configuration Ignite 2.x 클라이언트 설정
 * @return [IgniteClient] 씬 클라이언트 인스턴스
 */
fun igniteClient(configuration: ClientConfiguration): IgniteClient =
    Ignition.startClient(configuration)

/**
 * Apache Ignite 2.x 임베디드 노드를 시작합니다.
 *
 * **주의**: 임베디드 모드에서만 [org.apache.ignite.configuration.NearCacheConfiguration]을 통해
 * 진정한 Near Cache를 사용할 수 있습니다.
 *
 * ```kotlin
 * val ignite = igniteEmbedded {
 *     igniteInstanceName = "my-node"
 * }
 * ```
 *
 * @param setup [IgniteConfiguration] 추가 설정 블록
 * @return [Ignite] 임베디드 노드 인스턴스
 */
fun igniteEmbedded(setup: IgniteConfiguration.() -> Unit = {}): Ignite {
    val cfg = IgniteConfiguration().apply(setup)
    return Ignition.start(cfg)
}
