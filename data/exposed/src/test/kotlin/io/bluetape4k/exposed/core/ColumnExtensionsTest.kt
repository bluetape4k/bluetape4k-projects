package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class ColumnExtensionsTest: AbstractExposedTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    object ClientGenerated: IntIdTable() {
        val timebasedUuid = javaUUID("timebased_uuid").timebasedGenerated()
        val timebasedUuidBase62 = varchar("timebased_uuid_base62", 32).timebasedGenerated()
        val snowflake = long("snowflake").snowflakeGenerated()
        val ksuid = varchar("ksuid", 27).ksuidGenerated()
        val ksuidMillis = varchar("ksuid_millis", 27).ksuidMillisGenerated()
    }

    class ClientGeneratedEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<ClientGeneratedEntity>(ClientGenerated)

        var timebasedUuid by ClientGenerated.timebasedUuid
        var timebasedUuidBase62 by ClientGenerated.timebasedUuidBase62
        var snowflake by ClientGenerated.snowflake
        var ksuid by ClientGenerated.ksuid
        var ksuidMillis by ClientGenerated.ksuidMillis

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = toStringBuilder()
            .add("timebasedUuid", timebasedUuid)
            .add("timebasedUuidBase62", timebasedUuidBase62)
            .add("snowflake", snowflake)
            .add("ksuid", ksuid)
            .add("ksuidMillis", ksuidMillis)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 클라이언트에서 컬럼 값을 생헝합니다`(testDB: TestDB) {
        val entityCount = 100
        withTables(testDB, ClientGenerated) {
            val ids = List(entityCount) { it + 1 }
            val rows = ClientGenerated.batchInsert(ids) { }

            rows.map { it[ClientGenerated.timebasedUuid] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.timebasedUuidBase62] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.snowflake] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.ksuid] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.ksuidMillis] }.distinct() shouldHaveSize entityCount
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 클라이언트에서 컬럼 값을 생헝합니다`(testDB: TestDB) {
        val entityCount = 100
        withTables(testDB, ClientGenerated) {
            val entities = List(entityCount) {
                ClientGeneratedEntity.new {}
            }

            entities.map { it.timebasedUuid }.distinct() shouldHaveSize entityCount
            entities.map { it.timebasedUuidBase62 }.distinct() shouldHaveSize entityCount
            entities.map { it.snowflake }.distinct() shouldHaveSize entityCount
            entities.map { it.ksuid }.distinct() shouldHaveSize entityCount
            entities.map { it.ksuidMillis }.distinct() shouldHaveSize entityCount
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `테이블 ID의 랭귀지 타입을 판단한다`(testDB: TestDB) {
        withTables(testDB, ClientGenerated) {
            ClientGenerated.id.getLanguageType() shouldBeEqualTo Int::class
            ClientGenerated.timebasedUuid.getLanguageType() shouldBeEqualTo UUID::class
            ClientGenerated.timebasedUuidBase62.getLanguageType() shouldBeEqualTo String::class
            ClientGenerated.snowflake.getLanguageType() shouldBeEqualTo Long::class
            ClientGenerated.ksuid.getLanguageType() shouldBeEqualTo String::class
            ClientGenerated.ksuidMillis.getLanguageType() shouldBeEqualTo String::class
        }
    }
}
