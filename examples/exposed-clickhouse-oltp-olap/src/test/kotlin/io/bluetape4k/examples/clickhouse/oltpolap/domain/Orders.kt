package io.bluetape4k.examples.clickhouse.oltpolap.domain

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.io.Serializable
import java.time.Instant

/**
 * PostgreSQL OLTP 주문 테이블.
 *
 * 트랜잭션 기반 OLTP 워크로드에서 단일 주문 행을 저장합니다.
 */
object Orders: Table("orders") {
    val id = long("id").autoIncrement()
    val customerId = varchar("customer_id", 100)
    val productId = varchar("product_id", 100)
    val amount = double("amount")
    val region = varchar("region", 50)
    val orderedAt = long("ordered_at") // epoch millis

    override val primaryKey = PrimaryKey(id)
}

/**
 * 주문 데이터 클래스.
 *
 * @property id 주문 ID (DB autoIncrement)
 * @property customerId 고객 식별자
 * @property productId 상품 식별자
 * @property amount 주문 금액
 * @property region 리전 (예: ASIA, EUROPE, AMERICAS)
 * @property orderedAt 주문 시각 (epoch millis)
 */
data class Order(
    val id: Long = 0L,
    val customerId: String,
    val productId: String,
    val amount: Double,
    val region: String,
    val orderedAt: Long = Instant.now().toEpochMilli(),
): Serializable {
    companion object: KLogging() {
        const val serialVersionUID: Long = 1L
    }
}

/** [ResultRow]를 [Order]로 변환합니다. */
fun ResultRow.toOrder(): Order = Order(
    id = this[Orders.id],
    customerId = this[Orders.customerId],
    productId = this[Orders.productId],
    amount = this[Orders.amount],
    region = this[Orders.region],
    orderedAt = this[Orders.orderedAt],
)

/**
 * PostgreSQL OLTP 주문 저장소.
 *
 * 동기 JDBC 트랜잭션 기반의 단일 행 삽입과 단순 조회를 제공합니다.
 */
class OrdersRepository {
    companion object: KLogging()

    /**
     * 주문을 삽입하고 생성된 ID를 반환합니다.
     *
     * @param order 삽입할 주문 (id 무시)
     * @return autoIncrement로 생성된 PK
     */
    fun insert(order: Order): Long {
        val result = Orders.insert {
            it[customerId] = order.customerId
            it[productId] = order.productId
            it[amount] = order.amount
            it[region] = order.region
            it[orderedAt] = order.orderedAt
        }
        return result[Orders.id]
    }

    /** 전체 주문 목록을 반환합니다. */
    fun findAll(): List<Order> = Orders.selectAll().map { it.toOrder() }

    /** 특정 고객의 주문 목록을 반환합니다. */
    fun findByCustomer(customerId: String): List<Order> =
        Orders.selectAll().where { Orders.customerId eq customerId }.map { it.toOrder() }
}
