/**
 * 하위 호환성을 위한 Umbrella 모듈입니다.
 *
 * 기존에 `bluetape4k-exposed`를 의존하던 모듈은 변경 없이 계속 사용할 수 있습니다.
 * 실제 구현은 다음 세 모듈로 분리되어 있습니다:
 *
 * - `bluetape4k-exposed-core`: 핵심 Column 타입, 확장 함수 (JDBC 불필요)
 * - `bluetape4k-exposed-dao`: DAO 엔티티, ID 테이블 확장
 * - `bluetape4k-exposed-jdbc`: JDBC 기반 Repository, 트랜잭션, 쿼리 확장
 */
dependencies {
    api(project(":bluetape4k-exposed-core"))
    api(project(":bluetape4k-exposed-dao"))
    api(project(":bluetape4k-exposed-jdbc"))
}
