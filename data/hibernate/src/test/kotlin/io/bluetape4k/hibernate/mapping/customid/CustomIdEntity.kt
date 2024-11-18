package io.bluetape4k.hibernate.mapping.customid

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.AbstractJpaEntity
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Transient
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable
import java.time.Instant

/**
 * value class 를 속성으로 사용할 수 있습니다.
 */
@JvmInline
value class Email(val value: String): Serializable {
    companion object {
        val EMPTY = Email("")
    }
}

@JvmInline
value class Ssn(val value: String): Serializable {
    companion object {
        val EMPTY = Ssn("")
    }
}

@Entity
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class CustomIdEntity private constructor(): AbstractJpaEntity<Email>() {

    companion object {
        @JvmStatic
        operator fun invoke(email: Email, name: String): CustomIdEntity {
            email.value.requireNotBlank("email")
            name.requireNotBlank("name")

            return CustomIdEntity().also {
                it.id = email
                it.name = name
            }
        }
    }

    @Id
    @Column(name = "custom_id", nullable = false, unique = true, length = 64)
    override var id: Email? = null

    @get:Transient
    val email: Email get() = id ?: Email.EMPTY

    var name: String = ""
    var ssn: Ssn = Ssn.EMPTY

    var createdAt: Instant = Instant.now()

    override fun hashCode(): Int {
        return id?.hashCode() ?: hashOf(id, name)
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun equalProperties(other: Any): Boolean {
        return other is CustomIdEntity &&
                email == other.email &&
                name == other.name
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
            .add("ssn", ssn)
    }
}
