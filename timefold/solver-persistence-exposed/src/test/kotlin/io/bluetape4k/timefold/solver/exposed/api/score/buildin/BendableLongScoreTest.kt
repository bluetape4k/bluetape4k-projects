package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BendableLongScoreTest: AbstractScoreExposedTest() {

    companion object: KLogging()

    object T1: IntIdTable() {
        val name = varchar("name", 255)
        val bendableLong = bendableLongScore("bendable_long_score")
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name
        var bendableLong by T1.bendableLong

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("bendableLong", bendableLong)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `BendableLongScore 를 DB 에 저장 및 조회하기`(testDB: TestDB) {
        withTables(testDB, T1) {

            val name = faker.name().name()
            val bendableLong = BendableLongScore.of(
                longArrayOf(
                    faker.random().nextLong(),
                    faker.random().nextLong(),
                    faker.random().nextLong(),
                    faker.random().nextLong(),
                ),
                longArrayOf(
                    faker.random().nextLong(),
                    faker.random().nextLong(),
                    faker.random().nextLong(),
                    faker.random().nextLong(),
                )
            )

            val id = T1.insertAndGetId {
                it[T1.name] = name
                it[T1.bendableLong] = bendableLong
            }

            val row = T1
                .selectAll()
                .where { T1.id eq id }
                .single()

            log.debug { "row=$row" }

            row[T1.name] shouldBeEqualTo name
            row[T1.bendableLong] shouldBeEqualTo bendableLong
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `BendableLongScore 를 가진 인테티 저장 및 로드하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val saved = E1.new {
                this.name = faker.name().name()
                this.bendableLong = BendableLongScore.of(
                    longArrayOf(
                        faker.random().nextLong(),
                        faker.random().nextLong(),
                        faker.random().nextLong(),
                        faker.random().nextLong(),
                    ),
                    longArrayOf(
                        faker.random().nextLong(),
                        faker.random().nextLong(),
                        faker.random().nextLong(),
                        faker.random().nextLong(),
                    )
                )
            }
            log.debug { "saved=$saved" }
            flushCache()

            val loaded = E1.findById(saved.id)
            log.debug { "loaded=$loaded" }
            loaded shouldBeEqualTo saved
        }
    }
}
