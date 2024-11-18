package io.bluetape4k.hibernate.mapping.associations.onetomany.set

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.hibernate.annotations.Parent
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

class OneToManySetTest @Autowired constructor(
    private val biddingItemRepo: BiddingItemRepository,
    private val bidRepo: BidRepository,
    private val productRepo: ProductRepository,
): AbstractHibernateTest() {

    @Test
    fun `one-to-many set with bidirectional`() {
        val item = BiddingItem("TV")
        val bid1 = Bid(BigDecimal(100.0))
        val bid2 = Bid(BigDecimal(200.0))
        val bid3 = Bid(BigDecimal(300.0))
        item.addBids(bid1, bid2, bid3)
        item.bids.size shouldBeEqualTo 3

        biddingItemRepo.save(item)
        flushAndClear()

        val loaded = biddingItemRepo.findByIdOrNull(item.id)!!
        loaded shouldBeEqualTo item
        loaded.bids shouldBeEqualTo setOf(bid1, bid2, bid3)

        val bidToRemove = loaded.bids.first()
        loaded.removeBids(bidToRemove)
        biddingItemRepo.save(loaded)
        flushAndClear()

        val loaded2 = biddingItemRepo.findByIdOrNull(item.id)!!
        loaded2 shouldBeEqualTo item
        loaded2.bids shouldBeEqualTo setOf(bid1, bid2, bid3) - bidToRemove

        biddingItemRepo.delete(loaded2)
        flushAndClear()

        biddingItemRepo.findAll().shouldBeEmpty()
        bidRepo.findAll().shouldBeEmpty()
    }

    @Test
    fun `one-to-many with embeddable component by @ElementCollection`() {
        val product = Product("Car")
        val image1 = ProductImage("front")
        val image2 = ProductImage("interior")
        val image3 = ProductImage("engine room")

        product.addImages(image1, image2, image3)
        productRepo.save(product)
        flushAndClear()

        val loaded = productRepo.findByIdOrNull(product.id)!!
        loaded shouldBeEqualTo product
        loaded.images shouldBeEqualTo setOf(image1, image2, image3)

        val imageToRemove = image2
        loaded.removeImages(imageToRemove)
        productRepo.save(loaded)
        flushAndClear()

        val loaded2 = productRepo.findByIdOrNull(product.id)!!
        loaded2 shouldBeEqualTo product
        loaded2.images shouldBeEqualTo setOf(image1, image3)

        productRepo.delete(loaded2)
        flushAndClear()

        productRepo.existsById(product.id!!).shouldBeFalse()
    }
}

@Entity(name = "onetomany_bidding_item")
@Access(AccessType.FIELD)
class BiddingItem(val name: String): IntJpaEntity() {

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    val bids: MutableSet<Bid> = mutableSetOf()

    fun addBids(vararg bidsToAdd: Bid) {
        bidsToAdd.forEach {
            if (bids.add(it)) {
                it.item = this
            }
        }
    }

    fun removeBids(vararg bidsToRemove: Bid) {
        bidsToRemove.forEach {
            if (bids.remove(it)) {
                it.item = null
            }
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is BiddingItem && name == other.name
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

@Entity(name = "onetomany_bid")
@Access(AccessType.FIELD)
class Bid(val amount: BigDecimal = BigDecimal.ZERO): IntJpaEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    var item: BiddingItem? = null

    @Transient
    var timestamp: Timestamp? = null

    override fun equalProperties(other: Any): Boolean {
        return other is Bid && amount == other.amount
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("amount", amount)
    }
}

@Entity(name = "onetomany_product")
@Access(AccessType.FIELD)
class Product(val name: String): IntJpaEntity() {

    var description: String? = null
    var initialPrice: BigDecimal? = null
    var reservePrice: BigDecimal? = null
    var startDate: LocalDate? = null
    var endDate: LocalDate? = null

    @Enumerated
    var status: ProductStatus = ProductStatus.ACTIVE

    // ElementCollection의 모든 요소는 Entity가 아니라 Component이므로, 독립적으로 관리되지 않고, 컬렉션 전체가 삭제/추가가 된다.
    @CollectionTable(name = "onetomany_product_image_set", joinColumns = [JoinColumn(name = "product_id")])
    @ElementCollection(targetClass = ProductImage::class, fetch = FetchType.EAGER)
    val images: MutableSet<ProductImage> = mutableSetOf()

    fun addImages(vararg imagesToAdd: ProductImage) {
        imagesToAdd.forEach {
            if (images.add(it)) {
                it.product = this
            }
        }
    }

    fun removeImages(vararg imagesToRemove: ProductImage) {
        imagesToRemove.forEach {
            if (images.remove(it)) {
                it.product = null
            }
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is Product && name == other.name
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

enum class ProductStatus {
    UNKNOWN,
    ACTIVE,
    INACTIVE,
    SOLD_OUT,
}

@Embeddable
data class ProductImage(
    @Column(nullable = false)
    val name: String,
): Serializable {

    // NOTE: 소유자를 지정합니다.
    @Parent
    var product: Product? = null

    var filename: String? = null
    var sizeX: Int? = null
    var sizeY: Int? = null
}

interface BiddingItemRepository: JpaRepository<BiddingItem, Int>
interface BidRepository: JpaRepository<Bid, Int>
interface ProductRepository: JpaRepository<Product, Int>
