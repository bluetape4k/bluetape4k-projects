package io.bluetape4k.hibernate.mapping.associations.manytomany

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.logging.KLogging
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType.MERGE
import jakarta.persistence.CascadeType.PERSIST
import jakarta.persistence.CascadeType.REFRESH
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.validation.constraints.NotBlank
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull

class ManyToManyTest(
    @param:Autowired private val accountRepo: BankAccountRepository,
    @param:Autowired private val ownerRepo: AccountOwnerRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `many-to-many manipulation by owner`() {
        val owner1 = AccountOwner(faker.idNumber().ssnValid())
        val owner2 = AccountOwner(faker.idNumber().ssnValid())

        val account1 = BankAccount(faker.finance().creditCard())
        val account2 = BankAccount(faker.finance().creditCard())
        val account3 = BankAccount(faker.finance().creditCard())
        val account4 = BankAccount(faker.finance().creditCard())

        owner1.addAccounts(account1, account2)
        owner2.addAccounts(account1, account3, account4)

        ownerRepo.saveAll(listOf(owner1, owner2))
        flushAndClear()

        var loaded1 = ownerRepo.findByIdOrNull(owner1.id!!)!!
        loaded1.accounts.size shouldBeEqualTo owner1.accounts.size
        loaded1.accounts shouldContainSame listOf(account1, account2)

        var loaded2 = ownerRepo.findByIdOrNull(owner2.id!!)!!
        loaded2.accounts.size shouldBeEqualTo owner2.accounts.size
        loaded2.accounts shouldContainSame listOf(account1, account3, account4)

        loaded2.removeAccounts(account3)
        ownerRepo.save(loaded2)
        flushAndClear()

        loaded1 = ownerRepo.findByIdOrNull(owner1.id!!)!!
        loaded1.accounts.size shouldBeEqualTo owner1.accounts.size
        loaded1.accounts shouldContainSame listOf(account1, account2)

        // loaded2 에서는 account3을 삭제했다
        loaded2 = ownerRepo.findByIdOrNull(owner2.id!!)!!
        loaded2.accounts.size shouldBeEqualTo owner2.accounts.size - 1
        loaded2.accounts shouldContainSame listOf(account1, account4)

        //
        // cascade 에 REMOVE 가 빠져 있다면, many-to-many 관계만 삭제된다.

        ownerRepo.delete(loaded2)
        flushAndClear()

        val removedAccount = accountRepo.findByIdOrNull(account3.id!!)!!
        removedAccount.owners.shouldBeEmpty()
    }

    @Test
    fun `many-to-many manipulatation by account`() {
        val owner1 = AccountOwner(faker.idNumber().ssnValid())
        val owner2 = AccountOwner(faker.idNumber().ssnValid())

        val account1 = BankAccount(faker.finance().creditCard())
        val account2 = BankAccount(faker.finance().creditCard())
        val account3 = BankAccount(faker.finance().creditCard())
        val account4 = BankAccount(faker.finance().creditCard())

        owner1.addAccounts(account1, account2)
        owner2.addAccounts(account1, account3, account4)

        accountRepo.saveAll(listOf(account1, account2, account3, account4))
        flushAndClear()

        verifyExistsAccount(account1)
        verifyExistsAccount(account2)
        verifyExistsAccount(account3)
        verifyExistsAccount(account4)


        // NOTE: many-to-many 관계를 끊으려면 @JoinTable 를 정의한 entity를 갱신해야 join table에서 관계를 삭제합니다.
        account1.removeOwners(owner2)
        ownerRepo.save(owner2)
        // accountRepo.save(account1) // 이 것은 없어도 된다.
        flushAndClear()

        val loaded = ownerRepo.findByIdOrNull(owner2.id!!)!!
        loaded.accounts.size shouldBeEqualTo 2

    }

    private fun verifyExistsAccount(account: BankAccount) {
        val loaded = accountRepo.findByIdOrNull(account.id!!)!!
        loaded shouldBeEqualTo account
        loaded.owners.size shouldBeEqualTo account.owners.size
    }
}

@Entity(name = "manytomany_bank_account")
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class BankAccount(val number: String): IntJpaEntity() {

    // NOTE: many-to-many 관계에서는 cascade에 REMOVE, DETACH를 포함시키면 상대 entity도 삭제된다.
    @ManyToMany(mappedBy = "accounts", cascade = [PERSIST, MERGE, REFRESH], fetch = EAGER)
    val owners: MutableSet<AccountOwner> = mutableSetOf()

    fun addOwners(vararg owners: AccountOwner) {
        owners.forEach {
            if (this.owners.add(it)) {
                it.accounts.add(this)
            }
        }
    }

    fun removeOwners(vararg owners: AccountOwner) {
        owners.forEach {
            if (this.owners.remove(it)) {
                it.accounts.remove(this)
            }
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is BankAccount && number == other.number
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: number.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("number", number)
    }
}

@Entity(name = "manytomany_account_owner")
@Access(AccessType.FIELD)
class AccountOwner(@field:NotBlank val ssn: String): IntJpaEntity() {

    // NOTE: many-to-many 관계에서는 cascade에 REMOVE, DETACH를 포함시키면 상대 entity도 삭제된다.
    @ManyToMany(cascade = [PERSIST, MERGE, REFRESH], fetch = LAZY)
    @JoinTable(
        name = "account_owner_bank_account_map",
        joinColumns = [JoinColumn(name = "owner_id")],
        inverseJoinColumns = [JoinColumn(name = "account_id")]
    )
    val accounts: MutableSet<BankAccount> = mutableSetOf()

    fun addAccounts(vararg accounts: BankAccount) {
        accounts.forEach {
            if (this.accounts.add(it)) {
                it.owners.add(this)
            }
        }
    }

    fun removeAccounts(vararg accounts: BankAccount) {
        accounts.forEach {
            if (this.accounts.remove(it)) {
                it.owners.remove(this)
            }
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is AccountOwner && ssn == other.ssn
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: ssn.hashCode()
    }


    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("ssn", ssn)
    }
}

interface BankAccountRepository: JpaRepository<BankAccount, Int>
interface AccountOwnerRepository: JpaRepository<AccountOwner, Int>
