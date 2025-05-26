package io.bluetape4k.exposed.r2dbc.shared.samples

import io.bluetape4k.exposed.r2dbc.tests.R2dbcExposedTestBase
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.faker.Fakers
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll

/**
 * 은행 계좌 - 계좌 소유자에 대한 Many-to-Many 관계를 나타내는 스키마
 */
object BankSchema {

    val allTables = arrayOf(BankAccountTable, AccountOwnerTable, OwnerAccountMapTable)

    private val faker = Fakers.faker

    /**
     * 은행 계좌 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS bank_account (
     *      id SERIAL PRIMARY KEY,
     *      "number" VARCHAR(255) NOT NULL
     * );
     *
     * ALTER TABLE bank_account
     *   ADD CONSTRAINT bank_account_number_unique UNIQUE ("number");
     * ```
     */
    object BankAccountTable: IntIdTable("bank_account") {
        val number = varchar("number", 255).uniqueIndex()
    }

    /**
     * 계좌 소유자 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS account_owner (
     *      id SERIAL PRIMARY KEY,
     *      ssn VARCHAR(255) NOT NULL
     * );
     *
     * ALTER TABLE account_owner
     *   ADD CONSTRAINT account_owner_ssn_unique UNIQUE (ssn);
     * ```
     */
    object AccountOwnerTable: IntIdTable("account_owner") {
        val ssn = varchar("ssn", 255).uniqueIndex()
    }

    /**
     * 은행 계좌 - 계좌 소유자에 대한 Many-to-Many Mapping Table
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS owner_account_map (
     *      owner_id INT NOT NULL,
     *      account_id INT NOT NULL,
     *
     *      CONSTRAINT fk_owner_account_map_owner_id__id FOREIGN KEY (owner_id)
     *          REFERENCES account_owner(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_owner_account_map_account_id__id FOREIGN KEY (account_id)
     *          REFERENCES bank_account(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE owner_account_map
     *      ADD CONSTRAINT owner_account_map_owner_id_account_id_unique UNIQUE (owner_id, account_id);
     * ```
     */
    object OwnerAccountMapTable: Table("owner_account_map") {
        val ownerId = reference("owner_id", AccountOwnerTable)
        val accountId = reference("account_id", BankAccountTable)

        init {
            uniqueIndex(ownerId, accountId)
        }
    }

    data class BankAccountRecord(
        val id: Int,
        val number: String,
    )

    data class AccountOwnerRecord(
        val id: Int,
        val ssn: String,
    )

    @Suppress("UnusedReceiverParameter")
    suspend fun R2dbcExposedTestBase.withBankTables(
        testDB: TestDB,
        block: suspend R2dbcTransaction.(accounts: BankAccountTable, owners: AccountOwnerTable) -> Unit,
    ) {
        withTables(testDB, *allTables) {
            val owner1 = createAccountOwner(faker.idNumber().ssnValid())
            val owner2 = createAccountOwner(faker.idNumber().ssnValid())
            val account1 = createBankAccount(faker.finance().creditCard())
            val account2 = createBankAccount(faker.finance().creditCard())
            val account3 = createBankAccount(faker.finance().creditCard())
            val account4 = createBankAccount(faker.finance().creditCard())

            createOwnerAccountMap(owner1, account1)
            createOwnerAccountMap(owner1, account2)
            createOwnerAccountMap(owner2, account1)
            createOwnerAccountMap(owner2, account3)
            createOwnerAccountMap(owner2, account4)

            block(BankAccountTable, AccountOwnerTable)
        }
    }

    private suspend fun createAccountOwner(ssn: String): EntityID<Int> =
        AccountOwnerTable.insertAndGetId { it[AccountOwnerTable.ssn] = ssn }

    private suspend fun createBankAccount(number: String): EntityID<Int> =
        BankAccountTable.insertAndGetId { it[BankAccountTable.number] = number }

    private suspend fun createOwnerAccountMap(ownerId: EntityID<Int>, accountId: EntityID<Int>) {
        OwnerAccountMapTable.insert {
            it[OwnerAccountMapTable.ownerId] = ownerId
            it[OwnerAccountMapTable.accountId] = accountId
        }
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun R2dbcTransaction.getAccount(accountId: Int): BankAccountRecord? =
        BankAccountTable
            .selectAll()
            .where { BankAccountTable.id eq accountId }
            .singleOrNull()
            ?.let {
                BankAccountRecord(
                    id = it[BankAccountTable.id].value,
                    number = it[BankAccountTable.number]
                )
            }

    @Suppress("UnusedReceiverParameter")
    suspend fun R2dbcTransaction.getOwner(ownerId: Int): AccountOwnerRecord? =
        AccountOwnerTable
            .selectAll()
            .where { AccountOwnerTable.id eq ownerId }
            .singleOrNull()
            ?.let {
                AccountOwnerRecord(
                    id = it[AccountOwnerTable.id].value,
                    ssn = it[AccountOwnerTable.ssn]
                )
            }
}
