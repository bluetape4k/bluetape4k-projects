package io.bluetape4k.hibernate.mapping.embeddable

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.converters.RC4StringConverter
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

@Entity
@Table(
    indexes = [
        Index(name = "idx_embeddable_person_username", columnList = "user_id, password", unique = true),
        Index(name = "idx_embeddable_person_email", columnList = "email", unique = true)
    ]
)
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class EmbeddablePerson private constructor(
    @NotBlank
    @Column(name = "user_id", nullable = false, length = 32, unique = true)
    var userId: String,

    @NotBlank
    @Convert(converter = RC4StringConverter::class)
    var password: String,
): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(username: String, password: String): EmbeddablePerson {
            username.requireNotBlank("username")
            password.requireNotBlank("password")
            return EmbeddablePerson(username, password)
        }
    }

    @Column(nullable = false, unique = true, length = 64)
    var email: String = ""

    var active: Boolean = true

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "home_street", length = 128)),
        AttributeOverride(name = "city", column = Column(name = "home_city", length = 24)),
        AttributeOverride(name = "zipcode", column = Column(name = "home_zipcode", length = 8))
    )
    var homeAddress: EmbeddableAddress? = null
    // 위와 같이 nullable 을 사용하는 방식도 있고, EmbeddableAddress의 기본값을 적용해도 된다.
    // val homeAddress = EmbeddableAddress()

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "office_street", length = 128)),
        AttributeOverride(name = "city", column = Column(name = "office_city", length = 24)),
        AttributeOverride(name = "zipcode", column = Column(name = "office_zipcode", length = 8))
    )
    var officeAddress: EmbeddableAddress? = null // = EmbeddableAddress()

    override fun equalProperties(other: Any): Boolean {
        return other is EmbeddablePerson &&
                userId == other.userId &&
                email == other.email
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: hashOf(userId, email)
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("userId", userId)
            .add("email", email)
    }
}
