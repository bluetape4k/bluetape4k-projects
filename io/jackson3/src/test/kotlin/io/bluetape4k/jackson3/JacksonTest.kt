package io.bluetape4k.jackson3

import io.bluetape4k.jackson3.uuid.JsonUuidModule
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JacksonTest {

    companion object: KLogging()

    private val JsonMapper.registeredModuleIds get() = registeredModules().map { it.registrationId }

    @Test
    fun `classpath에 있는 모듈을 자동으로 등록하기`() {
        val mapper = Jackson.defaultJsonMapper

        mapper.registeredModuleIds.forEach { moduleId ->
            println(moduleId)
        }
        mapper.registeredModules().size shouldBeGreaterThan 0

        // classpath 에 있는 JsonUuidModule 을 자동으로 등록했다
        mapper.registeredModuleIds shouldContain JsonUuidModule::class.qualifiedName
    }
}
