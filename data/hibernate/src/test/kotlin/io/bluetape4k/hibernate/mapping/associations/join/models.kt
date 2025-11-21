package io.bluetape4k.hibernate.mapping.associations.join

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.AbstractJpaEntity
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SecondaryTable
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
data class Address(
    var street: String? = null,
    var city: String? = null,
    var zipcode: String? = null,
): Serializable

// FIXME: QueryDSL 에서 data class 는 제대로 kapt 가 되는데, 일반 클래스에서는 안된다 -> equals 를 재정의하지 않았기 때문이다 !!!
@Entity(name = "join_address_entity")
@Access(AccessType.FIELD)
class AddressEntity(
    var street: String? = null,
    var city: String? = null,
    var zipcode: String? = null,
): AbstractJpaEntity<Int>() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Int? = null

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun equalProperties(other: Any): Boolean {
        return other is AddressEntity &&
                street == other.street &&
                city == other.city &&
                zipcode == other.zipcode
    }

    override fun hashCode(): Int = id?.hashCode() ?: hashOf(street, city, zipcode)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("street", street)
            .add("city", city)
            .add("zipcode", zipcode)
    }
}

@Entity(name = "join_user")
@Access(AccessType.FIELD)
class JoinUser private constructor(
    @field:NotBlank
    val name: String,
): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): JoinUser {
            name.requireNotBlank("name")
            return JoinUser(name)
        }
    }

    // join_user_address_map 테이블에 user_id, address_name, address_id 컬럼을 정의해서 매핑을 저장합니다.
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(
        name = "join_user_address_map",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "address_id")]
    )
    @MapKeyColumn(name = "address_name")
    // @ElementCollection(targetClass = AddressEntity::class, fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    val addresses: MutableMap<String, AddressEntity> = hashMapOf()

    // join_user_nicknames 테이블을 만들고, user_id, nickname 이라는 컬럼을 만든다
    @JoinTable(
        name = "join_user_nicknames",
        joinColumns = [JoinColumn(name = "user_id")],
        indexes = [Index(name = "ix_join_user_nickname", columnList = "user_id")]
    )
    @ElementCollection(targetClass = String::class, fetch = FetchType.LAZY)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    val nicknames: MutableSet<String> = hashSetOf()

    override fun equalProperties(other: Any): Boolean =
        other is JoinUser && name == other.name

    override fun equals(other: Any?): Boolean =
        other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}


@EntityListeners(AuditingEntityListener::class)
@Entity(name = "join_customer")
@Access(AccessType.FIELD)
@SecondaryTable(name = "join_customer_address", pkJoinColumns = [PrimaryKeyJoinColumn(name = "customer_id")])
class JoinCustomer private constructor(
    @field:NotBlank
    val name: String,
    var email: String?,
): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String, email: String? = null): JoinCustomer {
            name.requireNotBlank("name")
            return JoinCustomer(name, email)
        }
    }

    // Embedded 이지만 주 테이블에 저장하지 않고, Secondary table에 저장한다
    @Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "street",
            column = Column(name = "street", table = "join_customer_address", length = 128)
        ),
        AttributeOverride(name = "city", column = Column(name = "city", table = "join_customer_address", length = 24)),
        AttributeOverride(
            name = "zipcode",
            column = Column(name = "zipcode", table = "join_customer_address", length = 8)
        )
    )
    val address = Address()

    // join_customer_address_map 테이블에 customer_id, address_name, address_id 컬럼을 정의해서 매핑을 저장합니다.
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(
        name = "join_customer_address_map",
        joinColumns = [JoinColumn(name = "customer_id")],
        inverseJoinColumns = [JoinColumn(name = "address_id")]
    )
    @MapKeyColumn(name = "address_name")
    @ElementCollection(targetClass = AddressEntity::class, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)  // Customer를 로드 한 후, 해당 Addresses 를 따로 로드합니다.
    val addresses: MutableMap<String, AddressEntity> = hashMapOf()

    @CreatedDate
    var createdAt: LocalDateTime? = null

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null

    override fun equals(other: Any?): Boolean =
        other != null && super.equals(other)

    override fun equalProperties(other: Any): Boolean =
        other is JoinCustomer && name == other.name && email == other.email

    override fun hashCode(): Int = id?.hashCode() ?: hashOf(name, email)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
            .add("email", email)
    }
}
