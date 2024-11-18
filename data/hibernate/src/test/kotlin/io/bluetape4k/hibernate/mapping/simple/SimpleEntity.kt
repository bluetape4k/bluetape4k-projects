package io.bluetape4k.hibernate.mapping.simple

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.LongJpaEntity
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

/**
 * Kotlin에서는 data class 로 entity를 정의하는 것이 가장 쉽고, 편하다.
 * 하지만 메모리 소모도 많다. 대신 `IntJpaEntity` 나 `LongJpaEntity` 를 상속 받아 사용하는 것을 추천합니다.
 */
@Entity(name = "simple_entity")
@Table(indexes = [Index(columnList = "name", unique = true)])
@Access(AccessType.FIELD)
class SimpleEntity private constructor(
    @Column(nullable = false)
    @NotBlank
    var name: String,
): LongJpaEntity() {

    companion object {
        operator fun invoke(name: String): SimpleEntity {
            name.requireNotBlank("name")
            return SimpleEntity(name)
        }
    }

    var description: String? = null

    override fun equalProperties(other: Any): Boolean =
        other is SimpleEntity && name == other.name

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
            .add("description", description)
    }
}
