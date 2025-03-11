package io.bluetape4k.hibernate.reactive

import io.bluetape4k.testcontainers.database.MySQL8Server
import org.hibernate.tool.schema.Action

object MySQLLauncher {

    private val mysql8: MySQL8Server by lazy { MySQL8Server.Launcher.mysql }

    val hibernateProperties: Map<String, Any?> by lazy {
        val props = mutableMapOf<String, Any?>()

        // for JPA 2.x (JPA 3.x 에서는 jakarta.xxxx 를 사용해야 함)
        // docker-run-mysql.sh로 미리 실행시켰을 때 사용합니다.
        // props["javax.persistence.jdbc.url"] = "jdbc:mysql://localhost:3306/bluetape4k"
        // props["javax.persistence.jdbc.user"] = "root"
        // props["javax.persistence.jdbc.password"] = "test"

        // Testcontainers 사용 시
        props["jakarta.persistence.jdbc.url"] = mysql8.jdbcUrl
        props["jakarta.persistence.jdbc.user"] = mysql8.username
        props["jakarta.persistence.jdbc.password"] = mysql8.password
        props["jakarta.persistence.schema-generation.database.action"] = Action.CREATE.externalJpaName

        props
    }
}
