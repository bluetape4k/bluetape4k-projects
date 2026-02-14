package io.bluetape4k.exposed.dao.id

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.exposed.tests.withTablesSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("DEPRECATION")
class TimebasedUUIDTableTest: AbstractCustomIdTableTest() {

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
    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource("getTestDBAndEntityCount")
    fun `Unique한 ID를 가진 복수의 엔티티를 생성한다`(testDB: TestDB, entityCount: Int) {
        withTables(testDB, T1) {
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

    /**
     * ```sql
     * INSERT INTO T1 ("name", AGE, ID) VALUES ('Aurelio Pouros II', 79, '1efe3b58-ccc7-65b3-9ee6-897d7aeb3be7')
     * ```
     */
    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource("getTestDBAndEntityCount")
    fun `batch insert`(testDB: TestDB, entityCount: Int) {
        withTables(testDB, T1) {
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

    /**
     * ```sql
     * INSERT INTO T1 (ID, "name", AGE) VALUES ('1efe3b58-de55-6f37-9ee6-897d7aeb3be7', 'Miss Lloyd Pollich', 70)
     * ```
     */
    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource("getTestDBAndEntityCount")
    fun `Coroutine 환경에서 복수의 Unique한 엔티티를 생성한다`(testDB: TestDB, entityCount: Int) = runSuspendIO {
        withTablesSuspending(testDB, T1) {
            val tasks = fastList(entityCount) {
                suspendedTransactionAsync(Dispatchers.IO) {
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

    /**
     * ```sql
     * INSERT INTO T1 ("name", AGE, ID) VALUES ('King Muller', 10, '1efe3b58-dc97-636a-9ee6-897d7aeb3be7')
     * ```
     */
    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource("getTestDBAndEntityCount")
    fun `batch insert in coroutines`(testDB: TestDB, entityCount: Int) = runSuspendIO {
        withTablesSuspending(testDB, T1) {
            val entities: Sequence<Pair<String, Int>> = generateSequence {
                val name = faker.name().fullName()
                val age = faker.number().numberBetween(8, 80)
                name to age
            }

            val task = suspendedTransactionAsync(Dispatchers.IO) {
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

    /**
     * ```sql
     * INSERT INTO T1 (ID, "name", AGE)
     * VALUES ('1efe3b58-c940-6036-9ee6-897d7aeb3be7', 'Miss Hung Kautzer', 30) ON CONFLICT DO NOTHING
     * ```
     */
    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource("getTestDBAndEntityCount")
    fun `insertIgnore as flow`(testDB: TestDB, entityCount: Int) = runSuspendIO {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB + TestDB.POSTGRESQL }

        withTablesSuspending(testDB, T1) {
            val entities: Sequence<Pair<String, Int>> = generateSequence {
                val name = faker.name().fullName()
                val age = faker.number().numberBetween(8, 80)
                name to age
            }

            entities.asFlow()
                .buffer(16)
                .take(entityCount)
                .flatMapMerge(16) { (name, age) ->
                    flow {
                        val insertCount = T1.insertIgnore {
                            it[T1.name] = name
                            it[T1.age] = age
                        }
                        emit(insertCount)
                    }
                }
                .collect()

            flushCache()

            T1.selectAll().count().toInt() shouldBeEqualTo entityCount
        }
    }
}
