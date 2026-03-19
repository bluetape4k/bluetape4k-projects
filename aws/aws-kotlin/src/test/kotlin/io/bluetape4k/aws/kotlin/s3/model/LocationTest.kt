package io.bluetape4k.aws.kotlin.s3.model

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class LocationTest {

    companion object: KLoggingChannel()

    @Test
    fun `parse location`() {
        val locationUrl = "s3://bucket/key^version"

        val location = Location(locationUrl)
        log.debug { "Location: $location" }
    }

    @Test
    fun `equals two location`() {
        val locationUrl1 = "s3://bucket/key^version1"
        val locationUrl2 = "s3://bucket/key^version2"

        val location1 = Location(locationUrl1)
        val location2 = Location(locationUrl2)
        val location3 = Location(locationUrl1)

        location1 shouldNotBeEqualTo location2
        location1 shouldBeEqualTo location3

    }

    @Test
    fun `url property`() {
        val locationUrl = "s3://bucket/key^version"

        val location = Location(locationUrl)

        location.url shouldBeEqualTo locationUrl
    }
}
