package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.CockroachServer
import io.bluetape4k.testcontainers.database.MariaDBServer
import io.bluetape4k.testcontainers.database.MySQL5Server
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer

object Containers: KLogging() {

    val MariaDB by lazy { MariaDBServer.Launcher.mariadb }

    val MySQL5 by lazy { MySQL5Server.Launcher.mysql }

    val MySQL8 by lazy { MySQL8Server.Launcher.mysql }

    val Postgres by lazy { PostgreSQLServer.Launcher.postgres }

    val Cockroach by lazy { CockroachServer.Launcher.cockroach }
}
