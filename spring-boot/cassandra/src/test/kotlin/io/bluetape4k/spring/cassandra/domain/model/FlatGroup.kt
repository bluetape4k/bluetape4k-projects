package io.bluetape4k.spring.cassandra.domain.model

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("flat_groups")
data class FlatGroup(
    @field:PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    val groupname: String = "",

    @field:PrimaryKeyColumn(name = "hash_prefix", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    val hashPrefix: String = "",

    @field:PrimaryKeyColumn(ordinal = 3, type = PrimaryKeyType.CLUSTERED)
    val username: String = "",
): Serializable {

    var email: String = ""
    var age: Int = 0

}
