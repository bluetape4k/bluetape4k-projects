package io.bluetape4k.spring.cassandra.model

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.domain.Auditable
import java.time.Instant
import java.util.*

/**
 * 생성자/수정자와 시각을 함께 보관하는 Cassandra 감사(Audit) 엔티티 기반 추상 클래스입니다.
 *
 * ## 동작/계약
 * - [isNew]는 `created_at` 컬럼에 매핑된 `_createdAt`이 `null`이면 `true`를 반환합니다.
 * - `get/setCreated*`, `get/setLastModified*`는 내부 필드를 그대로 감싸며 Spring Data [Auditable] 계약을 만족합니다.
 * - 공개 프로퍼티([createdBy], [createdAt], [lastModifiedBy], [lastModifiedAt])는 읽기 전용 뷰를 제공합니다.
 *
 * ```kotlin
 * val entity = object: AbstractCassandraAuditable<String, String>() {
 *     private var pk: String? = null
 *     override fun getId(): String? = pk
 *     override fun setId(id: String) { pk = id }
 * }
 * // result == entity.isNew()
 * ```
 *
 * @param U 생성/수정 작업 주체 타입
 * @param PK 엔티티 식별자 타입
 */
abstract class AbstractCassandraAuditable<U: Any, PK: Any>: AbstractCassandraPersistable<PK>(),
                                                            Auditable<U, PK, Instant> {
    @field:Column("created_by")
    private var _createdBy: U? = null

    @field:Column("created_at")
    private var _createdAt: Instant? = null

    @field:Column("lastModified_by")
    private var _lastModifiedBy: U? = null

    @field:Column("lastModified_at")
    private var _lastModifiedAt: Instant? = null

    val createdBy: U? get() = _createdBy
    val createdAt: Instant? get() = _createdAt
    val lastModifiedBy: U? get() = _lastModifiedBy
    val lastModifiedAt: Instant? get() = _lastModifiedAt

    /**
     * 엔티티가 아직 저장되지 않았는지 여부를 반환합니다.
     *
     * ## 동작/계약
     * - `_createdAt == null`이면 `true`를 반환합니다.
     * - `_createdAt`이 설정된 뒤에는 `false`를 반환합니다.
     *
     * ```kotlin
     * entity.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"))
     * // result == entity.isNew()
     * ```
     */
    override fun isNew(): Boolean = _createdAt == null

    /**
     * 생성자를 Optional 형태로 반환합니다.
     *
     * ## 동작/계약
     * - `_createdBy`가 `null`이면 `Optional.empty()`를 반환합니다.
     * - 값이 있으면 해당 값을 감싼 `Optional`을 반환합니다.
     *
     * ```kotlin
     * entity.setCreatedBy("debop")
     * // result == entity.getCreatedBy().orElse("unknown")
     * ```
     */
    override fun getCreatedBy(): Optional<U> = Optional.ofNullable(_createdBy)

    /**
     * 생성자를 저장합니다.
     *
     * ## 동작/계약
     * - 전달받은 값을 `_createdBy`에 그대로 대입합니다.
     *
     * ```kotlin
     * entity.setCreatedBy("debop")
     * // result == entity.createdBy
     * ```
     */
    override fun setCreatedBy(createdBy: U) {
        _createdBy = createdBy
    }

    /**
     * 생성 시각을 Optional 형태로 반환합니다.
     *
     * ## 동작/계약
     * - `_createdAt`이 `null`이면 `Optional.empty()`를 반환합니다.
     * - 값이 있으면 해당 시각을 감싼 `Optional`을 반환합니다.
     *
     * ```kotlin
     * entity.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"))
     * // result == entity.getCreatedDate().isPresent
     * ```
     */
    override fun getCreatedDate(): Optional<Instant> = Optional.ofNullable(_createdAt)

    /**
     * 생성 시각을 저장합니다.
     *
     * ## 동작/계약
     * - 전달받은 값을 `_createdAt`에 그대로 대입합니다.
     *
     * ```kotlin
     * val createdAt = Instant.parse("2024-01-01T00:00:00Z")
     * entity.setCreatedDate(createdAt)
     * // result == entity.createdAt
     * ```
     */
    override fun setCreatedDate(creationDate: Instant) {
        _createdAt = creationDate
    }

    /**
     * 마지막 수정자를 Optional 형태로 반환합니다.
     *
     * ## 동작/계약
     * - `_lastModifiedBy`가 `null`이면 `Optional.empty()`를 반환합니다.
     * - 값이 있으면 해당 값을 감싼 `Optional`을 반환합니다.
     *
     * ```kotlin
     * entity.setLastModifiedBy("mike")
     * // result == entity.getLastModifiedBy().orElse("unknown")
     * ```
     */
    override fun getLastModifiedBy(): Optional<U> = Optional.ofNullable(_lastModifiedBy)

    /**
     * 마지막 수정자를 저장합니다.
     *
     * ## 동작/계약
     * - 전달받은 값을 `_lastModifiedBy`에 그대로 대입합니다.
     *
     * ```kotlin
     * entity.setLastModifiedBy("mike")
     * // result == entity.lastModifiedBy
     * ```
     */
    override fun setLastModifiedBy(lastModifiedBy: U) {
        _lastModifiedBy = lastModifiedBy
    }

    /**
     * 마지막 수정 시각을 Optional 형태로 반환합니다.
     *
     * ## 동작/계약
     * - `_lastModifiedAt`이 `null`이면 `Optional.empty()`를 반환합니다.
     * - 값이 있으면 해당 시각을 감싼 `Optional`을 반환합니다.
     *
     * ```kotlin
     * entity.setLastModifiedDate(Instant.parse("2024-01-01T00:00:10Z"))
     * // result == entity.getLastModifiedDate().isPresent
     * ```
     */
    override fun getLastModifiedDate(): Optional<Instant> = Optional.ofNullable(_lastModifiedAt)

    /**
     * 마지막 수정 시각을 저장합니다.
     *
     * ## 동작/계약
     * - 전달받은 값을 `_lastModifiedAt`에 그대로 대입합니다.
     *
     * ```kotlin
     * val modifiedAt = Instant.parse("2024-01-01T00:00:10Z")
     * entity.setLastModifiedDate(modifiedAt)
     * // result == entity.lastModifiedAt
     * ```
     */
    override fun setLastModifiedDate(lastModifiedDate: Instant) {
        _lastModifiedAt = lastModifiedDate
    }
}
