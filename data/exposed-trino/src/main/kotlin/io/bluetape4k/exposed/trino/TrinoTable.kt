package io.bluetape4k.exposed.trino

import org.jetbrains.exposed.v1.core.Table

/**
 * Trino DDL과 호환되는 Exposed [Table] 베이스 클래스.
 *
 * Trino는 `CREATE TABLE` DDL에서 `PRIMARY KEY` 및 `CONSTRAINT ... PRIMARY KEY` 구문을
 * 지원하지 않습니다. 이 클래스는 [createStatement]를 오버라이드하여 PRIMARY KEY 관련
 * 구문을 자동으로 제거합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * object UserTable : TrinoTable("users") {
 *     val id = long("id")
 *     val name = varchar("name", 255)
 *     override val primaryKey = PrimaryKey(id)  // Exposed ORM용 메타데이터로만 사용됨
 * }
 * ```
 *
 * ## 주의
 *
 * - Trino memory 커넥터는 PRIMARY KEY, UNIQUE, FOREIGN KEY 제약을 모두 지원하지 않습니다.
 * - 운영 환경 BigQuery connector에서도 DDL 제약은 무시됩니다.
 * - Exposed ORM의 관계 참조(FK 등)를 위해 [primaryKey]는 정의할 수 있으나,
 *   실제 DDL에는 포함되지 않습니다.
 */
open class TrinoTable(name: String = ""): Table(name) {

    override fun createStatement(): List<String> =
        super.createStatement().map { sql -> sql.sanitizeForTrino() }
}

/**
 * SQL 문자열에서 Trino가 지원하지 않는 DDL 구문을 제거합니다.
 *
 * 제거 대상:
 * - 인라인 PRIMARY KEY: `col BIGINT PRIMARY KEY`
 * - 이름 있는 제약: `, CONSTRAINT pk_name PRIMARY KEY (col)`
 * - 명시적 NULL: `col TIMESTAMP NULL` → `col TIMESTAMP` (Trino는 NULL 키워드 미지원)
 *
 * 보존 대상:
 * - NOT NULL: Trino 418+ 지원
 */
private fun String.sanitizeForTrino(): String =
    this
        // CONSTRAINT pk_name PRIMARY KEY (col) 제거
        .replace(Regex(",\\s*CONSTRAINT\\s+\\S+\\s+PRIMARY\\s+KEY\\s*\\([^)]*\\)", RegexOption.IGNORE_CASE), "")
        // 인라인 PRIMARY KEY 제거 (NOT NULL 보존 위해 NOT NULL을 먼저 보호)
        .replace(Regex("\\s+PRIMARY\\s+KEY", RegexOption.IGNORE_CASE), "")
        // NOT NULL → 임시 치환 후 복원 (명시적 NULL 제거 시 NOT NULL이 손상되지 않도록)
        .replace("NOT NULL", "\u0000NOTNULL\u0000")
        .replace(Regex("\\s+NULL\\b"), "")
        .replace("\u0000NOTNULL\u0000", "NOT NULL")
