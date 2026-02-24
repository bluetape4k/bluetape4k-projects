package io.bluetape4k.hazelcast

import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.hazelcast.hazelcastClient as createHazelcastClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.HazelcastServer

/**
 * Hazelcast 클라이언트 기반 테스트를 위한 추상 기반 클래스입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고, 기본 클라이언트([hazelcastClient])를 공유합니다.
 * Near Cache가 필요한 테스트는 별도 클라이언트를 생성해서 사용합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyHazelcastTest: AbstractHazelcastTest() {
 *     @Test
 *     fun `something`() {
 *         val map = hazelcastClient.getMap<String, String>("my-map")
 *         map.put("key", "value")
 *     }
 * }
 * ```
 */
abstract class AbstractHazelcastTest {

    companion object: KLogging() {
        /** 테스트에서 공유하는 싱글턴 Hazelcast 서버 컨테이너 */
        @JvmStatic
        val hazelcastServer: HazelcastServer by lazy {
            HazelcastServer.Launcher.hazelcast
        }

        /** Near Cache 설정 없는 기본 공유 클라이언트 */
        @JvmStatic
        val hazelcastClient: HazelcastInstance by lazy {
            val clientConfig = ClientConfig().apply {
                networkConfig.addAddress(hazelcastServer.url)
            }
            createHazelcastClient(clientConfig)
        }
    }
}
