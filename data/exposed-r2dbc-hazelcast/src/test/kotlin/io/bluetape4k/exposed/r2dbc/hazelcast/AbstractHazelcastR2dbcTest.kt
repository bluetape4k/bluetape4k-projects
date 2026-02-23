package io.bluetape4k.exposed.r2dbc.hazelcast

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.hazelcast.hazelcastClient
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.HazelcastServer
import io.bluetape4k.utils.ShutdownQueue

/**
 * Hazelcast 클라이언트 + Exposed R2DBC 기반 테스트의 기본 클래스입니다.
 *
 * [HazelcastServer.Launcher]를 통해 Docker 컨테이너로 Hazelcast 서버를 자동 실행합니다.
 * 별도로 서버를 설치하거나 실행할 필요 없이 테스트를 수행할 수 있습니다.
 *
 * **사전 요구사항**: Docker가 실행 중이어야 합니다.
 *
 * **참고**: [hazelcast] 클라이언트는 Near Cache 설정 없이 생성됩니다.
 * Near Cache를 활성화하려면 서브클래스에서 `hazelcastClient(url) { addNearCacheConfig(...) }`를
 * 사용하여 별도의 클라이언트를 생성하세요.
 * 이 클래스의 테스트는 IMap 기반 Read-Through(DB → 캐시) 동작 검증에 초점을 맞춥니다.
 */
abstract class AbstractHazelcastR2dbcTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel() {

        /**
         * 테스트에서 공유하는 Hazelcast 클라이언트 인스턴스입니다.
         * Docker 컨테이너로 실행 중인 Hazelcast 서버에 연결합니다.
         */
        @JvmStatic
        val hazelcast: HazelcastInstance by lazy {
            HazelcastServer.Launcher.hazelcast.let { server ->
                hazelcastClient(server.url).also {
                    ShutdownQueue.register { it.shutdown() }
                }
            }
        }
    }
}
