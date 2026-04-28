package io.bluetape4k.images.vips.testfixtures

/** Known dimensions for test fixtures. */
object VipsTestFixtures {

    /** JPEG fixture: 800 × 600, gradient blue→red */
    const val SAMPLE_JPEG = "fixtures/sample.jpg"
    const val SAMPLE_JPEG_WIDTH = 800
    const val SAMPLE_JPEG_HEIGHT = 600

    /** PNG fixture: 640 × 480, gradient green→yellow */
    const val SAMPLE_PNG = "fixtures/sample.png"
    const val SAMPLE_PNG_WIDTH = 640
    const val SAMPLE_PNG_HEIGHT = 480

    /** WebP fixture: 400 × 300, gradient orange→purple */
    const val SAMPLE_WEBP = "fixtures/sample.webp"
    const val SAMPLE_WEBP_WIDTH = 400
    const val SAMPLE_WEBP_HEIGHT = 300

    /**
     * Loads a test fixture from classpath resources.
     *
     * @param resourcePath classpath-relative path (e.g., "fixtures/sample.jpg")
     * @return raw bytes of the resource
     * @throws IllegalArgumentException if the resource is not found
     */
    fun loadFixture(resourcePath: String): ByteArray {
        val stream = VipsTestFixtures::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: error("Test fixture not found on classpath: $resourcePath")
        return stream.use { it.readBytes() }
    }
}
