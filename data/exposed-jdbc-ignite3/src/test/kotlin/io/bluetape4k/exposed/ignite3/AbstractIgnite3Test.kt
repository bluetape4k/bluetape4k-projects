package io.bluetape4k.exposed.ignite3

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.ignite3.igniteClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.storage.Ignite3Server
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.client.IgniteClient
import org.junit.jupiter.api.BeforeAll

/**
 * Apache Ignite 3.x 씬 클라이언트 기반 테스트의 기본 클래스입니다.
 *
 * [Ignite3Server.Launcher]를 통해 Docker 컨테이너로 Ignite 3.x 서버를 자동 실행합니다.
 * 별도로 서버를 설치하거나 실행할 필요 없이 테스트를 수행할 수 있습니다.
 *
 * **사전 요구사항**: Docker가 실행 중이어야 합니다.
 */
abstract class AbstractIgnite3Test: AbstractExposedTest() {

    companion object: KLogging() {

        /** 테스트에서 사용하는 Ignite 3.x 캐시 테이블 이름 */
        const val TEST_TABLE_NAME = "TEST_USERS"

        /**
         * Ignite 3.x Docker 컨테이너 (테스트 클래스 간 공유).
         */
        @JvmStatic
        val ignite3Server: Ignite3Server by lazy {
            Ignite3Server.Launcher.ignite3
        }

        /**
         * Ignite 3.x 씬 클라이언트입니다.
         * Docker 컨테이너로 실행 중인 Ignite 3.x 서버에 연결합니다.
         */
        @JvmStatic
        val igniteClient: IgniteClient by lazy {
            igniteClient(ignite3Server.url).also {
                ShutdownQueue.register { it.close() }
            }
        }

        /**
         * 테스트용 Ignite 3.x 캐시 테이블을 생성합니다.
         * 테이블이 이미 존재하면 무시합니다.
         */
        @JvmStatic
        fun createIgnite3Table() {
            log.info { "Ignite 3.x 캐시 테이블 생성: $TEST_TABLE_NAME" }
            igniteClient.sql().execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS $TEST_TABLE_NAME (
                    ID BIGINT PRIMARY KEY,
                    DATA VARBINARY(65535)
                )
                """.trimIndent()
            ).close()
            log.info { "Ignite 3.x 캐시 테이블 생성 완료: $TEST_TABLE_NAME" }
        }
    }

    @BeforeAll
    fun setupIgnite3Table() {
        createIgnite3Table()
    }
}
