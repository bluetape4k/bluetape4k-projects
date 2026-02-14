package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * 노드 값을 암호화하여 직렬화하고, 복호화하는 기능을 제공하는 모듈입니다.
 *
 * @see JsonEncrypt
 * @see JsonEncryptSerializer
 * @see JsonEncryptDeserializer
 * @see JsonEncryptAnnotationInterospector
 */
class JsonEncryptModule: SimpleModule() {

    companion object: KLogging()

    /**
     * Jackson JSON 처리에서 `setupModule` 함수를 제공합니다.
     */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonEncryptModule ..." }
        context.insertAnnotationIntrospector(JsonEncryptAnnotationInterospector())
    }
}
