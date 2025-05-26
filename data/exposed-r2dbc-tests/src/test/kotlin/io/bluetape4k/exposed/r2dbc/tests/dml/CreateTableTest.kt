package io.bluetape4k.exposed.r2dbc.tests.dml

import io.bluetape4k.exposed.r2dbc.tests.R2dbcExposedTestBase
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.inProperCase
import io.bluetape4k.exposed.r2dbc.tests.withDb
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.exists
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFails

class CreateTableTest: R2dbcExposedTestBase() {

    companion object: KLogging()

    object TableWithDuplicatedColumn: Table("myTable") {
        val id1 = integer("id")
        val id2 = integer("id")  // 중복된 컬럼명
    }

    object IDTable: IntIdTable("IntIdTable")

    object TableDuplicatedColumnReferenceToIntIdTable: IntIdTable("myTable") {
        val reference = reference("id", IDTable)
    }

    object TableDuplicatedColumnReferToTable: Table("myTable") {
        val reference = reference("id", TableWithDuplicatedColumn.id1)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중복된 컬럼이 있는 테이블 생성은 예외를 발생시킵니다`(testDB: TestDB) = runTest {
        val errorMessage = "Can't create a table with multiple columns having the same name"

        withDb(testDB) {
            assertFails(errorMessage) {
                SchemaUtils.create(TableWithDuplicatedColumn)
            }
            assertFails(errorMessage) {
                SchemaUtils.create(TableDuplicatedColumnReferenceToIntIdTable)
            }
            assertFails(errorMessage) {
                SchemaUtils.create(TableDuplicatedColumnReferToTable)
            }
        }
    }

    /**
     * 컬럼에 `entityId()`를 사용하여 PRIMARY KEY를 지정할 수 있습니다
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `entityId를 이용하여 특정 컬럼을 PRIMARY KEY로 설정합니다`(testDB: TestDB) = runTest {
        val tester = object: IdTable<String>("tester") {
            val column1: Column<String> = varchar("column_1", 30)

            // column1 을 `entityId()`로 id 컬럼으로 지정합니다.
            override val id: Column<EntityID<String>> = column1.entityId()

            // primaryKey 함수를 이용하여 id를 primary key로 지정합니다.
            override val primaryKey = PrimaryKey(id)
        }

        withDb(testDB) {
            val singleColumnDescription = tester.columns.single().descriptionDdl(false)
            log.debug { "single column description: $singleColumnDescription" }
            singleColumnDescription shouldContain "PRIMARY KEY"

            log.debug { "DDL: ${tester.ddl.single()}" }

            // CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
            tester.ddl.single() shouldBeEqualTo
                    "CREATE TABLE ${addIfNotExistsIfSupported()}${tester.tableName.inProperCase()} (${singleColumnDescription})"
        }
    }

    /**
     * 컬럼에 `entityId()`를 사용하여 PRIMARY KEY를 지정할 수 있습니다
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `primaryKey 함수를 이용하여 컬럼을 primary key로 지정합니다`(testDB: TestDB) = runTest {
        val tester = object: IdTable<String>("tester") {
            val column1: Column<String> = varchar("column_1", 30)

            // column1 을 `entityId()`로 id 컬럼으로 지정합니다.
            override val id: Column<EntityID<String>> = column1.entityId()

            // primaryKey 함수를 이용하여 column1을 primary key로 지정합니다.
            override val primaryKey = PrimaryKey(column1)
        }

        withDb(testDB) {
            val singleColumnDescription = tester.columns.single().descriptionDdl(false)
            log.debug { "single column description: $singleColumnDescription" }
            singleColumnDescription shouldContain "PRIMARY KEY"

            log.debug { "DDL: ${tester.ddl.single()}" }

            // CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
            tester.ddl.single() shouldBeEqualTo
                    "CREATE TABLE ${addIfNotExistsIfSupported()}${tester.tableName.inProperCase()} (${singleColumnDescription})"
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS book (
     *      id SERIAL,
     *      CONSTRAINT PK_Book_ID PRIMARY KEY (id)
     * );
     * ```
     */
    object BookTable: Table("book") {
        val id = integer("id").autoIncrement()

        override val primaryKey = PrimaryKey(id, name = "PK_Book_ID")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS person (
     *      id1 INT,
     *      id2 INT,
     *
     *      CONSTRAINT PK_Person_ID PRIMARY KEY (id1, id2)
     * );
     * ```
     */
    object PersonTable: Table("person") {
        val id1 = integer("id1")
        val id2 = integer("id2")

        override val primaryKey = PrimaryKey(id1, id2, name = "PK_Person_ID")
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS book (
     *      id SERIAL,
     *      CONSTRAINT PK_Book_ID PRIMARY KEY (id)
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼이 하나이고, PRIMARY KEY로 지정된 테이블을 생성합니다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            val ddl = BookTable.ddl.single()
            log.debug { "DDL: $ddl" }

            SchemaUtils.create(BookTable)
            BookTable.exists().shouldBeTrue()

            SchemaUtils.drop(BookTable)
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS person (
     *      id1 INT,
     *      id2 INT,
     *      CONSTRAINT PK_Person_ID PRIMARY KEY (id1, id2)
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `2개의 컬럼이 PRIMARY KEY로 지정된 테이블을 생성합니다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            val ddl = PersonTable.ddl.single()
            log.debug { "DDL: $ddl" }

            SchemaUtils.create(PersonTable)
            PersonTable.exists().shouldBeTrue()

            SchemaUtils.drop(PersonTable)
        }
    }

    /**
     * `child1` 이 `parent1` 의 `id_a`, `id_b` 컬럼을 참조하는 Foreign Key를 가지고 있습니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS parent1 (
     *      id_a INT,
     *      id_b INT,
     *      CONSTRAINT pk_parent1 PRIMARY KEY (id_a, id_b)
     * );
     *
     * CREATE TABLE IF NOT EXISTS child1 (
     *      id_a INT NOT NULL,
     *      id_b INT NOT NULL,
     *
     *      CONSTRAINT myforeignkey1 FOREIGN KEY (id_a, id_b)
     *      REFERENCES parent1(id_a, id_b) ON DELETE CASCADE ON UPDATE CASCADE
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `2개의 컬럼이 Foreign Key 로 지정된 테이블을 생성합니다 - 01`(testDB: TestDB) = runTest {
        val fkName = "MyForeignKey1"
        val parent = object: Table("parent1") {
            val idA = integer("id_a")
            val idB = integer("id_b")
            override val primaryKey = PrimaryKey(idA, idB)
        }
        val child = object: Table("child1") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            init {
                foreignKey(
                    idA, idB,
                    target = parent.primaryKey,
                    onDelete = ReferenceOption.CASCADE,
                    onUpdate = ReferenceOption.CASCADE,
                    name = fkName
                )
            }
        }
        withDb(testDB) {
            val parentDdl = parent.ddl.single()
            val childDdl = child.ddl.single()

            log.debug { "Parent DDL: $parentDdl" }
            log.debug { "Child DDL: $childDdl" }

            SchemaUtils.create(parent, child)
            parent.exists().shouldBeTrue()
            child.exists().shouldBeTrue()
            SchemaUtils.drop(parent, child)
        }
    }

    /**
     * `child1` 이 `parent1` 의 `id_a`, `id_b` 컬럼을 참조하는 Foreign Key를 가지고 있습니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS parent1 (
     *      pid_a INT,
     *      pid_b INT,
     *      CONSTRAINT pk_parent1 PRIMARY KEY (pid_a, pid_b)
     * );
     *
     * CREATE TABLE IF NOT EXISTS child1 (
     *      id_a INT NOT NULL,
     *      id_b INT NOT NULL,
     *
     *      CONSTRAINT myforeignkey1 FOREIGN KEY (id_a, id_b)
     *      REFERENCES parent1(pid_a, pid_b) ON DELETE CASCADE ON UPDATE CASCADE
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `2개의 컬럼이 Foreign Key 로 지정된 테이블을 생성합니다 - 02`(testDB: TestDB) = runTest {
        val fkName = "MyForeignKey1"
        val parent = object: Table("parent1") {
            val pidA = integer("pid_a")
            val pidB = integer("pid_b")
            override val primaryKey = PrimaryKey(pidA, pidB)
        }
        val child = object: Table("child1") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            init {
                foreignKey(
                    idA to parent.pidA, idB to parent.pidB,
                    onDelete = ReferenceOption.CASCADE,
                    onUpdate = ReferenceOption.CASCADE,
                    name = fkName
                )
            }
        }
        withDb(testDB) {
            val parentDdl = parent.ddl.single()
            val childDdl = child.ddl.single()

            log.debug { "Parent DDL: $parentDdl" }
            log.debug { "Child DDL: $childDdl" }

            SchemaUtils.create(parent, child)
            parent.exists().shouldBeTrue()
            child.exists().shouldBeTrue()
            SchemaUtils.drop(parent, child)
        }
    }
}
