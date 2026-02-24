package io.bluetape4k.ignite3

import io.bluetape4k.ignite3.igniteClient as createIgniteClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.Ignite3Server
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.client.IgniteClient

/**
 * Apache Ignite 3.x 씬 클라이언트 기반 테스트를 위한 추상 기반 클래스입니다.
 *
 * Docker로 Ignite 3.x 서버를 실행하고, 씬 클라이언트([igniteClient])를 공유합니다.
 * 서버 컨테이너는 첫 접근 시 한 번만 시작되며, 테스트 간에 재사용됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyIgnite3Test: AbstractIgnite3Test() {
 *     @Test
 *     fun `something`() {
 *         igniteClient.sql().execute(null, "SELECT 1").close()
 *     }
 * }
 * ```
 */
abstract class AbstractIgnite3Test {

    companion object: KLogging() {
        /** 테스트에서 공유하는 싱글턴 Ignite 3.x 서버 컨테이너 */
        @JvmStatic
        val ignite3Server: Ignite3Server by lazy {
            Ignite3Server.Launcher.ignite3
        }

        /** Ignite 3.x 씬 클라이언트 (서버 컨테이너 시작 후 연결) */
        @JvmStatic
        val igniteClient: IgniteClient by lazy {
            ignite3Server  // 서버가 먼저 초기화되어야 함
            createIgniteClient(ignite3Server.url).also {
                ShutdownQueue.register { it.close() }
            }
        }
    }
}
