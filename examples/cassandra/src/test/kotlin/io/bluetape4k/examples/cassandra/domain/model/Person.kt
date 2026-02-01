package io.bluetape4k.examples.cassandra.domain.model

import io.bluetape4k.collections.eclipse.fastListOf
import org.eclipse.collections.impl.list.mutable.FastList
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Table
data class Person(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    val lastname: String = "",
    @field:PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    val firstname: String = "",
): Serializable {
    var nickname: String? = null
    var birthDate: Date? = null
    var numberOfChildren: Int = 0
    var cool: Boolean = false

    var createdDate: LocalDate = LocalDate.now()
    var zoneId: ZoneId = ZoneId.systemDefault()

    var mainAddress: AddressType = AddressType()
    val alternativeAddresses: FastList<AddressType> = fastListOf()
}
