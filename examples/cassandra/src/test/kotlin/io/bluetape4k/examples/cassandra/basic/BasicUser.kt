package io.bluetape4k.examples.cassandra.basic

import io.bluetape4k.spring.cassandra.model.AbstractCassandraAuditable
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.io.Serializable

@Table(value = "basic_users")
data class BasicUser(
    @field:PrimaryKey("user_id")
    private var id: Long = 0L,

    @field:Column("uname")
    var username: String = "",

    @field:Column("fname")
    var firstname: String = "",

    @field:Column("lname")
    var lastname: String = "",

    val address: Address = Address(),
): AbstractCassandraAuditable<String, Long>() {

    override fun getId(): Long = id
    override fun setId(id: Long) {
        this.id = id
    }
}

/**
 * `@Tuple` 을 사용하면 repository에서 `findByAddressCity` 같이 속성으로 조회하는 함수를 지원하지 않습니다.
 * `@UserDefinedType` 을 사용하면 가능합니다.
 *
 * @property city
 * @property country
 * @property zip
 * @constructor Create empty Address
 */
@UserDefinedType("basic_user_addr")
data class Address(
    val city: String = "",
    val country: String = "",
    val zip: String = "",
): Serializable
