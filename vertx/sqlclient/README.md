# Module bluetape4k-vertx-sqlclient

`bluetape4k-vertx-sqlclient` 는
[Vert.x SQL Client Template](https://vertx.io/docs/vertx-sql-client-templates/java/) 의 Async/Non-Blocking 작업 실행 부분과
[MyBatis Dynamic Sql](https://mybatis.org/mybatis-dynamic-sql/docs/introduction.html) 의 typesafe sql builder 기능을 혼합하여,
성능과 개발 생산성을 제공할 수 있습니다.

또한 `bluetape4k-vertx-core` 를 이용하여, Kotlin Coroutines 환경에서도 손쉽게 `Async/Non-Blocking` 을 지원할 수 있도록 해줍니다.

## Use Case

`bluetape4k-vertx-sqlclient` 는 다음과 같은 분야에 적용할 수 있습니다.

* MSA 의 CQRS 에서 Query 서비스 분야
* 복잡한 쿼리 작성 및 실행 분야
* 그 외, ORM 으로 구현하기 힘들거나, 성능을 높히기 위한 분야

## 주요 Library

### Vert.x SQL Client Template

Vertx 에서 제공하는 Database 용 Client 는 크게 2가지로 나뉩니다. 대표적인 Database 들에 대해서는 Async/Non-Blocking 방식을 지원합니다.

* [vertx-sql-client](https://github.com/eclipse-vertx/vertx-sql-client) - Full Async/Non-Blocking 방식을 지원한다 (eg:
  PostgreSQL, MySQL,
  DB2, Oracle, MSSQL)
* [vertx-jdbc-client](https://github.com/vert-x3/vertx-jdbc-client) - 그 외 Database로 Async 방식만 지원 (JDBC Driver 를 사용한다)

이러한 [여러가지 모듏](https://vertx.io/docs/#databases)을 손쉽게 사용할 수 있도록 제공하는
것이 [SQL Client Template](https://vertx.io/docs/vertx-sql-client-templates/java/) 이다.

### MyBatis Dynamic SQL

기존 MyBatis 는 SQL Mapping 을 XML 로 작성합니다. 이는 typesafe 하지 않아, 매번 실행 시에 검증을 해야 합니다. 이 부분은 JPA 가 복잡하지만,
typesafe 하다는 장점이 있었습니다. 이를 극복할 수 있는 방법이
[MyBatis Dynamic Sql](https://mybatis.org/mybatis-dynamic-sql/docs/introduction.html)을 이용하여
typesafe SQL 을 제작가능하도록 하는 것입니다.

특히 `mybatis-dynamic-sql` 은 Kotlin Language를 지원하여 DSL 작업이 더욱 쉽게 사용할 수 있습니다.

### 사용법

MyBatis Dynamic SQL 라이브러리를 이용하여, SQL 구문을 만들고, 이를 Vertx SQL Client Template 를 이용하여 실행하는 방식의 예입니다.

#### Insert Example

#### SELECT Example

```kotlin
@Test
fun `raw select with missing record`(vertx: Vertx, testContext: VertxTestContext) {
    withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
        val selectProvider = select(person.allColumns()) {
            from(person)
            where { person.id isEqualTo -1 }

        }.renderForVertx()

        val person = conn.selectOne(selectProvider, personMapper)
        person.shouldBeNull()
    }
}
```

#### SELECT with Subquery

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
    val selectProvider = select(person.allColumns()) {
        from(person)
        where {
            person.id isIn {
                select(person.id) {
                    from(person)
                    where { person.lastName isEqualTo "Rubble" }
                }
            }
        }
    }.renderForVertx()

    selectProvider.selectStatement shouldBeEqualTo
            "select * from Person " +
            "where id in (select id from Person where last_name = #{p1})"

    val persons = conn.selectList(selectProvider, personMapper)
    persons.forEach { log.debug { it } }
    persons shouldHaveSize 3
    persons.map { it.id } shouldContainSame listOf(4, 5, 6)
}
```

#### Insert Example

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
    val record = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)
    val insertProvider = insert(record) {
        into(person)
        map(person.id) toProperty Person::id.name
        map(person.firstName) toProperty Person::firstName.name
        map(person.lastName) toProperty Person::lastName.name
        map(person.birthDate) toProperty Person::birthDate.name
        map(person.employed) toProperty Person::employed.name
        map(person.occupation) toProperty Person::occupation.name
        map(person.addressId) toProperty Person::addressId.name
    }.renderForVertx()

    log.debug { "SQL: ${insertProvider.insertStatement}" }

    insertProvider.insertStatement shouldBeEqualTo
            "insert into Person (id, first_name, last_name, birth_date, employed, occupation, address_id) " +
            "values (#{id}, #{firstName}, #{lastName}, #{birthDate}, #{employed}, #{occupation}, #{addressId})"

    val result = conn.insert(insertProvider)
    result.rowCount() shouldBeEqualTo 1
}

```

#### Update Example

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
    val updateProvider = update(person) {
        set(person.firstName) equalTo "Sam"
        where {
            person.firstName isEqualTo "Fred"
            or { person.id isGreaterThan 3 }
        }
    }.renderForVertx()

    updateProvider.updateStatement shouldBeEqualTo
            "update Person " +
            "set first_name = #{p1} " +
            "where (first_name = #{p2} or id > #{p3})"

    val result = conn.update(updateProvider)
    result.rowCount() shouldBeEqualTo 4
}
```

#### Join Example

##### Full Join

MySQL 은 Full Join 을 지원하지 않아, Left Join 과 Right Join 을 Union 한다

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
    // select ol.order_id, ol.quantity, im.item_id, im.description
    // from OrderMaster om
    // join OrderLine ol on om.order_id = ol.order_id
    // left join ItemMaster im on ol.item_id = im.item_id
    //
    // union
    //
    // select ol.order_id, ol.quantity, im.item_id, im.description
    // from OrderMaster om
    // join OrderLine ol on om.order_id = ol.order_id
    // right join ItemMaster im on ol.item_id = im.item_id
    //
    // order by order_id, item_id
    val orderRecords = conn.select(
        listOf(
            JoinSchema.orderLine.orderId,
            JoinSchema.orderLine.quantity,
            JoinSchema.itemMaster.itemId,
            JoinSchema.itemMaster.description
        ),
        OrderRecordRowMapper
    ) {
        from(JoinSchema.orderMaster, "om")
        join(JoinSchema.orderLine, "ol") {
            on(JoinSchema.orderMaster.orderId) equalTo JoinSchema.orderLine.orderId
        }
        leftJoin(JoinSchema.itemMaster, "im") {
            on(JoinSchema.orderLine.itemId) equalTo JoinSchema.itemMaster.itemId
        }
        union {
            select(
                JoinSchema.orderLine.orderId,
                JoinSchema.orderLine.quantity,
                JoinSchema.itemMaster.itemId,
                JoinSchema.itemMaster.description
            ) {
                from(JoinSchema.orderMaster, "om")
                join(JoinSchema.orderLine, "ol") {
                    on(JoinSchema.orderMaster.orderId) equalTo JoinSchema.orderLine.orderId
                }
                rightJoin(JoinSchema.itemMaster, "im") {
                    on(JoinSchema.orderLine.itemId) equalTo JoinSchema.itemMaster.itemId
                }
            }
        }
        orderBy(JoinSchema.orderLine.orderId, JoinSchema.itemMaster.itemId)
    }

    orderRecords shouldHaveSize 6
    orderRecords shouldContainSame expected
}
```

##### Self Join Example

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
    // select u1.user_id, u1.user_name, u1.parent_id
    // from Users u1
    // join Users u2 on u1.user_id = u2.parent_id
    // where u2.user_id = #{p1}
    val user2 = JoinSchema.UsersTable()
    val users = conn.select(
        listOf(JoinSchema.user.userId, JoinSchema.user.userName, JoinSchema.user.parentId),
        UserRowMapper
    ) {
        from(JoinSchema.user, "u1")
        join(user2, "u2") { on(JoinSchema.user.userId) equalTo user2.parentId }
        where { user2.userId isEqualTo 4 }
    }

    users shouldHaveSize 1
    users shouldContainSame expectedUsers
}
```

##### Select with Covering index

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
    // select p1.*
    // from (select p2.id from Person p2 where p2.address_id = #{p1}) p2
    // join Person p1 on p2.id = p1.id
    // where p1.id < #{p2}
    //
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
    val persons = conn.selectList(selectProvider, personMapper)
    persons.forEach { log.debug { it } }
    persons shouldHaveSize 1
}
```

##### Subquery in join

```kotlin
withVertxRollback(vertx, testContext, pool) { conn: SqlConnection ->
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

    val persons = conn.selectList(selectProvider, personMapper)
    persons.forEach { log.debug { it } }
    persons shouldHaveSize 1
}
```
