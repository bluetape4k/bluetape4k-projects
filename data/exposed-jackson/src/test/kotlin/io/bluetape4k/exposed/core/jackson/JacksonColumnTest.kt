package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.exposed.core.jackson.JacksonSchema.DataHolder
import io.bluetape4k.exposed.core.jackson.JacksonSchema.User
import io.bluetape4k.exposed.core.jackson.JacksonSchema.withJacksonArrays
import io.bluetape4k.exposed.core.jackson.JacksonSchema.withJacksonTable
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
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
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
class JacksonColumnTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- H2
     * INSERT INTO JACKSON_TABLE (JACKSON_COLUMN)
     * VALUES (JSON '{"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"}' FORMAT JSON);
     *
     * -- MySQL V8
     * INSERT INTO jackson_table (jackson_column)
     * VALUES ({"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"});
     *
     * -- Postgres
     * INSERT INTO jackson_table (jackson_column)
     * VALUES ({"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"});
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `JSON 컬럼에 대해 삽입 및 조회하기`(testDB: TestDB) {
        withJacksonTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[jacksonColumn] = newData
            }

            val row = tester.selectAll().where { tester.id eq newId }.single()
            row[tester.jacksonColumn] shouldBeEqualTo newData
        }
    }

    /**
     * ```sql
     * -- Postgres
     * UPDATE jackson_table
     *    SET jackson_column={"user":{"name":"Admin","team":null},"logins":10,"active":false,"team":null}
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update JSON 컬럼`(testDB: TestDB) {
        withJacksonTable(testDB) { tester, _, data1 ->
            tester.selectAll().single()[tester.jacksonColumn] shouldBeEqualTo data1

            val updatedData = data1.copy(active = false)
            tester.update {
                it[tester.jacksonColumn] = updatedData
            }

            tester.selectAll().single()[tester.jacksonColumn] shouldBeEqualTo updatedData
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT JSON_EXTRACT_PATH(jackson_table.jackson_column, 'active') FROM jackson_table;
     * SELECT JSON_EXTRACT_PATH(jackson_table.jackson_column, 'user') FROM jackson_table;
     * SELECT JSON_EXTRACT_PATH_TEXT(jackson_table.jackson_column, 'user', 'name') FROM jackson_table;
     *
     * -- MySQL V8
     * SELECT JSON_EXTRACT(jackson_table.jackson_column, "$.active") FROM jackson_table;
     * SELECT JSON_EXTRACT(jackson_table.jackson_column, "$.user") FROM jackson_table;
     * SELECT JSON_UNQUOTE(JSON_EXTRACT(jackson_table.jackson_column, "$.user.name")) FROM jackson_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `JSON 컬럼의 특정 속성만 조회하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonTable(testDB) { tester, user1, data1 ->
            val pathPrefix = if (currentDialectTest is PostgreSQLDialect) "" else "."

            // SQLServer & Oracle return null if extracted JSON is not scalar
            val requiresScala = currentDialectTest is SQLServerDialect || currentDialectTest is OracleDialect
            val isActive = tester.jacksonColumn.extract<Boolean>("${pathPrefix}active", toScalar = requiresScala)
            val row = tester.select(isActive).singleOrNull()
            row?.get(isActive) shouldBeEqualTo data1.active

            val storedUser = tester.jacksonColumn.extract<User>("${pathPrefix}user", toScalar = false)
            val row2 = tester.select(storedUser).singleOrNull()
            row2?.get(storedUser) shouldBeEqualTo user1

            val path = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("user", "name")
                else -> arrayOf(".user.name")
            }
            val username = tester.jacksonColumn.extract<String>(*path)
            val row3 = tester.select(username).singleOrNull()
            row3?.get(username) shouldBeEqualTo user1.name
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_table.id
     *   FROM jackson_table
     *  WHERE CAST(JSON_EXTRACT_PATH_TEXT(jackson_table.jackson_column, 'logins') AS INT) >= 1000;
     *
     * -- MySQL V8
     * SELECT jackson_table.id
     *   FROM jackson_table
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(jackson_table.jackson_column, "$.logins")) >= 1000
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `extract 해서 검색하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonTable(testDB) { tester, _, data1 ->
            val newId = tester.insertAndGetId {
                it[jacksonColumn] = data1.copy(logins = 1000)
            }

            // Postgres requires type casting to compare json field as integer value in DB
            val logins = when (currentDialectTest) {
                is PostgreSQLDialect -> tester.jacksonColumn.extract<Int>("logins").castTo(IntegerColumnType())
                else -> tester.jacksonColumn.extract<Int>(".logins")
            }
            val tooManyLogins = logins greaterEq 1000

            val row = tester.select(tester.id).where { tooManyLogins }.singleOrNull()
            row?.get(tester.id) shouldBeEqualTo newId
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (j_col JSON NOT NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `data class 를 Jackson 으로 직렬화하기`(testDB: TestDB) {
        data class Fake(val number: Int)

        withDb(testDB) {
            val tester = object: Table("tester") {
                val jCol = jackson<Fake>("j_col")
            }
            SchemaUtils.create(tester)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * INSERT INTO jackson_table (jackson_column)
     * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null});
     *
     * UPDATE jackson_table
     *    SET jackson_column={"user":{"name":"Lead","team":"Beta"},"logins":10,"active":true,"team":null}
     *  WHERE jackson_table.id = 1;
     *
     * -- MySQL V8
     * INSERT INTO jackson_table (jackson_column)
     * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null});
     *
     * UPDATE jackson_table
     *    SET jackson_column={"user":{"name":"Lead","team":"Beta"},"logins":10,"active":true,"team":null}
     *  WHERE jackson_table.id = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 함수로 Jackson 컬럼 사용하기`(testDB: TestDB) {
        val dataTable = JacksonSchema.JacksonTable
        val dataEntity = JacksonSchema.JacksonEntity

        withTables(testDB, dataTable) {
            val dataA = DataHolder(User("Admin", "Alpha"), 10, true, null)
            val newUser = dataEntity.new {
                jacksonColumn = dataA
            }
            dataEntity.findById(newUser.id)?.jacksonColumn shouldBeEqualTo dataA

            val updatedUser = dataA.copy(user = User("Lead", "Beta"))
            dataTable.update({ dataTable.id eq newUser.id }) {
                it[jacksonColumn] = updatedUser
            }
            dataEntity.all().single().jacksonColumn shouldBeEqualTo updatedUser
        }
    }

    private val supportsJsonContains = TestDB.ALL_POSTGRES + TestDB.ALL_MYSQL_MARIADB

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_table.id, jackson_table.jackson_column
     *   FROM jackson_table
     *  WHERE jackson_table.jackson_column::jsonb @> '{"active":false}'::jsonb;
     *
     * SELECT COUNT(*)
     *   FROM jackson_table
     *  WHERE jackson_table.jackson_column::jsonb @> '{"user":{"name":"Admin","team":"Alpha"}}'::jsonb;
     *
     * -- MySQL V8
     * SELECT jackson_table.id, jackson_table.jackson_column
     *   FROM jackson_table
     *  WHERE JSON_CONTAINS(jackson_table.jackson_column, '{"active":false}');
     *
     * SELECT COUNT(*)
     *   FROM jackson_table
     *  WHERE JSON_CONTAINS(jackson_table.jackson_column, '{"user":{"name":"Admin","team":"Alpha"}}');
     *
     * SELECT jackson_table.id
     *   FROM jackson_table
     *  WHERE JSON_CONTAINS(jackson_table.jackson_column, '"Alpha"', '$.user.team');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json contains 함수 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportsJsonContains }

        withJacksonTable(testDB) { tester, user1, data1 ->
            val alphaTeamUser = user1.copy(team = "Alpha")
            val newId = tester.insertAndGetId {
                it[jacksonColumn] = data1.copy(user = alphaTeamUser)
            }

            val userIsInactive = tester.jacksonColumn.contains("""{"active":false}""")
            val rows = tester.selectAll().where { userIsInactive }.toList()
            rows.shouldBeEmpty()

            val alphaTeamUserAsJson = """{"user":${DefaultJacksonSerializer.serializeAsString(alphaTeamUser)}}"""
            var userIsInAlphaTeam = tester.jacksonColumn.contains(stringLiteral(alphaTeamUserAsJson))
            tester.selectAll().where { userIsInAlphaTeam }.count() shouldBeEqualTo 1L

            // test target contains candidate at specified path
            if (testDB in TestDB.ALL_MYSQL_LIKE) {
                userIsInAlphaTeam = tester.jacksonColumn.contains("\"Alpha\"", ".user.team")
                val alphaTeamUsers = tester.select(tester.id).where { userIsInAlphaTeam }
                alphaTeamUsers.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT COUNT(*) FROM jackson_table
     *  WHERE JSONB_PATH_EXISTS(CAST(jackson_table.jackson_column as jsonb), '$');
     *
     * SELECT COUNT(*) FROM jackson_table
     *  WHERE JSONB_PATH_EXISTS(CAST(jackson_table.jackson_column as jsonb), '$.fakeKey');
     *
     * SELECT COUNT(*) FROM jackson_table
     *  WHERE JSONB_PATH_EXISTS(CAST(jackson_table.jackson_column as jsonb), '$.logins');
     *
     * -- MySQL V8
     * SELECT COUNT(*) FROM jackson_table
     *  WHERE JSON_CONTAINS_PATH(jackson_table.jackson_column, 'one', '$');
     *
     * SELECT COUNT(*) FROM jackson_table
     *  WHERE JSON_CONTAINS_PATH(jackson_table.jackson_column, 'one', '$.fakeKey');
     *
     * SELECT COUNT(*) FROM jackson_table
     *  WHERE JSON_CONTAINS_PATH(jackson_table.jackson_column, 'one', '$.logins');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json exists 함수 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonTable(testDB) { tester, _, data1 ->
            val maximumLogins = 1000
            val teamA = "A"
            val newId = tester.insertAndGetId {
                it[jacksonColumn] = data1.copy(user = data1.user.copy(team = teamA), logins = maximumLogins)
            }

            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            // test data at path root '$' exists by providing no path arguments
            val hasAnyData = tester.jacksonColumn.exists(optional = optional)
            tester.selectAll().where { hasAnyData }.count() shouldBeEqualTo 2L

            val hasFakeKey = tester.jacksonColumn.exists(".fakeKey", optional = optional)
            tester.selectAll().where { hasFakeKey }.count() shouldBeEqualTo 0L

            val hasLogins = tester.jacksonColumn.exists(".logins", optional = optional)
            tester.selectAll().where { hasLogins }.count() shouldBeEqualTo 2L

            // test data at path exists with filter condition & optional arguments
            val testDialect = currentDialectTest
            if (testDialect is OracleDialect || testDialect is SQLServerDialect) {
                val filterPath = when (testDialect) {
                    is OracleDialect -> "?(@.logins == $maximumLogins)"
                    else -> ".logins ? (@ == $maximumLogins)"
                }
                val hasMaxLogins = tester.jacksonColumn.exists(filterPath)
                val usersWithMaxLogins = tester.select(tester.id).where { hasMaxLogins }
                usersWithMaxLogins.single()[tester.id] shouldBeEqualTo newId

                val (jsonPath, optionalArg) = when (testDialect) {
                    is OracleDialect -> "?(@.user.team == \$team)" to "PASSING '$teamA' AS \"team\""
                    else -> ".user.team ? (@ == \$team)" to "{\"team\":\"$teamA\"}"
                }
                val isOnTeamA = tester.jacksonColumn.exists(jsonPath, optional = optionalArg)
                val usersOnTeamA = tester.select(tester.id).where { isOnTeamA }
                usersOnTeamA.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT jackson_arrays.id, jackson_arrays."groups", jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSON_EXTRACT_PATH_TEXT(jackson_arrays."groups", 'users', '0', 'team') = 'Team A';
     *
     * SELECT JSON_EXTRACT_PATH_TEXT(jackson_arrays.numbers, '0')
     *   FROM jackson_arrays;
     *
     * -- MySQL V8
     * SELECT jackson_arrays.id, jackson_arrays.`groups`, jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(jackson_arrays.`groups`, "$.users[0].team")) = 'Team A';
     *
     * SELECT JSON_UNQUOTE(JSON_EXTRACT(jackson_arrays.numbers, "$[0]"))
     *   FROM jackson_arrays;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jackson 컬럼 배열 값에 extract 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonArrays(testDB) { tester, singleId, tripleId ->
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
     * SELECT jackson_arrays.id, jackson_arrays."groups", jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE jackson_arrays.numbers::jsonb @> '[3, 5]'::jsonb;
     *
     * -- MySQL V8
     * SELECT jackson_arrays.id, jackson_arrays.`groups`, jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSON_CONTAINS(jackson_arrays.numbers, '[3, 5]');
     *
     * SELECT jackson_arrays.id, jackson_arrays.`groups`, jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSON_CONTAINS(jackson_arrays.`groups`, '"B"', '$.users[0].name');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `배열 수형의 jackson 컬럼에서 contains 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportsJsonContains }

        withJacksonArrays(testDB) { tester, _, tripleId ->
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
     * SELECT jackson_arrays.id, jackson_arrays."groups", jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSONB_PATH_EXISTS(CAST(jackson_arrays."groups" as jsonb), '$.users[1]');
     *
     * SELECT jackson_arrays.id, jackson_arrays."groups", jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSONB_PATH_EXISTS(CAST(jackson_arrays.numbers as jsonb), '$[2]');
     *
     * -- MySQL V8
     * SELECT jackson_arrays.id, jackson_arrays.`groups`, jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSON_CONTAINS_PATH(jackson_arrays.`groups`, 'one', '$.users[1]');
     *
     * SELECT jackson_arrays.id, jackson_arrays.`groups`, jackson_arrays.numbers
     *   FROM jackson_arrays
     *  WHERE JSON_CONTAINS_PATH(jackson_arrays.numbers, 'one', '$[2]');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `배열 수형의 jackson 컬럼에 exists 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJacksonArrays(testDB) { tester, _, tripleId ->
            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            val hasMultipleUsers = tester.groups.exists(".users[1]", optional = optional)
            tester.selectAll().where { hasMultipleUsers }.single()[tester.id] shouldBeEqualTo tripleId

            val hasAtLeast3Numbers = tester.numbers.exists("[2]", optional = optional)
            tester.selectAll().where { hasAtLeast3Numbers }.single()[tester.id] shouldBeEqualTo tripleId
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT iterables.id FROM iterables
     *  WHERE iterables.user_list::jsonb @> '[{"name":"A","team":"Team A"}]'::jsonb;
     *
     * SELECT iterables.id FROM iterables
     *  WHERE iterables.user_set::jsonb @> '[{"name":"B","team":"Team B"}]'::jsonb;
     *
     * SELECT iterables.id FROM iterables
     *  WHERE iterables.user_array::jsonb @> '[{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}]'::jsonb;
     *
     * -- MySQL V8
     * SELECT iterables.id FROM iterables
     *  WHERE JSON_CONTAINS(iterables.user_list, '[{"name":"A","team":"Team A"}]');
     *
     * SELECT iterables.id FROM iterables
     *  WHERE JSON_CONTAINS(iterables.user_set, '[{"name":"B","team":"Team B"}]');
     *
     * SELECT iterables.id FROM iterables
     *  WHERE JSON_CONTAINS(iterables.user_array, '[{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}]');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Iterables 수형의 json 컬럼에 contains 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportsJsonContains }

        val iterables = object: IntIdTable("iterables") {
            val userList = jackson<List<User>>("user_list")
            val userSet = jackson<Set<User>>("user_set")
            val userArray = jackson<Array<User>>("user_array")
        }

        fun selectIdWhere(condition: SqlExpressionBuilder.() -> Op<Boolean>): List<EntityID<Int>> {
            val query = iterables.select(iterables.id).where(SqlExpressionBuilder.condition())
            return query.map { it[iterables.id] }
        }

        withTables(testDB, iterables) {
            val user1 = User("A", "Team A")
            val user2 = User("B", "Team B")

            val id1 = iterables.insertAndGetId {
                it[userList] = listOf(user1, user2)
                it[userSet] = setOf(user1)
                it[userArray] = arrayOf(user1, user2)
            }
            val id2 = iterables.insertAndGetId {
                it[userList] = listOf(user2)
                it[userSet] = setOf(user2)
                it[userArray] = arrayOf(user1, user2)
            }

            selectIdWhere { iterables.userList.contains(listOf(user1)) } shouldBeEqualTo listOf(id1)
            selectIdWhere { iterables.userSet.contains(listOf(user2)) } shouldBeEqualTo listOf(id2)
            selectIdWhere { iterables.userArray.contains(listOf(user1, user2)) } shouldBeEqualTo listOf(id1, id2)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS default_tester (
     *      user_1 JSON DEFAULT '{"name":"UNKNOWN","team":"UNASSIGNED"}'::json NOT NULL,
     *      user_2 JSON NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS default_tester (
     *      user_1 JSON DEFAULT ('{"name":"UNKNOWN","team":"UNASSIGNED"}') NOT NULL,
     *      user_2 JSON NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jackson 컬럼의 default 값 사용하기`(testDB: TestDB) {
        val defaultUser = User("UNKNOWN", "UNASSIGNED")
        val defaultTester = object: Table("default_tester") {
            val user1 = jackson<User>("user_1").default(defaultUser)
            val user2 = jackson<User>("user_2").clientDefault { defaultUser }
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
    fun `logging with json collections`(testDB: TestDB) {
        val iterables = object: Table("iterables_tester") {
            val userList = jackson<List<User>>("user_list")
            val intList = jackson<List<Int>>("int_list")
            val userArray = jackson<Array<User>>("user_array")
            val intArray = jackson<IntArray>("int_array")
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
    fun `nullable jackson column`(testDB: TestDB) {
        val tester = object: IntIdTable("nullable_tester") {
            val user = jackson<User>("user").nullable()
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
    fun `jackson column with upsert`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1)

        withJacksonTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[jacksonColumn] = newData
            }

            val newData2 = newData.copy(active = false)
            tester.upsert {
                it[tester.id] = newId
                it[jacksonColumn] = newData2
            }

            val newRow = tester.selectAll().where { tester.id eq newId }.single()
            newRow[tester.jacksonColumn] shouldBeEqualTo newData2
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (numbers JSON NOT NULL);
     *
     * INSERT INTO tester (numbers) VALUES ([1,2,3]);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jackson with transformer`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val numbers: Column<DoubleArray> = jackson<IntArray>("numbers").transform(
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

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS jackson_default (
     *      id SERIAL PRIMARY KEY,
     *      "value" JSON DEFAULT '{"name":"name","team":"team"}'::json NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS jackson_default (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `value` JSON DEFAULT ('{"name":"name","team":"team"}') NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jackson column with databaseGenerated`(testDB: TestDB) {

        val defaultUser = User("name", "team")

        val tester = object: IntIdTable("jackson_default") {
            val value = jackson<User>("value").default(defaultUser)
        }

        val testerDatabaseGenerated = object: IntIdTable("jackson_default") {
            val value = jackson<User>("value").databaseGenerated()
        }

        // MySQL versions prior to 8.0.13 do not accept default values on JSON columns
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }
        withTables(testDB, tester) {
            testerDatabaseGenerated.insert { }

            val value = testerDatabaseGenerated.selectAll().single()[tester.value]
            value shouldBeEqualTo defaultUser
        }

    }
}
