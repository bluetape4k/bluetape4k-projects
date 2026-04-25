package io.bluetape4k.examples.clickhouse.oltpolap.domain

import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.exposed.clickhouse.types.chFloat64
import io.bluetape4k.exposed.clickhouse.types.chString
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import java.io.Serializable

/**
 * ClickHouse OLAP 주문 이벤트 분석 테이블.
 *
 * MergeTree 엔진으로 region 파티셔닝, (order_id, customer_id) 정렬을 사용합니다.
 * OLTP에서 forwarding된 주문 이벤트를 누적 저장하여 집계 분석에 활용합니다.
 */
object OrderEvents: ClickHouseTable(
    name = "order_events",
    engine = mergeTree {
        orderBy("order_id", "customer_id")
        partitionBy("region")
    }
) {
    val orderId = long("order_id")
    val customerId = chString("customer_id")
    val productId = chString("product_id")
    val amount = chFloat64("amount")
    val region = chString("region")
    val orderedAt = long("ordered_at")
}

/**
 * 주문 분석 결과.
 *
 * @property totalOrders 총 주문 건수
 * @property uniqueCustomers 고유 고객 수 (uniqExact)
 * @property p95Amount 주문 금액 95 분위수 (quantile(0.95))
 * @property latestProductId 가장 최근 주문된 상품 (argMax(product_id, ordered_at))
 */
data class OrderAnalyticsResult(
    val totalOrders: Long,
    val uniqueCustomers: Long,
    val p95Amount: Double,
    val latestProductId: String?,
): Serializable {
    companion object: KLogging() {
        const val serialVersionUID: Long = 1L
    }
}

/**
 * ClickHouse OLAP 분석 저장소.
 *
 * - 배치 삽입을 통해 OLTP에서 forwarding된 이벤트를 누적합니다.
 * - 리전별 집계 분석은 raw aggregate SQL로 ClickHouse 함수를 직접 호출합니다.
 */
class AnalyticsRepository {
    companion object: KLogging()

    /**
     * OLTP 주문을 ClickHouse OLAP 테이블에 배치 삽입합니다.
     *
     * @param orders 삽입할 주문 목록
     */
    fun batchInsertOrders(orders: List<Order>) {
        OrderEvents.batchInsert(orders, shouldReturnGeneratedValues = false) { order ->
            this[OrderEvents.orderId] = order.id
            this[OrderEvents.customerId] = order.customerId
            this[OrderEvents.productId] = order.productId
            this[OrderEvents.amount] = order.amount
            this[OrderEvents.region] = order.region
            this[OrderEvents.orderedAt] = order.orderedAt
        }
    }

    /**
     * 리전별 집계 분석을 실행합니다.
     *
     * ClickHouse 집계 함수(count, uniqExact, quantile, argMax)를 raw SQL로 실행합니다.
     * Exposed의 transaction.exec 패턴을 사용하여 활성 트랜잭션 컨텍스트와 통합됩니다.
     *
     * @param tx 활성 [JdbcTransaction] (ClickHouse 데이터베이스에 연결됨)
     * @param region 분석 대상 리전 (예: "ASIA", "EUROPE", "AMERICAS")
     * @return [OrderAnalyticsResult] — 데이터가 없으면 0으로 채워진 결과 반환
     */
    fun analyzeByRegion(tx: JdbcTransaction, region: String): OrderAnalyticsResult {
        val sanitizedRegion = region.replace("'", "''")
        val sql = """
            SELECT
                count(*) AS total_orders,
                uniqExact(customer_id) AS unique_customers,
                quantile(0.95)(amount) AS p95_amount,
                argMax(product_id, ordered_at) AS latest_product
            FROM order_events
            WHERE region = '$sanitizedRegion'
        """.trimIndent()

        return tx.exec(sql) { rs ->
            if (rs.next()) {
                OrderAnalyticsResult(
                    totalOrders = rs.getLong("total_orders"),
                    uniqueCustomers = rs.getLong("unique_customers"),
                    p95Amount = rs.getDouble("p95_amount"),
                    latestProductId = rs.getString("latest_product"),
                )
            } else {
                OrderAnalyticsResult(0L, 0L, 0.0, null)
            }
        } ?: OrderAnalyticsResult(0L, 0L, 0.0, null)
    }
}
