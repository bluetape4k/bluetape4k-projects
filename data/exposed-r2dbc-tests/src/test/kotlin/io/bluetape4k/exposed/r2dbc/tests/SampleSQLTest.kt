package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.insert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SampleSQLTest: R2dbcExposedTestBase() {

    companion object: KLoggingChannel()

    object Users: Table() {
        val id: Column<String> = varchar("id", 10)
        val name: Column<String> = varchar("name", length = 50)
        val cityId: Column<Int?> = (integer("city_id") references Cities.id).nullable()

        override val primaryKey = PrimaryKey(id, name = "PK_User_ID") // name is optional here
    }

    object Cities: Table() {
        val id: Column<Int> = integer("id").autoIncrement()
        val name: Column<String> = varchar("name", 50)

        override val primaryKey = PrimaryKey(id, name = "PK_Cities_ID")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `r2dbc with H2`(testDB: TestDB) = runSuspendTest {
        withTables(testDB, Users, Cities) {
            val cityId = Cities.insert {
                it[name] = "Hanam"
            } get Cities.id

            log.debug { "Inserted city with ID: $cityId" }
        }
    }
}
