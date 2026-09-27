package io.bluetape4k.examples.cassandra.streamnullable

import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId

@Table("pizza_orders")
data class Order(
    @field:Id val id: String = "",
    val orderDate: LocalDate = LocalDate.MIN,
    val zoneId: ZoneId = ZoneId.systemDefault(),
): Serializable
