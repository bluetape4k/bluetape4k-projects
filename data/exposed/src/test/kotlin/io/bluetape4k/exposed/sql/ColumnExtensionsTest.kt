package io.bluetape4k.exposed.sql

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class ColumnExtensionsTest: AbstractExposedTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    object ClientGenerated: IntIdTable() {
        val timebasedUuid = uuid("timebased_uuid").timebasedGenerated()
        val snowflake = long("snowflake").snowflakeGenerated()
        val ksuid = varchar("ksuid", 27).ksuidGenerated()
        val ksuidMillis = varchar("ksuid_millis", 27).ksuidMillisGenerated()
    }

    class ClientGeneratedEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<ClientGeneratedEntity>(ClientGenerated)

        var timebasedUuid by ClientGenerated.timebasedUuid
        var snowflake by ClientGenerated.snowflake
        var ksuid by ClientGenerated.ksuid
        var ksuidMillis by ClientGenerated.ksuidMillis

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = toStringBuilder()
            .add("timebasedUuid", timebasedUuid)
            .add("snowflake", snowflake)
            .add("ksuid", ksuid)
            .add("ksuidMillis", ksuidMillis)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `client generated unique values`(testDB: TestDB) {
        val entityCount = 100
        withTables(testDB, ClientGenerated) {
            val entities = List(entityCount) {
                ClientGeneratedEntity.new {}
            }

            entities.map { it.timebasedUuid }.distinct() shouldHaveSize entityCount
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
            ClientGenerated.snowflake.getLanguageType() shouldBeEqualTo Long::class
        }
    }
}
