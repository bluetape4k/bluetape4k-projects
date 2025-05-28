package io.bluetape4k.exposed.shared.dml

import io.bluetape4k.exposed.shared.dml.DMLTestData.withCitiesAndUsers
import io.bluetape4k.exposed.shared.dml.DMLTestData.withSales
import io.bluetape4k.exposed.shared.dml.DMLTestData.withSalesAndSomeAmounts
import io.bluetape4k.exposed.shared.entities.BoardSchema
import io.bluetape4k.exposed.shared.entities.BoardSchema.Board
import io.bluetape4k.exposed.shared.entities.BoardSchema.Boards
import io.bluetape4k.exposed.shared.entities.BoardSchema.Category
import io.bluetape4k.exposed.shared.entities.BoardSchema.Post
import io.bluetape4k.exposed.shared.entities.BoardSchema.Posts
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.expectException
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toBigDecimal
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotContain
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.allFrom
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.anyFrom
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.compoundOr
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SelectTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * 간단한 조건절을 가진 SELECT 문
     *
     * ```sql
     * -- Postgres
     * SELECT users.id,
     *        users."name",
     *        users.city_id,
     *        users.flags
     *   FROM users
     *  WHERE users.id = 'andrey'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SELECT ALL - 하나의 조건`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val row = users.selectAll().where { users.id eq "andrey" }.single()

            val userId = row[users.id]
            val userName = row[users.name]
            log.debug { "User id: $userId, name: $userName" }
            userName shouldBeEqualTo "Andrey"
        }
    }

    /**
     * WHERE 조건들이 `AND` 로 연결된 경우
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE (users.id = 'andrey')
     *    AND (users."name" IS NOT NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SELECT ALL - 복수의 AND 조건`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val row = users.selectAll().where { users.id eq "andrey" }.andWhere { users.name.isNotNull() }.single()

            val userId = row[users.id]
            val userName = row[users.name]
            log.debug { "User id: $userId, name: $userName" }
            userName shouldBeEqualTo "Andrey"

            val row2 = users.selectAll()
                .where { (users.id eq "andrey") and (users.name.isNotNull()) }   // where 함수 내에서 조합해도 된다
                .single()

            val userId2 = row2[users.id]
            val userName2 = row2[users.name]
            log.debug { "User id: $userId2, name: $userName2" }
            userName2 shouldBeEqualTo "Andrey"
        }
    }

    /**
     * WHERE 조건들이 `OR` 로 연결된 경우
     *
     * ```sql
     * -- Postgres
     * SELECT users.id,
     *        users."name",
     *        users.city_id,
     *        users.flags
     *   FROM users
     *  WHERE (users.id = 'andrey')
     *     OR (users."name" = 'Andrey')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SELECT ALL - 복수 조건이 OR 인 경우`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val row = users.selectAll().where { users.id.eq("andrey") or users.name.eq("Andrey") } // orWhere 를 써도 된다.
                .single()

            val userId = row[users.id]
            val userName = row[users.name]
            log.debug { "User id: $userId, name: $userName" }
            userName shouldBeEqualTo "Andrey"
        }
    }

    /**
     * WHERE 조건에 NOT EQUAL에 해당하는 `<>` 연산자 사용
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE users.id <> 'andrey'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `조건절에 not equal 사용`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().where { users.id neq "andrey" }.toList()

            rows.map { it[users.id] } shouldNotContain "andrey"
        }
    }


    /**
     * [SizedIterable] 을 이용한 쿼리 실행
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SizedIterable 수형에 해당하는 쿼리 실행`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->

            // SELECT cities.city_id, cities."name" FROM cities
            cities.selectAll().shouldNotBeEmpty()

            // SELECT cities.city_id, cities."name" FROM cities WHERE cities."name" = 'Qwertt'
            cities.selectAll().where { cities.name eq "Qwertt" }.shouldBeEmpty()

            // SELECT COUNT(*) FROM cities WHERE cities."name" = 'Qwertt'
            cities.selectAll().where { cities.name eq "Qwertt" }.count() shouldBeEqualTo 0L

            // SELECT COUNT(*) FROM cities
            cities.selectAll().count() shouldBeEqualTo 3L

            val cityId: Int? = null

            // SELECT COUNT(*) FROM users WHERE users.city_id IS NULL
            users.selectAll().where { users.cityId eq cityId }.count() shouldBeEqualTo 2L   // isNull() 을 사용해도 된다.
        }
    }

    /**
     * `inList`, `notInList` 를 사용한 조회
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with single expression 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            /**
             * ```sql
             * -- Postgres
             * SELECT users.id, users."name", users.city_id, users.flags
             *   FROM users
             *  WHERE users.id IN ('andrey', 'alex')
             *  ORDER BY users."name" ASC
             * ```
             */
            val r1 = users.selectAll().where { users.id inList listOf("andrey", "alex") }.orderBy(users.name).toList()

            r1.size shouldBeEqualTo 2
            r1[0][users.name] shouldBeEqualTo "Alex"
            r1[1][users.name] shouldBeEqualTo "Andrey"

            /**
             * ```sql
             * -- Postgres
             * SELECT users.id, users."name", users.city_id, users.flags
             *   FROM users
             *  WHERE users.id NOT IN ('ABC', 'DEF')
             * ```
             */
            val r2 = users.selectAll().where { users.id notInList listOf("ABC", "DEF") }.toList()

            users.selectAll().count() shouldBeEqualTo r2.size.toLong()
        }
    }

    /**
     * `inList` 에 Pair 형식으로 사용하기
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE (users.id, users."name") IN (('andrey', 'Andrey'), ('sergey', 'Sergey'))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with pair expression 02`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().where {
                (users.id to users.name) inList listOf("andrey" to "Andrey", "sergey" to "Sergey")
            }.toList()

            rows shouldHaveSize 2
            rows[0][users.name] shouldBeEqualTo "Andrey"
            rows[1][users.name] shouldBeEqualTo "Sergey"
        }
    }

    /**
     * `EntityID` 에 `inList`, `notInList` 사용하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with entityID columns`(testDB: TestDB) {
        withTables(testDB, *BoardSchema.allTables) {
            val board1 = Board.new {
                name = "board1"
            }
            val post1 = Post.new {
                board = board1
            }
            Post.new {
                category = Category.new { title = "category1" }
            }

            /**
             * inList의 항목이 한개라면 `eq` 로 대체 가능
             *
             * ```sql
             * -- Postgres
             * SELECT posts.id,
             *        posts.board,
             *        posts.parent,
             *        posts.category,
             *        posts."optCategory"
             *   FROM posts
             *  WHERE posts.board = 1
             * ```
             */
            val result1 = Posts.selectAll().where {
                Posts.boardId inList listOf(board1.id)
            }.singleOrNull()?.get(Posts.id)

            result1 shouldBeEqualTo post1.id

            /**
             * `inList` with `EntityID` columns
             *
             * ```sql
             * -- Postgres
             * SELECT board.id, board."name"
             *   FROM board
             *  WHERE board.id IN (1, 2, 3, 4, 5)
             * ```
             */
            val result2 = Board.find {
                Boards.id inList listOf(1, 2, 3, 4, 5)
            }.singleOrNull()
            result2 shouldBeEqualTo board1

            /**
             * `notInList` with entityID columns
             *
             * ```sql
             * -- Postgres
             * SELECT board.id, board."name"
             *   FROM board
             *  WHERE board.id  NOT IN (1, 2, 3, 4, 5)
             * ```
             */
            val result3 = Board.find {
                Boards.id notInList listOf(1, 2, 3, 4, 5)
            }.singleOrNull()
            result3.shouldBeNull()
        }
    }

    /**
     * `inSubQuery` 연산자 예제
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM cities
     *  WHERE cities.city_id IN (SELECT cities.city_id
     *                             FROM cities
     *                            WHERE cities.city_id = 2)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inSubQuery 연산자 예제 - 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val subQuery: Query = cities.select(cities.id).where { cities.id eq 2 }

            val r: Query = cities.selectAll().where { cities.id inSubQuery subQuery }

            r.count() shouldBeEqualTo 1L
        }
    }

    /**
     * `notInSubQuery` 연산자 사용
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM cities
     *  WHERE cities.city_id NOT IN (SELECT cities.city_id FROM cities)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `notInSubQuery 연산자 예제`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val subQuery: Query = cities.select(cities.id)

            val r: Query = cities.selectAll().where { cities.id notInSubQuery subQuery }

            r.count() shouldBeEqualTo 0L
        }
    }

    //
    // inTable 연산자
    //

    private val supportingInAnyAllFromTables = TestDB.ALL_POSTGRES + TestDB.H2_PSQL + TestDB.MYSQL_V8

    /**
     * `inTable` example
     *
     * SomeAmount 테이블의 amount 컬럼의 값과 같은 sales 테이블의 amount 컬럼의 갯수를 조회합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM sales
     *  WHERE sales.amount IN (TABLE SomeAmounts)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inTable 연산자 예제`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingInAnyAllFromTables }

        withSalesAndSomeAmounts(testDB) { _, sales, someAmounts ->
            val rows = sales.selectAll().where {
                sales.amount inTable someAmounts
            }
            rows.count() shouldBeEqualTo 2L
        }
    }

    /**
     * `notInTable` example
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM sales
     *  WHERE sales.amount NOT IN (TABLE SomeAmounts)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `notInTable example`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingInAnyAllFromTables }

        withSalesAndSomeAmounts(testDB) { _, sales, someAmounts ->
            val rows = sales.selectAll().where {
                sales.amount notInTable someAmounts
            }
            rows.count() shouldBeEqualTo 5L
        }
    }

    private val supportingAnyAndAllFromSubQueries = TestDB.ALL
    private val supportingAnyAndAllFromArrays = TestDB.ALL_POSTGRES + TestDB.ALL_H2

    /**
     * `eq` [anyFrom] with SubQuery
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM cities
     *  WHERE cities.city_id = ANY (SELECT cities.city_id
     *                                FROM cities
     *                               WHERE cities.city_id = 2)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `eq AnyFrom SubQuery`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val subquery: Query = cities.select(cities.id).where { cities.id eq 2 }

            val rows = cities.selectAll().where {
                cities.id eq anyFrom(subquery)
            }

            rows.count() shouldBeEqualTo 1L
        }
    }

    /**
     * `neq` and [anyFrom] with SubQuery
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM cities
     *  WHERE cities.city_id <> ANY (SELECT cities.city_id
     *                                 FROM cities
     *                                WHERE cities.city_id = 2)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `neq AnyFrom SubQuery`(dialect: TestDB) {
        withCitiesAndUsers(dialect) { cities, _, _ ->
            val subquery: Query = cities.select(cities.id).where { cities.id eq 2 }

            val rows = cities.selectAll().where {
                cities.id neq anyFrom(subquery)
            }

            rows.count() shouldBeEqualTo 2L
        }
    }

    /**
     * `eq` [anyFrom] with Array
     *
     * 참고: [anyFrom]은 Postgres, H2 만 지원합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE users.id = ANY (ARRAY['andrey','alex'])
     *  ORDER BY users."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `eq AnyFrom Array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().where {
                users.id eq anyFrom(arrayOf("andrey", "alex"))
            }.orderBy(users.name).toList()

            rows shouldHaveSize 2
            rows[0][users.name] shouldBeEqualTo "Alex"
            rows[1][users.name] shouldBeEqualTo "Andrey"
        }
    }

    /**
     * `eq` [anyFrom] with List
     *
     * 참고: [anyFrom]은 Postgres, H2 만 지원합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE users.id = ANY (ARRAY['andrey','alex'])
     *  ORDER BY users."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `eq AnyFrom List`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().where {
                users.id eq anyFrom(listOf("andrey", "alex"))
            }.orderBy(users.name).toList()

            rows shouldHaveSize 2
            rows[0][users.name] shouldBeEqualTo "Alex"
            rows[1][users.name] shouldBeEqualTo "Andrey"
        }
    }

    /**
     * `neq` [anyFrom] with Array
     *
     * 참고: [anyFrom]은 Postgres, H2 만 지원합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM users
     *  WHERE users.id <> ANY (ARRAY['andrey'])
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `neq AnyFrom Array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().where {
                users.id neq anyFrom(arrayOf("andrey"))
            }.orderBy(users.name)

            rows.count() shouldBeEqualTo 4L
        }
    }

    /**
     * `neq` [anyFrom] with List
     *
     * 참고: [anyFrom]은 Postgres, H2 만 지원합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM users
     *  WHERE users.id <> ANY (ARRAY['andrey'])
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `neq AnyFrom List`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().where {
                users.id neq anyFrom(listOf("andrey"))
            }.orderBy(users.name)

            rows.count() shouldBeEqualTo 4L
        }
    }

    /**
     * `greaterEq` [anyFrom] with Array
     *
     * ```sql
     * -- Postgres
     * SELECT SALES."year", SALES."month", SALES.PRODUCT, SALES.AMOUNT
     *   FROM SALES
     *  WHERE SALES.AMOUNT >= ANY (ARRAY [100,1000])
     *  ORDER BY SALES.AMOUNT ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `greater eq AnyFrom Array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withSales(testDB) { _, sales ->
            val amounts = arrayOf(100, 1000).map { it.toBigDecimal() }.toTypedArray()

            val rows = sales.selectAll().where {
                sales.amount greaterEq anyFrom(amounts)
            }.orderBy(sales.amount).map { it[sales.product] }

            rows.subList(0, 3).forEach { it shouldBeEqualTo "tea" }
            rows.subList(3, 6).forEach { it shouldBeEqualTo "coffee" }
        }
    }

    /**
     * `greaterEq` [anyFrom] List
     *
     * ```sql
     * -- Postgres
     * SELECT SALES."year", SALES."month", SALES.PRODUCT, SALES.AMOUNT
     *   FROM SALES
     *  WHERE SALES.AMOUNT >= ANY (ARRAY [100.0,1000.0])
     *  ORDER BY SALES.AMOUNT ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `greater eq AnyFrom List`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withSales(testDB) { _, sales ->
            val amounts = listOf(100.0, 1000.0).map { it.toBigDecimal() }

            val rows = sales.selectAll().where {
                sales.amount greaterEq anyFrom(amounts)
            }.orderBy(sales.amount).map { it[sales.product] }

            rows.subList(0, 3).forEach { it shouldBeEqualTo "tea" }
            rows.subList(3, 6).forEach { it shouldBeEqualTo "coffee" }
        }
    }

    /**
     * `eq` [anyFrom] with Table
     *
     * Postgres:
     * ```sql
     * SELECT COUNT(*)
     *   FROM sales
     *  WHERE sales.amount = ANY (TABLE SomeAmounts)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Eq AnyFrom Table`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingInAnyAllFromTables }

        withSalesAndSomeAmounts(testDB) { _, sales, someAmounts ->
            val rows = sales.selectAll().where {
                sales.amount eq anyFrom(someAmounts)
            }

            rows.count() shouldBeEqualTo 2L        // 650.70, 1500.25
        }
    }

    /**
     *`neq` [anyFrom] with Table
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM sales
     *  WHERE sales.amount <> ANY (TABLE SomeAmounts)
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `neq AnyFrom Table`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingInAnyAllFromTables }

        withSalesAndSomeAmounts(testDB) { _, sales, someAmounts ->
            val rows = sales.selectAll().where {
                sales.amount neq anyFrom(someAmounts)
            }
            rows.count() shouldBeEqualTo 7L    // except 650.70, 1500.25 이어야 하는데 ...
        }
    }

    /**
     * `greaterEq` [allFrom] of SubQuery
     *
     * Subquery 에서 max() 를 사용하는 게 더 낫지 않나?
     *
     * ```sql
     * -- Postgres
     * SELECT sales."year", sales."month", sales.product, sales.amount
     *   FROM sales
     *  WHERE sales.amount >= ALL (SELECT sales.amount
     *                               FROM sales
     *                              WHERE sales.product = 'tea')
     *  ORDER BY sales.amount ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `greaterEq AllFrom SubQuery`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromSubQueries }
        // MySQL 5.x 에서는 지원되지 않습니다.
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        withSales(testDB) { _, sales ->
            val subquery = sales.select(sales.amount).where { sales.product eq "tea" }

            val rows = sales.selectAll().where {
                sales.amount greaterEq allFrom(subquery)
            }.orderBy(sales.amount).map { it[sales.product] }

            rows shouldHaveSize 4
            rows.first() shouldBeEqualTo "tea"
            rows.drop(1).all { it == "coffee" }.shouldBeTrue()
        }
    }

    /**
     * `greaterEq` [allFrom] with Array
     *
     * array 의 max() 를 사용하는 게 더 낫지 않나?
     *
     * ```sql
     * -- Postgres
     * SELECT sales."year", sales."month", sales.product, sales.amount
     *   FROM sales
     *  WHERE sales.amount >= ALL (ARRAY[100.0,1000.0])
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `greaterEq AllFrom Array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withSales(testDB) { _, sales ->
            val amounts = arrayOf(100.0, 1000.0).map { it.toBigDecimal() }.toTypedArray()

            val rows = sales.selectAll().where {
                sales.amount greaterEq allFrom(amounts)
            }.toList()

            rows shouldHaveSize 3
            rows.all { it[sales.product] == "coffee" }.shouldBeTrue()
        }
    }

    /**
     * `greaterEq` with [allFrom] of List
     *
     * list 의 max() 를 사용하는 게 더 낫지 않나?
     *
     * ```sql
     * -- Postgres
     * SELECT sales."year", sales."month", sales.product, sales.amount
     *   FROM sales
     *  WHERE sales.amount >= ALL (ARRAY[100.0,1000.0])
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `greaterEq AllFrom List`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingAnyAndAllFromArrays }

        withSales(testDB) { _, sales ->
            val amounts = arrayOf(100.0, 1000.0).map { it.toBigDecimal() }

            val rows = sales.selectAll().where {
                sales.amount greaterEq allFrom(amounts)
            }.toList()

            rows shouldHaveSize 3
            rows.all { it[sales.product] == "coffee" }.shouldBeTrue()
        }
    }

    /**
     * `greaterEq` with [allFrom] of Table
     * table 대신 subquery의 max() 를 사용하는 게 더 낫지 않나?
     *
     * ```sql
     * -- Postgres
     * SELECT sales."year", sales."month", sales.product, sales.amount
     *   FROM sales
     *  WHERE sales.amount >= ALL (TABLE SomeAmounts)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Greater Eq AllFrom Table`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportingInAnyAllFromTables }

        withSalesAndSomeAmounts(testDB) { _, sales, someAmounts ->
            val rows = sales.selectAll().where { sales.amount greaterEq allFrom(someAmounts) }.toList()

            rows shouldHaveSize 3
            rows.all { it[sales.product] == "coffee" }.shouldBeTrue()
        }
    }

    /**
     * ### SELECT DISTINCT 예제 (`withDistinct`, `withDistinctOn`)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select distinct`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_LIKE }

        val cities = DMLTestData.Cities
        withTables(testDB, cities) {
            cities.insert { it[cities.name] = "test" }
            cities.insert { it[cities.name] = "test" }

            cities.selectAll().count() shouldBeEqualTo 2L

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM (SELECT DISTINCT cities.city_id Cities_city_id,
             *                         cities."name" Cities_name
             *           FROM cities
             *   ) subquery
             * ```
             */
            cities.selectAll().withDistinct().count() shouldBeEqualTo 2L

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM (SELECT DISTINCT cities."name" Cities_name
             *           FROM cities
             *   ) subquery
             * ```
             */
            cities.select(cities.name).withDistinct().count() shouldBeEqualTo 1L

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM (SELECT DISTINCT ON (cities."name")
             *                cities.city_id Cities_city_id,
             *                cities."name" Cities_name
             *           FROM cities
             *   ) subquery
             * ```
             */
            cities.selectAll().withDistinctOn(cities.name).count() shouldBeEqualTo 1L
        }
    }

    /**
     * ### Compound Operations
     *
     * * [compoundOr] 함수는 여러 개의 [Op]를 OR 연산자로 결합합니다.
     * * [compoundAnd] 함수는 여러 개의 [Op]를 AND 연산자로 결합합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `compound operations`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val allUsers = setOf(
                "Andrey", "Sergey", "Eugene", "Alex", "Something"
            )

            /**
             * [compoundOr] 함수는 여러 개의 [Op]를 OR 연산자로 결합합니다.
             *
             * ```sql
             * SELECT users.id, users."name", users.city_id, users.flags
             *   FROM users
             *  WHERE (users."name" = 'Andrey')
             *     OR (users."name" = 'Sergey')
             *     OR (users."name" = 'Eugene')
             *     OR (users."name" = 'Alex')
             *     OR (users."name" = 'Something')
             * ```
             */
            val orOp = allUsers.map { Op.build { users.name eq it } }.compoundOr()
            val userNameOr = users.selectAll().where(orOp).map { it[users.name] }.toSet()
            userNameOr shouldBeEqualTo allUsers

            /**
             * [compoundAnd] 함수는 여러 개의 [Op]를 AND 연산자로 결합합니다.
             *
             * ```sql
             * SELECT COUNT(*)
             *   FROM users
             *  WHERE (users."name" = 'Andrey')
             *    AND (users."name" = 'Sergey')
             *    AND (users."name" = 'Eugene')
             *    AND (users."name" = 'Alex')
             *    AND (users."name" = 'Something')
             * ```
             */
            val andOp = allUsers.map { Op.build { users.name eq it } }.compoundAnd()
            users.selectAll().where(andOp).count() shouldBeEqualTo 0L
        }
    }

    /**
     * SELECT with Comment
     *
     * Prefix Comment
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM (/*additional_info*/ SELECT cities.city_id Cities_city_id,       -- prefix comment
     *                                    cities."name" Cities_name
     *                               FROM cities
     *                              WHERE cities."name" = 'Munich'
     *                              GROUP BY cities.city_id, cities."name"
     *                              LIMIT 1
     *        ) subquery
     * ```
     *
     * Suffix Comment
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM (/*additional_info*/ SELECT cities.city_id Cities_city_id,
     *                                    cities."name" Cities_name
     *                               FROM cities
     *                              WHERE cities."name" = 'Munich'
     *                              GROUP BY cities.city_id, cities."name"
     *                              LIMIT 1 /*additional_info*/                 -- suffix comment
     *       ) subquery
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select with comment`(testDB: TestDB) {
        val text = "additional_info"
        val updatedText = "${text}_updated"

        withCitiesAndUsers(testDB) { cities, _, _ ->
            val query = cities.selectAll().where { cities.name eq "Munich" }.limit(1).groupBy(cities.id, cities.name)
            val originalQuery = query.copy()
            val originalSql = query.prepareSQL(this, false)

            // query 선두에 comment 추가
            val commentedFrontSql = query.comment(text).prepareSQL(this, false)
            commentedFrontSql shouldBeEqualTo "/*$text*/ $originalSql"

            // query에는 comment가 선두에 추가되었고, 후미에 추가한다.
            val commentedTwiceSql = query.comment(text, AbstractQuery.CommentPosition.BACK).prepareSQL(this, false)
            commentedTwiceSql shouldBeEqualTo "/*$text*/ $originalSql /*$text*/"

            // 이미 query에는 comment가 존재하므로 IllegalStateException 발생
            expectException<IllegalStateException> {
                query.comment("Testing").toList()
            }

            val commentedBackSql =
                query.adjustComments(AbstractQuery.CommentPosition.FRONT) // 새로운 주석이 지정되지 않았으므로, 기존 주석이 삭제된다.
                    .adjustComments(AbstractQuery.CommentPosition.BACK, updatedText)  // 기존 주석이 삭제되고, 새로운 주석이 추가된다.
                    .prepareSQL(this, false)

            commentedBackSql shouldBeEqualTo "$originalSql /*$updatedText*/"

            originalQuery.comment(text).count() shouldBeEqualTo originalQuery.count()
            originalQuery.comment(text, AbstractQuery.CommentPosition.BACK)
                .count() shouldBeEqualTo originalQuery.count()
        }
    }

    /**
     * LIMIT and OFFSET
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select with limit and offset`(testDB: TestDB) {
        val alphabet = object: Table("alphabet") {
            val letter = char("letter")
        }

        withTables(testDB, alphabet) {
            val allLetters = ('A'..'Z').toList()
            val amount = 10
            val start = 8L

            alphabet.batchInsert(allLetters) { letter ->
                this[alphabet.letter] = letter
            }

            // SELECT alphabet.letter FROM alphabet LIMIT 10
            val limitResult = alphabet.selectAll().limit(amount).map { it[alphabet.letter] }
            limitResult shouldBeEqualTo allLetters.take(amount)

            // SELECT alphabet.letter FROM alphabet LIMIT 10 OFFSET 8
            val limitOffsetResult = alphabet.selectAll().limit(amount).offset(start).map { it[alphabet.letter] }
            limitOffsetResult shouldBeEqualTo allLetters.drop(start.toInt()).take(amount)

            if (testDB !in TestDB.ALL_MYSQL_LIKE) {
                // SELECT alphabet.letter FROM alphabet OFFSET 8
                val offsetResult = alphabet.selectAll().offset(start).map { it[alphabet.letter] }

                offsetResult shouldBeEqualTo allLetters.drop(start.toInt())
            }
        }
    }

}
