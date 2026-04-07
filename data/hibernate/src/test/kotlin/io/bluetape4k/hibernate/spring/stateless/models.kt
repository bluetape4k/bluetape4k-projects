package io.bluetape4k.hibernate.spring.stateless

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.IntJpaEntity
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

@Entity(name = "SpringStatelessEntity")
@Table(name = "spring_stateless_entity")
@DynamicInsert
@DynamicUpdate
class StatelessEntity(
    val name: String,
): IntJpaEntity() {
    var firstname: String? = null
    var lastname: String? = null
    var age: Int? = null
    var street: String? = null
    var city: String? = null
    var zipcode: String? = null

    override fun equalProperties(other: Any): Boolean = other is StatelessEntity && name == other.name

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder =
        super
            .buildStringHelper()
            .add("name", name)
            .add("firstname", firstname)
            .add("lastname", lastname)
            .add("age", age)
            .add("street", street)
            .add("city", city)
}

@Entity(name = "SpringStatelessMaster")
@Table(name = "spring_stateless_master")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class StatelessMaster(
    val name: String,
): IntJpaEntity() {
    @OneToMany(mappedBy = "master", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    val details: MutableList<StatelessDetail> = arrayListOf()

    override fun equalProperties(other: Any): Boolean = other is StatelessMaster && name == other.name

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder =
        super
            .buildStringHelper()
            .add("name", name)
}

@Entity(name = "SpringStatelessDetail")
@Table(name = "spring_stateless_detail")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class StatelessDetail(
    val name: String,
): IntJpaEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "master_id")
    var master: StatelessMaster? = null

    override fun equalProperties(other: Any): Boolean = other is StatelessDetail && name == other.name

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder =
        super
            .buildStringHelper()
            .add("name", name)
}
