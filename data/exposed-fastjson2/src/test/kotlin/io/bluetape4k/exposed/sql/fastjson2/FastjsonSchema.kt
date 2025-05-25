package io.bluetape4k.exposed.sql.fastjson2

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
object FastjsonSchema {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS fastjson_table (
     *      id SERIAL PRIMARY KEY,
     *      fastjson_column JSON NOT NULL
     * )
     * ```
     */
    object FastjsonTable: IntIdTable("fastjson_table") {
        val fastjsonColumn = fastjson<DataHolder>("fastjson_column")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS fastjson_b_table (
     *      id SERIAL PRIMARY KEY,
     *      fastjson_b_column JSONB NOT NULL
     * );
     * ```
     */
    object FastjsonBTable: IntIdTable("fastjson_b_table") {
        val fastjsonBColumn = fastjsonb<DataHolder>("fastjson_b_column")
    }

    class FastjsonEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<FastjsonEntity>(FastjsonTable)

        var fastjsonColumn by FastjsonTable.fastjsonColumn
    }

    class FastjsonBEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<FastjsonBEntity>(FastjsonBTable)

        var fastjsonBColumn by FastjsonBTable.fastjsonBColumn
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS fastjson_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSON NOT NULL,
     *      numbers JSON NOT NULL
     * );
     * ```
     */
    object FastjsonArrayTable: IntIdTable("fastjson_arrays") {
        val groups = fastjson<UserGroup>("groups")
        val numbers = fastjson<IntArray>("numbers")
    }

    object FastjsonBArrayTable: IntIdTable("fastjson_b_arrays") {
        val groups = fastjsonb<UserGroup>("groups")
        val numbers = fastjsonb<IntArray>("numbers")
    }


    data class DataHolder(val user: User, val logins: Int, val active: Boolean, val team: String?)

    data class User(val name: String, val team: String?)

    data class UserGroup(val users: List<User>)

    fun AbstractExposedTest.withFastjsonTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: FastjsonSchema.FastjsonTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = FastjsonSchema.FastjsonTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.fastjsonColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    fun AbstractExposedTest.withFastjsonBTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: FastjsonSchema.FastjsonBTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = FastjsonSchema.FastjsonBTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.fastjsonBColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    fun AbstractExposedTest.withFastjsonArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: FastjsonSchema.FastjsonArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = FastjsonSchema.FastjsonArrayTable

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

    fun AbstractExposedTest.withFastjsonBArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: FastjsonSchema.FastjsonBArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = FastjsonSchema.FastjsonBArrayTable

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
