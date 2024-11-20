package io.bluetape4k.aws.ses.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DestinationSupportTest {

    companion object: KLogging()

    @Test
    fun `build destination`() {
        val destination = destinationOf(
            toAddresses = listOf("user1@example.com", "user2@example.com"),
            ccAddresses = listOf("cc1@example.com"),
            bccAddresses = listOf("bcc1@example.com")
        )
        destination.toAddresses() shouldBeEqualTo listOf("user1@example.com", "user2@example.com")
        destination.ccAddresses() shouldBeEqualTo listOf("cc1@example.com")
        destination.bccAddresses() shouldBeEqualTo listOf("bcc1@example.com")
    }
}
