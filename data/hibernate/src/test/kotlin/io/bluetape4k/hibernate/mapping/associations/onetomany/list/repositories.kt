package io.bluetape4k.hibernate.mapping.associations.onetomany.list

import com.querydsl.jpa.impl.JPAQuery
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FatherRepository: JpaRepository<Father, Int>
interface ChildRepository: JpaRepository<Child, Int>

interface BatchRepository: JpaRepository<Batch, Int>
interface BatchItemRepository: JpaRepository<BatchItem, Int> {

    @Modifying
    fun deleteEachByBatchId(batchId: Int)

    @Modifying
    @Query("delete from onetomany_batch_item where batch.id = :batchId")
    fun deleteAllByBatchId(@Param("batchId") batchId: Int)
}

interface OrderRepository: JpaRepository<Order, Int>, OrderRepositoryExtensions {
    @Query(
        value = "SELECT DISTINCT o FROM onetomany_order o INNER JOIN FETCH o.items",
        countQuery = "SELECT COUNT(o) FROM onetomany_order o INNER JOIN o.items"
    )
    fun findAllWithItems(pageable: Pageable): Page<Order>

}

interface OrderItemRepository: JpaRepository<OrderItem, Int> {
    fun findByOrderIn(orders: Set<Order>): List<OrderItem>
}

interface OrderRepositoryExtensions {
    fun findAllWithInnerJoins(): List<Order>
}

class OrderRepositoryImpl: OrderRepositoryExtensions {

    @PersistenceContext
    lateinit var em: EntityManager

    // NOTE: Inner Join의 경우 OrderItem 없는 Order는 결과셋에서 빠진다 (left outer join 이 기본인 이유이다)
    /**
     * ```
     *         select
     *             order0_.order_id as order_id1_11_,
     *             order0_.no as no2_11_
     *         from
     *             onetomany_order order0_
     *         inner join
     *             onetomany_order_item items1_
     *                 on order0_.order_id=items1_.order_id
     *         order by
     *             items1_.name asc
     *             ```
     */
    override fun findAllWithInnerJoins(): List<Order> {
        val order = QOrder.order
        val item = QOrderItem.orderItem

        return JPAQuery<Order>(em)
            .from(order)
            .innerJoin(order.items, item)
            .orderBy(item.name.asc())
            .fetch()
    }
}
