package io.bluetape4k.exposed.r2dbc.shared.mapping

import io.bluetape4k.exposed.r2dbc.tests.R2dbcExposedTestBase
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import java.io.Serializable
import java.time.LocalDate

object PersonSchema {

    val allPersonTables = arrayOf(AddressTable, PersonTable)

    /**
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS addresses (
     *      id BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      street VARCHAR(255) NOT NULL,
     *      city VARCHAR(255) NOT NULL,
     *      `state` VARCHAR(32) NOT NULL,
     *      zip VARCHAR(10) NULL
     * );
     *
     * CREATE INDEX idx_address_city_zip ON addresses (city, zip, id)
     *
     * ```
     */
    object AddressTable: LongIdTable("addresses") {
        val street = varchar("street", 255)
        val city = varchar("city", 255)
        val state = varchar("state", 32)
        val zip = varchar("zip", 10).nullable()

        init {
            index("idx_address_city_zip", false, city, zip, id)
        }
    }

    /**
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS persons (
     *      id BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      first_name VARCHAR(50) NOT NULL,
     *      last_name VARCHAR(50) NOT NULL,
     *      birth_date DATE NOT NULL,
     *      employeed BOOLEAN DEFAULT TRUE NOT NULL,
     *      occupation VARCHAR(255) NULL,
     *      address_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_persons_address_id__id FOREIGN KEY (address_id)
     *      REFERENCES addresses(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE persons ADD CONSTRAINT idx_person_addr UNIQUE (id, address_id)
     * ```
     */
    object PersonTable: LongIdTable("persons") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val birthDate = date("birth_date")
        val employeed = bool("employeed").default(true)
        val occupation = varchar("occupation", 255).nullable()
        val addressId = reference("address_id", AddressTable)  // many to one

        init {
            uniqueIndex("idx_person_addr", id, addressId)
        }
    }

    /**
     * INSERT SELECT 등 SQL 만을 위해서 사용하기 위한 테이블 정의.
     *
     * `PersonTable`은 `Person` 엔티티를 위한 테이블이다. 하지만 같은 테이블을 바라보고 있다.
     */
    object PersonTableDML: Table("persons") {
        val id = long("id")   // autoIncrement() 를 지정하면 insert select 같은 id 에 값을 지정하는 것이 불가능하다.
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val birthDate = date("birth_date")
        val employeed = bool("employeed").default(true)
        val occupation = varchar("occupation", 255).nullable()
        val addressId = long("address_id")  // many to one

        override val primaryKey = PrimaryKey(id)
    }

    data class PersonRecord(
        val id: Long? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val birthDate: LocalDate? = null,
        val employeed: Boolean? = null,
        val occupation: String? = null,
        val address: Long? = null,
    ): Serializable

    data class PersonWithAddress(
        var id: Long? = null,
        var firstName: String? = null,
        var lastName: String? = null,
        var birthDate: LocalDate? = null,
        var employeed: Boolean? = null,
        var occupation: String? = null,
        var address: AddressRecord? = null,
    ): Serializable

    data class AddressRecord(
        val id: Long? = null,
        val street: String? = null,
        val city: String? = null,
        val state: String? = null,
        val zip: String? = null,
    ): Serializable

    @Suppress("UnusedReceiverParameter")
    suspend fun R2dbcExposedTestBase.withPersons(
        testDB: TestDB,
        block: suspend R2dbcTransaction.(PersonTable, AddressTable) -> Unit,
    ) {
        withTables(testDB, *allPersonTables) {
            block(PersonTable, AddressTable)
        }
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun R2dbcExposedTestBase.withPersonsAndAddress(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.(
            persons: PersonSchema.PersonTable,
            addresses: PersonSchema.AddressTable,
        ) -> Unit,
    ) {
        val persons = PersonSchema.PersonTable
        val addresses = PersonSchema.AddressTable

        withTables(testDB, *PersonSchema.allPersonTables) {

            val addr1 = AddressTable.insertAndGetId {
                it[street] = "123 Main St"
                it[city] = "Bedrock"
                it[state] = "IN"
                it[zip] = "12345"
            }
            val addr2 = AddressTable.insertAndGetId {
                it[street] = "456 Elm St"
                it[city] = "Bedrock"
                it[state] = "IN"
                it[zip] = "12345"
            }

            val addr3 = AddressTable.insertAndGetId {
                it[street] = "165 Kangnam-daero"
                it[city] = "Seoul"
                it[state] = "Seoul"
                it[zip] = "11111"
            }

            PersonTable.insert {
                it[firstName] = "Fred"
                it[lastName] = "Flintstone"
                it[birthDate] = LocalDate.of(1935, 2, 1)
                it[employeed] = true
                it[occupation] = "Brontosaurus Operator"
                it[addressId] = addr1
            }
            PersonTable.insert {
                it[firstName] = "Wilma"
                it[lastName] = "Flintstone"
                it[birthDate] = LocalDate.of(1940, 2, 1)
                it[employeed] = false
                it[occupation] = "Accountant"
                it[addressId] = addr1
            }
            PersonTable.insert {
                it[firstName] = "Pebbles"
                it[lastName] = "Flintstone"
                it[birthDate] = LocalDate.of(1960, 5, 6)
                it[employeed] = false
                it[addressId] = addr1
            }

            PersonTable.insert {
                it[firstName] = "Barney"
                it[lastName] = "Rubble"
                it[birthDate] = LocalDate.of(1937, 2, 1)
                it[employeed] = true
                it[occupation] = "Brontosaurus Operator"
                it[addressId] = addr2
            }
            PersonTable.insert {
                it[firstName] = "Betty"
                it[lastName] = "Rubble"
                it[birthDate] = LocalDate.of(1943, 2, 1)
                it[employeed] = false
                it[occupation] = "Engineer"
                it[addressId] = addr2
            }
            PersonTable.insert {
                it[firstName] = "Bamm Bamm"
                it[lastName] = "Rubble"
                it[birthDate] = LocalDate.of(1963, 7, 8)
                it[employeed] = false
                it[addressId] = addr2
            }

            PersonTable.insert {
                it[firstName] = "Sunghyouk"
                it[lastName] = "Bae"
                it[birthDate] = LocalDate.of(1968, 10, 14)
                it[employeed] = false
                it[addressId] = addr3
            }

            PersonTable.insert {
                it[firstName] = "Jehyoung"
                it[lastName] = "Bae"
                it[birthDate] = LocalDate.of(1996, 5, 22)
                it[employeed] = false
                it[addressId] = addr3
            }

            statement(persons, addresses)
        }
    }

}
