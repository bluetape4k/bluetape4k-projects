package io.bluetape4k.exposed.tests

/**
 * 테스트를 위한 환경 설정입니다.
 */
object TestDBConfig {
    /**
     * true 라면 Testcontainers 를 사용합니다.
     * false 라면 Postgres 등 서버는 로컬에 설치하셔야 합니다.
     */
    var useTestcontainers = true

    /**
     * true 라면 메모리 DB인 H2 만을 대상으로 테스트합니다.
     * false 라면 Postgres, MySQL V5, MySQL V8, MariaDB 도 포함해서 테스트합니다.
     */
    var useFastDB = false
}
