package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UuidGeneratorTest {
    companion object : KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val ID_SIZE = 100
        private const val CONCURRENCY_COUNT = 5_000
    }

    @Test
    fun `기본 생성자는 V7을 사용한다`() {
        val gen = UuidGenerator()
        val id = gen.nextUUID()
        id.shouldNotBeNull()
        id.version() shouldBeEqualTo 7
    }

    @Test
    fun `커스텀 전략 주입으로 V1을 사용할 수 있다`() {
        val gen = UuidGenerator(Uuid.V1)
        val id = gen.nextUUID()
        id.version() shouldBeEqualTo 1
    }

    @Test
    fun `커스텀 전략 주입으로 V6을 사용할 수 있다`() {
        val gen = UuidGenerator(Uuid.V6)
        val id = gen.nextUUID()
        id.version() shouldBeEqualTo 6
    }

    @Test
    fun `IdGenerator 인터페이스로 사용할 수 있다`() {
        val gen: IdGenerator<UUID> = UuidGenerator()
        val id = gen.nextId()
        id.shouldNotBeNull()
    }

    @Test
    fun `nextIds는 요청한 크기만큼 시퀀스를 반환한다`() {
        val gen = UuidGenerator()
        val ids = gen.nextIds(ID_SIZE).toList()
        ids shouldHaveSize ID_SIZE
        ids.distinct() shouldHaveSize ID_SIZE
    }

    @Test
    fun `nextIdsAsString은 요청한 크기만큼 문자열 시퀀스를 반환한다`() {
        val gen = UuidGenerator()
        val strs = gen.nextIdsAsString(ID_SIZE).toList()
        strs shouldHaveSize ID_SIZE
        strs.all { it.isNotBlank() } shouldBeEqualTo true
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `멀티스레드 환경에서 중복 없이 UUID를 생성한다`() {
        val gen = UuidGenerator()
        val idMap = ConcurrentHashMap<UUID, Int>()

        MultithreadingTester()
            .workers(Runtimex.availableProcessors)
            .rounds(CONCURRENCY_COUNT / Runtimex.availableProcessors)
            .add {
                val id = gen.nextUUID()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }.run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread 환경에서 중복 없이 UUID를 생성한다`() {
        val gen = UuidGenerator()
        val idMap = ConcurrentHashMap<UUID, Int>()

        StructuredTaskScopeTester()
            .rounds(CONCURRENCY_COUNT)
            .add {
                val id = gen.nextUUID()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }.run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Coroutine 환경에서 중복 없이 UUID를 생성한다`() =
        runSuspendDefault {
            val gen = UuidGenerator()
            val idMap = ConcurrentHashMap<UUID, Int>()

            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(CONCURRENCY_COUNT)
                .add {
                    val id = gen.nextUUID()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }
}
