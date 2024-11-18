package io.bluetape4k.hibernate.mapping.associations.onetomany.list

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class OneToManyListTest @Autowired constructor(
    private val fatherRepo: FatherRepository,
    private val childRepo: ChildRepository,
    private val orderRepo: OrderRepository,
    private val orderItemRepo: OrderItemRepository,
    private val batchRepo: BatchRepository,
    private val batchItemRepo: BatchItemRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `context loading`() {
        // Nothing to do 
    }

    @Test
    fun `one-to-one mappedBy for show who is owner`() {
        val order = Order("N-123")
        val item1 = OrderItem("item1")
        val item2 = OrderItem("item2")
        val item3 = OrderItem("item3")
        order.addItems(item1, item2, item3)   // 역순으로 추가해도, @OrderBy("name") 으로 정렬된다

        orderRepo.save(order)
        flushAndClear()

        val loaded = orderRepo.findByIdOrNull(order.id)!!
        loaded shouldBeEqualTo order

        // select count(id) from order_item
        loaded.items.size shouldBeEqualTo order.items.size
        loaded.items shouldContainSame listOf(item1, item2, item3)

        // Inner join을 이용하여 조회한다
        val loaded2 = orderRepo.findAllWithInnerJoins()
        loaded2.shouldNotBeEmpty()
        loaded2.first().items.size shouldBeEqualTo order.items.size
        loaded2.first().items shouldContainSame listOf(item1, item2, item3)

        loaded.removeItems(item1)
        orderRepo.saveAndFlush(loaded)
        flushAndClear()

        orderItemRepo.findByIdOrNull(item1.id).shouldBeNull()

        orderRepo.deleteAll()
        flushAndClear()

        orderItemRepo.findAll().shouldBeEmpty()
    }

    @Test
    fun `one-to-many with @JoinColumn`() {
        val batch = Batch(name = "B-123")
        val item1 = BatchItem(name = "Item1")
        val item2 = BatchItem(name = "Item2")
        val item3 = BatchItem(name = "Item3")
        batch.addItems(item1, item2, item3)

        batchRepo.save(batch)
        // cascade가 설정되지 않았으므로, 직접 저장해야 합니다.
        batchItemRepo.saveAll(listOf(item1, item2, item3))
        flushAndClear()

        val loaded = batchRepo.findByIdOrNull(batch.id)!!
        loaded shouldBeEqualTo batch
        loaded.items shouldContainSame listOf(item1, item2, item3)

        // @JoinColumn 이 있다면, batch_id 값을 null로 만들어 관계를 끊기만 한다. (orphantRemoval=true를 준다면 삭제까지 수행된다)
        /*
            update
                onetomany_batch_item
            set
                batch_id=null
            where
                batch_id=?
                and batch_item_id=?
        */
        loaded.removeItems(item1)
        batchRepo.save(loaded)
        flushAndClear()
        /*
            delete
            from
                onetomany_batch_item
            where
                batch_item_id=?
        */
        batchItemRepo.delete(item1)
        flushAndClear()

        val loaded2 = batchRepo.findByIdOrNull(batch.id)!!
        loaded2.items shouldContainSame listOf(item2, item3)

        // batchId 에 속하는 batchItem을 Query를 통해 Loading없이 한번에 삭제한다
        batchItemRepo.deleteAllByBatchId(loaded2.id!!)
        flushAndClear()

        batchItemRepo.findAll().shouldBeEmpty()
    }

    @Test
    fun `one-to-many unidirectional`() {
        val father = Father("이성계")
        val child1 = Child("방원", LocalDate.of(1390, 2, 10))
        val child2 = Child("방석", LocalDate.of(1400, 1, 21))
        val child3 = Child("방번", LocalDate.of(1380, 10, 5))

        father.orderedChildren.addAll(listOf(child1, child2, child3))
        fatherRepo.save(father)
        flushAndClear()

        val loaded = fatherRepo.findByIdOrNull(father.id)!!
        loaded shouldBeEqualTo father
        loaded.orderedChildren shouldContainSame listOf(child1, child2, child3)

        loaded.orderedChildren.removeAt(0)
        fatherRepo.save(loaded)
        flushAndClear()

        val loaded2 = fatherRepo.findByIdOrNull(father.id)!!
        loaded2 shouldBeEqualTo father
        loaded2.orderedChildren shouldContainSame listOf(child2, child3)

        fatherRepo.delete(loaded2)
        flushAndClear()

        fatherRepo.findAll().shouldBeEmpty()
        childRepo.findAll().shouldBeEmpty()
    }

    @Test
    fun `fetch join with pagination`() {
        val order = Order("N-123")
        val item1 = OrderItem("item1")
        val item2 = OrderItem("item2")
        val item3 = OrderItem("item3")
        order.addItems(item1, item2, item3)

        orderRepo.save(order)

        repeat(10) {
            val orderN = Order("ORDER-$it").apply {
                addItems(
                    OrderItem("item1-$it"),
                    OrderItem("item2-$it"),
                    OrderItem("item3-$it")
                )
            }
            orderRepo.save(orderN)
        }
        flushAndClear()

        // FIX ME: join fetch 를 사용하면 paging 이 적용되지 않습니다.
        // HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!
        // 아래 글에서는 된다는데 ???
        // 참고: https://codingexplained.com/coding/java/spring-framework/fetch-query-not-working-spring-data-jpa-pageable
        val loaded = orderRepo.findAllWithItems(PageRequest.of(0, 10))

        loaded shouldContain order
        loaded.first().items shouldHaveSize 3
    }

    @Test
    fun `fetch lazy with pagination`() {
        val order = Order("N-123")
        val item1 = OrderItem("item1")
        val item2 = OrderItem("item2")
        val item3 = OrderItem("item3")
        order.addItems(item1, item2, item3)

        orderRepo.save(order)

        repeat(10) {
            val orderN = Order("ORDER-$it").apply {
                addItems(
                    OrderItem("item1-$it"),
                    OrderItem("item2-$it"),
                    OrderItem("item3-$it")
                )
            }
            orderRepo.save(orderN)
        }
        flushAndClear()

        // 차라리 이렇게 Order 만 조회 후, 관련 Comment를 order.id로 조회하는 것이 낫겠다
        //
        //
        val orders = orderRepo.findAll(PageRequest.of(0, 10)).content
        log.info { "Load oders." }

        val items = orderItemRepo.findByOrderIn(orders.toSet())

        items shouldBeEqualTo orders.flatMap { it.items }
    }
}
