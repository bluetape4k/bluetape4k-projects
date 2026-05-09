package io.bluetape4k.examples.cassandra.udt

import com.datastax.oss.driver.api.core.data.UdtValue
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.io.Serializable

@Table("udt_type_person")
data class Person(
    @field:Id val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
): Serializable {

    var current: Address = Address.EMPTY
    var previous: List<Address> = emptyList()

    /**
     * UserDefinedType 인 [Address] 를 [UdtValue] 로 표현할 수 있습니다.
     */
    @field:CassandraType(type = CassandraType.Name.UDT, userTypeName = "udt_address")
    var alternative: UdtValue? = null
}

@UserDefinedType("udt_address")
data class Address(
    val street: String,
    val zip: String,
    val city: String,
): Serializable {
    companion object {
        val EMPTY = Address("", "", "")
    }
}
