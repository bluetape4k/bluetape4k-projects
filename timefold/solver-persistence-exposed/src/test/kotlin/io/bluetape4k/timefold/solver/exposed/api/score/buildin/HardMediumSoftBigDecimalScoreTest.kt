package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toBigDecimal
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

class HardMediumSoftBigDecimalScoreTest: AbstractScoreExposedTest() {

    companion object: KLogging()

    object T1: IntIdTable() {
        val name = varchar("name", 255)
        val hardMediumSoftBigDecimalScore = hardMediumSoftBigDecimalScore("hardmediumsoft_bigdecimal_score")
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name
        var hardMediumSoftBigDecimalScore by T1.hardMediumSoftBigDecimalScore

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("hardMediumSoftBigDecimalScore", hardMediumSoftBigDecimalScore)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `HardMediumSoftBigDecimalScore 를 DB 에 저장 및 조회하기`(testDB: TestDB) {
        withTables(testDB, T1) {

            val name = faker.name().name()
            val hardMediumSoftBigDecimalScore = HardMediumSoftBigDecimalScore.of(
                faker.random().nextDouble().toBigDecimal(),
                faker.random().nextDouble().toBigDecimal(),
                faker.random().nextDouble().toBigDecimal(),
            )

            val id = T1.insertAndGetId {
                it[T1.name] = name
                it[T1.hardMediumSoftBigDecimalScore] = hardMediumSoftBigDecimalScore
            }

            val row = T1
                .selectAll()
                .where { T1.id eq id }
                .single()

            log.debug { "row=$row" }

            row[T1.name] shouldBeEqualTo name
            row[T1.hardMediumSoftBigDecimalScore] shouldBeEqualTo hardMediumSoftBigDecimalScore
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `HardMediumSoftBigDecimalScore 수형의 속성을 가진 엔티티 사용`(testDB: TestDB) {
        withTables(testDB, T1) {
            // Creates entity with hard medium soft bigdecimal score
            val saved = E1.new {
                this.name = faker.name().name()
                this.hardMediumSoftBigDecimalScore = HardMediumSoftBigDecimalScore.of(
                    faker.random().nextDouble().toBigDecimal(),
                    faker.random().nextDouble().toBigDecimal(),
                    faker.random().nextDouble().toBigDecimal(),
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
