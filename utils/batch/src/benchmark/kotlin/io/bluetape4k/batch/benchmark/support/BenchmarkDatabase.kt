package io.bluetape4k.batch.benchmark.support

/**
 * 배치 벤치마크에서 사용하는 데이터베이스 종류를 나타냅니다.
 *
 * @property slug 벤치마크 식별자 및 리포트 경로 등에 사용할 짧은 이름
 * @property displayName 화면 표시용 이름
 */
enum class BenchmarkDatabase(val slug: String, val displayName: String) {
    H2("h2", "H2"),
    POSTGRESQL("postgresql", "PostgreSQL"),
    MYSQL("mysql", "MySQL"),
}

/**
 * 배치 벤치마크 실행 드라이버 종류를 나타냅니다.
 *
 * @property displayName 화면 표시용 이름
 */
enum class BenchmarkDriver(val displayName: String) {
    JDBC("JDBC"),
    R2DBC("R2DBC"),
}

/**
 * 배치 벤치마크 시나리오 종류를 나타냅니다.
 *
 * @property displayName 화면 표시용 이름
 */
enum class BenchmarkKind(val displayName: String) {
    SEED("Seed"),
    END_TO_END("End-to-End"),
}
