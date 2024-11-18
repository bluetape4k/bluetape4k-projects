package io.bluetape4k.testcontainers.storage

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.Version
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info

private val log by lazy { KotlinLogging.logger { } }

/**
 * [CqlSession]이 접속한 Cassandra Server의 release version 을 조회한다
 *
 * @return Cassandra release version
 */
fun CqlSession.getCassandraReleaseVersion(): Version? {
    val row = execute("SELECT release_version FROM system.local").one()
    val releaseVersion = row?.getString(0)
    log.info { "Cassandra Release Version=$releaseVersion" }
    return Version.parse(releaseVersion)
}
