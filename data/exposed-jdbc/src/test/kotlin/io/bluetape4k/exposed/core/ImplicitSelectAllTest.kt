package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainIgnoringCase
import org.amshove.kluent.shouldNotContain
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ImplicitSelectAllTest: AbstractExposedTest() {

    private object Tester: IntIdTable("implicit_select_all_tester") {
        val name = varchar("name", 50)
        val amount = integer("amount")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `selectImplicitAll 은 SELECT 절을 별표로 생성한다`(testDB: TestDB) {
        withTables(testDB, Tester) {
            val query = Tester.selectImplicitAll()
            val sql = query.prepareSQL(QueryBuilder(prepared = true))

            sql shouldContainIgnoringCase "SELECT * FROM"
            sql.shouldNotContain(Tester.name.name)
            sql.shouldNotContain(Tester.amount.name)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `selectImplicitAll 로 실행한 결과는 selectAll 과 동일하다`(testDB: TestDB) {
        withTables(testDB, Tester) {
            Tester.insert {
                it[name] = "Alice"
                it[amount] = 100
            }

            val implicit = Tester.selectImplicitAll().single()
            val explicit = Tester.selectAll().single()

            implicit[Tester.name] shouldBeEqualTo explicit[Tester.name]
            implicit[Tester.amount] shouldBeEqualTo explicit[Tester.amount]
        }
    }
}
