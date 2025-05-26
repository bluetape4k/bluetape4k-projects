package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.MariaDBServer
import io.bluetape4k.testcontainers.database.MySQL5Server
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer

/**
 * 테스트를 위한 DB 컨테이너를 제공합니다.
 */
object ContainerProvider: KLogging() {

    val mariadb: MariaDBServer by lazy { MariaDBServer.Launcher.mariadb }
    val mysql5: MySQL5Server by lazy { MySQL5Server.Launcher.mysql }
    val mysql8: MySQL8Server by lazy { MySQL8Server.Launcher.mysql }
    val postgres: PostgreSQLServer by lazy { PostgreSQLServer.Launcher.postgres }
}
