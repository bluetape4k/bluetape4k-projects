package io.bluetape4k.vertx.sqlclient.mybatis.joins

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.AbstractVertxSqlClientTest
import io.bluetape4k.vertx.sqlclient.mybatis.renderForVertx
import io.bluetape4k.vertx.sqlclient.mybatis.select
import io.bluetape4k.vertx.sqlclient.mybatis.selectList
import io.bluetape4k.vertx.sqlclient.schema.JoinSchema
import io.bluetape4k.vertx.sqlclient.schema.JoinSchema.itemMaster
import io.bluetape4k.vertx.sqlclient.schema.JoinSchema.orderDetail
import io.bluetape4k.vertx.sqlclient.schema.JoinSchema.orderLine
import io.bluetape4k.vertx.sqlclient.schema.JoinSchema.orderMaster
import io.bluetape4k.vertx.sqlclient.schema.JoinSchema.user
import io.bluetape4k.vertx.sqlclient.schema.OrderDetail
import io.bluetape4k.vertx.sqlclient.schema.OrderLineRowMapper
import io.bluetape4k.vertx.sqlclient.schema.OrderMaster
import io.bluetape4k.vertx.sqlclient.schema.OrderRecord
import io.bluetape4k.vertx.sqlclient.schema.OrderRecordRowMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.schema.User
import io.bluetape4k.vertx.sqlclient.schema.UserRowMapper
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendRollback
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.util.kotlin.KInvalidSQLException
import org.mybatis.dynamic.sql.util.kotlin.elements.invoke
import org.mybatis.dynamic.sql.util.kotlin.elements.max
import org.mybatis.dynamic.sql.util.kotlin.model.select
import kotlin.test.assertFailsWith

abstract class AbstractJoinTest: AbstractVertxSqlClientTest() {

    companion object: KLoggingChannel()

    override val schemaFileNames: List<String> = listOf("person.sql", "generatedAlways.sql", "joins.sql")

    // NOTE: One-To-Many Association은 어쩔 수 없이 수동으로 Merge 해야 한다
    private fun RowSet<Row>.mapToOrderMasters(): List<OrderMaster> {
        val orderMasters = mutableMapOf<Int, OrderMaster>()
        this.forEach { row ->
            val orderId = row.getInteger("order_id")
            val orderMaster = orderMasters.computeIfAbsent(orderId) {
                OrderMaster(orderId, row.getLocalDate("order_date"))
            }
            orderMaster.details.add(
                OrderDetail(
                    orderId = orderId,
                    lineNumber = row.getInteger("line_number"),
                    description = row.getString("description"),
                    quantity = row.getInteger("quantity")
                )
            )
        }
        return orderMasters.values.toFastList()
    }

    @Nested
    inner class SimpleJoinTest {

        @Test
        fun `single table join`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val rows = conn.select(
                    orderMaster.orderId, orderMaster.orderDate,
                    orderDetail.lineNumber, orderDetail.description, orderDetail.quantity
                ) {
                    from(orderMaster, "om")
                    join(orderDetail, "od") { on(orderMaster.orderId) equalTo orderDetail.orderId }
                    orderBy(orderMaster.orderId)
                }
                rows.size() shouldBeEqualTo 3

                val orderMasters = rows.mapToOrderMasters()
                orderMasters shouldHaveSize 2
            }
        }

        @Test
        fun `compound join 1`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val rows = conn.select(
                    orderMaster.orderId, orderMaster.orderDate,
                    orderDetail.lineNumber, orderDetail.description, orderDetail.quantity
                ) {
                    from(orderMaster, "om")
                    join(orderDetail, "od") {
                        on(orderMaster.orderId) equalTo orderDetail.orderId
                        and(orderMaster.orderId) equalTo orderDetail.orderId
                    }
                }
                rows.size() shouldBeEqualTo 3

                val orderMasters = rows.mapToOrderMasters()
                orderMasters shouldHaveSize 2
            }
        }

        @Test
        fun `compound join 2`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val rows = conn.select(
                    orderMaster.orderId, orderMaster.orderDate,
                    orderDetail.lineNumber, orderDetail.description, orderDetail.quantity
                ) {
                    from(orderMaster, "om")
                    join(orderDetail, "od") {
                        on(orderMaster.orderId) equalTo orderDetail.orderId
                        and(orderMaster.orderId) equalTo orderDetail.orderId
                    }
                    where { orderMaster.orderId isEqualTo 1 }  // where 절 추가
                }
                rows.size() shouldBeEqualTo 2

                val orderMasters = rows.mapToOrderMasters()
                orderMasters shouldHaveSize 1
            }
        }

        @Test
        fun `multiple table join where clause`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val rowSet = conn.select(
                    orderMaster.orderId, orderMaster.orderDate,
                    orderLine.lineNumber, orderLine.itemId, orderLine.quantity, itemMaster.description
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") { on(orderMaster.orderId) equalTo orderLine.orderId }
                    join(itemMaster, "im") { on(orderLine.itemId) equalTo itemMaster.itemId }
                    where { orderMaster.orderId isEqualTo 2 }
                }

                // {"order_id":2,"order_date":"2017-01-18","line_number":1,"item_id":22,"quantity":1,"description":"Helmet"},
                // {"order_id":2,"order_date":"2017-01-18","line_number":2,"item_id":44,"quantity":1,"description":"Outfield Glove"}
                val rows = rowSet.toFastList()
                rows shouldHaveSize 2
                with(rows[0]) {
                    getInteger("order_id") shouldBeEqualTo 2
                    getInteger("line_number") shouldBeEqualTo 1
                    getInteger("item_id") shouldBeEqualTo 22
                }
                with(rows[1]) {
                    getInteger("order_id") shouldBeEqualTo 2
                    getInteger("line_number") shouldBeEqualTo 2
                    getInteger("item_id") shouldBeEqualTo 44
                }
            }
        }

    }

    @Nested
    inner class FullJoinTest {

        private val expected = listOf(
            OrderRecord(itemId = 55, orderId = null, quantity = null, description = "Catcher Glove"),
            OrderRecord(itemId = 22, orderId = 1, quantity = 1, description = "Helmet"),
            OrderRecord(itemId = 33, orderId = 1, quantity = 1, description = "First Base Glove"),
            OrderRecord(itemId = null, orderId = 2, quantity = 6, description = null),
            OrderRecord(itemId = 22, orderId = 2, quantity = 1, description = "Helmet"),
            OrderRecord(itemId = 44, orderId = 2, quantity = 1, description = "Outfield Glove")
        )

        /**
         * // NOTE: H2, MySQL 는 FULL JOIN 을 지원하지 않는다. LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
         * // NOTE: import org.mybatis.dynamic.sql.util.kotlin.elements.invoke 를 추가해야 alias 를 제대로 인식한다
         *
         * [mysql에서 full outer join 사용하기](https://wkdgusdn3.tistory.com/entry/mysql%EC%97%90%EC%84%9C-full-outer-join-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0)
         */
        @Test
        fun `full join with aliases`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->

                /**
                 * ```sql
                 * select ol.order_id, ol.quantity, im.item_id, im.description
                 * from OrderMaster om
                 * join OrderLine ol on om.order_id = ol.order_id
                 * left join ItemMaster im on ol.item_id = im.item_id
                 *
                 * union
                 *
                 * select ol.order_id, ol.quantity, im.item_id, im.description
                 * from OrderMaster om
                 * join OrderLine ol on om.order_id = ol.order_id
                 * right join ItemMaster im on ol.item_id = im.item_id
                 *
                 * order by order_id, item_id
                 * ```
                 */
                val orderRecords: RowSet<OrderRecord> = conn.select(
                    listOf(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description),
                    OrderRecordRowMapper
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") {
                        on(orderMaster.orderId) equalTo orderLine.orderId
                    }
                    leftJoin(itemMaster, "im") {
                        on(orderLine.itemId) equalTo itemMaster.itemId
                    }
                    union {
                        select(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description) {
                            from(orderMaster, "om")
                            join(orderLine, "ol") {
                                on(orderMaster.orderId) equalTo orderLine.orderId
                            }
                            rightJoin(itemMaster, "im") {
                                on(orderLine.itemId) equalTo itemMaster.itemId
                            }
                        }
                    }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }

                orderRecords shouldHaveSize 6
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }

        /**
         * // NOTE: H2, MySQL 는 FULL JOIN 을 지원하지 않는다. LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
         * // NOTE: import org.mybatis.dynamic.sql.util.kotlin.elements.invoke 를 추가해야 alias 를 제대로 인식한다
         *
         * [mysql에서 full outer join 사용하기](https://wkdgusdn3.tistory.com/entry/mysql%EC%97%90%EC%84%9C-full-outer-join-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0)
         */
        @Test
        fun `full join with subquery`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->

                /**
                 * ```sql
                 * select ol.order_id, quantity, im.item_id, description
                 * from (select * from OrderMaster) om
                 * join (select * from OrderLine) ol on om.order_id = ol.order_id
                 * left join (select * from ItemMaster) im on ol.item_id = im.item_id
                 *
                 * union all
                 *
                 * select ol.order_id, quantity, im.item_id, description
                 * from (select * from OrderMaster) om
                 * join (select * from OrderLine) ol on om.order_id = ol.order_id
                 * right join (select * from ItemMaster) im on ol.item_id = im.item_id
                 *
                 * order by order_id, item_id
                 * ```
                 */
                val orderRecords = conn.select(
                    listOf(
                        "ol"(orderLine.orderId),
                        orderLine.quantity,
                        "im"(itemMaster.itemId),
                        itemMaster.description
                    ),
                    OrderRecordRowMapper
                ) {
                    from {
                        select(orderMaster.allColumns()) { from(orderMaster) }
                        +"om"
                    }
                    join(
                        subQuery = {
                            select(orderLine.allColumns()) { from(orderLine) }
                            +"ol"
                        },
                        joinCriteria = {
                            on("om"(orderMaster.orderId)) equalTo "ol"(orderLine.orderId)
                        }
                    )
                    leftJoin(
                        subQuery = {
                            select(itemMaster.allColumns()) { from(itemMaster) }
                            +"im"
                        },
                        joinCriteria = {
                            on("ol"(orderLine.itemId)) equalTo "im"(itemMaster.itemId)
                        }
                    )
                    union {
                        select(
                            "ol"(orderLine.orderId), orderLine.quantity, "im"(itemMaster.itemId), itemMaster.description
                        ) {
                            from {
                                select(orderMaster.allColumns()) { from(orderMaster) }
                                +"om"
                            }
                            join(
                                subQuery = {
                                    select(orderLine.allColumns()) { from(orderLine) }
                                    +"ol"
                                },
                                joinCriteria = {
                                    on("om"(orderMaster.orderId)) equalTo "ol"(orderLine.orderId)
                                }
                            )
                            rightJoin(
                                subQuery = {
                                    select(itemMaster.allColumns()) { from(itemMaster) }
                                    +"im"
                                },
                                joinCriteria = {
                                    on("ol"(orderLine.itemId)) equalTo "im"(itemMaster.itemId)
                                }
                            )
                        }
                    }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }

                orderRecords shouldHaveSize 6
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }

        /**
         * // NOTE: H2, MySQL 는 FULL JOIN 을 지원하지 않는다. LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
         * // NOTE: import org.mybatis.dynamic.sql.util.kotlin.elements.invoke 를 추가해야 alias 를 제대로 인식한다
         *
         * [mysql에서 full outer join 사용하기](https://wkdgusdn3.tistory.com/entry/mysql%EC%97%90%EC%84%9C-full-outer-join-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0)
         */
        @Test
        fun `full join without aliases`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->

                /**
                 * ```sql
                 * select ol.order_id, ol.quantity, ItemMaster.item_id, ItemMaster.description
                 * from OrderMaster om
                 * join OrderLine ol on om.order_id = ol.order_id
                 * left join ItemMaster on ol.item_id = ItemMaster.item_id
                 *
                 * union
                 *
                 * select ol2.order_id, ol2.quantity, ItemMaster.item_id, ItemMaster.description
                 * from OrderMaster om2
                 * join OrderLine ol2 on om2.order_id = ol2.order_id
                 * right join ItemMaster on ol2.item_id = ItemMaster.item_id
                 *
                 * order by order_id, item_id
                 * ```
                 */
                val orderRecords: RowSet<OrderRecord> = conn.select(
                    listOf(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description),
                    OrderRecordRowMapper
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") {
                        on(orderMaster.orderId) equalTo orderLine.orderId
                    }
                    leftJoin(itemMaster) {
                        on(orderLine.itemId) equalTo itemMaster.itemId
                    }
                    union {
                        select(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description) {
                            from(orderMaster, "om2")
                            join(orderLine, "ol2") {
                                on(orderMaster.orderId) equalTo orderLine.orderId
                            }
                            rightJoin(itemMaster) {
                                on(orderLine.itemId) equalTo itemMaster.itemId
                            }
                        }
                    }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }

                orderRecords shouldHaveSize 6
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }
    }

    @Nested
    inner class LeftJoinTest {

        private val expected = listOf(
            OrderRecord(itemId = 22, orderId = 1, quantity = 1, description = "Helmet"),
            OrderRecord(itemId = 33, orderId = 1, quantity = 1, description = "First Base Glove"),
            OrderRecord(itemId = null, orderId = 2, quantity = 6, description = null),
            OrderRecord(itemId = 22, orderId = 2, quantity = 1, description = "Helmet"),
            OrderRecord(itemId = 44, orderId = 2, quantity = 1, description = "Outfield Glove")
        )

        @Test
        fun `left join with aliases`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderRecords = conn.select(
                    listOf(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description),
                    OrderRecordRowMapper
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") { on(orderMaster.orderId) equalTo orderLine.orderId }
                    leftJoin(itemMaster, "im") { on(orderLine.itemId) equalTo itemMaster.itemId }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }
                // orderRecords shouldHaveSize 5
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }

        @Test
        fun `left join with subquery`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderRecords = conn.select(
                    listOf(
                        "ol"(orderLine.orderId),
                        orderLine.quantity,
                        "im"(itemMaster.itemId),
                        itemMaster.description
                    ),
                    OrderRecordRowMapper
                ) {
                    from {
                        select(orderMaster.allColumns()) { from(orderMaster) }
                        +"om"
                    }
                    join(
                        subQuery = {
                            select(orderLine.allColumns()) { from(orderLine) }
                            +"ol"
                        },
                        joinCriteria = {
                            on("om"(orderMaster.orderId)) equalTo "ol"(orderLine.orderId)
                        }
                    )
                    leftJoin(
                        subQuery = {
                            select(itemMaster.allColumns()) { from(itemMaster) }
                            +"im"
                        },
                        joinCriteria = {
                            on("ol"(orderLine.itemId)) equalTo "im"(itemMaster.itemId)
                        }
                    )
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }


                orderRecords shouldHaveSize 5
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }

        @Test
        fun `left join without aliases`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderRecords = conn.select(
                    listOf(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description),
                    OrderRecordRowMapper
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") {
                        on(orderMaster.orderId) equalTo orderLine.orderId
                    }
                    leftJoin(itemMaster) {
                        on(orderLine.itemId) equalTo itemMaster.itemId
                    }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }

                orderRecords shouldHaveSize 5
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }
    }

    @Nested
    inner class RightJoinTest {

        private val expected: List<OrderRecord> = listOf(
            OrderRecord(itemId = 55, orderId = null, quantity = null, description = "Catcher Glove"),
            OrderRecord(itemId = 22, orderId = 1, quantity = 1, description = "Helmet"),
            OrderRecord(itemId = 33, orderId = 1, quantity = 1, description = "First Base Glove"),
            OrderRecord(itemId = 22, orderId = 2, quantity = 1, description = "Helmet"),
            OrderRecord(itemId = 44, orderId = 2, quantity = 1, description = "Outfield Glove")
        )

        @Test
        fun `right join with aliases`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderRecords: RowSet<OrderRecord> = conn.select(
                    listOf(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description),
                    OrderRecordRowMapper
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") { on(orderMaster.orderId) equalTo orderLine.orderId }
                    rightJoin(itemMaster, "im") { on(orderLine.itemId) equalTo itemMaster.itemId }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }
                orderRecords shouldHaveSize 5
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }

        @Test
        fun `right join with subquery`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderRecords: RowSet<OrderRecord> = conn.select(
                    listOf(
                        "ol"(orderLine.orderId),
                        orderLine.quantity,
                        "im"(itemMaster.itemId),
                        itemMaster.description
                    ),
                    OrderRecordRowMapper
                ) {
                    from {
                        select(orderMaster.allColumns()) { from(orderMaster) }
                        +"om"
                    }
                    join(
                        subQuery = {
                            select(orderLine.allColumns()) { from(orderLine) }
                            +"ol"
                        },
                        joinCriteria = {
                            on("om"(orderMaster.orderId)) equalTo "ol"(orderLine.orderId)
                        }
                    )
                    rightJoin(
                        subQuery = {
                            select(itemMaster.allColumns()) { from(itemMaster) }
                            +"im"
                        },
                        joinCriteria = {
                            on("ol"(orderLine.itemId)) equalTo "im"(itemMaster.itemId)
                        }
                    )
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }


                orderRecords shouldHaveSize 5
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }

        @Test
        fun `right join without aliases`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderRecords: RowSet<OrderRecord> = conn.select(
                    listOf(orderLine.orderId, orderLine.quantity, itemMaster.itemId, itemMaster.description),
                    OrderRecordRowMapper
                ) {
                    from(orderMaster, "om")
                    join(orderLine, "ol") {
                        on(orderMaster.orderId) equalTo orderLine.orderId
                    }
                    rightJoin(itemMaster) {
                        on(orderLine.itemId) equalTo itemMaster.itemId
                    }
                    orderBy(orderLine.orderId, itemMaster.itemId)
                }

                orderRecords shouldHaveSize 5
                orderRecords.toFastList() shouldBeEqualTo expected
            }
        }
    }

    @Nested
    inner class SelfJoinTest {

        private val expectedUsers = listOf(
            User(userId = 2, userName = "Barney", parentId = null)
        )

        @Test
        fun `self join`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                /**
                 * ```sql
                 * select u1.user_id, u1.user_name, u1.parent_id
                 *   from Users u1
                 *        join Users u2 on u1.user_id = u2.parent_id
                 *  where u2.user_id = #{p1}
                 * ```
                 */
                val user2 = JoinSchema.UsersTable()
                val users: RowSet<User> = conn.select(
                    listOf(user.userId, user.userName, user.parentId),
                    UserRowMapper
                ) {
                    from(user, "u1")
                    join(user2, "u2") { on(user.userId) equalTo user2.parentId }
                    where { user2.userId isEqualTo 4 }
                }

                users shouldHaveSize 1
                users.toFastList() shouldBeEqualTo expectedUsers
            }
        }

        @Test
        fun `self join with new alias`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                /**
                 * ```sql
                 * select Users.user_id, Users.user_name, Users.parent_id
                 * from Users
                 * join Users u2 on Users.user_id = u2.parent_id
                 * ```
                 */
                val user2 = user.withAlias("u2")
                val users = conn.select(
                    listOf(user.userId, user.userName, user.parentId),
                    UserRowMapper
                ) {
                    from(user)
                    join(user2) { on(user.userId) equalTo user2.parentId }
                    where { user2.userId isEqualTo 4 }
                }

                users shouldHaveSize 1
                users.toFastList() shouldBeEqualTo expectedUsers
            }
        }

        @Test
        fun `self join with new alias and override`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                /**
                 * ```sql
                 *  select u1.user_id, u1.user_name, u1.parent_id
                 *  from Users u1
                 *  join Users u2 on u1.user_id = u2.parent_id
                 *  where u2.user_id = #{p1}
                 *  ```
                 */
                val user2 = user.withAlias("other_user")
                val users = conn.select(
                    listOf(user.userId, user.userName, user.parentId),
                    UserRowMapper
                ) {
                    from(user, "u1")
                    join(user2, "u2") { on(user.userId) equalTo user2.parentId }
                    where { user2.userId isEqualTo 4 }
                }

                users shouldHaveSize 1
                users.toFastList() shouldBeEqualTo expectedUsers
            }
        }
    }

    @Nested
    inner class CoveringIndexTest {

        @Test
        fun `covering index`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                /**
                 * ```sql
                 * select p1.*
                 * from (select p2.id from Person p2 where p2.address_id = #{p1}) p2
                 * join Person p1 on p2.id = p1.id
                 * where p1.id < #{p2}
                 * ```
                 */
                val p2 = person.withAlias("p2")
                val selectProvider = select(person.allColumns()) {
                    from {
                        select(p2.id) {
                            from(p2)
                            where { p2.addressId isEqualTo 2 }
                        }
                        +"p2"
                    }
                    join(person, "p1") {
                        on(p2.id) equalTo person.id
                    }
                    where { person.id isLessThan 5 }
                }.renderForVertx()

                val persons = conn.selectList(selectProvider, PersonMapper)
                persons.forEach { log.debug { it } }
                persons shouldHaveSize 1
            }
        }

        @Test
        fun `subquery in join`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val p2 = person.withAlias("p2")
                val selectProvider = select(person.allColumns()) {
                    from(person, "p1")
                    join({
                        select(p2.id) {
                            from(p2)
                            where { p2.addressId isEqualTo 2 }
                            orderBy(p2.id)
                        }
                        +"p2"
                    }
                    ) {
                        on(person.id).equalTo(p2.id)    // NOTE: PersonTable 이 AliasableSqlTable 이어야 합니다.
                    }
                    where { person.id isLessThan 5 }
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select p1.* " +
                        "from Person p1 " +
                        "join (select p2.id from Person p2 where p2.address_id = #{p1} order by id) p2 " +
                        "on p1.id = p2.id " +
                        "where p1.id < #{p2}"

                val persons = conn.selectList(selectProvider, PersonMapper)
                persons.forEach { log.debug { it } }
                persons shouldHaveSize 1
            }
        }
    }

    @Nested
    inner class MiscJoinTest {
        @Test
        fun `join with no on condition`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                assertFailsWith<KInvalidSQLException> {
                    val user2 = user.withAlias("other_user")
                    conn.select(
                        listOf(user.userId, user.userName, user.parentId),
                        UserRowMapper
                    ) {
                        from(user, "u1")
                        join(user2, "u2") { and(user.userId) equalTo user2.parentId }
                        where { user2.userId isEqualTo 4 }
                    }
                }
            }
        }

        @Test
        fun `aliases propagate to subquery condition`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val orderLine2 = JoinSchema.OrderLineTable()
                val orderLines = conn.selectList(
                    listOf(orderLine.orderId, orderLine.lineNumber),
                    OrderLineRowMapper
                ) {
                    from(orderLine, "ol")
                    where {
                        orderLine.lineNumber isEqualTo {
                            select(max(orderLine2.lineNumber)) {
                                from(orderLine2, "ol2")
                                where { orderLine2.orderId isEqualTo orderLine.orderId }
                            }
                        }
                    }
                    orderBy(orderLine.orderId)
                }

                orderLines shouldHaveSize 2
            }
        }
    }
}
