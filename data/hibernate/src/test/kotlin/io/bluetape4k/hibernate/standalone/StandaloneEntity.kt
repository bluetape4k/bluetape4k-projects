package io.bluetape4k.hibernate.standalone

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.LongJpaEntity
import io.bluetape4k.support.hashOf
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "standalone_entity")
class StandaloneEntity(
    @Column(nullable = false)
    var name: String = "",
): LongJpaEntity() {
    override fun equalProperties(other: Any): Boolean =
        other is StandaloneEntity && name == other.name

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: hashOf(name)

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("name", name)
}
