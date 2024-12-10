package io.bluetape4k.workshop.quarkus.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable

@Entity
@Table(name = "Fruit")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Fruit internal constructor(): Serializable {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(name: String, description: String? = null): Fruit {
            name.requireNotBlank("name")

            return Fruit().apply {
                this.name = name
                this.description = description
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false, unique = true, length = 40)
    var name: String = ""

    var description: String? = null

    fun equalProperties(other: Any): Boolean {
        return other is Fruit && name == other.name
    }

    fun buildStringHelper(): ToStringBuilder {
        return ToStringBuilder(this)
            .add("id", id)
            .add("name", name)
    }

    override fun equals(other: Any?): Boolean = other != null && equalProperties(other)

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun toString(): String = buildStringHelper().toString()
}
