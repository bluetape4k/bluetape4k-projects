package io.bluetape4k.exposed.core.tink

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.tink.daead.TinkDaeads
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TinkColumnTypeDaoTest: AbstractExposedTest() {

    companion object: KLogging()

    object T1: IntIdTable("tink_dao_t1") {
        val varchar = tinkDaeadVarChar("varchar", 512, TinkDaeads.AES256_SIV).index()
        val binary = tinkDaeadBinary("binary", 512, TinkDaeads.AES256_SIV)
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var varchar by T1.varchar
        var binary by T1.binary

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("varchar", varchar)
            .add("binary", binary.toUtf8String())
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식의 컬럼 값 암호화`(testDB: TestDB) {
        withTables(testDB, T1) {
            val insertedVarchar = faker.name().firstName()
            val insertedBinary = faker.address().fullAddress().toUtf8Bytes()

            val entity = E1.new {
                varchar = insertedVarchar
                binary = insertedBinary
            }
            flushCache()

            val saved = E1.findById(entity.id)!!

            saved.varchar shouldBeEqualTo insertedVarchar
            saved.binary shouldBeEqualTo insertedBinary

            T1.selectAll().count() shouldBeEqualTo 1L

            val row = T1.selectAll().where { T1.id eq entity.id }.single()

            row[T1.varchar] shouldBeEqualTo insertedVarchar
            row[T1.binary] shouldBeEqualTo insertedBinary
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `암호화된 컬럼으로 검색하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val insertedVarchar = faker.name().firstName()
            val insertedBinary = faker.address().fullAddress().toUtf8Bytes()

            val entity = E1.new {
                varchar = insertedVarchar
                binary = insertedBinary
            }
            entityCache.clear()

            println("varchar=${entity.varchar}, binary=${entity.binary.toUtf8String()}")
            entity.varchar shouldBeEqualTo insertedVarchar
            entity.binary shouldBeEqualTo insertedBinary

            val e1 = E1.findById(entity.id)!!
            println("varchar=${e1.varchar}, binary=${e1.binary.toUtf8String()}")

            e1.varchar shouldBeEqualTo insertedVarchar
            e1.binary shouldBeEqualTo insertedBinary

            /**
             * Tink DAEAD(결정적 암호화)는 항상 같은 결과를 반환하므로, WHERE 절로 검색이 가능합니다.
             * ```sql
             * -- Postgres
             * SELECT t1.id, t1."varchar", t1."binary" FROM tink_dao_t1 WHERE tink_dao_t1."varchar" = '<암호문>'
             * ```
             */
            T1.selectAll().where { T1.varchar eq insertedVarchar }.single().let {
                it[T1.varchar] shouldBeEqualTo insertedVarchar
                it[T1.binary] shouldBeEqualTo insertedBinary
            }

            E1.find { T1.varchar eq insertedVarchar }.single().let {
                it.varchar shouldBeEqualTo insertedVarchar
                it.binary shouldBeEqualTo insertedBinary
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT t1.id, t1."varchar", t1."binary" FROM tink_dao_t1 WHERE tink_dao_t1."binary" = '<암호문 바이트>'
             * ```
             */
            T1.selectAll().where { T1.binary eq insertedBinary }.single().let {
                it[T1.varchar] shouldBeEqualTo insertedVarchar
                it[T1.binary] shouldBeEqualTo insertedBinary
            }

            T1.selectAll().where { T1.binary eq insertedBinary }.single().let {
                it[T1.varchar] shouldBeEqualTo insertedVarchar
                it[T1.binary] shouldBeEqualTo insertedBinary

                val e3 = E1.wrapRow(it)
                e3.varchar shouldBeEqualTo insertedVarchar
                e3.binary shouldBeEqualTo insertedBinary
            }

            E1.find { T1.binary eq insertedBinary }.single().let {
                it.varchar shouldBeEqualTo insertedVarchar
                it.binary shouldBeEqualTo insertedBinary
            }
        }
    }
}
