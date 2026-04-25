package io.bluetape4k.exposed.clickhouse

import io.bluetape4k.exposed.clickhouse.engine.ClickHouseEngine
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Table

/**
 * ClickHouse DDL과 호환되는 Exposed Table 베이스 클래스.
 *
 * [createStatement] 오버라이드를 통해:
 * 1. CREATE TABLE 구문만 유지 (ALTER/SEQUENCE/COMMENT 등 제거)
 * 2. PRIMARY KEY / CONSTRAINT / REFERENCES / NOT NULL / NULL 제거
 * 3. ENGINE 절 부착
 *
 * ## 주의
 * - column comment DDL을 지원하지 않음 (filter로 제거됨) — KDoc 또는 README Caveats 참고
 * - PRIMARY KEY는 ORDER BY로 표현 (DSL 빌더에서 설정)
 * - FK 참조는 ClickHouse 미지원
 */
abstract class ClickHouseTable(
    name: String = "",
    val engine: ClickHouseEngine = mergeTree { orderBy("id") },
): Table(name) {

    companion object: KLogging()

    override fun createStatement(): List<String> =
        super.createStatement()
            // CREATE TABLE 구문만 유지 (ALTER TABLE ADD CONSTRAINT, CREATE SEQUENCE, COMMENT ON 등 제거)
            .filter { sql -> sql.trimStart().startsWith("CREATE TABLE", ignoreCase = true) }
            .map { sql -> sanitizeForClickHouse(sql) + "\n${engine.toClause()}" }
}

/**
 * ClickHouse와 호환되도록 SQL DDL을 정제합니다.
 *
 * 제거 대상:
 * - `CONSTRAINT pk_name PRIMARY KEY (...)` 절
 * - 인라인 `PRIMARY KEY` 키워드
 * - `REFERENCES ...` FK 절
 * - `NOT NULL` 토큰 (ClickHouse는 Nullable(T) sqlType으로 표현)
 * - `NULL` 토큰 (Exposed 1.2가 nullable 컬럼에 ` NULL` suffix를 붙임)
 */
internal fun sanitizeForClickHouse(sql: String): String =
    sql
        // CONSTRAINT pk_name PRIMARY KEY (...) 절 제거
        .replace(Regex(",?\\s*CONSTRAINT\\s+\\S+\\s+PRIMARY\\s+KEY\\s*\\([^)]*\\)", RegexOption.IGNORE_CASE), "")
        // 인라인 PRIMARY KEY 제거 (PRIMARY KEY ( ... ) 형태가 아닌 컬럼 인라인 키워드)
        .replace(Regex("\\s+PRIMARY\\s+KEY(?!\\s*\\()", RegexOption.IGNORE_CASE), "")
        // REFERENCES ... 절 제거 (FK 미지원)
        .replace(Regex("\\s+REFERENCES\\s+\\S+\\s*\\([^)]*\\)(\\s+ON\\s+\\w+\\s+\\w+)?", RegexOption.IGNORE_CASE), "")
        // NOT NULL 토큰 제거 (NULL보다 먼저 처리)
        .replace(Regex("\\s+NOT\\s+NULL\\b", RegexOption.IGNORE_CASE), "")
        // NULL 토큰 제거
        .replace(Regex("\\s+NULL\\b", RegexOption.IGNORE_CASE), "")
