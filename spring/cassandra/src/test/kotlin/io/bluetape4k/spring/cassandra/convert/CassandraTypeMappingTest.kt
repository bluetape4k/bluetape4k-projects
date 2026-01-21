package io.bluetape4k.spring.cassandra.convert

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraOperations
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(classes = [ConvertTestConfiguration::class])
class CassandraTypeMappingTest(
    @param:Autowired private val operations: CassandraOperations,
): io.bluetape4k.spring.cassandra.AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private val initialized = AtomicBoolean()
    }

    @BeforeEach
    fun beforeEach() {
        if (initialized.compareAndSet(false, true)) {
            // TODO: 초기화                                  
        }
    }

    @Test
    fun `context loading`() {
        operations.shouldNotBeNull()
        session.shouldNotBeNull()
        log.info { "Current keyspace=${session.keyspace}" }
    }
}
