package io.bluetape4k.hibernate.mapping.associations.manytoone

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.IntJpaEntity
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.CascadeType.MERGE
import jakarta.persistence.CascadeType.PERSIST
import jakarta.persistence.CascadeType.REFRESH
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

@Entity(name = "manytoone_bear")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Beer(val name: String): IntJpaEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = [MERGE, PERSIST, REFRESH])
    @JoinColumn(name = "brewery_id", nullable = false)
    var brewery: Brewery? = null

    override fun equalProperties(other: Any): Boolean {
        return other is Beer && name == other.name
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

@Entity(name = "manytoone_brewery")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Brewery(val name: String): IntJpaEntity() {

    @OneToMany(mappedBy = "brewery", cascade = [ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val beers: MutableSet<Beer> = mutableSetOf()

    fun addBeers(vararg beersToAdd: Beer) {
        beersToAdd.forEach {
            if (this.beers.add(it)) {
                it.brewery = this
            }
        }
    }

    fun removeBeers(vararg beersToRemove: Beer) {
        beersToRemove.forEach {
            if (this.beers.remove(it)) {
                it.brewery = null
            }
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is Brewery && name == other.name
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

@Entity(name = "manytoone_jug")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Jug(val name: String): IntJpaEntity() {

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun equalProperties(other: Any): Boolean {
        return other is Jug && name == other.name
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}

@Entity(name = "manytoone_jugmeter")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class JugMeter(val name: String): IntJpaEntity() {

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "jug_id", nullable = false)
    var memberOf: Jug? = null

    override fun equalProperties(other: Any): Boolean {
        return other is JugMeter && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}

@Entity(name = "manytoone_salesguy")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class SalesGuy(val name: String): IntJpaEntity() {

    @ManyToOne(fetch = FetchType.LAZY, cascade = [MERGE, PERSIST, REFRESH])
    @JoinColumn(name = "sales_force_id")
    var salesForce: SalesForce? = null

    override fun equalProperties(other: Any): Boolean {
        return other is SalesGuy && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}

@Entity(name = "manytoone_salesforce")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class SalesForce(val name: String): IntJpaEntity() {

    @OneToMany(mappedBy = "salesForce", cascade = [ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val salesGuys: MutableSet<SalesGuy> = mutableSetOf()

    fun addGuys(vararg guysToAdd: SalesGuy) {
        guysToAdd.forEach {
            if (this.salesGuys.add(it)) {
                it.salesForce = this
            }
        }
    }

    fun removeGuys(vararg guysToRemove: SalesGuy) {
        guysToRemove.forEach {
            if (this.salesGuys.remove(it)) {
                it.salesForce = null
            }
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is SalesForce && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
