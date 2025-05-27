package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assumptions

@Suppress("UnusedReceiverParameter")
object JacksonSchema {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS jackson_table (
     *      id SERIAL PRIMARY KEY,
     *      jackson_column JSON NOT NULL
     * )
     * ```
     */
    object JacksonTable: IntIdTable("jackson_table") {
        val jacksonColumn = jackson<DataHolder>("jackson_column")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS jackson_b_table (
     *      id SERIAL PRIMARY KEY,
     *      jackson_b_column JSONB NOT NULL
     * );
     * ```
     */
    object JacksonBTable: IntIdTable("jackson_b_table") {
        val jacksonBColumn = jacksonb<DataHolder>("jackson_b_column")
    }

    class JacksonEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<JacksonEntity>(JacksonTable)

        var jacksonColumn by JacksonTable.jacksonColumn
    }

    class JacksonBEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<JacksonBEntity>(JacksonBTable)

        var jacksonBColumn by JacksonBTable.jacksonBColumn
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS jackson_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSON NOT NULL,
     *      numbers JSON NOT NULL
     * );
     * ```
     */
    object JacksonArrayTable: IntIdTable("jackson_arrays") {
        val groups = jackson<UserGroup>("groups")
        val numbers = jackson<IntArray>("numbers")
    }

    object JacksonBArrayTable: IntIdTable("jackson_b_arrays") {
        val groups = jacksonb<UserGroup>("groups")
        val numbers = jacksonb<IntArray>("numbers")
    }


    data class DataHolder(val user: User, val logins: Int, val active: Boolean, val team: String?)

    data class User(val name: String, val team: String?)

    data class UserGroup(val users: List<User>)

    fun AbstractExposedTest.withJacksonTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: JacksonSchema.JacksonTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = JacksonSchema.JacksonTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.jacksonColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    fun AbstractExposedTest.withJacksonBTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: JacksonSchema.JacksonBTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = JacksonSchema.JacksonBTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.jacksonBColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    fun AbstractExposedTest.withJacksonArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: JacksonSchema.JacksonArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = JacksonSchema.JacksonArrayTable

        withTables(testDB, tester) {
            val singleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(listOf(User("A", "Team A")))
                it[tester.numbers] = intArrayOf(100)
            }
            val tripleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(List(3) { i -> User("${'B' + i}", "Team ${'B' + i}") })
                it[tester.numbers] = intArrayOf(3, 4, 5)
            }

            statement(tester, singleId, tripleId)
        }
    }

    fun AbstractExposedTest.withJacksonBArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: JacksonSchema.JacksonBArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = JacksonSchema.JacksonBArrayTable

        withTables(testDB, tester) {
            val singleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(listOf(User("A", "Team A")))
                it[tester.numbers] = intArrayOf(100)
            }
            val tripleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(List(3) { i -> User("${'B' + i}", "Team ${'B' + i}") })
                it[tester.numbers] = intArrayOf(3, 4, 5)
            }

            statement(tester, singleId, tripleId)
        }
    }
}
