package io.bluetape4k.vertx.sqlclient.schema

import org.mybatis.dynamic.sql.AliasableSqlTable
import org.mybatis.dynamic.sql.util.kotlin.elements.column
import java.io.Serializable
import java.time.LocalDate


object PersonSchema {
    val address = AddressTable()
    val person = PersonTable()

    class AddressTable: AliasableSqlTable<AddressTable>("Address", PersonSchema::AddressTable) {
        val id = column<Int>(name = "address_id")
        val streetAddress = column<String>(name = "street_address")
        val city = column<String>(name = "city")
        val state = column<String>(name = "state")
    }

    /**
     * [AliasableSqlTable] 로 설정해야 alias 를 이용하여 같은 테이블를 구분할 수 있다
     *
     * ```
     * select(person.id, "p2"(person.firstName)) {
     *      from(person)
     *      innerJoin(
     *          {
     *          }
     *          + "p2
     *      ) {
     *          on(person.id) equalTo "p2"(person.id)
     *      }
     *      where { person.id isLessThan 5 }
     * }
     * ```
     */
    class PersonTable: AliasableSqlTable<PersonTable>("Person", PersonSchema::PersonTable) {
        val id = column<Int>("id")
        val firstName = column<String>("first_name")
        val lastName = column<String>("last_name")
        val birthDate = column<LocalDate>("birth_date")
        val employed = column<Boolean>("employed")
        val occupation = column<String>("occupation")
        val addressId = column<Int>("address_id")
    }
}

data class Address(
    var id: Int? = null,
    var streetAddress: String? = null,
    var city: String? = null,
    var state: String? = null,
    var addressType: AddressType? = null,
): Serializable

enum class AddressType {
    HOME,
    BUSINESS
}


data class Person(
    val id: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: LocalDate? = null,
    val employed: Boolean? = null,
    val occupation: String? = null,
    val addressId: Int? = null,
): Serializable


data class PersonWithAddress(
    val id: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: LocalDate? = null,
    val employed: Boolean? = null,
    val occupation: String? = null,
    val address: Address? = null,
): Serializable
