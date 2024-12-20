package io.bluetape4k.exposed.dao

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.utils.runSuspendWithTables
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SnowflakeIdTableTest: AbstractExposedTest() {

    companion object: KLogging()

    object T1: SnowflakeIdTable() {
        val name = varchar("name", 255)
        val age = integer("age")
    }

    class E1(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<E1>(T1)

        var name by T1.name
        var age by T1.age
    }

    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `Unique한 ID를 가진 복수의 엔티티를 생성한다`(entityCount: Int) {
        runWithTables(T1) {
            repeat(entityCount) {
                E1.new {
                    name = faker.name().fullName()
                    age = faker.number().numberBetween(8, 80)
                }
            }

            T1.selectAll().count() shouldBeEqualTo entityCount.toLong()
        }
    }

    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `Coroutine 환경에서 복수의 Unique한 엔티티를 생성한다`(entityCount: Int) = runSuspendIO {
        runSuspendWithTables(T1) {
            val tasks = List(entityCount) {
                suspendedTransactionAsync {
                    E1.new {
                        name = faker.name().fullName()
                        age = faker.number().numberBetween(8, 80)
                    }
                }
            }
            tasks.awaitAll()
            T1.selectAll().count() shouldBeEqualTo entityCount.toLong()
        }
    }
}
