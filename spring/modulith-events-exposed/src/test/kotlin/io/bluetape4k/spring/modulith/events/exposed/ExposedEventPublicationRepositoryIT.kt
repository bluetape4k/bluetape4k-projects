package io.bluetape4k.spring.modulith.events.exposed

import com.ninjasquad.springmockk.MockkBean
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.modulith.events.exposed.schema.ArchivedExposedEventPublicationTable
import io.bluetape4k.spring.modulith.events.exposed.schema.DefaultExposedEventPublicationTable
import io.bluetape4k.support.uninitialized
import io.mockk.every
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.modulith.events.core.PublicationTargetIdentifier
import org.springframework.modulith.events.core.TargetEventPublication
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

class ExposedEventPublicationRepositoryIT {

    companion object: KLogging() {
        @JvmStatic
        val TARGET_IDENTIFIER: PublicationTargetIdentifier = PublicationTargetIdentifier.of("listener")
    }

    @Import(TestApplication::class)
    @ContextConfiguration(classes = [ExposedEventPublicationAutoConfiguration::class])
    abstract class TestBase {

        @Autowired
        private lateinit var exposedConfigurationProperties: ExposedConfigurationProperties

        @Autowired
        private val repository: ExposedEventPublicationRepository = uninitialized()

        @Autowired
        private val properties: ExposedRepositorySettings = uninitialized()

        @MockkBean
        private val serializer: EventSerializer = uninitialized()

        @AfterEach
        @BeforeEach
        fun cleanUp() {
            transaction {
                exec("TRUNCATE TABLE $tableName")

                if (properties.isArchiveCompletion) {
                    exec("TRUNCATE TABLE $archiveTableName")
                }
            }
        }

        val tableName: String get() = DefaultExposedEventPublicationTable.tableName
        val archiveTableName: String get() = ArchivedExposedEventPublicationTable.tableName

        @Test
        fun `persist and update event publication`() {
            val testEvent = TestEvent("id")
            val serializedEvent = "{\"eventId\":\"id\"}"

            every { serializer.serialize(testEvent) } returns serializedEvent
            every { serializer.deserialize(serializedEvent, TestEvent::class.java) } returns testEvent

            val publication = repository.create(TargetEventPublication.of(testEvent, TARGET_IDENTIFIER))
            val eventPublications = repository.findIncompletePublications()

            eventPublications shouldHaveSize 1
            val eventPublication = eventPublications.first()!!
            eventPublication.event shouldBeEqualTo publication.event
            eventPublication.targetIdentifier shouldBeEqualTo publication.targetIdentifier

            repository.findIncompletePublicationsByEventAndTargetIdentifier(
                testEvent,
                TARGET_IDENTIFIER
            ).isPresent.shouldBeTrue()

            // Complete publication
            repository.markCompleted(publication, Instant.now())

            repository.findIncompletePublications().shouldBeEmpty()
        }
    }


    class H2Test: TestBase() {

    }

    data class TestEvent(val eventId: String)

    class Sample {}
}
