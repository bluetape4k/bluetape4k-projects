package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.MariaDBServer
import io.bluetape4k.testcontainers.database.MySQL5Server
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue

object Containers: KLogging() {

    val MariaDB: MariaDBServer by lazy {
        MariaDBServer()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }

    val MySQL5: MySQL5Server by lazy {
        MySQL5Server()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }

    val MySQL8: MySQL8Server by lazy {
        MySQL8Server()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }

    val Postgres by lazy { PostgreSQLServer.Launcher.postgres }

    // val Cockroach by lazy { CockroachServer.Launcher.cockroach }
}
