package io.bluetape4k.exposed.jasypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class JasyptColumnTypeDaoTest: AbstractExposedTest() {

    companion object: KLogging()

    object T1: IntIdTable() {
        val varchar = jasyptVarChar("varchar", 255, Encryptors.AES)
        val binary = jasyptBinary("binary", 255, Encryptors.AES)
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var varchar by T1.varchar
        var binary by T1.binary

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("varchar", varchar)
            .add("binary", binary)
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
            entityCache.clear()

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
            flushCache()
            entityCache.clear()

            /**
             * Jasypt 암호화는 항상 같은 결과를 반환하므로, WHERE 절로 검색이 가능합니다.
             * ```sql
             * -- Postgres
             * SELECT t1.id, t1."varchar", t1."binary" FROM t1 WHERE t1."varchar" = xHJZumy4xB5idgnKqmp2pQ==
             * ```
             */
            E1.find { T1.varchar eq insertedVarchar }.single().let {
                it.varchar shouldBeEqualTo insertedVarchar
                it.binary shouldBeEqualTo insertedBinary
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT t1.id, t1."varchar", t1."binary" FROM t1 WHERE t1."binary" = [B@20040c6e
             * ```
             */
            E1.find { T1.binary eq insertedBinary }.single().let {
                it.varchar shouldBeEqualTo insertedVarchar
                it.binary shouldBeEqualTo insertedBinary
            }
        }
    }
}
