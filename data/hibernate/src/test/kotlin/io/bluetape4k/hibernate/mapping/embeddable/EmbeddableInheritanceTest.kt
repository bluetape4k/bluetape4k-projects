package io.bluetape4k.hibernate.mapping.embeddable

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.uninitialized
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull

class EmbeddableInheritanceTest: AbstractHibernateTest() {

    @Autowired
    private val orderRepo: EmbeddableInheritanceOrderRepository = uninitialized()

    @Test
    fun `embeddable inheritance 는 concrete subtype 을 복원한다`() {
        val cardOrder = EmbeddableInheritanceOrder("card").apply {
            payment = CardPaymentDetail("debop").apply {
                cardNumber = "1111-2222-3333-4444"
            }
        }
        val bankOrder = EmbeddableInheritanceOrder("bank").apply {
            payment = BankTransferDetail("bluetape").apply {
                bankCode = "004"
            }
        }

        orderRepo.save(cardOrder)
        orderRepo.save(bankOrder)
        flushAndClear()

        val loadedCardOrder = orderRepo.findByIdOrNull(cardOrder.id!!)!!
        val loadedBankOrder = orderRepo.findByIdOrNull(bankOrder.id!!)!!

        (loadedCardOrder.payment is CardPaymentDetail).shouldBeTrue()
        (loadedBankOrder.payment is BankTransferDetail).shouldBeTrue()

        val loadedCard = loadedCardOrder.payment as CardPaymentDetail
        val loadedBank = loadedBankOrder.payment as BankTransferDetail

        loadedCard.billingName shouldBeEqualTo "debop"
        loadedCard.cardNumber shouldBeEqualTo "1111-2222-3333-4444"
        loadedBank.billingName shouldBeEqualTo "bluetape"
        loadedBank.bankCode shouldBeEqualTo "004"
    }
}

@Entity(name = "embeddable_inheritance_order")
@Table(name = "embeddable_inheritance_order")
class EmbeddableInheritanceOrder(
    var orderName: String = "",
): IntJpaEntity() {

    @Embedded
    var payment: PaymentDetail? = null

    override fun equalProperties(other: Any): Boolean =
        other is EmbeddableInheritanceOrder &&
                orderName == other.orderName
}

@Embeddable
@DiscriminatorColumn(name = "payment_detail_type")
open class PaymentDetail(
    var billingName: String = "",
)

@Embeddable
@DiscriminatorValue("CARD")
class CardPaymentDetail(
    billingName: String = "",
): PaymentDetail(billingName) {
    var cardNumber: String = ""
}

@Embeddable
@DiscriminatorValue("BANK")
class BankTransferDetail(
    billingName: String = "",
): PaymentDetail(billingName) {
    var bankCode: String = ""
}

interface EmbeddableInheritanceOrderRepository: JpaRepository<EmbeddableInheritanceOrder, Int>
