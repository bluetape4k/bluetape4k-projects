package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.exposed.core.jackson.JacksonSchema.DataHolder
import io.bluetape4k.exposed.core.jackson.JacksonSchema.User
import io.bluetape4k.exposed.core.jackson.JacksonSchema.withJacksonBArrays
import io.bluetape4k.exposed.core.jackson.JacksonSchema.withJacksonBTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.currentDialectTest
import io.bluetape4k.exposed.tests.expectException
import io.bluetape4k.exposed.tests.withDb
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.addLogger
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("DEPRECATION")
class JacksonBColumnTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * INSERT INTO jackson_b_table (jackson_b_column)
     * VALUES ({"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"});
     *
     * -- MySQL V8
     * INSERT INTO jackson_b_table (jackson_b_column)
     * VALUES ({"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"});
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select with jacksonb`(testDB: TestDB) {
        withJacksonBTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[jacksonBColumn] = newData
            }

            val newRow = tester.selectAll().where { tester.id eq newId }.single()
            newRow[tester.jacksonBColumn] shouldBeEqualTo newData
        }
    }

    /**
     * ```sql
     * -- Postgres
     * UPDATE jackson_b_table
     *    SET jackson_b_column={"user":{"name":"Admin","team":null},"logins":10,"active":false,"team":null}
     *
     * -- MySQL V8
     * UPDATE jackson_b_table
     *    SET jackson_b_column={"user":{"name":"Admin","team":null},"logins":10,"active":false,"team":null}
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update jacksonb column`(testDB: TestDB) {
        withJacksonBTable(testDB) { tester, _, data1 ->
            tester.selectAll().single()[tester.jacksonBColumn] shouldBeEqualTo data1

            val updatedData = data1.copy(active = false)
            tester.update {
                it[jacksonBColumn] = updatedData
            }

            tester.selectAll().single()[tester.jacksonBColumn] shouldBeEqualTo updatedData
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT JSONB_EXTRACT_PATH(jackson_b_table.jackson_b_column, 'active') FROM jackson_b_table;
     * SELECT JSONB_EXTRACT_PATH(jackson_b_table.jackson_b_column, 'user') FROM jackson_b_table;
     * SELECT JSONB_EXTRACT_PATH_TEXT(jackson_b_table.jackson_b_column, 'user', 'name') FROM jackson_b_table;
     *
     * -- MySQL V8
     * SELECT JSON_EXTRACT(jackson_b_table.jackson_b_column, "$.active") FROM jackson_b_table;
     * SELECT JSON_EXTRACT(jackson_b_table.jackson_b_column, "$.user") FROM jackson_b_table;
     * SELECT JSON_UNQUOTE(JSON_EXTRACT(jackson_b_table.jackson_b_column, "$.user.name")) FROM jackson_b_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select with slice extract`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBTable(testDB) { tester, user1, data1 ->
            val pathPrefix = if (currentDialectTest is PostgreSQLDialect) "" else "."
            val isActive = tester.jacksonBColumn.extract<Boolean>("${pathPrefix}active", toScalar = false)
            val row1 = tester.select(isActive).singleOrNull()
            row1?.get(isActive) shouldBeEqualTo data1.active

            val storedUser = tester.jacksonBColumn.extract<User>("${pathPrefix}user", toScalar = false)
            val row2 = tester.select(storedUser).singleOrNull()
            row2?.get(storedUser) shouldBeEqualTo user1

            val path = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("user", "name")
                else -> arrayOf(".user.name")
            }
            val username = tester.jacksonBColumn.extract<String>(*path)
            val row3 = tester.select(username).singleOrNull()
            row3?.get(username) shouldBeEqualTo user1.name
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_b_table.id
     *   FROM jackson_b_table
     *  WHERE CAST(JSONB_EXTRACT_PATH_TEXT(jackson_b_table.jackson_b_column, 'logins') AS INT) >= 1000;
     *
     * -- MySQL V8
     * SELECT jackson_b_table.id
     *   FROM jackson_b_table
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(jackson_b_table.jackson_b_column, "$.logins")) >= 1000;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select where with extract`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBTable(testDB) { tester, _, data1 ->
            val newId = tester.insertAndGetId {
                it[jacksonBColumn] = data1.copy(logins = 1000)
            }

            // Postgres requires type casting to compare jsonb field as integer value in DB ???
            val logins = when (currentDialectTest) {
                is PostgreSQLDialect ->
                    tester.jacksonBColumn.extract<Int>("logins").castTo(IntegerColumnType())
                else ->
                    tester.jacksonBColumn.extract<Int>(".logins")
            }
            val tooManyLogins = logins greaterEq 1000

            val row = tester.select(tester.id).where { tooManyLogins }.singleOrNull()
            row?.get(tester.id) shouldBeEqualTo newId
        }
    }

    /**
     * ```sql
     * -- Postgres
     * UPDATE jackson_b_table
     *   SET jackson_b_column={"user":{"name":"Admin","team":"Alpha"},"logins":99,"active":true,"team":null}
     * WHERE jackson_b_table.id = 1;
     *
     * SELECT jackson_b_table.id, jackson_b_table.jackson_b_column
     *   FROM jackson_b_table
     *  WHERE CAST(JSONB_EXTRACT_PATH_TEXT(jackson_b_table.jackson_b_column, 'logins') AS INT) >= 50;
     *
     * -- MySQL V8
     * UPDATE jackson_b_table
     *    SET jackson_b_column={"user":{"name":"Admin","team":"Alpha"},"logins":99,"active":true,"team":null}
     *  WHERE jackson_b_table.id = 1;
     *
     * SELECT jackson_b_table.id, jackson_b_table.jackson_b_column
     *   FROM jackson_b_table
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(jackson_b_table.jackson_b_column, "$.logins")) >= 50;
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 함수로 JacksonB 컬럼 사용하기`(testDB: TestDB) {
        val dataTable = JacksonSchema.JacksonBTable
        val dataEntity = JacksonSchema.JacksonBEntity

        withTables(testDB, dataTable) {
            val dataA = DataHolder(User("Admin", "Alpha"), 10, true, null)
            val newUser = dataEntity.new {
                jacksonBColumn = dataA
            }
            dataEntity.findById(newUser.id)?.jacksonBColumn shouldBeEqualTo dataA

            val updatedUser = dataA.copy(logins = 99)
            dataTable.update({ dataTable.id eq newUser.id }) {
                it[jacksonBColumn] = updatedUser
            }
            dataEntity.all().single().jacksonBColumn shouldBeEqualTo updatedUser

            if (testDB !in TestDB.ALL_H2) {
                dataEntity.new { jacksonBColumn = dataA }
                val loginCount = when (currentDialectTest) {
                    is PostgreSQLDialect -> dataTable.jacksonBColumn.extract<Int>("logins").castTo(IntegerColumnType())
                    else -> dataTable.jacksonBColumn.extract<Int>(".logins")
                }
                val frequentUser = dataEntity.find { loginCount greaterEq 50 }.single()
                frequentUser.jacksonBColumn shouldBeEqualTo updatedUser
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT COUNT(*) FROM jackson_b_table
     *  WHERE jackson_b_table.jackson_b_column @> '{"active":false}';
     *
     * SELECT COUNT(*) FROM jackson_b_table
     *  WHERE jackson_b_table.jackson_b_column @> '{"user":{"name":"Admin","team":"Alpha"}}';
     *
     * -- MySQL V8
     * SELECT COUNT(*) FROM jackson_b_table
     *  WHERE JSON_CONTAINS(jackson_b_table.jackson_b_column, '{"active":false}');
     *
     * SELECT COUNT(*) FROM jackson_b_table
     *  WHERE JSON_CONTAINS(jackson_b_table.jackson_b_column, '{"user":{"name":"Admin","team":"Alpha"}}');
     *
     * SELECT jackson_b_table.id FROM jackson_b_table
     *  WHERE JSON_CONTAINS(jackson_b_table.jackson_b_column, '"Alpha"', '$.user.team');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb contains`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBTable(testDB) { tester, user1, data1 ->
            val alphaTeamUser = user1.copy(team = "Alpha")
            val newId = tester.insertAndGetId {
                it[jacksonBColumn] = data1.copy(user = alphaTeamUser)
            }

            val userIsInActive = tester.jacksonBColumn.contains("""{"active":false}""")
            tester.selectAll().where { userIsInActive }.count() shouldBeEqualTo 0L

            val alphaTeamUserAsJson = """{"user":${DefaultJacksonSerializer.serializeAsString(alphaTeamUser)}}"""
            val userIsInAlphaTeam = tester.jacksonBColumn.contains(alphaTeamUserAsJson)
            tester.selectAll().where { userIsInAlphaTeam }.count() shouldBeEqualTo 1L

            // test target contains candidate at specified path
            if (testDB in TestDB.ALL_MYSQL_MARIADB) {
                val userIsInAlphaTeam2 = tester.jacksonBColumn.contains("\"Alpha\"", ".user.team")
                val alphaTeamUsers = tester.select(tester.id).where { userIsInAlphaTeam2 }
                alphaTeamUsers.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb exists 함수 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBTable(testDB) { tester, _, data1 ->
            val maximumLogins = 1000
            val teamA = "A"
            val newId = tester.insertAndGetId {
                it[jacksonBColumn] = data1.copy(user = data1.user.copy(team = teamA), logins = maximumLogins)
            }

            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            // test data at path root '$' exists by providing no path arguments
            // SELECT COUNT(*) FROM jackson_b_table WHERE JSONB_PATH_EXISTS(jackson_b_table.jackson_b_column, '$')
            val hasAnyData = tester.jacksonBColumn.exists(optional = optional)
            tester.selectAll().where { hasAnyData }.count() shouldBeEqualTo 2L

            // SELECT COUNT(*) FROM jackson_b_table WHERE JSONB_PATH_EXISTS(jackson_b_table.jackson_b_column, '$.fakeKey')
            val hasFakeKey = tester.jacksonBColumn.exists(".fakeKey", optional = optional)
            tester.selectAll().where { hasFakeKey }.count() shouldBeEqualTo 0L

            // SELECT COUNT(*) FROM jackson_b_table WHERE JSONB_PATH_EXISTS(jackson_b_table.jackson_b_column, '$.logins')
            val hasLogins = tester.jacksonBColumn.exists(".logins", optional = optional)
            tester.selectAll().where { hasLogins }.count() shouldBeEqualTo 2L

            // test data at path exists with filter condition & optional arguments
            if (currentDialectTest is PostgreSQLDialect) {
                // SELECT jackson_b_table.id FROM jackson_b_table
                //  WHERE JSONB_PATH_EXISTS(jackson_b_table.jackson_b_column, '$.logins ? (@ == 1000)')
                val filterPath = ".logins ? (@ == $maximumLogins)"
                val hasMaxLogins = tester.jacksonBColumn.exists(filterPath)
                val usersWithMaxLogin = tester.select(tester.id).where { hasMaxLogins }
                usersWithMaxLogin.single()[tester.id] shouldBeEqualTo newId

                // SELECT jackson_b_table.id FROM jackson_b_table
                //  WHERE JSONB_PATH_EXISTS(jackson_b_table.jackson_b_column, '$.user.team ? (@ == $team)', '{"team":"A"}')
                val (jsonPath, optionalArg) = ".user.team ? (@ == \$team)" to "{\"team\":\"$teamA\"}"
                val isOnTeamA = tester.jacksonBColumn.exists(jsonPath, optional = optionalArg)
                val usersOnTeamA = tester.select(tester.id).where { isOnTeamA }
                usersOnTeamA.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_b_arrays.id, jackson_b_arrays."groups", jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSONB_EXTRACT_PATH_TEXT(jackson_b_arrays."groups", 'users', '0', 'team') = 'Team A';
     *
     * SELECT JSONB_EXTRACT_PATH_TEXT(jackson_b_arrays.numbers, '0')
     *   FROM jackson_b_arrays;
     *
     * -- MySQL V8
     * SELECT jackson_b_arrays.id, jackson_b_arrays.`groups`, jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(jackson_b_arrays.`groups`, "$.users[0].team")) = 'Team A';
     *
     * SELECT JSON_UNQUOTE(JSON_EXTRACT(jackson_b_arrays.numbers, "$[0]"))
     *   FROM jackson_b_arrays;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb 컬럼 배열 값에 extract 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBArrays(testDB) { tester, singleId, tripleId ->
            val path1 = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("users", "0", "team")
                else -> arrayOf(".users[0].team")
            }
            val firstIsOnTeamA = tester.groups.extract<String>(*path1) eq "Team A"
            tester.selectAll().where { firstIsOnTeamA }.single()[tester.id] shouldBeEqualTo singleId

            // older MySQL and MariaDB versions require non-scalar extracted value from JSON Array
            val toScalar = testDB != TestDB.MYSQL_V5
            val path2 = when (currentDialectTest) {
                is PostgreSQLDialect -> "0"
                else -> "[0]"
            }
            val firstNumber = tester.numbers.extract<Int>(path2, toScalar = toScalar)
            tester.select(firstNumber).map { it[firstNumber] } shouldBeEqualTo listOf(100, 3)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_b_arrays.id, jackson_b_arrays."groups", jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE jackson_b_arrays.numbers @> '[3, 5]';
     *
     * -- MySQL V8
     * SELECT jackson_b_arrays.id, jackson_b_arrays.`groups`, jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSON_CONTAINS(jackson_b_arrays.numbers, '[3, 5]');
     *
     * SELECT jackson_b_arrays.id, jackson_b_arrays.`groups`, jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSON_CONTAINS(jackson_b_arrays.`groups`, '"B"', '$.users[0].name');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `배열 수형의 jacksonb 컬럼에서 contains 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBArrays(testDB) { tester, _, tripleId ->
            val hasSmallNumbers = tester.numbers.contains("[3, 5]")
            tester.selectAll().where { hasSmallNumbers }.single()[tester.id] shouldBeEqualTo tripleId

            if (testDB in TestDB.ALL_MYSQL_LIKE) {
                val hasUserNamB = tester.groups.contains("\"B\"", ".users[0].name")
                tester.selectAll().where { hasUserNamB }.single()[tester.id] shouldBeEqualTo tripleId
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_b_arrays.id, jackson_b_arrays."groups", jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSONB_PATH_EXISTS(jackson_b_arrays."groups", '$.users[1]');
     *
     * SELECT jackson_b_arrays.id, jackson_b_arrays."groups", jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSONB_PATH_EXISTS(jackson_b_arrays.numbers, '$[2]');
     *
     * -- MySQL V8
     * SELECT jackson_b_arrays.id, jackson_b_arrays.`groups`, jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSON_CONTAINS_PATH(jackson_b_arrays.`groups`, 'one', '$.users[1]');
     *
     * SELECT jackson_b_arrays.id, jackson_b_arrays.`groups`, jackson_b_arrays.numbers
     *   FROM jackson_b_arrays
     *  WHERE JSON_CONTAINS_PATH(jackson_b_arrays.numbers, 'one', '$[2]');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `배열 수형의 jacksonb 컬럼에 exists 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonBArrays(testDB) { tester, _, tripleId ->
            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            val hasMultipleUsers = tester.groups.exists(".users[1]", optional = optional)
            tester.selectAll().where { hasMultipleUsers }.single()[tester.id] shouldBeEqualTo tripleId

            val hasAtLeast3Numbers = tester.numbers.exists("[2]", optional = optional)
            tester.selectAll().where { hasAtLeast3Numbers }.single()[tester.id] shouldBeEqualTo tripleId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb 컬럼의 default 값 사용하기`(testDB: TestDB) {
        val defaultUser = User("UNKNOWN", "UNASSIGNED")
        val defaultTester = object: Table("default_tester") {
            val user1 = jacksonb<User>("user_1").default(defaultUser)
            val user2 = jacksonb<User>("user_2").clientDefault { defaultUser }
        }

        withDb(testDB) {
            if (testDB == TestDB.MYSQL_V5) {
                expectException<UnsupportedByDialectException> {
                    SchemaUtils.createMissingTablesAndColumns(defaultTester)
                }
            } else {
                SchemaUtils.createMissingTablesAndColumns(defaultTester)
                defaultTester.exists().shouldBeTrue()

                // ensure defaults match returned metadata defaults
                val alters = SchemaUtils.statementsRequiredToActualizeScheme(defaultTester)
                alters.shouldBeEmpty()

                defaultTester.insert { }

                defaultTester.selectAll().single().also {
                    it[defaultTester.user1].name shouldBeEqualTo defaultUser.name
                    it[defaultTester.user1].team shouldBeEqualTo defaultUser.team

                    it[defaultTester.user2].name shouldBeEqualTo defaultUser.name
                    it[defaultTester.user2].team shouldBeEqualTo defaultUser.team
                }

                SchemaUtils.drop(defaultTester)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `logging with jacksonb collections`(testDB: TestDB) {
        val iterables = object: Table("iterables_tester") {
            val userList = jacksonb<List<User>>("user_list")
            val intList = jacksonb<List<Int>>("int_list")
            val userArray = jacksonb<Array<User>>("user_array")
            val intArray = jacksonb<IntArray>("int_array")
        }

        withTables(testDB, iterables) {
            // the logger is left in to test that it does not throw ClassCastException on insertion of iterables
            addLogger(StdOutSqlLogger)

            val user1 = User("A", "Team A")
            val user2 = User("B", "Team B")
            val integerList = listOf(1, 2, 3)
            val integerArray = intArrayOf(1, 2, 3)

            iterables.insert {
                it[userList] = listOf(user1, user2)
                it[intList] = integerList
                it[userArray] = arrayOf(user1, user2)
                it[intArray] = integerArray
            }

            val row = iterables.selectAll().single()
            row[iterables.userList] shouldBeEqualTo listOf(user1, user2)
            row[iterables.intList] shouldBeEqualTo integerList
            row[iterables.userArray] shouldBeEqualTo arrayOf(user1, user2)
            row[iterables.intArray] shouldBeEqualTo integerArray
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nullable jacksonb column`(testDB: TestDB) {
        val tester = object: IntIdTable("nullable_tester") {
            val user = jacksonb<User>("user").nullable()
        }

        withTables(testDB, tester) {
            val nullId = tester.insertAndGetId {
                it[user] = null
            }

            val nonNullId = tester.insertAndGetId {
                it[user] = User("A", "Team A")
            }

            val row1 = tester.select(tester.user).where { tester.id eq nullId }.single()
            row1[tester.user].shouldBeNull()

            val row2 = tester.select(tester.user).where { tester.id eq nonNullId }.single()
            row2[tester.user] shouldBeEqualTo User("A", "Team A")
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb column with upsert`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1)

        withJacksonBTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[jacksonBColumn] = newData
            }

            val newData2 = newData.copy(active = false)
            tester.upsert {
                it[tester.id] = newId
                it[jacksonBColumn] = newData2
            }

            val newRow = tester.selectAll().where { tester.id eq newId }.single()
            newRow[tester.jacksonBColumn] shouldBeEqualTo newData2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb with transformer`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val numbers: Column<DoubleArray> = jacksonb<IntArray>("numbers").transform(
                wrap = { DoubleArray(it.size) { i -> 1.0 * it[i] } },
                unwrap = { IntArray(it.size) { i -> it[i].toInt() } }
            )
        }
        withTables(testDB, tester) {
            val data = doubleArrayOf(1.0, 2.0, 3.0)
            tester.insert {
                it[numbers] = data
            }

            tester.selectAll().single()[tester.numbers] shouldBeEqualTo data
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jacksonb column with databaseGenerated`(testDB: TestDB) {

        val defaultUser = User("name", "team")

        val tester = object: IntIdTable("jackson_default") {
            val value = jacksonb<User>("value").default(defaultUser)
        }

        val testerDatabaseGenerated = object: IntIdTable("jackson_default") {
            val value = jacksonb<User>("value").databaseGenerated()
        }

        // MySQL versions prior to 8.0.13 do not accept default values on JSON columns
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }
        withTables(testDB, tester) {
            testerDatabaseGenerated.insert { }

            val value = testerDatabaseGenerated.selectAll().single()[tester.value]
            value shouldBeEqualTo defaultUser
        }
    }

    private class KeyExistsOp(left: Expression<*>, right: Expression<*>): ComparisonOp(left, right, "??")

    private infix fun ExpressionWithColumnType<*>.keyExists(other: String) = KeyExistsOp(this, stringParam(other))

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_b_table.id, jackson_b_table.jackson_b_column
     *   FROM jackson_b_table
     *  WHERE jackson_b_table.jackson_b_column ?? 'logins';
     *
     * SELECT jackson_b_table.id, jackson_b_table.jackson_b_column
     *   FROM jackson_b_table
     *  WHERE jackson_b_table.jackson_b_column ?? 'name';
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `escaped placeholder in custom operator`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB in TestDB.ALL_POSTGRES)

        withJacksonBTable(testDB) { tester, _, data1 ->
            val topLevelKeyResult = tester.selectAll().where { tester.jacksonBColumn keyExists "logins" }.single()
            topLevelKeyResult[tester.jacksonBColumn] shouldBeEqualTo data1

            val nestedKeyResult = tester.selectAll().where { tester.jacksonBColumn keyExists "name" }.toList()
            nestedKeyResult.shouldBeEmpty()
        }
    }
}
