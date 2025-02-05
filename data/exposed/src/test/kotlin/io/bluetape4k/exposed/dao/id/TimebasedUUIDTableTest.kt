package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.utils.runSuspendWithTables
import io.bluetape4k.exposed.utils.runWithTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.take
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TimebasedUUIDTableTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS T1 (
     *      ID uuid PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      AGE INT NOT NULL
     * )
     * ```
     */
    object T1: TimebasedUUIDTable() {
        val name = varchar("name", 255)
        val age = integer("age")
    }

    class E1(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<E1>(T1)

        var name by T1.name
        var age by T1.age

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("age", age)
            .toString()
    }

    /**
     * ```sql
     * INSERT INTO T1 (ID, "name", AGE) VALUES ('1efe3b58-d0c6-6440-9ee6-897d7aeb3be7', 'Mrs. Reynaldo Rogahn', 73)
     * ```
     */
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
            entityCache.clear()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    /**
     * ```sql
     * INSERT INTO T1 (ID, "name", AGE) VALUES ('1efe3b58-de55-6f37-9ee6-897d7aeb3be7', 'Miss Lloyd Pollich', 70)
     * ```
     */
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
            entityCache.clear()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    /**
     * ```sql
     * INSERT INTO T1 ("name", AGE, ID) VALUES ('Aurelio Pouros II', 79, '1efe3b58-ccc7-65b3-9ee6-897d7aeb3be7')
     * ```
     */
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

            entityCache.clear()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    /**
     * ```sql
     * INSERT INTO T1 ("name", AGE, ID) VALUES ('King Muller', 10, '1efe3b58-dc97-636a-9ee6-897d7aeb3be7')
     * ```
     */
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
            entityCache.clear()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }

    /**
     * ```sql
     * INSERT INTO T1 (ID, "name", AGE)
     * VALUES ('1efe3b58-c940-6036-9ee6-897d7aeb3be7', 'Miss Hung Kautzer', 30) ON CONFLICT DO NOTHING
     * ```
     */
    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `insertIgnore as flow`(entityCount: Int) = runSuspendIO {
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
            entityCache.clear()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }
}
