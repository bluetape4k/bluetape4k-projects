package io.bluetape4k.hibernate.standalone

import io.bluetape4k.hibernate.model.LongJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "standalone_entity")
class StandaloneEntity(
    @Column(nullable = false)
    var name: String = "",
) : LongJpaEntity() {
    override fun equalProperties(other: Any): Boolean =
        other is StandaloneEntity && name == other.name
}
