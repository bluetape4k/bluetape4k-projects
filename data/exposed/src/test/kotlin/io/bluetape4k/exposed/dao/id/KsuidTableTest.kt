package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.utils.withSuspendedTables
import io.bluetape4k.exposed.utils.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KsuidTableTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- H2
     * CREATE TABLE IF NOT EXISTS T1 (
     *      ID VARCHAR(27) PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      AGE INT NOT NULL
     * );
     * ```
     */
    object T1: KsuidTable() {
        val name = varchar("name", 255)
        val age = integer("age")
    }

    class E1(id: KsuidEntityID): KsuidEntity(id) {
        companion object: KsuidEntityClass<E1>(T1)

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
     * INSERT INTO T1 (ID, "name", AGE)
     * VALUES ('UAzfLT1vmZdk8GcJBM0qOfeeCEk', 'Daine Friesen Sr.', 62);
     * ```
     */
    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `Unique한 ID를 가진 복수의 엔티티를 생성한다`(entityCount: Int) {
        withTables(T1) {
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
     * INSERT INTO T1 (ID, "name", AGE) VALUES ('UAzfPzT6yF4gkA18YunPEZdntQS', 'Bruce Kris', 8)
     * ```
     */
    @ParameterizedTest(name = "entity count={0}")
    @ValueSource(ints = [1, 100, 1000, 10000])
    fun `Coroutine 환경에서 복수의 Unique한 엔티티를 생성한다`(entityCount: Int) = runSuspendIO {
        withSuspendedTables(T1) {
            val tasks: List<Deferred<E1>> = List(entityCount) {
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
}
