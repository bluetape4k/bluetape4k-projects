package io.bluetape4k.hibernate.cache.lettuce.model

import io.bluetape4k.support.hashOf
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "versioned_items")
@jakarta.persistence.Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class VersionedItem: Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var name: String = ""
    var price: Int = 0

    @Version
    var version: Long = 0

    override fun equals(other: Any?): Boolean =
        other is VersionedItem && id == other.id && name == other.name && price == other.price && version == other.version

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(name, price, version)

    override fun toString(): String =
        "VersionedItem(id=$id, name='$name', price=$price, version=$version)"
}

@Entity
@Table(name = "versioned_categories")
@jakarta.persistence.Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class VersionedCategory: Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var label: String = ""

    @Version
    var version: Long = 0

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    val items: MutableSet<VersionedCategoryItem> = linkedSetOf()

    override fun equals(other: Any?): Boolean =
        other is VersionedCategory && id == other.id && label == other.label && version == other.version

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(label, version)

    override fun toString(): String =
        "VersionedCategory(id=$id, label='$label', version=$version)"
}

@Entity
@Table(name = "versioned_category_items")
@jakarta.persistence.Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class VersionedCategoryItem: Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var name: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: VersionedCategory? = null

    override fun equals(other: Any?): Boolean =
        other is VersionedCategoryItem && id == other.id && name == other.name && category == other.category

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(name, category)

    override fun toString(): String =
        "VersionedCategoryItem(id=$id, name='$name', category=$category)"
}
