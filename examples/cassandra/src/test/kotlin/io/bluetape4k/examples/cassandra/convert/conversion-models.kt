package io.bluetape4k.examples.cassandra.convert

import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Element
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.core.mapping.Tuple
import java.io.Serializable
import java.util.*

@Tuple
data class Address(
    @field:Element(0) val address: String,
    @field:Element(1) val city: String,
    @field:Element(2) val zip: String,
): Serializable {
    companion object {
        val EMPTY = Address("", "", "")
    }
}

@Table
data class Addressbook(
    @field:Id val id: String,

    var me: Contact,
    val friends: MutableList<Contact> = mutableListOf(),

    var address: Address = Address.EMPTY,
    val preferredCurrencies: MutableMap<Int, Currency> = mutableMapOf(),
)

// ContactWriteConverter, ContactReadConverter 를 통해 JSON 으로 저장
data class Contact(
    val firstname: String,
    val lastname: String,
)

data class CustomAddressbook(
    val theId: String?,
    val myDetailsAsJson: String?,
)
