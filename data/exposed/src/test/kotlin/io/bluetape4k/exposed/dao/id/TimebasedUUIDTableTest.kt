package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.utils.runSuspendWithTables
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.take
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TimebasedUUIDTableTest: AbstractExposedTest() {

    companion object: KLogging()

    object T1: TimebasedUUIDTable() {
        val name = varchar("name", 255)
        val age = integer("age")
    }

    class E1(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<E1>(T1)

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
            flushCache()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
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
            flushCache()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `batch insert`(entityCount: Int) {
        runWithTables(T1) {
            val entities = generateSequence {
                val name = faker.name().fullName()
                val age = faker.number().numberBetween(8, 80)
                name to age
            }

            T1.batchInsert(entities.take(entityCount), shouldReturnGeneratedValues = false) { (name, age) ->
                this[T1.name] = name
                this[T1.age] = age
            }

            flushCache()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `batch insert in coroutines`(entityCount: Int) = runSuspendIO {
        runSuspendWithTables(T1) {
            val entities = generateSequence<Pair<String, Int>> {
                val name = faker.name().fullName()
                val age = faker.number().numberBetween(8, 80)
                name to age
            }

            val task = suspendedTransactionAsync {
                T1.batchInsert(entities.take(entityCount)) {
                    this[T1.name] = it.first
                    this[T1.age] = it.second
                }
            }
            task.await()
            flushCache()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `batch insert as flow`(entityCount: Int) = runSuspendIO {
        runSuspendWithTables(T1) {
            val entities = generateSequence<Pair<String, Int>> {
                val name = faker.name().fullName()
                val age = faker.number().numberBetween(8, 80)
                name to age
            }

            entities.asFlow()
                .buffer()
                .take(entityCount)
                .collect { item ->
                    T1.insertIgnore {
                        it[name] = item.first
                        it[age] = item.second
                    }
                }
            flushCache()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }
}
