package io.bluetape4k.exposed.core.auditable

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * 감사(Auditing) 컬럼(`created_by`, `created_at`, `updated_by`, `updated_at`)을 포함한
 * Exposed `IdTable` 베이스 클래스입니다.
 *
 * > **주의**: 이 클래스는 `exposed-java-time` 모듈에 의존합니다.
 * > `build.gradle.kts`에 `compileOnly(Libs.exposed_java_time)` 의존성이 필요합니다.
 *
 * ## 컬럼 동작
 * | 컬럼        | INSERT 시                          | UPDATE 시                          |
 * |------------|-----------------------------------|------------------------------------|
 * | created_by | [UserContext.getCurrentUser()] 자동 설정 | 변경 없음                          |
 * | created_at | DB `CURRENT_TIMESTAMP` 자동 설정    | 변경 없음                          |
 * | updated_by | null (Repository에서 명시적 설정)    | [UserContext.getCurrentUser()] 설정 |
 * | updated_at | null (Repository에서 명시적 설정)    | DB `CURRENT_TIMESTAMP` 설정        |
 *
 * ```kotlin
 * object Users : AuditableLongIdTable("users") {
 *     val name = varchar("name", 255)
 * }
 * ```
 */
abstract class AuditableIdTable<ID: Any>(name: String = ""): IdTable<ID>(name) {

    /**
     * 레코드를 생성한 사용자명 컬럼입니다.
     *
     * - 길이 128의 `varchar`이며 non-nullable입니다.
     * - INSERT 시 [UserContext.getCurrentUser()]로 자동 설정됩니다.
     *   사용자 컨텍스트가 없으면 `"system"`이 기본값으로 사용됩니다.
     */
    val createdBy: Column<String> = varchar("created_by", 128)
        .clientDefault { UserContext.getCurrentUser() }

    /**
     * 레코드가 생성된 시각(UTC) 컬럼입니다.
     *
     * - `timestamp` 타입이며 nullable입니다.
     * - INSERT 시 DB의 `CURRENT_TIMESTAMP`로 자동 설정됩니다.
     */
    val createdAt: Column<Instant?> = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
        .nullable()

    /**
     * 레코드를 마지막으로 수정한 사용자명 컬럼입니다.
     *
     * - 길이 128의 `varchar`이며 nullable입니다.
     * - Repository의 `auditedUpdate*` 메서드에서 [UserContext.getCurrentUser()]로 설정됩니다.
     */
    val updatedBy: Column<String?> = varchar("updated_by", 128).nullable()

    /**
     * 레코드가 마지막으로 수정된 시각(UTC) 컬럼입니다.
     *
     * - `timestamp` 타입이며 nullable입니다.
     * - Repository의 `auditedUpdate*` 메서드에서 `CURRENT_TIMESTAMP`로 설정됩니다.
     */
    val updatedAt: Column<Instant?> = timestamp("updated_at").nullable()
}
