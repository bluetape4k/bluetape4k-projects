package io.bluetape4k.images

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class IIORegistryUtilsTest {

    companion object: KLogging()

    @Test
    fun `get reader image format names`() {
        val readImageFormatNames = IIORegistryUtils.imageReaderFormatNames
        readImageFormatNames.shouldNotBeEmpty()
        readImageFormatNames.sorted().forEach {
            log.debug { "read image format: $it" }
        }
    }

    @Test
    fun `get writer image format names`() {
        val writeImageFormatNames = IIORegistryUtils.imageWriterFormatNames
        writeImageFormatNames.shouldNotBeEmpty()
        writeImageFormatNames.sorted().forEach {
            log.debug { "writer image format: $it" }
        }
    }
}
