package io.bluetape4k.testcontainers.storage

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.Version
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info

private val log by lazy { KotlinLogging.logger { } }

/**
 * [CqlSession]이 접속한 Cassandra Server의 release version 을 조회한다
 *
 * ## 동작/계약
 * - `system.local` 테이블에서 `release_version` 단일 값을 조회합니다.
 * - 결과가 없으면 null을 반환하고, 값이 있으면 [Version]으로 파싱해 반환합니다.
 *
 * ```kotlin
 * val version = session.getCassandraReleaseVersion()
 * // version != null
 * ```
 *
 * @return Cassandra release version
 */
fun CqlSession.getCassandraReleaseVersion(): Version? {
    val row = execute("SELECT release_version FROM system.local").one()
    val releaseVersion = row?.getString(0)
    log.info { "Cassandra Release Version=$releaseVersion" }
    return Version.parse(releaseVersion)
}
