package io.bluetape4k.hibernate.mapping.associations.unidirection

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireZeroOrPositiveNumber
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy

@Entity(name = "unidirection_cloud")
@Access(AccessType.FIELD)
class Cloud private constructor(var kind: String, var length: Double): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(kind: String, length: Double = 0.0): Cloud {
            kind.requireNotBlank("kind")
            length.requireZeroOrPositiveNumber("length")
            return Cloud(kind, length)
        }
    }

    // Join table을 이용하여 cloud와 snowflake의 매핑을 관리한다
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinTable(
        name = "unidirection_clod_snowflakes",
        joinColumns = [JoinColumn(name = "cloud_id")],
        inverseJoinColumns = [JoinColumn(name = "snowflake_id")]
    )
    @OrderBy("name")
    val producedSnowflakes: MutableSet<Snowflake> = hashSetOf()

    override fun equalProperties(other: Any): Boolean =
        other is Cloud && kind == other.kind && length == other.length

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: hashOf(kind, length)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("kind", kind)
            .add("length", length)
    }
}

@Entity(name = "unidirection_snowflake")
@Access(AccessType.FIELD)
class Snowflake private constructor(val name: String): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String, description: String? = null): Snowflake {
            name.requireNotBlank("name")
            return Snowflake(name).also {
                it.description = description
            }
        }
    }

    @Column(length = 256)
    var description: String? = null

    override fun equalProperties(other: Any): Boolean =
        other is Snowflake && name == other.name

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
