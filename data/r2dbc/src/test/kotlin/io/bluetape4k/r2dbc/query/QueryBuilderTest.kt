package io.bluetape4k.r2dbc.query

import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.Parameter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.test.assertFailsWith

class QueryBuilderTest {

    companion object: KLogging()

    @Test
    fun `select statement 지원`() {
        val query = query {
            select("select * from actor")
        }
        query.sql shouldBeEqualTo """
            select * from actor
        """.trimIndent()
    }

    @Test
    fun `select with order by 지원`() {
        val query = query {
            select("select * from actor")
            orderBy("actor_id")
        }
        query.sql shouldBeEqualTo """
            select * from actor
            order by actor_id
            """.trimIndent()
    }

    @Test
    fun `select with where`() {
        val query = query {
            select("select * from actor")
            whereGroup {
                where("first_name = 'Kate'")
            }
        }
        query.sql shouldBeEqualTo """
            select * from actor
            where first_name = 'Kate'
            """.trimIndent()
    }

    @Test
    fun `select with where with param`() {
        val query = query {
            select("select * from actor")
            whereGroup {
                where("first_name = :first_name")
                parameter("first_name", "Kate")
            }
        }
        query.sql shouldBeEqualTo """
            select * from actor
            where first_name = :first_name
            """.trimIndent()

        query.parameters.keys shouldBeEqualTo setOf("first_name")
    }

    @Test
    fun `select with where with multiple params`() {
        val query = query {
            select("select * from actor")
            whereGroup {
                where("first_name = :first_name")
                parameter("first_name", "Kate")

                where("last_name = :last_name")
                parameter("last_name", "Bae")
            }
        }
        query.sql shouldBeEqualTo """
            select * from actor
            where first_name = :first_name and last_name = :last_name
            """.trimIndent()

        query.parameters.keys shouldBeEqualTo setOf("first_name", "last_name")
    }

    @Test
    fun `select with where with params and operator`() {
        val query = query {
            select("select * from actor")
            whereGroup("or") {
                where("first_name = :first_name")
                parameter("first_name", "Kate")

                where("last_name = :last_name")
                parameter("last_name", "Bae")
            }
        }
        query.sql shouldBeEqualTo """
            select * from actor
            where first_name = :first_name or last_name = :last_name
            """.trimIndent()

        query.parameters.keys shouldBeEqualTo setOf("first_name", "last_name")
    }

    @Test
    fun `select with nested where`() {
        val query = query {
            select("select * from actor")
            whereGroup {
                where("last_name = 'Bae'")
                whereGroup("or") {
                    where("first_name = 'Kate'")
                    where("first_name = 'Cate'")
                }
            }
        }
        query.sql shouldBeEqualTo """
            select * from actor
            where last_name = 'Bae' and 
            (first_name = 'Kate' or first_name = 'Cate')
            """.trimIndent()
    }

    @Test
    fun `select with empty where group`() {
        val query = query {
            select("select * from actor")
            whereGroup {
                whereGroup("or") {
                }
            }
        }
        query.sql shouldBeEqualTo """
            select * from actor
            """.trimIndent()
    }

    @Test
    fun `select with group by`() {
        val query = query {
            select("select * from actor")
            groupBy("actor_id")
        }
        query.sql shouldBeEqualTo """
            select * from actor
            group by actor_id
            """.trimIndent()
    }

    @Test
    fun `select with group by kotlin property`() {
        val query = query {
            select("select * from actor")
            groupBy(Actor::id)
        }
        query.sql shouldBeEqualTo """
            select * from actor
            group by id
            """.trimIndent()
    }

    @Test
    fun `select with limit`() {
        val query = query {
            select("select * from actor")
            groupBy("actor_id")
            limit(10)

        }
        query.sql shouldBeEqualTo """
            select * from actor
            group by actor_id
            limit 10
            """.trimIndent()
    }

    @Test
    fun `select with limit and offset`() {
        val query = query {
            select("select * from actor")
            groupBy("actor_id")
            limit(10)
            offset(100)
        }
        query.sql shouldBeEqualTo """
            select * from actor
            group by actor_id
            limit 10
            offset 100
            """.trimIndent()
    }

    @Test
    fun `select with group by and having`() {
        val query = query {
            select("select actor_id, count(*) as cnt from actor")
            groupBy("actor_id")
            having("count(*) > 1")

        }
        query.sql shouldBeEqualTo """
            select actor_id, count(*) as cnt from actor
            group by actor_id
            having count(*) > 1
            """.trimIndent()
    }

    @Test
    fun `select with all options`() {
        val query = query {
            select("select a.actor_id, count(*) from actor a")
            select("inner join actor a2 on a2.actor_id = a.actor_id")
            whereGroup("or") {
                where("a.actor_id = :id_1")
                parameter("id_1", 1)
                whereGroup("or") {
                    where("a.actor_id = :id_2")
                    parameter("id_2", 2)
                    where("a.actor_id = :id_3")
                    parameter("id_3", 3)
                }
            }
            groupBy("actor_id")
            having("count(*) > 1")
            orderBy("a.actor_id")
        }
        query.sql shouldBeEqualTo """
            select a.actor_id, count(*) from actor a
            inner join actor a2 on a2.actor_id = a.actor_id
            where a.actor_id = :id_1 or 
            (a.actor_id = :id_2 or a.actor_id = :id_3)
            group by actor_id
            having count(*) > 1
            order by a.actor_id
            """.trimIndent()

        query.parameters.keys shouldBeEqualTo setOf("id_1", "id_2", "id_3")
    }

    @Test
    fun `multiple select clauses`() {
        val query = query {
            select("select a.* from actor a")
            select("inner join actor a2 on a2.actor_id = a.actor_id")
        }

        query.sql shouldBeEqualTo """
            select a.* from actor a
            inner join actor a2 on a2.actor_id = a.actor_id
            """.trimIndent()
    }

    @Test
    fun `no select clauses`() {
        val query = query {
            whereGroup {
                where("first_name = :first_name")
                parameter("first_name", "Kate")
            }
        }

        query.sql shouldBeEqualTo """
            where first_name = :first_name
            """.trimIndent()

        query.parameters.keys shouldBeEqualTo setOf("first_name")
    }

    @Test
    fun `counting select with where with params`() {
        val query = queryCount {
            selectCount("select count(*) from actor")
            whereGroup {
                where("first_name = :first_name")
                parameter("first_name", "Kate")

                where("last_name = :last_name")
                parameter("last_name", "Bae")
            }
        }

        query.sql shouldBeEqualTo """
            select count(*) from actor
            where first_name = :first_name and last_name = :last_name
            """.trimIndent()

        query.parameters.keys shouldBeEqualTo setOf("first_name", "last_name")
    }

    @Test
    fun `nullable parameter helper는 typed null parameter를 생성한다`() {
        val query = query {
            select("select * from actor")
            whereGroup {
                where("name = :name")
                parameterNullable<String>("name", null)
            }
        }

        val param = query.parameters["name"] as Parameter
        (param.value == null).shouldBeTrue()
    }

    @Test
    fun `blank where clause는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            query {
                select("select * from actor")
                whereGroup {
                    where("   ")
                }
            }
        }
    }

    @Test
    fun `blank operator는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            query {
                select("select * from actor")
                whereGroup("   ") {
                    where("id = :id")
                    parameter("id", 1)
                }
            }
        }
    }

    data class Actor(
        val id: Long,
        val name: String,
    ): Serializable
}
