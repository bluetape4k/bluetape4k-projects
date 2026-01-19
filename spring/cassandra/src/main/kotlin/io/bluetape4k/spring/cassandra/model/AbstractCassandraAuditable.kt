package io.bluetape4k.spring.cassandra.model

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.domain.Auditable
import java.time.Instant
import java.util.*

/**
 * Auditable Entity 를 나타내는 추상화 클래스입니다.
 *
 * @param U createdBy, lastModifiedBy 등 엔티티의 변화를 수행하는 Actor 의 수형 (보통 String 이다)
 * @param PK Entity PrimaryKey의 수형
 */
abstract class AbstractCassandraAuditable<U: Any, PK: Any>
    : AbstractCassandraPersistable<PK>(), Auditable<U, PK, Instant> {

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
     * Transient object 인지, Persistant Object 인지 판단한다
     */
    override fun isNew(): Boolean = _createdAt == null

    /**
     * Returns the user who created this entity.
     *
     * @return the createdBy
     */
    override fun getCreatedBy(): Optional<U> = Optional.ofNullable(_createdBy)

    /**
     * Sets the user who created this entity.
     *
     * @param createdBy the creating entity to set
     */
    override fun setCreatedBy(createdBy: U) {
        _createdBy = createdBy
    }

    /**
     * Returns the creation date of the entity.
     *
     * @return the createdDate
     */
    override fun getCreatedDate(): Optional<Instant> = Optional.ofNullable(_createdAt)

    /**
     * Sets the creation date of the entity.
     *
     * @param creationDate the creation date to set
     */
    override fun setCreatedDate(creationDate: Instant) {
        _createdAt = creationDate
    }

    /**
     * Returns the user who modified the entity lastly.
     *
     * @return the lastModifiedBy
     */
    override fun getLastModifiedBy(): Optional<U> = Optional.ofNullable(_lastModifiedBy)

    /**
     * Sets the user who modified the entity lastly.
     *
     * @param lastModifiedBy the last modifying entity to set
     */
    override fun setLastModifiedBy(lastModifiedBy: U) {
        _lastModifiedBy = lastModifiedBy
    }

    /**
     * Returns the date of the last modification.
     *
     * @return the lastModifiedDate
     */
    override fun getLastModifiedDate(): Optional<Instant> = Optional.ofNullable(_lastModifiedAt)

    /**
     * Sets the date of the last modification.
     *
     * @param lastModifiedDate the date of the last modification to set
     */
    override fun setLastModifiedDate(lastModifiedDate: Instant) {
        _lastModifiedAt = lastModifiedDate
    }
}
