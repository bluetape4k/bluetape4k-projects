package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.buffer
import okio.source
import org.junit.jupiter.api.Test
import java.io.File

class ReadJavaIoFileLineByLine: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `read file line by line`() {
        val file = File("../../README.md")
        file.source().use { source ->
            source.buffer().use { buffered ->
                buffered.readUtf8Lines()
                    .forEach {
                        log.debug { "Line: $it" }
                    }
            }
        }
    }
}
