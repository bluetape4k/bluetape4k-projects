package io.bluetape4k.exposed.tests

import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class WithAutoCommitTest: AbstractExposedTest() {
    private object TestTable: IntIdTable("test_autocommit") {
        val name = varchar("name", 255)
    }

    @Test
    fun `withAutoCommit should set autoCommit to true by default`() {
        withDb(TestDB.H2) {
            transaction {
                val originalAutoCommit = connection.autoCommit

                withAutoCommit {
                    connection.autoCommit shouldBeEqualTo true
                }

                connection.autoCommit shouldBeEqualTo originalAutoCommit
            }
        }
    }

    @Test
    fun `withAutoCommit should set autoCommit to specified value`() {
        withDb(TestDB.H2) {
            transaction {
                val originalAutoCommit = connection.autoCommit

                withAutoCommit(autoCommit = false) {
                    connection.autoCommit shouldBeEqualTo false
                }

                connection.autoCommit shouldBeEqualTo originalAutoCommit
            }
        }
    }

    @Test
    fun `withAutoCommit should restore autoCommit on exception`() {
        withDb(TestDB.H2) {
            transaction {
                val originalAutoCommit = connection.autoCommit

                try {
                    withAutoCommit {
                        throw RuntimeException("Test exception")
                    }
                } catch (ex: RuntimeException) {
                    // ignore
                }

                connection.autoCommit shouldBeEqualTo originalAutoCommit
            }
        }
    }

    @Test
    fun `withAutoCommit should execute statement within context`() {
        withDb(TestDB.H2) {
            transaction {
                var executed = false
                withAutoCommit {
                    executed = true
                }
                executed shouldBeEqualTo true
            }
        }
    }
}
