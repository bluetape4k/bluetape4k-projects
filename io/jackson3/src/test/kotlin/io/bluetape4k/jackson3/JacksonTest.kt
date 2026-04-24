package io.bluetape4k.jackson3

import io.bluetape4k.jackson3.uuid.JsonUuidModule
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class JacksonTest {

    companion object: KLogging()

    @Test
    fun `classpath에 있는 모듈을 자동으로 등록하기`() {
        val mapper = Jackson.defaultJsonMapper

        mapper.registeredModuleIds().forEach { moduleId ->
            println(moduleId)
        }
        mapper.registeredModules().size shouldBeGreaterThan 0

        // classpath 에 있는 JsonUuidModule 을 자동으로 등록했다
        mapper.registeredModuleIds() shouldContain JsonUuidModule::class.qualifiedName
    }

    @Test
    fun `defaultJsonMapper는 lazy singleton이다`() {
        val mapper1 = Jackson.defaultJsonMapper
        val mapper2 = Jackson.defaultJsonMapper
        (mapper1 === mapper2).shouldBeTrue()
    }

    @Test
    fun `createDefaultJsonMapper는 호출 시마다 새 인스턴스를 반환한다`() {
        val mapper1 = Jackson.createDefaultJsonMapper()
        val mapper2 = Jackson.createDefaultJsonMapper()
        (mapper1 !== mapper2).shouldBeTrue()
    }

    @Test
    fun `createDefaultJsonMapper로 생성한 매퍼는 기본 직렬화가 동작한다`() {
        val mapper = Jackson.createDefaultJsonMapper()
        mapper.shouldNotBeNull()
        data class Sample(val name: String, val value: Int)
        val json = mapper.writeValueAsString(Sample("test", 42))
        json.shouldNotBeNull()
        (json.contains("name")).shouldBeTrue()
        (json.contains("test")).shouldBeTrue()
    }

    @Test
    fun `registeredModuleNames - 등록된 모듈 이름 목록 반환`() {
        val names = Jackson.defaultJsonMapper.registeredModuleNames()
        names.shouldNotBeNull()
        (names.isNotEmpty()).shouldBeTrue()
    }

    @Test
    fun `registeredModuleIds - 등록된 모듈 ID 목록 반환`() {
        val ids = Jackson.defaultJsonMapper.registeredModuleIds()
        ids.shouldNotBeNull()
        (ids.isNotEmpty()).shouldBeTrue()
    }
}
