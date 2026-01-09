package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SimpleLongScoreTest: AbstractScoreExposedTest() {

    companion object: KLogging()

    object T1: IntIdTable() {
        val name = varchar("name", 255)
        val simpleLongScore = simpleLongScore("simple_long_score")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SimpleLongScore 를 DB 에 저장 및 조회하기`(testDB: TestDB) {
        withTables(testDB, T1) {

            val name = faker.name().name()
            val simpleLongScore = SimpleLongScore.of(faker.number().randomDigit().toLong())

            val id = T1.insertAndGetId {
                it[T1.name] = name
                it[T1.simpleLongScore] = simpleLongScore
            }

            val row = T1
                .selectAll()
                .where { T1.id eq id }
                .single()

            log.debug { "row=$row" }

            row[T1.name] shouldBeEqualTo name
            row[T1.simpleLongScore] shouldBeEqualTo simpleLongScore
        }
    }
}
