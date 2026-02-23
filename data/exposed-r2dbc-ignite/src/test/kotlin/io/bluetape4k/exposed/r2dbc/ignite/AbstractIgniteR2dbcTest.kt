package io.bluetape4k.exposed.r2dbc.ignite

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.ignite.igniteClient
import io.bluetape4k.ignite.igniteEmbedded
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.Ignite
import org.apache.ignite.client.IgniteClient

/**
 * Apache Ignite 2.x 씬 클라이언트 + Exposed R2DBC 기반 테스트의 기본 클래스입니다.
 *
 * 임베디드 Ignite 2.x 서버를 로컬에서 시작하고, 씬 클라이언트로 연결합니다.
 * 씬 클라이언트 커넥터는 Ignite 2.4+에서 기본으로 활성화되어 있습니다.
 *
 * **사전 요구사항**: Java 실행 시 `--add-opens` JVM 옵션이 필요합니다 (build.gradle.kts 참고).
 */
abstract class AbstractIgniteR2dbcTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel() {

        /** 테스트에서 사용하는 Ignite 2.x 캐시 이름 */
        const val TEST_CACHE_NAME = "TEST_USERS"

        /**
         * 테스트용 임베디드 Ignite 2.x 서버 노드입니다.
         * 씬 클라이언트가 10800 포트로 연결할 수 있습니다.
         */
        @JvmStatic
        val server: Ignite by lazy {
            igniteEmbedded {
                igniteInstanceName = "bt4k-r2dbc-test-embedded-server"
            }.also {
                ShutdownQueue.register { it.close() }
            }
        }

        /**
         * 임베디드 서버에 연결하는 Ignite 2.x 씬 클라이언트입니다.
         */
        @JvmStatic
        val igniteClient: IgniteClient by lazy {
            // server가 먼저 초기화되어야 합니다
            server
            igniteClient("localhost:10800").also {
                ShutdownQueue.register { it.close() }
            }
        }
    }
}
