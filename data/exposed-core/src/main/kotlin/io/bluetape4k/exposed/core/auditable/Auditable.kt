package io.bluetape4k.exposed.core.auditable

import java.time.Instant

/**
 * JPA의 감사(Auditing) 어노테이션에 대응하는 Exposed 엔티티 인터페이스입니다.
 *
 * | JPA 어노테이션         | 대응 프로퍼티  |
 * |----------------------|------------|
 * | `@CreatedBy`         | [createdBy] |
 * | `@CreatedDate`       | [createdAt] |
 * | `@LastModifiedBy`    | [updatedBy] |
 * | `@LastModifiedDate`  | [updatedAt] |
 *
 * ## 동작/계약
 * - [createdBy]는 non-nullable이며 기본값은 `"system"`입니다.
 * - [createdAt], [updatedBy], [updatedAt]는 nullable로 상황에 따라 설정됩니다.
 *
 * ```kotlin
 * class UserEntity(id: EntityID<Long>) : LongEntity(id), Auditable {
 *     override val createdBy: String by UserTable.createdBy
 *     override val createdAt: Instant? by UserTable.createdAt
 *     override val updatedBy: String? by UserTable.updatedBy
 *     override val updatedAt: Instant? by UserTable.updatedAt
 * }
 * ```
 */
interface Auditable {

    /**
     * 레코드를 생성한 사용자명입니다.
     *
     * INSERT 시 [UserContext.getCurrentUser()]로 자동 설정됩니다.
     * 기본값은 `"system"`입니다.
     */
    val createdBy: String

    /**
     * 레코드가 생성된 시각(UTC)입니다.
     *
     * INSERT 시 DB의 `CURRENT_TIMESTAMP`로 자동 설정됩니다.
     */
    val createdAt: Instant?

    /**
     * 레코드를 마지막으로 수정한 사용자명입니다.
     *
     * UPDATE 시 [UserContext.getCurrentUser()]로 설정됩니다.
     */
    val updatedBy: String?

    /**
     * 레코드가 마지막으로 수정된 시각(UTC)입니다.
     *
     * UPDATE 시 DB의 `CURRENT_TIMESTAMP`로 설정됩니다.
     */
    val updatedAt: Instant?
}
