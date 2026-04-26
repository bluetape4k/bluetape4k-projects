package io.bluetape4k.nats.client

import io.bluetape4k.nats.client.api.keyValueConfiguration
import io.bluetape4k.nats.client.api.streamConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamManagement
import io.nats.client.KeyValueManagement
import io.nats.client.ObjectStoreManagement
import io.nats.client.api.KeyValueStatus
import io.nats.client.api.StreamConfiguration
import io.nats.client.api.StreamInfo
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException

class JetStreamManagementExtensionsTest {
    private val jetStreamManagement = mockk<JetStreamManagement>()

    @Test
    fun `forcedDeleteStream returns false when stream is missing`() {
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        every { jetStreamManagement.deleteStream("orders") } throws notFound

        val deleted = jetStreamManagement.forcedDeleteStream("orders")

        deleted.shouldBeFalse()
    }

    @Test
    fun `forcedDeleteStream rethrows unexpected exception`() {
        val unexpected = mockJetStreamException(99999)
        every { jetStreamManagement.deleteStream("orders") } throws unexpected

        val thrown =
            assertThrows(JetStreamApiException::class.java) {
                jetStreamManagement.forcedDeleteStream("orders")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `forcedDeleteStream rethrows io exception`() {
        val unexpected = IOException("network down")
        every { jetStreamManagement.deleteStream("orders") } throws unexpected

        val thrown =
            assertThrows(IOException::class.java) {
                jetStreamManagement.forcedDeleteStream("orders")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `forcedDeleteConsumer returns false when consumer is missing`() {
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        every { jetStreamManagement.deleteConsumer("orders", "consumer-a") } throws notFound

        val deleted = jetStreamManagement.forcedDeleteConsumer("orders", "consumer-a")

        deleted.shouldBeFalse()
    }

    @Test
    fun `forcedDeleteConsumer rethrows io exception`() {
        val unexpected = IOException("socket closed")
        every { jetStreamManagement.deleteConsumer("orders", "consumer-a") } throws unexpected

        val thrown =
            assertThrows(IOException::class.java) {
                jetStreamManagement.forcedDeleteConsumer("orders", "consumer-a")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `forcedPurgeStream returns null when stream is missing`() {
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        every { jetStreamManagement.purgeStream("orders") } throws notFound

        val purged = jetStreamManagement.forcedPurgeStream("orders")

        purged.shouldBeNull()
    }

    @Test
    fun `forcedPurgeStream rethrows io exception`() {
        val unexpected = IOException("io timeout")
        every { jetStreamManagement.purgeStream("orders") } throws unexpected

        val thrown =
            assertThrows(IOException::class.java) {
                jetStreamManagement.forcedPurgeStream("orders")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `createOrReplaceStream rethrows delete failure that is not not-found`() {
        val unexpected = mockJetStreamException(99999)
        every { jetStreamManagement.deleteStream("orders") } throws unexpected

        val thrown =
            assertThrows(JetStreamApiException::class.java) {
                jetStreamManagement.createOrReplaceStream("orders", subjects = arrayOf("orders.created"))
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `createOrReplaceStream rethrows io exception from delete`() {
        val unexpected = IOException("broken pipe")
        every { jetStreamManagement.deleteStream("orders") } throws unexpected

        val thrown =
            assertThrows(IOException::class.java) {
                jetStreamManagement.createOrReplaceStream("orders", subjects = arrayOf("orders.created"))
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `tryPurgeStream creates stream when purge reports missing stream`() {
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        val created = mockk<StreamInfo>()
        val createdConfig =
            streamConfiguration("orders") {
                subjects("orders.created")
            }
        every { jetStreamManagement.purgeStream("orders") } throws notFound
        every { jetStreamManagement.addStream(createdConfig) } returns created

        val result = jetStreamManagement.tryPurgeStream("orders") { createdConfig }

        result.shouldBeNull()
        verify(exactly = 1) { jetStreamManagement.addStream(createdConfig) }
    }

    @Test
    fun `createStreamOrUpdateSubjects appends only missing subjects in order`() {
        val info = mockk<StreamInfo>()
        val updated = mockk<StreamInfo>()
        val streamConfigSlot = slot<StreamConfiguration>()
        val existingConfig =
            streamConfiguration("orders") {
                subjects("orders.created", "orders.updated")
            }

        every { jetStreamManagement.getStreamInfo("orders") } returns info
        every { info.configuration } returns existingConfig
        every { jetStreamManagement.updateStream(capture(streamConfigSlot)) } returns updated

        val result =
            jetStreamManagement.createStreamOrUpdateSubjects(
                "orders",
                subjects = arrayOf("orders.updated", "orders.shipped", "orders.created")
            )

        result shouldBeEqualTo updated
        streamConfigSlot.captured.subjects shouldBeEqualTo
                listOf("orders.created", "orders.updated", "orders.shipped")
    }

    @Test
    fun `createStreamOrUpdateSubjects skips update when all subjects already exist`() {
        val info = mockk<StreamInfo>()
        val existingConfig =
            streamConfiguration("orders") {
                subjects("orders.created", "orders.updated")
            }

        every { jetStreamManagement.getStreamInfo("orders") } returns info
        every { info.configuration } returns existingConfig

        val result =
            jetStreamManagement.createStreamOrUpdateSubjects(
                "orders",
                subjects = arrayOf("orders.updated")
            )

        result shouldBeEqualTo info
        verify(exactly = 0) { jetStreamManagement.updateStream(any()) }
    }
}

class KeyValueManagementExtensionsTest {
    private val keyValueManagement = mockk<KeyValueManagement>()

    @Test
    fun `createOrUpdate updates existing bucket first`() {
        val config = keyValueConfiguration("events") {}
        val status = mockk<KeyValueStatus>()
        every { keyValueManagement.update(config) } returns status

        val result = keyValueManagement.createOrUpdate(config)

        result shouldBeEqualTo status
        verify(exactly = 0) { keyValueManagement.create(config) }
    }

    @Test
    fun `createOrUpdate creates bucket when update reports missing bucket`() {
        val config = keyValueConfiguration("events") {}
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        val created = mockk<KeyValueStatus>()
        every { keyValueManagement.update(config) } throws notFound
        every { keyValueManagement.create(config) } returns created

        val result = keyValueManagement.createOrUpdate(config)

        result shouldBeEqualTo created
    }

    @Test
    fun `forcedDelete rethrows unexpected exception`() {
        val unexpected = mockJetStreamException(99999)
        every { keyValueManagement.delete("events") } throws unexpected

        val thrown =
            assertThrows(JetStreamApiException::class.java) {
                keyValueManagement.forcedDelete("events")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `forcedDelete rethrows io exception`() {
        val unexpected = IOException("io failure")
        every { keyValueManagement.delete("events") } throws unexpected

        val thrown =
            assertThrows(IOException::class.java) {
                keyValueManagement.forcedDelete("events")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `getStatusOrNull returns null when bucket is missing`() {
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        every { keyValueManagement.getStatus("events") } throws notFound

        val result = keyValueManagement.getStatusOrNull("events")

        result.shouldBeNull()
    }
}

class ObjectStreamManagementExtensionsTest {
    private val objectStoreManagement = mockk<ObjectStoreManagement>()

    @Test
    fun `tryDelete ignores not-found exception`() {
        val notFound = mockJetStreamException(JET_STREAM_NOT_FOUND)
        every { objectStoreManagement.delete("artifacts") } throws notFound

        objectStoreManagement.tryDelete("artifacts")
    }

    @Test
    fun `tryDelete rethrows unexpected exception`() {
        val unexpected = mockJetStreamException(99999)
        every { objectStoreManagement.delete("artifacts") } throws unexpected

        val thrown =
            assertThrows(JetStreamApiException::class.java) {
                objectStoreManagement.tryDelete("artifacts")
            }

        thrown shouldBeEqualTo unexpected
    }

    @Test
    fun `tryDelete rethrows io exception`() {
        val unexpected = IOException("connection reset")
        every { objectStoreManagement.delete("artifacts") } throws unexpected

        val thrown =
            assertThrows(IOException::class.java) {
                objectStoreManagement.tryDelete("artifacts")
            }

        thrown shouldBeEqualTo unexpected
    }
}

private fun mockJetStreamException(apiErrorCode: Int): JetStreamApiException =
    mockk {
        every { this@mockk.apiErrorCode } returns apiErrorCode
    }
