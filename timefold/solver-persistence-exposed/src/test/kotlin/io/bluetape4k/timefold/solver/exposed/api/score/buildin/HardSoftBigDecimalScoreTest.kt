package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore
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

class HardSoftBigDecimalScoreTest: AbstractScoreExposedTest() {

    companion object: KLogging()

    object T1: IntIdTable() {
        val name = varchar("name", 255)
        val hardSoftBigDecimalScore = hardSoftBigDecimalScore("hardsoft_bigdecimal_score")
    }


    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name
        var hardSoftBigDecimalScore by T1.hardSoftBigDecimalScore

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("hardSoftBigDecimalScore", hardSoftBigDecimalScore)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `HardSoftBigDecimalScore 를 DB 에 저장 및 조회하기`(testDB: TestDB) {
        withTables(testDB, T1) {

            val name = faker.name().name()
            val hardSoftBigDecimalScore = HardSoftBigDecimalScore.of(
                faker.random().nextDouble().toBigDecimal(),
                faker.random().nextDouble().toBigDecimal()
            )

            val id = T1.insertAndGetId {
                it[T1.name] = name
                it[T1.hardSoftBigDecimalScore] = hardSoftBigDecimalScore
            }

            val row = T1
                .selectAll()
                .where { T1.id eq id }
                .single()

            log.debug { "row=$row" }

            row[T1.name] shouldBeEqualTo name
            row[T1.hardSoftBigDecimalScore] shouldBeEqualTo hardSoftBigDecimalScore
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `HardSoftBigDecimalScore 수형의 속성을 가진 엔티티 사용`(testDB: TestDB) {
        withTables(testDB, T1) {
            // Creates entity with name and hardSoftLongScore
            val saved = E1.new {
                this.name = faker.name().name()
                this.hardSoftBigDecimalScore = HardSoftBigDecimalScore.of(
                    faker.random().nextDouble().toBigDecimal(),
                    faker.random().nextDouble().toBigDecimal()
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
