package io.bluetape4k.exposed.trino

import io.bluetape4k.exposed.trino.domain.Events
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * [SchemaUtils]를 사용하여 Trino memory 커넥터에서 테이블 DDL(생성/삭제)이
 * 정상 동작하는지 검증하는 테스트.
 */
class SchemaUtilsTest : AbstractTrinoTest() {

    @Test
    fun `SchemaUtils create Events 테이블 생성 성공`() {
        transaction(db) { SchemaUtils.create(Events) }
        transaction(db) { SchemaUtils.drop(Events) }
    }

    @Test
    fun `SchemaUtils drop Events 테이블 삭제 성공`() {
        transaction(db) { SchemaUtils.create(Events) }
        transaction(db) { SchemaUtils.drop(Events) }
    }

    @Test
    fun `create 후 Events 테이블은 비어있다`() = withEventsTable {
        transaction(db) {
            Events.selectAll().count() shouldBeEqualTo 0L
        }
    }

    @Test
    fun `TrinoTable createStatement 는 PRIMARY KEY 구문을 제거한다`() {
        transaction(db) {
            val ddl = Events.createStatement().single()

            ddl.contains("PRIMARY KEY").shouldBeFalse()
            ddl.contains("CONSTRAINT").shouldBeFalse()
        }
    }

    @Test
    fun `create drop create 순서로 테이블 재생성 가능`() {
        transaction(db) { SchemaUtils.create(Events) }
        transaction(db) { SchemaUtils.drop(Events) }
        transaction(db) { SchemaUtils.create(Events) }
        transaction(db) { SchemaUtils.drop(Events) }
    }
}
