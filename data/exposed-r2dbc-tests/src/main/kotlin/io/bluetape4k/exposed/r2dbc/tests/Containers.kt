package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.MariaDBServer
import io.bluetape4k.testcontainers.database.MySQL5Server
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue

/**
 * R2DBC 테스트를 위한 Testcontainers 기반 DB 컨테이너를 제공합니다.
 *
 * 모든 컨테이너는 `lazy`로 초기화되며, 최초 접근 시 Docker 컨테이너를 시작합니다.
 * JVM 종료 시 [ShutdownQueue]에 등록된 순서로 정리됩니다.
 *
 * ```kotlin
 * // MariaDB 컨테이너 사용 예시
 * val port = Containers.MariaDB.port
 * val database = Containers.MariaDB.databaseName
 * ```
 */
object Containers: KLogging() {

    /**
     * MariaDB Testcontainers 서버 인스턴스입니다.
     *
     * `utf8mb4` 문자셋과 `utf8mb4_bin` 콜레이션으로 설정됩니다.
     * [TestDB.MARIADB] 연결에서 [TestDBConfig.useTestcontainers]가 `true`일 때 사용됩니다.
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
     * MySQL 5.x Testcontainers 서버 인스턴스입니다.
     *
     * `utf8mb4` 문자셋과 `utf8mb4_bin` 콜레이션으로 설정됩니다.
     * [TestDB.MYSQL_V5] 연결에서 [TestDBConfig.useTestcontainers]가 `true`일 때 사용됩니다.
     *
     * **주의**: [TestDB.MYSQL_V5]는 R2DBC 드라이버 호환성 문제로 [TestDB.enabledDialects]에서 제외됩니다.
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
     * MySQL 8.x Testcontainers 서버 인스턴스입니다.
     *
     * `utf8mb4` 문자셋과 `utf8mb4_bin` 콜레이션으로 설정됩니다.
     * [TestDB.MYSQL_V8] 연결에서 [TestDBConfig.useTestcontainers]가 `true`일 때 사용됩니다.
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
     * PostgreSQL Testcontainers 서버 인스턴스입니다.
     *
     * [PostgreSQLServer.Launcher]의 공유 인스턴스를 사용합니다.
     * [TestDB.POSTGRESQL] 연결에서 [TestDBConfig.useTestcontainers]가 `true`일 때 사용됩니다.
     */
    val Postgres by lazy { PostgreSQLServer.Launcher.postgres }


}
