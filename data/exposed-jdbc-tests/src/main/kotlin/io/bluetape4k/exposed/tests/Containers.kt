package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.MariaDBServer
import io.bluetape4k.testcontainers.database.MySQL5Server
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue

/**
 * Exposed JDBC 테스트에서 사용하는 Testcontainers 컨테이너 인스턴스를 관리합니다.
 *
 * 각 속성은 lazy 초기화로 최초 접근 시 컨테이너를 시작하고, JVM 종료 시 자동으로 정리됩니다.
 * Docker 환경이 필요하며, [TestDBConfig.useTestcontainers]가 `true`일 때 사용됩니다.
 */
object Containers: KLogging() {

    /**
     * MariaDB 컨테이너 인스턴스입니다.
     *
     * utf8mb4 문자셋과 utf8mb4_bin 콜레이션으로 구성됩니다.
     */
    val MariaDB: MariaDBServer by lazy {
        MariaDBServer()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            } as MariaDBServer
    }

    /**
     * MySQL 5.7 컨테이너 인스턴스입니다.
     *
     * utf8mb4 문자셋과 utf8mb4_bin 콜레이션으로 구성됩니다.
     */
    val MySQL5: MySQL5Server by lazy {
        MySQL5Server()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            } as MySQL5Server
    }

    /**
     * MySQL 8.0 컨테이너 인스턴스입니다.
     *
     * utf8mb4 문자셋과 utf8mb4_bin 콜레이션으로 구성됩니다.
     */
    val MySQL8: MySQL8Server by lazy {
        MySQL8Server()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            } as MySQL8Server
    }

    /**
     * PostgreSQL 컨테이너 인스턴스입니다.
     *
     * [PostgreSQLServer.Launcher]에서 공유 인스턴스를 가져옵니다.
     */
    val Postgres by lazy { PostgreSQLServer.Launcher.postgres }

    // val Cockroach by lazy { CockroachServer.Launcher.cockroach }
}
