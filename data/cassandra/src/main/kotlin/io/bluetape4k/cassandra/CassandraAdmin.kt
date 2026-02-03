package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.Version
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import io.bluetape4k.LibraryName
import io.bluetape4k.cassandra.CassandraAdmin.DEFAULT_KEYSPACE
import io.bluetape4k.cassandra.CassandraAdmin.DEFAULT_REPLICATION_FACTOR
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank

object CassandraAdmin: KLogging() {

    private const val DEFAULT_KEYSPACE = LibraryName
    private const val DEFAULT_REPLICATION_FACTOR = 1

    /**
     * 새로운 [keyspace]를 생성합니다.
     *
     * @param session Cassandra session
     * @param keyspace 생성할 keyspace 이름 (기본값: [DEFAULT_KEYSPACE])
     * @param replicationFactor 복제 팩터 (기본값: [DEFAULT_REPLICATION_FACTOR])
     * @return 생성 성공 여부
     */
    fun createKeyspace(
        session: CqlSession,
        keyspace: String = DEFAULT_KEYSPACE,
        replicationFactor: Int = DEFAULT_REPLICATION_FACTOR,
    ): Boolean {
        keyspace.requireNotBlank("keyspace")

        val stmt = SchemaBuilder.createKeyspace(keyspace)
            .ifNotExists()
            .withSimpleStrategy(replicationFactor)
            .build()

        return session.execute(stmt).wasApplied().apply {
            log.info { "Create Keyspace[$keyspace], replicationFactor[$replicationFactor] wasApplied [$this]" }
        }
    }

    /**
     * [keyspace]를 삭제합니다.
     *
     * @param session Cassandra session
     * @param keyspace 삭제할 keyspace 이름
     * @return 삭제 성공 여부
     */
    fun dropKeyspace(session: CqlSession, keyspace: String): Boolean {
        keyspace.requireNotBlank("keyspace")

        val stmt = SchemaBuilder.dropKeyspace(keyspace).ifExists().build()

        return session.execute(stmt).wasApplied().apply {
            log.info { "Drop Keyspace[$keyspace] wasApplied [$this]" }
        }
    }

    /**
     * Cassandra의 release version을 가져옵니다.
     *
     * @param session Cassandra session
     * @return Cassandra release version or null
     */
    fun getReleaseVersion(session: CqlSession): Version? {
        val stmt = QueryBuilder
            .selectFrom("system", "local")
            .column("release_version")
            .build()

        val row = session.execute(stmt).one()
        val releaseVersion = row?.getString(0)
        log.debug { "Cassandra release version=$releaseVersion" }

        return Version.parse(releaseVersion)
    }
}
