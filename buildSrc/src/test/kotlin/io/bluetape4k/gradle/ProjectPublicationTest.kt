package io.bluetape4k.gradle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectPublicationTest {

    @Test
    fun `library modules are publishable`() {
        assertTrue(isPublishableLibraryProject("bluetape4k/core", "bluetape4k-core"))
        assertTrue(isPublishableLibraryProject("spring-boot/core", "bluetape4k-spring-boot-core"))
        assertTrue(isPublishableLibraryProject("virtualthread/jdk21", "bluetape4k-virtualthread-jdk21"))
    }

    @Test
    fun `BOM is published without being treated as a library module`() {
        assertFalse(isPublishableLibraryProject("bluetape4k/bom", "bluetape4k-bom"))
        assertTrue(isPublishedProject("bluetape4k/bom", "bluetape4k-bom"))
    }

    @Test
    fun `published project classification includes libraries and excludes applications`() {
        assertTrue(isPublishedProject("bluetape4k/core", "bluetape4k-core"))
        assertFalse(isPublishedProject("workshop/kafka", "kafka-workshop"))
        assertFalse(isPublishedProject("spring-boot/mock-web-server", "bluetape4k-mock-web-server"))
    }

    @Test
    fun `sample and benchmark modules are not publishable`() {
        assertFalse(isPublishableLibraryProject("workshop/kafka", "kafka-workshop"))
        assertFalse(isPublishableLibraryProject("examples/cache", "cache-example"))
        assertFalse(isPublishableLibraryProject("spring-boot/demo", "spring-demo-app"))
        assertFalse(isPublishableLibraryProject("benchmark/protobuf", "protobuf-codec-benchmark"))
    }

    @Test
    fun `application-only mock servers are not publishable`() {
        assertFalse(isPublishableLibraryProject("spring-boot/mock-web-server", "bluetape4k-mock-web-server"))
        assertFalse(isPublishableLibraryProject("spring-boot/mock-webflux-server", "bluetape4k-mock-webflux-server"))
    }
}
