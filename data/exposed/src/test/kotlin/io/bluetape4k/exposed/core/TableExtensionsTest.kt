package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withDb
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainIgnoringCase
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TableExtensionsTest: AbstractExposedTest() {

    companion object: KLogging()


    val tester = object: IntIdTable("tester") {
        val name = varchar("name", 255)
        val price = integer("price")

        init {
            index("tester_by_name", isUnique = false, name)
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Column Metadata 조회`(testDB: TestDB) {
        withTables(testDB, tester) {
            tester.exists().shouldBeTrue()

            val metadatas = tester.getColumnMetadata()
            metadatas.forEach {
                log.debug { "metadata=$it" }
            }
            metadatas shouldHaveSize 3
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Index 정보 조회하기`(testDB: TestDB) {
        withTables(testDB, tester) {
            tester.exists().shouldBeTrue()

            val indices = tester.getIndices()
            indices.forEach {
                log.debug { "index=$it" }
            }
            indices shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get primary key's metadata`(testDB: TestDB) {
        withTables(testDB, tester) {
            tester.exists().shouldBeTrue()

            val primaryKeyMetadata = tester.getPrimaryKeyMetadata()
            log.debug { "primaryKey=$primaryKeyMetadata" }
            primaryKeyMetadata.shouldNotBeNull()
            primaryKeyMetadata.columnNames shouldContainIgnoringCase "ID"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get sequences`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }

        val identityTable = object: IntIdTable("identity_table") {}

        withDb(testDB) {
            try {
                SchemaUtils.create(identityTable)

                val sequences = identityTable.getSequences()
                log.debug { "sequences=$sequences" }
                sequences.shouldNotBeEmpty()
            } finally {
                SchemaUtils.drop(identityTable)
            }
        }
    }
}
