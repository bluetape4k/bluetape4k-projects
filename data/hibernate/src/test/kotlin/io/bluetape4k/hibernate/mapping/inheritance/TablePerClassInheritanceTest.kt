package io.bluetape4k.hibernate.mapping.inheritance

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.AbstractJpaEntity
import io.bluetape4k.support.uninitialized
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class TablePerClassInheritanceTest: AbstractHibernateTest() {

    @Autowired
    private val cardRepo: TablePerClassCreditCardRepository = uninitialized()

    @Autowired
    private val accountRepo: TablePerClassBankAccountRepository = uninitialized()

    @Test
    fun `entity has its own table`() {

        val account = UuidBankAccount("debop").apply {
            account = "123-accunt"
            bankname = "KB"
            swift = "kb-swift"
        }
        accountRepo.save(account)

        val card = UuidCreditCard("debop").apply {
            number = "1111-2222-3333-4444"
            companyName = "KakaoBank"
            expYear = 2024
            expMonth = 12
            startDate = Date()
        }

        cardRepo.save(card)

        flushAndClear()

        val account2 = accountRepo.findByIdOrNull(account.id)!!
        account2 shouldBeEqualTo account

        val card2 = cardRepo.findByIdOrNull(card.id)!!
        card2 shouldBeEqualTo card

        accountRepo.deleteAll()
        accountRepo.flush()

        // 다른 테이블이므로, bank account 정보가 삭제되어도 credit card 정보는 남아 있어야 한다.
        cardRepo.findAll().shouldNotBeEmpty()

        cardRepo.deleteAll()
        flushAndClear()
        cardRepo.findAll().shouldBeEmpty()
    }
}


@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
abstract class AbstractUuidBilling(
    @Id
    @GeneratedValue(generator = "uuid")
    override var id: UUID? = null,
    val owner: String = "",
    var swift: String? = null,
): AbstractJpaEntity<UUID>() {

    override fun equalProperties(other: Any): Boolean =
        other is AbstractUuidBilling && id == other.id && owner == other.owner

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("owner", owner)
    }
}

/**
 * ```sql
 * create table tableperclass_creditcard (
 *         id uuid not null,
 *         owner varchar(255),
 *         company_name varchar(255),
 *         end_date timestamp(6),
 *         exp_month integer,
 *         exp_year integer,
 *         number varchar(255),
 *         start_date timestamp(6),
 *         swift varchar(255),
 *         primary key (id)
 *     )
 * ```
 */
@Entity
@Table(
    name = "tableperclass_creditcard",
    indexes = [
        Index(name = "ix_tableperclass_creditcard_owner", columnList = "owner")
    ]
)
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class UuidCreditCard(owner: String, swift: String? = null): AbstractUuidBilling(owner = owner, swift = swift) {

    var number: String? = null

    var companyName: String? = null
    var expMonth: Int? = null
    var expYear: Int? = null

    @Temporal(TemporalType.TIMESTAMP)
    var startDate: Date? = null

    @Temporal(TemporalType.TIMESTAMP)
    var endDate: Date? = null


    override fun equalProperties(other: Any): Boolean =
        other is UuidCreditCard && number == other.number && super.equalProperties(other)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("number", number)
            .add("companyName", companyName)
            .add("expMonth", expMonth)
            .add("expYear", expYear)
    }
}

/**
 * ```sql
 * create table tableperclass_bankaccount (
 *         id uuid not null,
 *         owner varchar(255),
 *         account varchar(255),
 *         bankname varchar(255),
 *         swift varchar(255),
 *         primary key (id)
 *     )
 * ```
 */
@Entity
@Table(
    name = "tableperclass_bankaccount",
    indexes = [
        Index(name = "ix_tableperclass_bankaccount_owner", columnList = "owner")
    ]
)
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class UuidBankAccount(owner: String, swift: String? = null): AbstractUuidBilling(owner = owner, swift = swift) {
    var account: String? = null
    var bankname: String? = null

    override fun equalProperties(other: Any): Boolean =
        other is UuidBankAccount && bankname == other.bankname && super.equalProperties(other)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("account", account)
            .add("bankname", bankname)
            .add("swift", swift)
    }
}

interface TablePerClassCreditCardRepository: JpaRepository<UuidCreditCard, UUID>

interface TablePerClassBankAccountRepository: JpaRepository<UuidBankAccount, UUID>
