package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

class JsonEncryptModule: SimpleModule() {

    companion object: KLogging()

    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonEncryptModule ..." }
        context.insertAnnotationIntrospector(JsonEncryptAnnotationInterospector())
    }
}
