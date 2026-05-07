package io.bluetape4k.r2dbc.query

import io.r2dbc.spi.Parameter
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [QueryBuilderSupport]의 DSL 헬퍼 함수를 검증합니다.
 *
 * - [parameterNullable]: typed null 파라미터를 올바르게 바인딩
 * - [queryWithCount]: 일반 쿼리와 카운트 쿼리를 함께 반환
 */
class QueryBuilderSupportTest {

    /**
     * [parameterNullable]은 null 값을 typed-null Parameter로 바인딩해야 합니다.
     * InferredParameter.type은 Raw Class와 달라 null 여부와 Parameter 타입 여부만 확인합니다.
     */
    @Test
    fun `parameterNullable - null 값을 typed null Parameter 로 바인딩한다`() {
        val query = query {
            select("SELECT * FROM users")
            whereGroup {
                where("name = :name")
                parameterNullable<String>("name", null)
            }
        }

        val param = query.parameters["name"] as Parameter
        (param.value == null).shouldBeTrue()
    }

    /**
     * [parameterNullable]은 non-null 값을 올바르게 바인딩해야 합니다.
     */
    @Test
    fun `parameterNullable - non-null 값도 정상 바인딩한다`() {
        val query = query {
            select("SELECT * FROM users")
            whereGroup {
                where("age = :age")
                parameterNullable<Int>("age", 30)
            }
        }

        val param = query.parameters["age"] as Parameter
        param.value shouldBeEqualTo 30
    }

    /**
     * [parameterNullable]은 프로퍼티 이름을 키로 사용해야 합니다.
     */
    @Test
    fun `parameterNullable(property) - 프로퍼티 이름을 키로 사용한다`() {
        data class Item(val title: String?)

        val query = query {
            select("SELECT * FROM items")
            whereGroup {
                where("title = :title")
                parameterNullable(Item::title, null as String?)
            }
        }

        val param = query.parameters["title"] as Parameter
        (param.value == null).shouldBeTrue()
    }

    /**
     * [queryWithCount]는 일반 쿼리와 카운트 쿼리를 쌍으로 반환해야 합니다.
     * 두 쿼리 모두 동일한 파라미터를 가져야 합니다.
     */
    @Test
    fun `queryWithCount - 일반 쿼리와 카운트 쿼리를 함께 반환한다`() {
        val (dataQuery, countQuery) = queryWithCount {
            select("SELECT * FROM users")
            selectCount("SELECT COUNT(*) FROM users")
            whereGroup {
                where("active = :active")
                parameter("active", true)
            }
        }

        dataQuery.shouldNotBeNull()
        countQuery.shouldNotBeNull()

        // 두 쿼리 모두 동일한 파라미터를 가짐
        dataQuery.parameters.keys shouldContainAll countQuery.parameters.keys
        dataQuery.parameters["active"] shouldBeEqualTo countQuery.parameters["active"]
    }

    /**
     * [queryWithCount]의 카운트 쿼리는 selectCount 절을 포함해야 합니다.
     */
    @Test
    fun `queryWithCount - 카운트 쿼리는 selectCount 절을 포함한다`() {
        val (_, countQuery) = queryWithCount {
            select("SELECT * FROM users")
            selectCount("SELECT COUNT(*) FROM users")
        }

        countQuery.sql shouldBeEqualTo "SELECT COUNT(*) FROM users"
    }
}
