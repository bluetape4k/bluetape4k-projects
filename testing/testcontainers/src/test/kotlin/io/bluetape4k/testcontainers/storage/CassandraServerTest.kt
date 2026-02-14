package io.bluetape4k.testcontainers.storage

import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.storage.CassandraServer.Companion.LOCAL_DATACENTER1
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CassandraServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val CQL_GET_VERSION = "SELECT release_version FROM system.local"
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch cassandra version 4+ with default port`() {
            CassandraServer(useDefaultPort = true).use { server ->
                server.start()

                CqlSessionBuilder()
                    .addContactPoint(server.contactPoint)
                    .withLocalDatacenter(LOCAL_DATACENTER1)
                    .build()
                    .use { session ->
                        log.debug { "Execute cql script. $CQL_GET_VERSION" }
                        session.execute(CQL_GET_VERSION).wasApplied().shouldBeTrue()
                    }
            }
        }

        @Test
        fun `launch cassandra with init script with default port`() {
            CassandraServer(useDefaultPort = true).use { server ->
                server.withInitScript("cassandra/init.cql")
                server.start()

                CqlSessionBuilder()
                    .addContactPoint(server.contactPoint)
                    .withLocalDatacenter(LOCAL_DATACENTER1)
                    .withConfigLoader(DriverConfigLoader.fromClasspath("application.conf"))
                    .build()
                    .use { session ->
                        log.debug { "Execute cql script. $CQL_GET_VERSION" }
                        session.execute(CQL_GET_VERSION).wasApplied().shouldBeTrue()
                    }
            }
        }
    }

    @Nested
    inner class UseDockerPort {

        @Test
        fun `build cql with custom driver configuration`() {
            CassandraServer().use { server ->
                server.start()

                CqlSessionBuilder()
                    .addContactPoint(server.contactPoint)
                    .withLocalDatacenter(LOCAL_DATACENTER1)
                    .withConfigLoader(DriverConfigLoader.fromClasspath("application.conf"))
                    .build()
                    .use { session ->
                        session.context.config.profiles.forEach { (name, profile) ->
                            println("profile name : $name")
                            println("profile options : ${profile.entrySet().joinToString()}")
                        }

                        log.debug { "Execute cql script. $CQL_GET_VERSION" }
                        val stmt = SimpleStatementBuilder(CQL_GET_VERSION)
                            .setExecutionProfileName("oltp")
                            .build()
                        session.execute(stmt).wasApplied().shouldBeTrue()
                    }
            }
        }

        @Test
        fun `launch cassandra server by launcher`() {
            val cassandra = CassandraServer.Launcher.cassandra4
            cassandra.isRunning.shouldBeTrue()

            val session = CassandraServer.Launcher.getOrCreateSession("examples")
            val version = session.getCassandraReleaseVersion()
            version.shouldNotBeNull()
            log.info { "Cassandra release version=$version" }
            session.close()
        }

        @Test
        fun `get session directly`() {
            val session = CassandraServer.Launcher.getOrCreateSession("examples")

            val rs = session.execute(CQL_GET_VERSION)
            rs.wasApplied().shouldBeTrue()
            rs.one()!!.getString("release_version")!!.shouldNotBeEmpty()

            val metadata = session.metadata

            // Node
            metadata.nodes.values.forEach { node ->
                println("Datacenter: ${node.datacenter}, Host: ${node.endPoint}, Rack: ${node.rack}")
            }

            // Keyspace
            metadata.keyspaces.values.forEach { keyspace ->
                println("Keyspace: ${keyspace.name}")
                keyspace.tables.values.forEach { table ->
                    println("\tTable: ${table.name}")
                }
            }
        }

        @Test
        fun `get release version`() {
            val session = CassandraServer.Launcher.getOrCreateSession("examples")
            val version = session.getCassandraReleaseVersion()
            version.shouldNotBeNull()
            log.info { "Release version=$version" }
            session.close()
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { CassandraServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { CassandraServer(tag = " ") }
    }
}
