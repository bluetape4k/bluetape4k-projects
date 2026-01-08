package io.bluetape4k.exposed.shared.mapping

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.exposed.shared.mapping.PersonSchema.Address
import io.bluetape4k.exposed.shared.mapping.PersonSchema.AddressTable
import io.bluetape4k.exposed.shared.mapping.PersonSchema.Person
import io.bluetape4k.exposed.shared.mapping.PersonSchema.PersonTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.time.LocalDate

object PersonSchema {

    val allPersonTables = arrayOf(AddressTable, PersonTable)

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS addresses (
     *      id BIGSERIAL PRIMARY KEY,
     *      street VARCHAR(255) NOT NULL,
     *      city VARCHAR(255) NOT NULL,
     *      "state" VARCHAR(2) NOT NULL, zip VARCHAR(10) NULL
     * );
     * ```
     */
    object AddressTable: LongIdTable("addresses") {
        val street = varchar("street", 255)
        val city = varchar("city", 255)
        val state = varchar("state", 2)
        val zip = varchar("zip", 10).nullable()
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS persons (
     *      id BIGSERIAL PRIMARY KEY,
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
     */
    object PersonTable: LongIdTable("persons") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val birthDate = date("birth_date")
        val employeed = bool("employeed").default(true)
        val occupation = varchar("occupation", 255).nullable()
        val addressId = reference("address_id", AddressTable)  // many to one
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

    class Address(id: EntityID<Long>): LongEntity(id), java.io.Serializable {
        companion object: LongEntityClass<Address>(AddressTable)

        var street by AddressTable.street
        var city by AddressTable.city
        var state by AddressTable.state
        var zip by AddressTable.zip

        override fun toString(): String = ToStringBuilder(this)
            .add("street", street)
            .add("city", city)
            .add("state", state)
            .add("zip", zip)
            .toString()
    }

    class Person(id: EntityID<Long>): LongEntity(id), java.io.Serializable {
        companion object: LongEntityClass<Person>(PersonTable)

        var firstName by PersonTable.firstName
        var lastName by PersonTable.lastName
        var birthDate by PersonTable.birthDate
        var employeed by PersonTable.employeed
        var occupation by PersonTable.occupation
        var address by Address referencedOn PersonTable.addressId

        override fun toString(): String = ToStringBuilder(this)
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("birthDate", birthDate)
            .add("employeed", employeed)
            .add("occupation", occupation)
            .add("address", address)
            .toString()
    }

    data class PersonRecord(
        val id: Long? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val birthDate: java.time.LocalDate? = null,
        val employeed: Boolean? = null,
        val occupation: String? = null,
        val address: Long? = null,
    ): java.io.Serializable

    data class PersonWithAddress(
        var id: Long? = null,
        var firstName: String? = null,
        var lastName: String? = null,
        var birthDate: java.time.LocalDate? = null,
        var employeed: Boolean? = null,
        var occupation: String? = null,
        var address: Address? = null,
    ): java.io.Serializable

    fun withPersons(
        testDB: TestDB,
        block: JdbcTransaction.(PersonTable, AddressTable) -> Unit,
    ) {
        withTables(testDB, *allPersonTables) {
            block(PersonTable, AddressTable)
        }
    }
}


@Suppress("UnusedReceiverParameter")
fun AbstractExposedTest.withPersonsAndAddress(
    testDB: TestDB,
    statement: JdbcTransaction.(
        persons: PersonTable,
        addresses: AddressTable,
    ) -> Unit,
) {
    val persons = PersonTable
    val addresses = AddressTable

    withTables(testDB, *PersonSchema.allPersonTables) {

        val addr1 = Address.new {
            street = "123 Main St"
            city = "Bedrock"
            state = "IN"
            zip = "12345"
        }
        val addr2 = Address.new {
            street = "456 Elm St"
            city = "Bedrock"
            state = "IN"
            zip = "12345"
        }
        flushCache()

        Person.new {
            firstName = "Fred"
            lastName = "Flintstone"
            birthDate = LocalDate.of(1935, 2, 1)
            employeed = true
            occupation = "Brontosaurus Operator"
            address = addr1
        }
        Person.new {
            firstName = "Wilma"
            lastName = "Flintstone"
            birthDate = LocalDate.of(1940, 2, 1)
            employeed = false
            occupation = "Accountant"
            address = addr1
        }
        Person.new {
            firstName = "Pebbles"
            lastName = "Flintstone"
            birthDate = LocalDate.of(1960, 5, 6)
            employeed = false
            address = addr1
        }
        flushCache()
        Person.new {
            firstName = "Barney"
            lastName = "Rubble"
            birthDate = LocalDate.of(1937, 2, 1)
            employeed = true
            occupation = "Brontosaurus Operator"
            address = addr2
        }
        Person.new {
            firstName = "Betty"
            lastName = "Rubble"
            birthDate = LocalDate.of(1943, 2, 1)
            employeed = false
            occupation = "Engineer"
            address = addr2
        }
        Person.new {
            firstName = "Bamm Bamm"
            lastName = "Rubble"
            birthDate = LocalDate.of(1963, 7, 8)
            employeed = false
            address = addr2
        }
        flushCache()

        statement(persons, addresses)
    }
}
