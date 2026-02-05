package io.bluetape4k.exposed.r2dbc.shared.dml

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import java.math.BigDecimal

object DMLTestData {

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS cities (
     *      city_id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Cities: Table() {
        val id = integer("city_id").autoIncrement()
        val name = varchar("name", 50)

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS users (
     *      id VARCHAR(10) PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      city_id INT NULL,
     *      flags INT DEFAULT 0 NOT NULL,
     *
     *      CONSTRAINT fk_users_city_id__city_id FOREIGN KEY (city_id) REFERENCES cities(city_id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Users: Table() {
        val id = varchar("id", 10)
        val name = varchar("name", 50)
        val cityId = optReference("city_id", Cities.id)
        val flags = integer("flags").default(0)

        override val primaryKey = PrimaryKey(id)

        object Flags {
            const val IS_ADMIN = 0b1
            const val HAS_DATA = 0b1000
        }
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS userdata (
     *      user_id VARCHAR(10) NOT NULL,
     *      "comment" VARCHAR(30) NOT NULL,
     *      "value" INT NOT NULL,
     *
     *      CONSTRAINT fk_userdata_user_id__id FOREIGN KEY (user_id) REFERENCES users(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object UserData: Table() {
        val userId = reference("user_id", Users.id)
        val comment = varchar("comment", 30)
        val value = integer("value")
    }

    /**
     * Postgres:
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS sales (
     *      "year" INT NOT NULL,
     *      "month" INT NOT NULL,
     *      product VARCHAR(30) NULL,
     *      amount DECIMAL(8, 2) NOT NULL
     * )
     * ```
     */
    object Sales: Table() {
        val year = integer("year")
        val month = integer("month")
        val product = varchar("product", 30).nullable()
        val amount = decimal("amount", 8, 2)
    }

    /**
     * Postgres:
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS someamounts (
     *      amount DECIMAL(8, 2) NOT NULL
     * )
     * ```
     */
    object SomeAmounts: Table() {
        val amount = decimal("amount", 8, 2)
    }


    fun Iterable<ResultRow>.toCityNameList(): List<String> =
        map { it[Cities.name] }

    suspend fun Flow<ResultRow>.toCityNameList(): List<String> =
        map { it[Cities.name] }.toFastList()


    @Suppress("UnusedReceiverParameter")
    suspend fun AbstractExposedR2dbcTest.withCitiesAndUsers(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.(
            cities: Cities,
            users: Users,
            userData: UserData,
        ) -> Unit,
    ) {
        val users = Users
        // val userFlags = Flags
        val cities = Cities
        val userData = UserData

        withTables(testDB, cities, users, userData) {
            val saintPetersburgId = cities.insert {
                it[name] = "St. Petersburg"
            } get Cities.id

            val munichId = cities.insert {
                it[name] = "Munich"
            } get Cities.id

            cities.insert {
                it[name] = "Prague"
            }

            users.insert {
                it[id] = "andrey"
                it[name] = "Andrey"
                it[cityId] = saintPetersburgId
                it[flags] = Users.Flags.IS_ADMIN
            }

            users.insert {
                it[id] = "sergey"
                it[name] = "Sergey"
                it[cityId] = munichId
                it[flags] = Users.Flags.IS_ADMIN or Users.Flags.HAS_DATA
            }

            users.insert {
                it[id] = "eugene"
                it[name] = "Eugene"
                it[cityId] = munichId
                it[flags] = Users.Flags.HAS_DATA
            }

            users.insert {
                it[id] = "alex"
                it[name] = "Alex"
                it[cityId] = null
            }

            users.insert {
                it[id] = "smth"
                it[name] = "Something"
                it[cityId] = null
                it[flags] = Users.Flags.HAS_DATA
            }

            userData.insert {
                it[userId] = "smth"
                it[comment] = "Something is here"
                it[value] = 10
            }

            userData.insert {
                it[userId] = "smth"
                it[comment] = "Comment #2"
                it[value] = 20
            }

            userData.insert {
                it[userId] = "eugene"
                it[comment] = "Comment for Eugene"
                it[value] = 20
            }

            userData.insert {
                it[userId] = "sergey"
                it[comment] = "Comment for Sergey"
                it[value] = 30
            }

            statement(cities, users, userData)
        }
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun AbstractExposedR2dbcTest.withSales(
        dialect: TestDB,
        statement: suspend R2dbcTransaction.(testDB: TestDB, sales: Sales) -> Unit,
    ) {
        val sales = Sales

        withTables(dialect, sales) { testDB ->
            insertSale(2018, 11, "tea", "550.10")
            insertSale(2018, 12, "coffee", "1500.25")
            insertSale(2018, 12, "tea", "900.30")
            insertSale(2019, 1, "coffee", "1620.10")
            insertSale(2019, 1, "tea", "650.70")
            insertSale(2019, 2, "coffee", "1870.90")
            insertSale(2019, 2, null, "10.20")

            statement(testDB, sales)
        }
    }

    private suspend fun insertSale(year: Int, month: Int, product: String?, amount: String) {
        val sales = Sales
        sales.insert {
            it[Sales.year] = year
            it[Sales.month] = month
            it[Sales.product] = product
            it[Sales.amount] = amount.toBigDecimal()
        }
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun AbstractExposedR2dbcTest.withSomeAmounts(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.(testDB: TestDB, someAmounts: SomeAmounts) -> Unit,
    ) {
        val someAmounts = SomeAmounts

        suspend fun insertAmount(amount: BigDecimal) {
            someAmounts.insert {
                it[SomeAmounts.amount] = amount
            }
        }

        withTables(testDB, someAmounts) {
            insertAmount("650.70".toBigDecimal())
            insertAmount("1500.25".toBigDecimal())
            insertAmount("1000.00".toBigDecimal())

            statement(it, someAmounts)
        }
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun AbstractExposedR2dbcTest.withSalesAndSomeAmounts(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.(
            testDB: TestDB,
            sales: Sales,
            someAmounts: SomeAmounts,
        ) -> Unit,
    ) {
        val sales = Sales
        val someAmounts = SomeAmounts

        withTables(testDB, sales, someAmounts) { testDB ->
            insertSale(2018, 11, "tea", "550.10")
            insertSale(2018, 12, "coffee", "1500.25")
            insertSale(2018, 12, "tea", "900.30")
            insertSale(2019, 1, "coffee", "1620.10")
            insertSale(2019, 1, "tea", "650.70")
            insertSale(2019, 2, "coffee", "1870.90")
            insertSale(2019, 2, null, "10.20")

            suspend fun insertAmount(amount: BigDecimal) {
                someAmounts.insert {
                    it[SomeAmounts.amount] = amount
                }
            }
            insertAmount("650.70".toBigDecimal())
            insertAmount("1500.25".toBigDecimal())
            insertAmount("1000.00".toBigDecimal())

            statement(testDB, sales, someAmounts)
        }
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS orgs (
     *      id SERIAL PRIMARY KEY,
     *      uid VARCHAR(36) NOT NULL,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ALTER TABLE orgs ADD CONSTRAINT orgs_uid_unique UNIQUE (uid)
     * ```
     */
    object Orgs: IntIdTable() {
        val uid = varchar("uid", 36)
            .clientDefault { TimebasedUuid.Epoch.nextIdAsString() }
            .uniqueIndex()

        val name = varchar("name", 255)
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS orgmemberships (
     *      id SERIAL PRIMARY KEY,
     *      org VARCHAR(36) NOT NULL,
     *
     *      CONSTRAINT fk_orgmemberships_org__uid FOREIGN KEY (org) REFERENCES orgs(uid)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object OrgMemberships: IntIdTable() {
        val orgId = reference("org", Orgs.uid)
    }

}
