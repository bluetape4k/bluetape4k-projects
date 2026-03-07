package io.bluetape4k.exposed.core.dao.id

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [KsuidTable], [KsuidMillisTable], [SnowflakeIdTable], [TimebasedUUIDTable],
 * [TimebasedUUIDBase62Table] 의 클라이언트 측 ID 생성을 검증하는 통합 테스트입니다.
 */
class CustomIdTablesTest: AbstractExposedTest() {

    private object KsuidItems: KsuidTable("ksuid_items") {
        val name = varchar("name", 100)
    }

    private object KsuidMillisItems: KsuidMillisTable("ksuid_millis_items") {
        val name = varchar("name", 100)
    }

    private object SnowflakeItems: SnowflakeIdTable("snowflake_items") {
        val name = varchar("name", 100)
    }

    private object TimebasedItems: TimebasedUUIDTable("timebased_items") {
        val name = varchar("name", 100)
    }

    private object TimebasedBase62Items: TimebasedUUIDBase62Table("timebased_base62_items") {
        val name = varchar("name", 100)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `KsuidTable 은 27자 KSUID를 기본키로 자동 생성한다`(testDB: TestDB) {
        withTables(testDB, KsuidItems) {
            val id = KsuidItems.insert {
                it[name] = "ksuid-item"
            }[KsuidItems.id]

            id.shouldNotBeNull()
            id.value.length shouldBeEqualTo 27
            KsuidItems.selectAll().toList() shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `KsuidTable 은 서로 다른 레코드에 고유한 ID를 생성한다`(testDB: TestDB) {
        withTables(testDB, KsuidItems) {
            val id1 = KsuidItems.insert { it[name] = "item1" }[KsuidItems.id]
            val id2 = KsuidItems.insert { it[name] = "item2" }[KsuidItems.id]

            id1.shouldNotBeNull()
            id2.shouldNotBeNull()
            (id1.value == id2.value) shouldBeEqualTo false
            KsuidItems.selectAll().toList() shouldHaveSize 2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `KsuidMillisTable 은 27자 KsuidMillis를 기본키로 자동 생성한다`(testDB: TestDB) {
        withTables(testDB, KsuidMillisItems) {
            val id = KsuidMillisItems.insert {
                it[name] = "ksuid-millis-item"
            }[KsuidMillisItems.id]

            id.shouldNotBeNull()
            id.value.length shouldBeEqualTo 27
            KsuidMillisItems.selectAll().toList() shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `KsuidMillisTable 은 서로 다른 레코드에 고유한 ID를 생성한다`(testDB: TestDB) {
        withTables(testDB, KsuidMillisItems) {
            val id1 = KsuidMillisItems.insert { it[name] = "item1" }[KsuidMillisItems.id]
            val id2 = KsuidMillisItems.insert { it[name] = "item2" }[KsuidMillisItems.id]

            id1.shouldNotBeNull()
            id2.shouldNotBeNull()
            (id1.value == id2.value) shouldBeEqualTo false
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SnowflakeIdTable 은 Long Snowflake ID를 기본키로 자동 생성한다`(testDB: TestDB) {
        withTables(testDB, SnowflakeItems) {
            val id = SnowflakeItems.insert {
                it[name] = "snowflake-item"
            }[SnowflakeItems.id]

            id.shouldNotBeNull()
            (id.value > 0L) shouldBeEqualTo true
            SnowflakeItems.selectAll().toList() shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SnowflakeIdTable 은 서로 다른 레코드에 고유한 ID를 생성한다`(testDB: TestDB) {
        withTables(testDB, SnowflakeItems) {
            val id1 = SnowflakeItems.insert { it[name] = "item1" }[SnowflakeItems.id]
            val id2 = SnowflakeItems.insert { it[name] = "item2" }[SnowflakeItems.id]

            id1.shouldNotBeNull()
            id2.shouldNotBeNull()
            (id1.value == id2.value) shouldBeEqualTo false
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TimebasedUUIDTable 은 UUID를 기본키로 자동 생성한다`(testDB: TestDB) {
        withTables(testDB, TimebasedItems) {
            val id = TimebasedItems.insert {
                it[name] = "timebased-item"
            }[TimebasedItems.id]

            id.shouldNotBeNull()
            id.value.shouldNotBeNull()
            TimebasedItems.selectAll().toList() shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TimebasedUUIDTable 은 서로 다른 레코드에 고유한 UUID를 생성한다`(testDB: TestDB) {
        withTables(testDB, TimebasedItems) {
            val id1 = TimebasedItems.insert { it[name] = "item1" }[TimebasedItems.id]
            val id2 = TimebasedItems.insert { it[name] = "item2" }[TimebasedItems.id]

            id1.shouldNotBeNull()
            id2.shouldNotBeNull()
            (id1.value == id2.value) shouldBeEqualTo false
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TimebasedUUIDBase62Table 은 24자 Base62 문자열을 기본키로 자동 생성한다`(testDB: TestDB) {
        withTables(testDB, TimebasedBase62Items) {
            val id = TimebasedBase62Items.insert {
                it[name] = "base62-item"
            }[TimebasedBase62Items.id]

            id.shouldNotBeNull()
            (id.value.length in 1..24).shouldBeTrue()
            TimebasedBase62Items.selectAll().toList() shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TimebasedUUIDBase62Table 은 서로 다른 레코드에 고유한 ID를 생성한다`(testDB: TestDB) {
        withTables(testDB, TimebasedBase62Items) {
            val id1 = TimebasedBase62Items.insert { it[name] = "item1" }[TimebasedBase62Items.id]
            val id2 = TimebasedBase62Items.insert { it[name] = "item2" }[TimebasedBase62Items.id]

            id1.shouldNotBeNull()
            id2.shouldNotBeNull()
            (id1.value == id2.value) shouldBeEqualTo false
        }
    }
}
