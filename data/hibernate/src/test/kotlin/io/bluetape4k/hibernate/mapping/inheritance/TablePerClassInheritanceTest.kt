package io.bluetape4k.hibernate.mapping.inheritance

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.AbstractJpaEntity
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
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

class TablePerClassInheritanceTest: AbstractHibernateTest() {
}


@Access(AccessType.FIELD)
@Entity(name = "tableperclass_billing")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(
    name = "tableperclass_billing",
    indexes = [
        Index(name = "ix_tableperclass_billing_owner", columnList = "owner")
    ]
)
abstract class AbstractUuidBilling(
    @Id
    @GeneratedValue(generator = "uuid")
    override var id: UUID? = null,

    val owner: String = "",
): AbstractJpaEntity<UUID>() {

    override fun equalProperties(other: Any): Boolean =
        other is AbstractUuidBilling && id == other.id && owner == other.owner

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("owner", owner)
    }
}

@Entity(name = "tableperclass_creditcard")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class UuidCreditCard(owner: String): AbstractUuidBilling(owner = owner) {

    var number: String? = null

    var companyName: String? = null
    var expMonth: Int? = null
    var expYear: Int? = null

    @Temporal(TemporalType.TIMESTAMP)
    var startDate: Date? = null

    @Temporal(TemporalType.TIMESTAMP)
    var endDate: Date? = null

    var swift: String? = null

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

@Entity(name = "tableperclass_bankaccount")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class UuidBankAccount(owner: String): AbstractUuidBilling(owner = owner) {
    var account: String? = null
    var bankname: String? = null
    var swift: String? = null

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
