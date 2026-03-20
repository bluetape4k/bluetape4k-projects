package io.bluetape4k.examples.cassandra.streamnullable

import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.Repository
import java.time.LocalDate
import java.time.ZoneId

interface OrderRepository: Repository<Order, String> {

    @Query("SELECT * FROM pizza_orders WHERE orderdate = ?0 and zoneid = ?1 ALLOW FILTERING")
    fun findOrderByOrderDateAndZoneId(orderDate: LocalDate, zoneId: ZoneId): Order?

    fun deleteAll()

    fun save(order: Order): Order
}
