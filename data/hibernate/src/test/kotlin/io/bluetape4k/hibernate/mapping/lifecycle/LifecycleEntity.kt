package io.bluetape4k.hibernate.mapping.lifecycle

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.validation.constraints.NotBlank
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * AuditingEntityListener 를 등록하면 @CreatedDate, @LastModifiedDate 가 자동으로 채워진다.
 * 단 SpringBoot 환경에 `@EnableJpaAuditing(modifyOnCreate = true)` 설정이 필요하다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Access(AccessType.FIELD)
class LifecycleEntity private constructor(
    @Column(nullable = false)
    @NotBlank
    var name: String,
): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): LifecycleEntity {
            name.requireNotBlank("name")
            return LifecycleEntity(name)
        }
    }

    @CreatedBy
    @Column(updatable = false)
    var createdBy: String? = null

    @CreatedDate
    @Column(updatable = false)
    var createdAt: LocalDateTime? = null

    @LastModifiedBy
    @Column(insertable = false)
    var updatedBy: String? = null

    @LastModifiedDate
    @Column(insertable = false)
    var updatedAt: LocalDateTime? = null


    override fun equalProperties(other: Any): Boolean {
        return other is LifecycleEntity && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: name.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
